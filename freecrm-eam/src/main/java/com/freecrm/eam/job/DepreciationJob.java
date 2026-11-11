/*
 * 码域CRM（MaoyuCRM）— CRM + OA + EAM 一体化系统
 * Copyright (c) 2026 合肥市码域科技 (Maoyu Technology, Hefei)
 *
 * 本程序是自由软件：您可以根据自由软件基金会发布的 GNU Affero 通用公共许可证
 * （第 3 版或任意后续版本）的条款重新分发和/或修改它。
 * 传播本程序时须保留本版权声明；商业闭源使用请联系作者获取商业授权。
 * 详见仓库根目录 LICENSE（AGPL-3.0）。
 */
package com.freecrm.eam.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.freecrm.eam.entity.EamAsset;
import com.freecrm.eam.entity.EamDepreciation;
import com.freecrm.eam.mapper.EamAssetMapper;
import com.freecrm.eam.mapper.EamDepreciationMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 资产折旧（直线法，残值率 5%）：每月 1 日 02:30 生成上月折旧
 * 月折旧 = (购置金额 - 残值) / (年限*12)；净值 <= 残值后停提
 */
@Component
public class DepreciationJob {

    private static final Logger log = LoggerFactory.getLogger(DepreciationJob.class);
    private static final BigDecimal SALVAGE_RATE = new BigDecimal("0.05");

    private final EamAssetMapper assetMapper;
    private final EamDepreciationMapper depMapper;

    public DepreciationJob(EamAssetMapper assetMapper, EamDepreciationMapper depMapper) {
        this.assetMapper = assetMapper;
        this.depMapper = depMapper;
    }

    /** 每月 1 日 02:30 执行；也可手动触发 */
    @Scheduled(cron = "0 30 2 1 * ?")
    public void monthly() {
        run(YearMonth.now().minusMonths(1));
    }

    /** 生成某月折旧（幂等：已生成跳过） */
    public int run(YearMonth month) {
        String ym = month.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        List<EamAsset> assets = assetMapper.selectList(new LambdaQueryWrapper<EamAsset>()
                .ne(EamAsset::getStatus, EamAsset.SCRAPPED)
                .isNotNull(EamAsset::getPurchaseAmount));
        int count = 0;
        for (EamAsset a : assets) {
            if (a.getPurchaseDate() != null && YearMonth.from(a.getPurchaseDate()).isAfter(month)) {
                continue; // 购置月晚于折旧月
            }
            Long exists = depMapper.selectCount(new LambdaQueryWrapper<EamDepreciation>()
                    .eq(EamDepreciation::getAssetId, a.getId())
                    .eq(EamDepreciation::getDepMonth, ym));
            if (exists != null && exists > 0) {
                continue;
            }
            BigDecimal cost = a.getPurchaseAmount() == null ? BigDecimal.ZERO : a.getPurchaseAmount();
            BigDecimal salvage = cost.multiply(SALVAGE_RATE).setScale(2, RoundingMode.HALF_UP);
            int months = (a.getUseYears() == null ? 5 : a.getUseYears()) * 12;
            BigDecimal monthly = cost.subtract(salvage)
                    .divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);
            BigDecimal lastNet = depMapper.selectList(new LambdaQueryWrapper<EamDepreciation>()
                            .eq(EamDepreciation::getAssetId, a.getId())
                            .orderByDesc(EamDepreciation::getId).last("LIMIT 1"))
                    .stream().findFirst().map(EamDepreciation::getNetValue).orElse(cost);
            if (lastNet.compareTo(salvage) <= 0) {
                continue; // 已提完
            }
            BigDecimal net = lastNet.subtract(monthly).max(salvage);
            EamDepreciation d = new EamDepreciation();
            d.setAssetId(a.getId());
            d.setDepMonth(ym);
            d.setAmount(monthly);
            d.setNetValue(net);
            depMapper.insert(d);
            count++;
        }
        if (count > 0) {
            log.info("折旧完成 {} 月：{} 条", ym, count);
        }
        return count;
    }

    /** 判断是否已超使用年限（台账展示用） */
    public static boolean expired(EamAsset a) {
        if (a.getPurchaseDate() == null) {
            return false;
        }
        return LocalDate.now().isAfter(a.getPurchaseDate().plusYears(a.getUseYears() == null ? 5 : a.getUseYears()));
    }
}
