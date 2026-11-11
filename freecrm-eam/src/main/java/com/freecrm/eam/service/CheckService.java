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

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.freecrm.common.exception.BizException;
import com.freecrm.eam.entity.EamAsset;
import com.freecrm.eam.entity.EamCheck;
import com.freecrm.eam.entity.EamCheckItem;
import com.freecrm.eam.mapper.EamAssetMapper;
import com.freecrm.eam.mapper.EamCheckItemMapper;
import com.freecrm.eam.mapper.EamCheckMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** 资产盘点：创建（全部/按部门快照）→ 逐项实盘 → 完成汇总差异 */
@Service
public class CheckService {

    private final EamCheckMapper checkMapper;
    private final EamCheckItemMapper itemMapper;
    private final EamAssetMapper assetMapper;

    public CheckService(EamCheckMapper checkMapper, EamCheckItemMapper itemMapper, EamAssetMapper assetMapper) {
        this.checkMapper = checkMapper;
        this.itemMapper = itemMapper;
        this.assetMapper = assetMapper;
    }

    /** 创建盘点单：按范围快照资产到明细 */
    @Transactional(rollbackFor = Exception.class)
    public Long create(Integer scopeType, Long deptId, String remark) {
        List<EamAsset> assets = assetMapper.selectList(new LambdaQueryWrapper<EamAsset>()
                .ne(EamAsset::getStatus, EamAsset.SCRAPPED)
                .eq(deptId != null, EamAsset::getDeptId, deptId));
        if (assets.isEmpty()) {
            throw new BizException("盘点范围内没有在册资产");
        }
        EamCheck c = new EamCheck();
        c.setCheckNo("PD" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + IdUtil.fastSimpleUUID().substring(0, 4).toUpperCase());
        c.setScopeType(scopeType == null ? 1 : scopeType);
        c.setDeptId(deptId);
        c.setStatus(0);
        c.setTotalCount(assets.size());
        c.setDiffCount(0);
        c.setRemark(remark);
        checkMapper.insert(c);
        for (EamAsset a : assets) {
            EamCheckItem item = new EamCheckItem();
            item.setCheckId(c.getId());
            item.setAssetId(a.getId());
            item.setExpectStatus(a.getStatus());
            item.setDiff(0);
            itemMapper.insert(item);
        }
        return c.getId();
    }

    /** 实盘一项：记录实际状态/保管人，标差异 */
    @Transactional(rollbackFor = Exception.class)
    public void checkItem(Long itemId, Integer actualStatus, Long actualKeeperId, boolean missing) {
        EamCheckItem item = itemMapper.selectById(itemId);
        if (item == null) {
            throw new BizException("盘点项不存在");
        }
        EamCheck c = checkMapper.selectById(item.getCheckId());
        if (c.getStatus() != 0) {
            throw new BizException("盘点单已完成");
        }
        EamAsset a = assetMapper.selectById(item.getAssetId());
        boolean diff = missing
                || (actualStatus != null && !actualStatus.equals(item.getExpectStatus()))
                || (actualKeeperId != null && a != null && a.getKeeperUserId() != null
                    && !actualKeeperId.equals(a.getKeeperUserId()));
        boolean wasDiff = item.getDiff() != null && item.getDiff() == 1;
        item.setActualStatus(missing ? EamAsset.SCRAPPED : actualStatus);
        item.setActualKeeperId(actualKeeperId);
        item.setDiff(diff ? 1 : 0);
        item.setCheckedBy(null);
        item.setCheckedTime(LocalDateTime.now());
        itemMapper.updateById(item);
        if (diff != wasDiff) {
            EamCheck upd = new EamCheck();
            upd.setId(c.getId());
            upd.setDiffCount(c.getDiffCount() + (diff ? 1 : -1));
            checkMapper.updateById(upd);
        }
    }

    /** 完成盘点 */
    @Transactional(rollbackFor = Exception.class)
    public void finish(Long checkId) {
        EamCheck c = checkMapper.selectById(checkId);
        if (c == null || c.getStatus() != 0) {
            throw new BizException("盘点单不存在或已完成");
        }
        EamCheck upd = new EamCheck();
        upd.setId(checkId);
        upd.setStatus(1);
        checkMapper.updateById(upd);
    }

    public Page<EamCheck> page(long pageNo, long pageSize) {
        return checkMapper.selectPage(new Page<>(pageNo, pageSize),
                new LambdaQueryWrapper<EamCheck>().orderByDesc(EamCheck::getId));
    }

    public List<EamCheckItem> items(Long checkId, Boolean onlyDiff) {
        return itemMapper.selectList(new LambdaQueryWrapper<EamCheckItem>()
                .eq(EamCheckItem::getCheckId, checkId)
                .eq(Boolean.TRUE.equals(onlyDiff), EamCheckItem::getDiff, 1)
                .orderByAsc(EamCheckItem::getId));
    }
}
