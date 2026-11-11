/*
 * 码域CRM（MaoyuCRM）— CRM + OA + EAM 一体化系统
 * Copyright (c) 2026 合肥市码域科技 (Maoyu Technology, Hefei)
 *
 * 本程序是自由软件：您可以根据自由软件基金会发布的 GNU Affero 通用公共许可证
 * （第 3 版或任意后续版本）的条款重新分发和/或修改它。
 * 传播本程序时须保留本版权声明；商业闭源使用请联系作者获取商业授权。
 * 详见仓库根目录 LICENSE（AGPL-3.0）。
 */
package com.freecrm.eam.service;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.freecrm.common.exception.BizException;
import com.freecrm.eam.entity.EamAsset;
import com.freecrm.eam.entity.EamAssetChange;
import com.freecrm.eam.mapper.EamAssetChangeMapper;
import com.freecrm.eam.mapper.EamAssetMapper;
import com.freecrm.oa.service.FlowEngine;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 资产台账 + 变更状态机：
 * 闲置 --领用/借出--> 在用(保管人) --退回/归还--> 闲置
 * 在用/闲置 --报修--> 维修 --维修完成--> 闲置
 * 任意 --报废申请--> 走审批流(bizType=4)，通过后置报废
 */
@Service
public class AssetService {

    /** 审批流业务类型：资产报废 */
    public static final int FLOW_BIZ_SCRAP = 4;

    private final EamAssetMapper assetMapper;
    private final EamAssetChangeMapper changeMapper;
    private final FlowEngine flowEngine;

    public AssetService(EamAssetMapper assetMapper, EamAssetChangeMapper changeMapper,
                        @org.springframework.context.annotation.Lazy FlowEngine flowEngine) {
        this.assetMapper = assetMapper;
        this.changeMapper = changeMapper;
        this.flowEngine = flowEngine;
    }

    // ---------- 台账 ----------

    public Long create(EamAsset a) {
        if (a.getAssetNo() == null || a.getAssetNo().isEmpty()) {
            a.setAssetNo("ZC" + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"))
                    + IdUtil.fastSimpleUUID().substring(0, 4).toUpperCase());
        }
        if (a.getUseYears() == null || a.getUseYears() <= 0) {
            a.setUseYears(5);
        }
        if (a.getStatus() == null) {
            a.setStatus(EamAsset.IDLE);
            if (a.getKeeperUserId() != null) {
                a.setStatus(EamAsset.IN_USE);
            }
        }
        assetMapper.insert(a);
        return a.getId();
    }

    public void update(EamAsset a) {
        EamAsset db = require(a.getId());
        if (db.getStatus() == EamAsset.SCRAPPED) {
            throw new BizException("已报废资产不能修改");
        }
        a.setAssetNo(null);
        assetMapper.updateById(a);
    }

    public void delete(Long id) {
        EamAsset db = require(id);
        if (db.getStatus() != EamAsset.IDLE) {
            throw new BizException("仅闲置资产可删除");
        }
        assetMapper.deleteById(id);
    }

    public EamAsset get(Long id) {
        return require(id);
    }

    public Page<EamAsset> page(long pageNo, long pageSize, String keyword, Long categoryId,
                               Integer status, Long deptId, Long keeperUserId) {
        LambdaQueryWrapper<EamAsset> qw = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            qw.and(w -> w.like(EamAsset::getName, keyword).or().like(EamAsset::getAssetNo, keyword));
        }
        qw.eq(categoryId != null, EamAsset::getCategoryId, categoryId)
                .eq(status != null, EamAsset::getStatus, status)
                .eq(deptId != null, EamAsset::getDeptId, deptId)
                .eq(keeperUserId != null, EamAsset::getKeeperUserId, keeperUserId)
                .orderByDesc(EamAsset::getId);
        return assetMapper.selectPage(new Page<>(pageNo, pageSize), qw);
    }

    // ---------- 变更 ----------

    /** 领用：闲置 -> 在用（保管人=领用人） */
    @Transactional(rollbackFor = Exception.class)
    public void receive(Long assetId, Long toUserId, String reason) {
        EamAsset a = require(assetId);
        if (a.getStatus() != EamAsset.IDLE) {
            throw new BizException("仅闲置资产可领用，当前状态：" + statusName(a.getStatus()));
        }
        updStatus(a, EamAsset.IN_USE, toUserId);
        logChange(assetId, EamAssetChange.RECEIVE, null, toUserId, reason);
    }

    /** 退回：在用 -> 闲置 */
    @Transactional(rollbackFor = Exception.class)
    public void giveBack(Long assetId, String reason) {
        EamAsset a = require(assetId);
        if (a.getStatus() != EamAsset.IN_USE) {
            throw new BizException("仅在用资产可退回，当前状态：" + statusName(a.getStatus()));
        }
        Long from = a.getKeeperUserId();
        updStatus(a, EamAsset.IDLE, null);
        logChange(assetId, EamAssetChange.RETURN, from, null, reason);
    }

    /** 借出：闲置/在用 -> 在用（借用人） */
    @Transactional(rollbackFor = Exception.class)
    public void lend(Long assetId, Long toUserId, String reason) {
        EamAsset a = require(assetId);
        if (a.getStatus() == EamAsset.SCRAPPED) {
            throw new BizException("已报废资产不能借出");
        }
        Long from = a.getKeeperUserId();
        updStatus(a, EamAsset.IN_USE, toUserId);
        logChange(assetId, EamAssetChange.LEND, from, toUserId, reason);
    }

    /** 归还：在用 -> 闲置（或还原给原保管人由领用场景区分，这里统一退库） */
    @Transactional(rollbackFor = Exception.class)
    public void returnAsset(Long assetId, String reason) {
        giveBack(assetId, reason);
    }

    /** 报修：非报废 -> 维修 */
    @Transactional(rollbackFor = Exception.class)
    public void repair(Long assetId, String reason) {
        EamAsset a = require(assetId);
        if (a.getStatus() == EamAsset.SCRAPPED) {
            throw new BizException("已报废资产不能报修");
        }
        Long from = a.getKeeperUserId();
        updStatus(a, EamAsset.REPAIR, null);
        logChange(assetId, EamAssetChange.REPAIR, from, null, reason);
    }

    /** 维修完成：维修 -> 闲置 */
    @Transactional(rollbackFor = Exception.class)
    public void repairDone(Long assetId, String reason) {
        EamAsset a = require(assetId);
        if (a.getStatus() != EamAsset.REPAIR) {
            throw new BizException("仅维修中资产可完成维修");
        }
        updStatus(a, EamAsset.IDLE, null);
        logChange(assetId, EamAssetChange.REPAIR, null, null, "维修完成：" + (reason == null ? "" : reason));
    }

    /** 报废申请：有流程走审批(bizType=4)，无流程直接报废 */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> scrap(Long assetId, String reason) {
        EamAsset a = require(assetId);
        if (a.getStatus() == EamAsset.SCRAPPED) {
            throw new BizException("资产已是报废状态");
        }
        Map<String, Object> r = new HashMap<>();
        if (flowEngine.hasActiveFlow(FLOW_BIZ_SCRAP)) {
            Map<String, Object> vars = new HashMap<>();
            vars.put("amount", a.getPurchaseAmount() == null ? 0 : a.getPurchaseAmount());
            long instId = flowEngine.start(FLOW_BIZ_SCRAP, assetId,
                    "资产报废：" + a.getAssetNo() + " " + a.getName() + (reason == null ? "" : "（" + reason + "）"),
                    StpUtil.getLoginIdAsLong(), vars);
            r.put("flowStarted", true);
            r.put("instanceId", instId);
            return r;
        }
        doScrap(assetId, a.getKeeperUserId(), reason);
        r.put("flowStarted", false);
        return r;
    }

    /** 审批通过后实际报废（由 EamScrapFlowCallback 调用） */
    @Transactional(rollbackFor = Exception.class)
    public void doScrap(Long assetId, Long fromUser, String reason) {
        EamAsset a = require(assetId);
        Long from = fromUser != null ? fromUser : a.getKeeperUserId();
        updStatus(a, EamAsset.SCRAPPED, null);
        logChange(assetId, EamAssetChange.SCRAP, from, null, reason);
    }

    public Page<EamAssetChange> pageChanges(long pageNo, long pageSize, Long assetId, Integer type) {
        return changeMapper.selectPage(new Page<>(pageNo, pageSize), new LambdaQueryWrapper<EamAssetChange>()
                .eq(assetId != null, EamAssetChange::getAssetId, assetId)
                .eq(type != null, EamAssetChange::getType, type)
                .orderByDesc(EamAssetChange::getId));
    }

    public static String statusName(int s) {
        return new String[]{"闲置", "在用", "维修", "报废"}[s];
    }

    public static String changeName(int t) {
        return new String[]{"", "领用", "退回", "借出", "归还", "维修", "报废"}[t];
    }

    private void updStatus(EamAsset a, int status, Long keeper) {
        EamAsset upd = new EamAsset();
        upd.setId(a.getId());
        upd.setStatus(status);
        upd.setKeeperUserId(keeper);
        if (keeper == null) {
            // 置空保管人需显式 set
            assetMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<EamAsset>()
                    .eq(EamAsset::getId, a.getId())
                    .set(EamAsset::getStatus, status)
                    .set(EamAsset::getKeeperUserId, null));
            return;
        }
        assetMapper.updateById(upd);
    }

    private void logChange(Long assetId, int type, Long from, Long to, String reason) {
        EamAssetChange c = new EamAssetChange();
        c.setAssetId(assetId);
        c.setType(type);
        c.setFromUserId(from);
        c.setToUserId(to);
        c.setReason(reason);
        c.setChangeTime(LocalDateTime.now());
        changeMapper.insert(c);
    }

    private EamAsset require(Long id) {
        EamAsset a = assetMapper.selectById(id);
        if (a == null) {
            throw new BizException("资产不存在：" + id);
        }
        return a;
    }
}
