/*
 * 码域CRM（MaoyuCRM）— CRM + OA + EAM 一体化系统
 * Copyright (c) 2026 合肥市码域科技 (Maoyu Technology, Hefei)
 *
 * 本程序是自由软件：您可以根据自由软件基金会发布的 GNU Affero 通用公共许可证
 * （第 3 版或任意后续版本）的条款重新分发和/或修改它。
 * 传播本程序时须保留本版权声明；商业闭源使用请联系作者获取商业授权。
 * 详见仓库根目录 LICENSE（AGPL-3.0）。
 */
package com.freecrm.crm.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.freecrm.common.exception.BizException;
import com.freecrm.common.log.OpLog;
import com.freecrm.common.result.R;
import com.freecrm.crm.entity.CrmContract;
import com.freecrm.crm.entity.CrmCustomer;
import com.freecrm.crm.entity.CrmLead;
import com.freecrm.crm.service.CustomerService;
import com.freecrm.crm.service.TeamService;
import com.freecrm.crm.service.impl.ContractServiceImpl;
import com.freecrm.crm.service.impl.CustomerServiceImpl;
import com.freecrm.crm.service.impl.LeadServiceImpl;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.util.*;

/**
 * Excel 导入导出：客户 / 线索 / 合同
 * 导出走当前用户数据范围；导入逐行校验（名称必填，重名跳过）
 */
@RestController
@RequestMapping("/api/crm/excel")
public class ExcelController {

    private final CustomerService customerService;
    private final LeadServiceImpl leadService;
    private final ContractServiceImpl contractService;
    private final TeamService teamService;

    public ExcelController(CustomerService customerService, LeadServiceImpl leadService,
                           ContractServiceImpl contractService, TeamService teamService) {
        this.customerService = customerService;
        this.leadService = leadService;
        this.contractService = contractService;
        this.teamService = teamService;
    }

    // ---------- 导出 ----------

    @GetMapping("/export")
    @SaCheckPermission("crm:customer:list")
    @OpLog(module = "Excel", action = "导出数据")
    public void export(@RequestParam String type,
                       @RequestParam(required = false) String keyword,
                       HttpServletResponse resp) throws IOException {
        String file = type + "-" + System.currentTimeMillis() + ".xlsx";
        resp.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        resp.setCharacterEncoding("utf-8");
        resp.setHeader("Content-Disposition", "attachment;filename*=utf-8''" + URLEncoder.encode(file, "UTF-8"));
        if ("customer".equals(type)) {
            List<CrmCustomer> rows = customerService.page(1, 100000, keyword, false).getRecords();
            List<List<String>> head = heads("ID", "客户名称", "手机", "电话", "邮箱", "网址", "地址", "来源", "行业", "级别", "成交状态", "锁定", "负责人ID", "备注", "创建时间");
            List<List<Object>> data = new ArrayList<>();
            for (CrmCustomer c : rows) {
                data.add(row(c.getId(), c.getName(), c.getMobile(), c.getTelephone(), c.getEmail(), c.getWebsite(),
                        c.getAddress(), c.getSource(), c.getIndustry(), c.getLevel(),
                        c.getDealStatus() != null && c.getDealStatus() == 1 ? "已成交" : "未成交",
                        c.getLockStatus() != null && c.getLockStatus() == 1 ? "锁定" : "正常",
                        c.getOwnerUserId(), c.getRemark(), c.getCreateTime()));
            }
            write(resp, head, data, "客户");
        } else if ("lead".equals(type)) {
            Page<CrmLead> p = leadService.page(1, 100000, keyword, null);
            List<List<String>> head = heads("ID", "线索名称", "联系人", "手机", "邮箱", "来源", "行业", "状态", "负责人ID", "创建时间");
            List<List<Object>> data = new ArrayList<>();
            for (CrmLead l : p.getRecords()) {
                data.add(row(l.getId(), l.getName(), l.getContactName(), l.getMobile(), l.getEmail(),
                        l.getSource(), l.getIndustry(),
                        l.getTransformStatus() != null && l.getTransformStatus() == 1 ? "已转化" : "跟进中",
                        l.getOwnerUserId(), l.getCreateTime()));
            }
            write(resp, head, data, "线索");
        } else if ("contract".equals(type)) {
            Page<CrmContract> p = contractService.page(1, 100000, keyword, null, null);
            List<List<String>> head = heads("ID", "合同编号", "名称", "客户ID", "下单日期", "总金额", "已回款", "审核状态", "负责人ID", "备注");
            List<List<Object>> data = new ArrayList<>();
            for (CrmContract c : p.getRecords()) {
                data.add(row(c.getId(), c.getContractNo(), c.getName(), c.getCustomerId(), c.getOrderDate(),
                        c.getTotalAmount(), c.getReceivedAmount(),
                        new String[]{"草稿", "审批中", "已通过", "已驳回"}[c.getAuditStatus() == null ? 0 : c.getAuditStatus()],
                        c.getOwnerUserId(), c.getRemark()));
            }
            write(resp, head, data, "合同");
        } else {
            throw new BizException("不支持的导出类型：" + type);
        }
    }

    // ---------- 模板 + 导入（客户） ----------

    @GetMapping("/template")
    public void template(@RequestParam String type, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        resp.setHeader("Content-Disposition", "attachment;filename*=utf-8''" + URLEncoder.encode(type + "-template.xlsx", "UTF-8"));
        if ("customer".equals(type)) {
            write(resp, heads("客户名称*", "手机", "邮箱", "地址", "来源(1-6)", "行业(1-6)", "级别(1-4=A-D)", "备注"),
                    Collections.singletonList(row("示例客户科技有限公司", "13800001111", "ex@a.cn", "杭州市西湖区", 2, 2, 1, "示例行导入前请删除")), "客户");
        } else {
            write(resp, heads("线索名称*", "联系人", "手机", "邮箱", "来源(1-6)", "备注"),
                    Collections.singletonList(row("示例线索-张先生", "张先生", "13900002222", "", 1, "示例行导入前请删除")), "线索");
        }
    }

    @PostMapping("/import")
    @SaCheckPermission("crm:customer:save")
    @OpLog(module = "Excel", action = "导入数据")
    public R<Map<String, Object>> importExcel(@RequestParam String type,
                                              @RequestParam("file") MultipartFile file) throws IOException {
        List<Map<Integer, String>> rows = EasyExcel.read(file.getInputStream()).sheet().doReadSync();
        long uid = StpUtil.getLoginIdAsLong();
        int ok = 0, skipped = 0;
        List<String> errors = new ArrayList<>();
        int line = 1;
        for (Map<Integer, String> r : rows) {
            line++;
            String name = r.get(0);
            if (name == null || name.trim().isEmpty() || name.contains("示例")) {
                skipped++;
                continue;
            }
            try {
                if ("customer".equals(type)) {
                    if (!customerService.checkDuplicate(name.trim(), r.get(1), null).isEmpty()) {
                        skipped++;
                        continue;
                    }
                    CrmCustomer c = new CrmCustomer();
                    c.setName(name.trim());
                    c.setMobile(r.get(1));
                    c.setEmail(r.get(2));
                    c.setAddress(r.get(3));
                    c.setSource(parseInt(r.get(4)));
                    c.setIndustry(parseInt(r.get(5)));
                    c.setLevel(parseInt(r.get(6)));
                    c.setRemark(r.get(7));
                    customerService.create(c);
                } else if ("lead".equals(type)) {
                    CrmLead l = new CrmLead();
                    l.setName(name.trim());
                    l.setContactName(r.get(1));
                    l.setMobile(r.get(2));
                    l.setEmail(r.get(3));
                    l.setSource(parseInt(r.get(4)));
                    l.setRemark(r.get(5));
                    leadService.create(l);
                } else {
                    throw new BizException("不支持的导入类型");
                }
                ok++;
            } catch (Exception e) {
                errors.add("第 " + line + " 行：" + e.getMessage());
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("ok", ok);
        result.put("skipped", skipped);
        result.put("errors", errors);
        return R.ok(result);
    }

    private Integer parseInt(String s) {
        if (s == null || s.isEmpty()) {
            return null;
        }
        try {
            double d = Double.parseDouble(s.trim());
            return (int) d;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private List<List<String>> heads(String... names) {
        List<List<String>> head = new ArrayList<>();
        for (String n : names) {
            head.add(Collections.singletonList(n));
        }
        return head;
    }

    private List<Object> row(Object... cells) {
        return new ArrayList<>(Arrays.asList(cells));
    }

    private void write(HttpServletResponse resp, List<List<String>> head, List<List<Object>> data, String sheet) throws IOException {
        EasyExcel.write(resp.getOutputStream()).head(head).sheet(sheet).doWrite(data);
    }
}
