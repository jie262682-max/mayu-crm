/*
 * 码域CRM（MaoyuCRM）— CRM + OA + EAM 一体化系统
 * Copyright (c) 2026 合肥市码域科技 (Maoyu Technology, Hefei)
 *
 * AGPL-3.0，详见仓库根目录 LICENSE。
 */
package com.freecrm.oa.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.freecrm.common.exception.BizException;
import com.freecrm.common.log.OpLog;
import com.freecrm.common.result.R;
import com.freecrm.oa.entity.OaFormApply;
import com.freecrm.oa.entity.OaFormDef;
import com.freecrm.oa.mapper.OaFormApplyMapper;
import com.freecrm.oa.mapper.OaFormDefMapper;
import com.freecrm.oa.service.FlowEngine;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 通用审批申请单：自定义表单定义 + 提交申请（自动进审批流 bizType=100）+ 我的申请
 */
@RestController
@RequestMapping("/api/oa/form")
public class FormController {

    /** 通用申请的流程 bizType（需配一条流程定义） */
    public static final int BIZ_TYPE = 100;

    private final OaFormDefMapper defMapper;
    private final OaFormApplyMapper applyMapper;
    private final FlowEngine flowEngine;

    public FormController(OaFormDefMapper defMapper, OaFormApplyMapper applyMapper, FlowEngine flowEngine) {
        this.defMapper = defMapper;
        this.applyMapper = applyMapper;
        this.flowEngine = flowEngine;
    }

    // ---- 表单定义 ----

    @GetMapping("/def/list")
    public R<Page<OaFormDef>> defList(@RequestParam(defaultValue = "1") long pageNo,
                                       @RequestParam(defaultValue = "50") long pageSize) {
        return R.ok(defMapper.selectPage(new Page<>(pageNo, pageSize),
                new LambdaQueryWrapper<OaFormDef>()
                        .orderByDesc(OaFormDef::getId)));
    }

    /** 启用中的表单（发起申请用） */
    @GetMapping("/def/enabled")
    public R<java.util.List<OaFormDef>> enabled() {
        return R.ok(defMapper.selectList(new LambdaQueryWrapper<OaFormDef>()
                .eq(OaFormDef::getStatus, 1).orderByAsc(OaFormDef::getId)));
    }

    @PostMapping("/def/save")
    @SaCheckPermission("oa:flow:save")
    @OpLog(module = "表单管理", action = "保存表单")
    public R<Long> defSave(@RequestBody OaFormDef def) {
        try {
            JSONArray arr = JSONUtil.parseArray(def.getFields());
            for (Object o : arr) {
                JSONObject f = (JSONObject) o;
                if (f.getStr("key") == null || f.getStr("label") == null || f.getStr("type") == null) {
                    throw new BizException("字段必须含 key/label/type");
                }
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException("fields 不是合法的字段数组 JSON");
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
    @OpLog(module = "表单管理", action = "删除表单")
    public R<Void> defDelete(@RequestParam Long id) {
        defMapper.deleteById(id);
        return R.ok();
    }

    // ---- 申请 ----

    /** 提交申请：校验必填 → 落库 → 自动进审批流 */
    @PostMapping("/apply")
    @OpLog(module = "通用审批", action = "提交申请")
    @Transactional(rollbackFor = Exception.class)
    public R<Map<String, Object>> apply(@RequestBody OaFormApply apply) {
        if (apply.getFormId() == null) {
            throw new BizException("缺少表单");
        }
        OaFormDef def = defMapper.selectById(apply.getFormId());
        if (def == null || def.getStatus() != 1) {
            throw new BizException("表单不存在或已停用");
        }
        // 必填校验
        JSONObject data;
        try {
            data = JSONUtil.parseObj(apply.getFormData() == null ? "{}" : apply.getFormData());
        } catch (Exception e) {
            throw new BizException("form_data 必须是 JSON 对象");
        }
        for (Object o : JSONUtil.parseArray(def.getFields())) {
            JSONObject f = (JSONObject) o;
            if (f.getBool("required", false) && data.getStr(f.getStr("key")) == null) {
                throw new BizException("必填项缺失：" + f.getStr("label"));
            }
        }
        long uid = StpUtil.getLoginIdAsLong();
        apply.setUserId(uid);
        apply.setAuditStatus(OaFormApply.AUDITING);
        applyMapper.insert(apply);
        // 进审批流（bizType=100，变量 amount 供条件路由）
        Map<String, Object> vars = new HashMap<>();
        try {
            Object amt = data.get("amount");
            if (amt != null) {
                vars.put("amount", Double.parseDouble(String.valueOf(amt)));
            }
        } catch (Exception ignore) { }
        Map<String, Object> r = new HashMap<>();
        r.put("id", apply.getId());
        if (flowEngine.hasActiveFlow(BIZ_TYPE)) {
            flowEngine.start(BIZ_TYPE, apply.getId(), def.getName() + "：" + (apply.getTitle() == null ? "" : apply.getTitle()), uid, vars);
            r.put("flowStarted", true);
        } else {
            // 未配流程直接通过
            OaFormApply upd = new OaFormApply();
            upd.setId(apply.getId());
            upd.setAuditStatus(OaFormApply.PASSED);
            applyMapper.updateById(upd);
            r.put("flowStarted", false);
        }
        return R.ok(r);
    }

    /** 申请列表（我的 / 全部） */
    @GetMapping("/apply/page")
    public R<Page<OaFormApply>> applyPage(@RequestParam(defaultValue = "1") long pageNo,
                                          @RequestParam(defaultValue = "10") long pageSize,
                                          @RequestParam(required = false) Boolean mine,
                                          @RequestParam(required = false) Long formId,
                                          @RequestParam(required = false) Integer auditStatus) {
        LambdaQueryWrapper<OaFormApply> qw = new LambdaQueryWrapper<>();
        if (Boolean.TRUE.equals(mine)) {
            qw.eq(OaFormApply::getUserId, StpUtil.getLoginIdAsLong());
        }
        qw.eq(formId != null, OaFormApply::getFormId, formId)
                .eq(auditStatus != null, OaFormApply::getAuditStatus, auditStatus)
                .orderByDesc(OaFormApply::getId);
        return R.ok(applyMapper.selectPage(new Page<>(pageNo, pageSize), qw));
    }

    @GetMapping("/apply/get")
    public R<OaFormApply> applyGet(@RequestParam Long id) {
        return R.ok(applyMapper.selectById(id));
    }
}
