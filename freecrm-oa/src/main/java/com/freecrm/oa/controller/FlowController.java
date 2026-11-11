/*
 * 码域CRM（MaoyuCRM）— CRM + OA + EAM 一体化系统
 * Copyright (c) 2026 合肥市码域科技 (Maoyu Technology, Hefei)
 *
 * 本程序是自由软件：您可以根据自由软件基金会发布的 GNU Affero 通用公共许可证
 * （第 3 版或任意后续版本）的条款重新分发和/或修改它。
 * 传播本程序时须保留本版权声明；商业闭源使用请联系作者获取商业授权。
 * 详见仓库根目录 LICENSE（AGPL-3.0）。
 */
package com.freecrm.oa.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.freecrm.common.exception.BizException;
import com.freecrm.common.result.R;
import com.freecrm.oa.entity.OaFlowDef;
import com.freecrm.oa.entity.OaFlowInstance;
import com.freecrm.oa.entity.OaFlowTask;
import com.freecrm.oa.mapper.OaFlowDefMapper;
import com.freecrm.oa.mapper.OaFlowInstanceMapper;
import com.freecrm.oa.mapper.OaFlowTaskMapper;
import com.freecrm.oa.service.FlowEngine;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 审批流：定义 / 待办 / 审批 / 撤回
 */
@RestController
@RequestMapping("/api/oa/flow")
public class FlowController {

    private final FlowEngine flowEngine;
    private final OaFlowDefMapper defMapper;
    private final OaFlowInstanceMapper instMapper;
    private final OaFlowTaskMapper taskMapper;

    public FlowController(FlowEngine flowEngine, OaFlowDefMapper defMapper,
                          OaFlowInstanceMapper instMapper, OaFlowTaskMapper taskMapper) {
        this.flowEngine = flowEngine;
        this.defMapper = defMapper;
        this.instMapper = instMapper;
        this.taskMapper = taskMapper;
    }

    // ---- 流程定义 ----

    @GetMapping("/def/list")
    public R<List<OaFlowDef>> defList() {
        return R.ok(defMapper.selectList(new LambdaQueryWrapper<OaFlowDef>().orderByDesc(OaFlowDef::getId)));
    }

    @PostMapping("/def/save")
    @SaCheckPermission("oa:flow:save")
    @com.freecrm.common.log.OpLog(module = "流程定义", action = "保存流程定义")
    public R<Long> defSave(@RequestBody OaFlowDef def) {
        // 校验 nodes JSON 结构（v2：APPROVAL 须有审批人；CONDITION 须有分支）
        try {
            cn.hutool.json.JSONArray arr = cn.hutool.json.JSONUtil.parseArray(def.getNodes());
            if (arr.isEmpty()) {
                throw new BizException("至少一个节点");
            }
            for (Object o : arr) {
                cn.hutool.json.JSONObject node = (cn.hutool.json.JSONObject) o;
                if ("CONDITION".equalsIgnoreCase(node.getStr("type", ""))) {
                    cn.hutool.json.JSONArray branches = node.getJSONArray("branches");
                    if (branches == null || branches.isEmpty()) {
                        throw new BizException("条件节点【" + node.getStr("name", "?") + "】必须有 branches");
                    }
                    for (Object b : branches) {
                        if (((cn.hutool.json.JSONObject) b).getInt("goto") == null) {
                            throw new BizException("条件分支必须指定 goto（目标节点 level）");
                        }
                    }
                } else if (node.getJSONArray("approverUserIds") == null || node.getJSONArray("approverUserIds").isEmpty()) {
                    throw new BizException("审批节点【" + node.getStr("name", "?") + "】必须有审批人");
                }
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("nodes 不是合法的节点数组 JSON");
        }
        if (def.getStatus() == null) {
            def.setStatus(1);
        }
        if (def.getId() == null) {
            defMapper.insert(def);
        } else {
            defMapper.updateById(def);
        }
        return R.ok(def.getId());
    }

    @DeleteMapping("/def/delete")
    @SaCheckPermission("oa:flow:save")
    public R<Void> defDelete(@RequestParam Long id) {
        defMapper.deleteById(id);
        return R.ok();
    }

    // ---- 我的待办 ----

    @GetMapping("/todo")
    public R<List<OaFlowTask>> todo() {
        return R.ok(flowEngine.myTodo(StpUtil.getLoginIdAsLong()));
    }

    /** 任务详情：实例 + 节点任务列表 */
    @GetMapping("/instance")
    public R<OaFlowInstance> instance(@RequestParam int bizType, @RequestParam Long bizId) {
        OaFlowInstance inst = flowEngine.getInstance(bizType, bizId);
        return R.ok(inst);
    }

    /** 按实例 ID 查（移动端待办 -> 业务单摘要） */
    @GetMapping("/instance-by-id")
    public R<OaFlowInstance> instanceById(@RequestParam Long instanceId) {
        return R.ok(instMapper.selectById(instanceId));
    }

    @GetMapping("/tasks")
    public R<List<OaFlowTask>> tasks(@RequestParam Long instanceId) {
        return R.ok(flowEngine.tasksOf(instanceId));
    }

    // ---- 审批 / 撤回 ----

    @PutMapping("/audit")
    public R<Void> audit(@RequestParam Long taskId, @RequestParam boolean pass,
                         @RequestParam(required = false) String opinion) {
        flowEngine.audit(taskId, pass, opinion);
        return R.ok();
    }

    @PutMapping("/withdraw")
    public R<Void> withdraw(@RequestParam Long instanceId) {
        flowEngine.withdraw(instanceId, StpUtil.getLoginIdAsLong());
        return R.ok();
    }

    /** 我发起的流程 */
    @GetMapping("/mine")
    public R<Page<OaFlowInstance>> mine(@RequestParam(defaultValue = "1") long pageNo,
                                        @RequestParam(defaultValue = "10") long pageSize) {
        return R.ok(instMapper.selectPage(new Page<>(pageNo, pageSize),
                new LambdaQueryWrapper<OaFlowInstance>()
                        .eq(OaFlowInstance::getApplicantUserId, StpUtil.getLoginIdAsLong())
                        .orderByDesc(OaFlowInstance::getId)));
    }
}
