/*
 * 码域CRM（MaoyuCRM）— CRM + OA + EAM 一体化系统
 * Copyright (c) 2026 合肥市码域科技 (Maoyu Technology, Hefei)
 *
 * 本程序是自由软件：您可以根据自由软件基金会发布的 GNU Affero 通用公共许可证
 * （第 3 版或任意后续版本）的条款重新分发和/或修改它。
 * 传播本程序时须保留本版权声明；商业闭源使用请联系作者获取商业授权。
 * 详见仓库根目录 LICENSE（AGPL-3.0）。
 */
package com.freecrm.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.freecrm.system.entity.SysConfig;
import com.freecrm.system.mapper.SysConfigMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/** 系统配置（key-value，DB 缓存每分钟刷新一次由调用方决定；读频低，直接查库） */
@Service
public class ConfigService {

    private final SysConfigMapper configMapper;

    public ConfigService(SysConfigMapper configMapper) {
        this.configMapper = configMapper;
    }

    public String get(String key, String def) {
        SysConfig c = configMapper.selectOne(new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getConfigKey, key));
        return c == null || c.getConfigValue() == null || c.getConfigValue().isEmpty() ? def : c.getConfigValue();
    }

    public int getInt(String key, int def) {
        try {
            return Integer.parseInt(get(key, String.valueOf(def)));
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public void set(String key, String value) {
        SysConfig c = configMapper.selectOne(new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getConfigKey, key));
        if (c == null) {
            c = new SysConfig();
            c.setConfigKey(key);
            c.setConfigValue(value);
            configMapper.insert(c);
        } else {
            c.setConfigValue(value);
            configMapper.updateById(c);
        }
    }

    public List<SysConfig> listAll() {
        return configMapper.selectList(new LambdaQueryWrapper<SysConfig>().orderByAsc(SysConfig::getId));
    }
}
