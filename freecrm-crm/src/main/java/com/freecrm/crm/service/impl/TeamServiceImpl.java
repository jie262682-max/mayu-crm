/*
 * 码域CRM（MaoyuCRM）— CRM + OA + EAM 一体化系统
 * Copyright (c) 2026 合肥市码域科技 (Maoyu Technology, Hefei)
 *
 * 本程序是自由软件：您可以根据自由软件基金会发布的 GNU Affero 通用公共许可证
 * （第 3 版或任意后续版本）的条款重新分发和/或修改它。
 * 传播本程序时须保留本版权声明；商业闭源使用请联系作者获取商业授权。
 * 详见仓库根目录 LICENSE（AGPL-3.0）。
 */
package com.freecrm.crm.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.freecrm.common.exception.BizException;
import com.freecrm.crm.entity.CrmTeamMember;
import com.freecrm.crm.mapper.CrmTeamMemberMapper;
import com.freecrm.crm.service.TeamService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TeamServiceImpl implements TeamService {

    public static final int OWNER = 1;
    public static final int WRITE = 2;
    public static final int READ = 3;

    private final CrmTeamMemberMapper teamMapper;

    public TeamServiceImpl(CrmTeamMemberMapper teamMapper) {
        this.teamMapper = teamMapper;
    }

    @Override
    public void checkPermission(int bizType, long bizId, int minLevel) {
        long userId = StpUtil.getLoginIdAsLong();
        CrmTeamMember member = teamMapper.selectOne(new LambdaQueryWrapper<CrmTeamMember>()
                .eq(CrmTeamMember::getBizType, bizType)
                .eq(CrmTeamMember::getBizId, bizId)
                .eq(CrmTeamMember::getUserId, userId));
        if (member == null || member.getLevel() > minLevel) {
            throw new BizException(403, "没有该记录的" + levelName(minLevel) + "权限");
        }
    }

    @Override
    public void addOwner(int bizType, long bizId, long userId) {
        // 业务上同一记录只有一个负责人：清旧设新
        teamMapper.physicalDeleteByBizAndLevel(bizType, bizId, OWNER);
        CrmTeamMember m = new CrmTeamMember();
        m.setBizType(bizType);
        m.setBizId(bizId);
        m.setUserId(userId);
        m.setLevel(OWNER);
        teamMapper.insert(m);
    }

    @Override
    public void saveMember(CrmTeamMember member) {
        if (member.getId() == null) {
            Long dup = teamMapper.selectCount(new LambdaQueryWrapper<CrmTeamMember>()
                    .eq(CrmTeamMember::getBizType, member.getBizType())
                    .eq(CrmTeamMember::getBizId, member.getBizId())
                    .eq(CrmTeamMember::getUserId, member.getUserId()));
            if (dup != null && dup > 0) {
                throw new BizException("该成员已存在");
            }
            teamMapper.insert(member);
        } else {
            teamMapper.updateById(member);
        }
    }

    @Override
    public void removeMember(long id) {
        CrmTeamMember m = teamMapper.selectById(id);
        if (m != null && m.getLevel() == OWNER) {
            throw new BizException("负责人不能移除，请先转移");
        }
        teamMapper.physicalDeleteById(id);
    }

    @Override
    public List<CrmTeamMember> listMembers(int bizType, long bizId) {
        return teamMapper.selectList(new LambdaQueryWrapper<CrmTeamMember>()
                .eq(CrmTeamMember::getBizType, bizType)
                .eq(CrmTeamMember::getBizId, bizId));
    }

    @Override
    public void deleteByBiz(int bizType, long bizId) {
        teamMapper.physicalDeleteByBiz(bizType, bizId);
    }

    @Override
    public List<Long> listMemberBizIds(int bizType, long userId) {
        return teamMapper.selectList(new LambdaQueryWrapper<CrmTeamMember>()
                        .eq(CrmTeamMember::getBizType, bizType)
                        .eq(CrmTeamMember::getUserId, userId))
                .stream().map(CrmTeamMember::getBizId).distinct().collect(Collectors.toList());
    }

    private String levelName(int level) {
        switch (level) {
            case OWNER: return "负责人";
            case WRITE: return "读写";
            default: return "只读";
        }
    }
}
