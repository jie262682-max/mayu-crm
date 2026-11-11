/*
 * 码域CRM（MaoyuCRM）— CRM + OA + EAM 一体化系统
 * Copyright (c) 2026 合肥市码域科技 (Maoyu Technology, Hefei)
 *
 * 本程序是自由软件：您可以根据自由软件基金会发布的 GNU Affero 通用公共许可证
 * （第 3 版或任意后续版本）的条款重新分发和/或修改它。
 * 传播本程序时须保留本版权声明；商业闭源使用请联系作者获取商业授权。
 * 详见仓库根目录 LICENSE（AGPL-3.0）。
 */
package com.freecrm.oa.service;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.freecrm.common.exception.BizException;
import com.freecrm.common.flow.FlowCallback;
import com.freecrm.oa.entity.OaFlowDef;
import com.freecrm.oa.entity.OaFlowInstance;
import com.freecrm.oa.entity.OaFlowTask;
import com.freecrm.oa.mapper.OaFlowDefMapper;
import com.freecrm.oa.mapper.OaFlowInstanceMapper;
import com.freecrm.oa.mapper.OaFlowTaskMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.freecrm.system.service.SmsService;
import com.freecrm.system.mapper.SysUserMapper;
import com.freecrm.system.entity.SysUser;
import java.util.stream.Collectors;

/**
 * 审批流引擎 v2：
 * - APPROVAL 节点：signType=ANY（或签，任一通过）| ALL（会签，全部通过；多人同时收待办即并行）
 * - CONDITION 节点：无审批人，按业务变量路由跳转（goto 指向节点 level）
 * - 兼容 v1 nodes：缺省 signType=ANY
 * <p>
 * nodes 示例：
 * [
 *   {"level":1,"name":"销售经理","signType":"ANY","approverUserIds":[2]},
 *   {"level":2,"type":"CONDITION","name":"金额分支","branches":[
 *       {"expr":"amount >= 50000","goto":3},{"default":true,"goto":4}]},
 *   {"level":3,"name":"总监会签","signType":"ALL","approverUserIds":[2,8]},
 *   {"level":4,"name":"总经理","signType":"ANY","approverUserIds":[1]}
 * ]
 */
@Service
public class FlowEngine {

    public static final int RUNNING = 0;
    public static final int PASSED = 1;
    public static final int REJECTED = 2;
    public static final int WITHDRAWN = 3;

    private static final Pattern COND_PATTERN = Pattern.compile("\\s*(\\w+)\\s*(>=|<=|==|!=|>|<)\\s*(-?\\d+(?:\\.\\d+)?)\\s*");

    private final OaFlowDefMapper defMapper;
    private final OaFlowInstanceMapper instMapper;
    private final OaFlowTaskMapper taskMapper;
    private final com.freecrm.oa.mapper.OaLeaveMapper leaveMapper;
    private final com.freecrm.system.service.NoticeService noticeService;
    private final SysUserMapper userMapper;
    private final SmsService smsService;
    private final com.freecrm.oa.mapper.OaBizMapper bizMapper;
    /** bizType -> callback */
    private final Map<Integer, FlowCallback> callbacks;

    public FlowEngine(OaFlowDefMapper defMapper, OaFlowInstanceMapper instMapper,
                      OaFlowTaskMapper taskMapper, List<FlowCallback> callbackList,
                      com.freecrm.oa.mapper.OaLeaveMapper leaveMapper,
                      com.freecrm.system.service.NoticeService noticeService,
                      SysUserMapper userMapper, SmsService smsService,
                      com.freecrm.oa.mapper.OaBizMapper bizMapper) {
        this.defMapper = defMapper;
        this.instMapper = instMapper;
        this.taskMapper = taskMapper;
        this.leaveMapper = leaveMapper;
        this.noticeService = noticeService;
        this.userMapper = userMapper;
        this.smsService = smsService;
        this.bizMapper = bizMapper;
        this.callbacks = callbackList.stream()
                .collect(Collectors.toMap(FlowCallback::bizType, c -> c));
    }

    /** 是否存在启用中的流程定义 */
    public boolean hasActiveFlow(int bizType) {
        return defMapper.selectCount(new LambdaQueryWrapper<OaFlowDef>()
                .eq(OaFlowDef::getBizType, bizType)
                .eq(OaFlowDef::getStatus, 1)) > 0;
    }

    /** 发起流程（无业务变量，条件节点一律走 default 分支） */
    public long start(int bizType, long bizId, String title, long applicantUserId) {
        return start(bizType, bizId, title, applicantUserId, new HashMap<>());
    }

    /** 发起流程：vars 供条件节点求值（如 amount=合同金额、days=请假天数） */
    @Transactional(rollbackFor = Exception.class)
    public long start(int bizType, long bizId, String title, long applicantUserId, Map<String, Object> vars) {
        OaFlowDef def = defMapper.selectOne(new LambdaQueryWrapper<OaFlowDef>()
                .eq(OaFlowDef::getBizType, bizType).eq(OaFlowDef::getStatus, 1));
        if (def == null) {
            throw new BizException("该业务未配置审批流程");
        }
        Long exists = instMapper.selectCount(new LambdaQueryWrapper<OaFlowInstance>()
                .eq(OaFlowInstance::getBizType, bizType).eq(OaFlowInstance::getBizId, bizId));
        if (exists != null && exists > 0) {
            throw new BizException("该业务已发起过审批");
        }
        JSONArray nodes = JSONUtil.parseArray(def.getNodes());
        if (nodes.isEmpty()) {
            throw new BizException("流程定义没有节点");
        }
        OaFlowInstance inst = new OaFlowInstance();
        inst.setFlowDefId(def.getId());
        inst.setBizType(bizType);
        inst.setBizId(bizId);
        inst.setBizTitle(title);
        inst.setApplicantUserId(applicantUserId);
        inst.setCurrentLevel(0);
        inst.setStatus(RUNNING);
        instMapper.insert(inst);
        advanceTo(inst, nodes, 1, vars);
        return inst.getId();
    }

    /** 审批一个任务：或签任一通过即过级；会签需全部通过 */
    @Transactional(rollbackFor = Exception.class)
    public void audit(long taskId, boolean pass, String opinion) {
        OaFlowTask task = taskMapper.selectById(taskId);
        if (task == null || task.getStatus() != 0) {
            throw new BizException("任务不存在或已处理");
        }
        OaFlowInstance inst = instMapper.selectById(task.getInstanceId());
        if (inst == null || inst.getStatus() != RUNNING) {
            throw new BizException("流程已结束");
        }
        if (task.getNodeLevel() != inst.getCurrentLevel()) {
            throw new BizException("非当前节点任务");
        }
        // 记录本任务
        OaFlowTask tUpd = new OaFlowTask();
        tUpd.setId(taskId);
        tUpd.setStatus(pass ? 1 : 2);
        tUpd.setOpinion(opinion);
        tUpd.setAuditTime(LocalDateTime.now());
        taskMapper.updateById(tUpd);

        OaFlowDef def = defMapper.selectById(inst.getFlowDefId());
        if (def == null) {
            throw new BizException("流程定义不存在");
        }
        JSONArray nodes = JSONUtil.parseArray(def.getNodes());
        JSONObject node = nodeOf(nodes, inst.getCurrentLevel());
        String signType = node == null || node.getStr("signType") == null ? "ANY" : node.getStr("signType");

        if (!pass) {
            // 驳回：关闭同节点其余任务，流程结束
            closeOthers(inst.getId(), inst.getCurrentLevel(), "审批人已驳回");
            finish(inst, REJECTED);
            return;
        }
        boolean all = "ALL".equalsIgnoreCase(signType);
        if (all) {
            long remaining = taskMapper.selectCount(new LambdaQueryWrapper<OaFlowTask>()
                    .eq(OaFlowTask::getInstanceId, inst.getId())
                    .eq(OaFlowTask::getNodeLevel, inst.getCurrentLevel())
                    .eq(OaFlowTask::getStatus, 0));
            if (remaining > 0) {
                return; // 会签：等其他审批人
            }
        } else {
            closeOthers(inst.getId(), inst.getCurrentLevel(), "同节点其他人已通过");
        }
        advanceTo(inst, nodes, inst.getCurrentLevel() + 1, instVars(inst));
    }

    /** 申请人撤回 */
    @Transactional(rollbackFor = Exception.class)
    public void withdraw(long instanceId, long operatorUserId) {
        OaFlowInstance inst = instMapper.selectById(instanceId);
        if (inst == null || inst.getStatus() != RUNNING) {
            throw new BizException("流程不可撤回");
        }
        if (!inst.getApplicantUserId().equals(operatorUserId)) {
            throw new BizException("只有申请人可撤回");
        }
        finish(inst, WITHDRAWN);
    }

    /** 我的待办 */
    public List<OaFlowTask> myTodo(long userId) {
        return taskMapper.selectList(new LambdaQueryWrapper<OaFlowTask>()
                .eq(OaFlowTask::getApproverUserId, userId)
                .eq(OaFlowTask::getStatus, 0)
                .orderByDesc(OaFlowTask::getId));
    }

    /** 业务的实例 */
    public OaFlowInstance getInstance(int bizType, long bizId) {
        return instMapper.selectOne(new LambdaQueryWrapper<OaFlowInstance>()
                .eq(OaFlowInstance::getBizType, bizType).eq(OaFlowInstance::getBizId, bizId));
    }

    public List<OaFlowTask> tasksOf(long instanceId) {
        return taskMapper.selectList(new LambdaQueryWrapper<OaFlowTask>()
                .eq(OaFlowTask::getInstanceId, instanceId)
                .orderByAsc(OaFlowTask::getNodeLevel));
    }

    // ---------- 内部 ----------

    /** 推进到目标 level：CONDITION 节点沿路由跳转；APPROVAL 建任务；越界则完成 */
    private void advanceTo(OaFlowInstance inst, JSONArray nodes, int level, Map<String, Object> vars) {
        int guard = 0;
        int target = level;
        while (true) {
            if (guard++ > 50) {
                throw new BizException("流程条件路由出现死循环");
            }
            if (target > nodes.size()) {
                finish(inst, PASSED);
                return;
            }
            JSONObject node = nodeOf(nodes, target);
            if (node == null) {
                target++;
                continue;
            }
            if ("CONDITION".equalsIgnoreCase(node.getStr("type", ""))) {
                Integer next = routeOf(node, vars);
                target = next == null ? target + 1 : next;
                continue;
            }
            // APPROVAL：记录当前节点并建任务
            OaFlowInstance iUpd = new OaFlowInstance();
            iUpd.setId(inst.getId());
            iUpd.setCurrentLevel(target);
            instMapper.updateById(iUpd);
            createTasks(inst, node);
            return;
        }
    }

    /** 条件路由：返回应跳转的 level；无命中走 default，无 default 则顺序下一节点 */
    private Integer routeOf(JSONObject condNode, Map<String, Object> vars) {
        JSONArray branches = condNode.getJSONArray("branches");
        Integer fallback = null;
        if (branches != null) {
            for (Object o : branches) {
                JSONObject b = (JSONObject) o;
                if (b.containsKey("default") && b.getBool("default", false)) {
                    fallback = b.getInt("goto");
                    continue;
                }
                String expr = b.getStr("expr");
                if (expr != null && evalExpr(expr, vars)) {
                    return b.getInt("goto");
                }
            }
        }
        return fallback;
    }

    /** 简单表达式求值：`a >= 100 && b < 3`（数值比较，支持 >= <= == != > <） */
    static boolean evalExpr(String expr, Map<String, Object> vars) {
        for (String part : expr.split("&&")) {
            Matcher m = COND_PATTERN.matcher(part.trim());
            if (!m.matches()) {
                return false;
            }
            double left;
            Object v = vars.get(m.group(1));
            try {
                left = v == null ? 0 : Double.parseDouble(String.valueOf(v));
            } catch (NumberFormatException e) {
                return false;
            }
            double right = Double.parseDouble(m.group(3));
            boolean ok;
            switch (m.group(2)) {
                case ">=": ok = left >= right; break;
                case "<=": ok = left <= right; break;
                case "==": ok = left == right; break;
                case "!=": ok = left != right; break;
                case ">":  ok = left > right; break;
                case "<":  ok = left < right; break;
                default: ok = false;
            }
            if (!ok) {
                return false;
            }
        }
        return true;
    }

    /** 实例变量（从业务单反查：合同/回款金额、请假天数）——供审批中途条件节点使用 */
    private Map<String, Object> instVars(OaFlowInstance inst) {
        Map<String, Object> vars = new HashMap<>();
        try {
            if (inst.getBizType() == null) {
                return vars;
            }
            if (inst.getBizType() == 1) {
                // 合同：amount = 合同总金额
                java.math.BigDecimal amt = bizMapper.contractAmount(inst.getBizId());
                vars.put("amount", amt == null ? 0 : amt.doubleValue());
            } else if (inst.getBizType() == 2) {
                // 回款：amount = 回款金额，contractAmount = 合同总额
                java.math.BigDecimal amt = bizMapper.paymentAmount(inst.getBizId());
                vars.put("amount", amt == null ? 0 : amt.doubleValue());
                Long contractId = bizMapper.contractIdOfPayment(inst.getBizId());
                if (contractId != null) {
                    java.math.BigDecimal total = bizMapper.contractAmount(contractId);
                    vars.put("contractAmount", total == null ? 0 : total.doubleValue());
                }
            } else if (inst.getBizType() == 3) {
                com.freecrm.oa.entity.OaLeave leave = leaveMapper.selectById(inst.getBizId());
                if (leave != null) {
                    vars.put("days", leave.getDays());
                }
            }
        } catch (Exception ignore) { }
        return vars;
    }

    private JSONObject nodeOf(JSONArray nodes, int level) {
        for (Object o : nodes) {
            JSONObject n = (JSONObject) o;
            if (n.getInt("level", 0) == level) {
                return n;
            }
        }
        return null;
    }

    private void closeOthers(long instanceId, int level, String note) {
        taskMapper.selectList(new LambdaQueryWrapper<OaFlowTask>()
                        .eq(OaFlowTask::getInstanceId, instanceId)
                        .eq(OaFlowTask::getNodeLevel, level)
                        .eq(OaFlowTask::getStatus, 0))
                .forEach(other -> {
                    OaFlowTask o = new OaFlowTask();
                    o.setId(other.getId());
                    o.setStatus(1);
                    o.setOpinion(note);
                    o.setAuditTime(LocalDateTime.now());
                    taskMapper.updateById(o);
                });
    }

    private void createTasks(OaFlowInstance inst, JSONObject node) {
        JSONArray approvers = node.getJSONArray("approverUserIds");
        if (approvers == null || approvers.isEmpty()) {
            throw new BizException("节点【" + node.getStr("name", "#" + node.getInt("level")) + "】没有审批人");
        }
        for (Object uid : approvers) {
            OaFlowTask t = new OaFlowTask();
            t.setInstanceId(inst.getId());
            t.setNodeLevel(node.getInt("level"));
            t.setApproverUserId(Long.valueOf(uid.toString()));
            t.setStatus(0);
            taskMapper.insert(t);
            try {
                String nodeName = node.getStr("name");
                noticeService.send(Long.valueOf(uid.toString()), "您有新的审批待办",
                        (nodeName == null ? "" : "【" + nodeName + "】") + inst.getBizTitle(), 1,
                        inst.getBizType(), inst.getBizId());
                smsNotify(Long.valueOf(uid.toString()), inst.getBizTitle());
            } catch (Exception ignore) { /* 通知失败不影响流程 */ }
        }
    }

    /** 审批待办短信通知（sms.notify.todo=1 且已配置阿里云时生效） */
    private void smsNotify(long approverUserId, String title) {
        try {
            if (!"1".equals(smsService.configView().getOrDefault("sms.notify.todo", "1"))) {
                return;
            }
            if (!smsService.enabled()) {
                return;
            }
            SysUser u = userMapper.selectById(approverUserId);
            if (u != null && u.getMobile() != null && !u.getMobile().isEmpty()) {
                smsService.send(u.getMobile(), java.util.Collections.singletonMap(smsService.varName(), title));
            }
        } catch (Exception ignore) { /* 短信失败不影响流程 */ }
    }

    private void finish(OaFlowInstance inst, int status) {
        OaFlowInstance iUpd = new OaFlowInstance();
        iUpd.setId(inst.getId());
        iUpd.setStatus(status);
        instMapper.updateById(iUpd);
        try {
            String result = status == PASSED ? "已通过" : (status == REJECTED ? "已驳回" : "已撤回");
            noticeService.send(inst.getApplicantUserId(), "审批结果：" + result,
                    inst.getBizTitle() + " —— " + result, 2, inst.getBizType(), inst.getBizId());
        } catch (Exception ignore) { /* 通知失败不影响流程 */ }
        FlowCallback cb = callbacks.get(inst.getBizType());
        if (cb != null) {
            if (status == PASSED) {
                cb.onPassed(inst.getBizId());
            } else if (status == REJECTED) {
                cb.onRejected(inst.getBizId());
            }
        }
    }
}
