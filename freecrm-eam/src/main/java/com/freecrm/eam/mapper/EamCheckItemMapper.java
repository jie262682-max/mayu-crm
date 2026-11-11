/*
 * 码域CRM（MaoyuCRM）— CRM + OA + EAM 一体化系统
 * Copyright (c) 2026 合肥市码域科技 (Maoyu Technology, Hefei)
 *
 * 本程序是自由软件：您可以根据自由软件基金会发布的 GNU Affero 通用公共许可证
 * （第 3 版或任意后续版本）的条款重新分发和/或修改它。
 * 传播本程序时须保留本版权声明；商业闭源使用请联系作者获取商业授权。
 * 详见仓库根目录 LICENSE（AGPL-3.0）。
 */
package com.freecrm.eam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.freecrm.eam.entity.EamCheckItem;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EamCheckItemMapper extends BaseMapper<EamCheckItem> {
}
