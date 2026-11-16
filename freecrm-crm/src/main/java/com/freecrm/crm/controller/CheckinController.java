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
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.freecrm.common.result.R;
import com.freecrm.crm.entity.CrmCheckin;
import com.freecrm.crm.entity.CrmCustomer;
import com.freecrm.crm.entity.CrmFollowUp;
import com.freecrm.crm.mapper.CrmCheckinMapper;
import com.freecrm.crm.mapper.CrmCustomerMapper;
import com.freecrm.crm.mapper.CrmFollowUpMapper;
import com.freecrm.crm.service.TeamService;
import com.freecrm.crm.service.impl.LeadServiceImpl;
import com.freecrm.crm.service.impl.TeamServiceImpl;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 拜访签到：定位打卡 + 自动生成上门跟进记录 */
@RestController
@RequestMapping("/api/crm/checkin")
public class CheckinController {

    private final CrmCheckinMapper checkinMapper;
    private final CrmCustomerMapper customerMapper;
    private final CrmFollowUpMapper followUpMapper;
    private final TeamService teamService;

    public CheckinController(CrmCheckinMapper checkinMapper, CrmCustomerMapper customerMapper,
                             CrmFollowUpMapper followUpMapper, TeamService teamService) {
        this.checkinMapper = checkinMapper;
        this.customerMapper = customerMapper;
        this.followUpMapper = followUpMapper;
        this.teamService = teamService;
    }

    /** 到场签到：需对该客户有写权限（能跟进即能签到） */
    @PostMapping("/do")
    @SaCheckPermission("crm:customer:save")
    public R<CrmCheckin> doCheckin(@RequestBody CrmCheckin req) {
        if (req.getCustomerId() == null) {
            throw new IllegalArgumentException("客户不能为空");
        }
        teamService.checkPermission(LeadServiceImpl.BIZ_CUSTOMER, req.getCustomerId(), TeamServiceImpl.WRITE);
        CrmCustomer c = customerMapper.selectById(req.getCustomerId());
        if (c == null) {
            throw new IllegalArgumentException("客户不存在");
        }
        Integer distance = null;
        if (req.getLng() != null && req.getLat() != null
                && c.getAddressLng() != null && c.getAddressLat() != null) {
            distance = haversineMeters(req.getLat().doubleValue(), req.getLng().doubleValue(),
                    c.getAddressLat().doubleValue(), c.getAddressLng().doubleValue());
        }
        CrmCheckin ck = new CrmCheckin();
        ck.setCustomerId(req.getCustomerId());
        ck.setUserId(StpUtil.getLoginIdAsLong());
        ck.setLng(req.getLng());
        ck.setLat(req.getLat());
        ck.setDistance(distance);
        ck.setRemark(req.getRemark());
        checkinMapper.insert(ck);

        // 自动生成上门跟进
        CrmFollowUp fu = new CrmFollowUp();
        fu.setBizType(LeadServiceImpl.BIZ_CUSTOMER);
        fu.setBizId(req.getCustomerId());
        fu.setType(3);
        fu.setContent("[拜访签到]" + (distance != null ? " 距客户约 " + distance + " 米" : "")
                + (req.getRemark() != null && !req.getRemark().isEmpty() ? " " + req.getRemark() : ""));
        fu.setNextTime(LocalDateTime.now());
        followUpMapper.insert(fu);
        return R.ok(ck);
    }

    /** 签到列表：指定客户 = 该客户全部签到（需读权限）；否则超管看全部、普通用户看自己 */
    @GetMapping("/page")
    @SaCheckPermission("crm:checkin:list")
    public R<Page<CrmCheckin>> page(@RequestParam(defaultValue = "1") long pageNo,
                                    @RequestParam(defaultValue = "10") long pageSize,
                                    @RequestParam(required = false) Long customerId) {
        LambdaQueryWrapper<CrmCheckin> w = new LambdaQueryWrapper<CrmCheckin>()
                .eq(customerId != null, CrmCheckin::getCustomerId, customerId)
                .orderByDesc(CrmCheckin::getId);
        if (customerId != null) {
            teamService.checkPermission(LeadServiceImpl.BIZ_CUSTOMER, customerId, TeamServiceImpl.READ);
        } else if (!StpUtil.getPermissionList().contains("*")) {
            w.eq(CrmCheckin::getUserId, StpUtil.getLoginIdAsLong());
        }
        return R.ok(checkinMapper.selectPage(new Page<>(pageNo, pageSize), w));
    }

    /** haversine 球面距离（米） */
    static int haversineMeters(double lat1, double lng1, double lat2, double lng2) {
        double r = 6371000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return (int) Math.round(2 * r * Math.asin(Math.sqrt(a)));
    }

    @lombok.Data
    public static class DoReq {
        private Long customerId;
        private BigDecimal lng;
        private BigDecimal lat;
        private String remark;
    }
}
