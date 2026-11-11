/*
 * 码域CRM（MaoyuCRM）— CRM + OA + EAM 一体化系统
 * Copyright (c) 2026 合肥市码域科技 (Maoyu Technology, Hefei)
 *
 * 本程序是自由软件：您可以根据自由软件基金会发布的 GNU Affero 通用公共许可证
 * （第 3 版或任意后续版本）的条款重新分发和/或修改它。
 * 传播本程序时须保留本版权声明；商业闭源使用请联系作者获取商业授权。
 * 详见仓库根目录 LICENSE（AGPL-3.0）。
 */
package com.freecrm.crm.service;

import com.freecrm.crm.entity.CrmTeamMember;

import java.util.List;

/**
 * 团队数据权限：1负责人 2读写 3只读
 */
public interface TeamService {

    /** 校验当前登录用户对业务记录具备至少指定级别权限 */
    void checkPermission(int bizType, long bizId, int minLevel);

    /** 加负责人进团队（业务创建/领取时） */
    void addOwner(int bizType, long bizId, long userId);

    /** 添加/更新成员 */
    void saveMember(CrmTeamMember member);

    /** 移除成员 */
    void removeMember(long id);

    /** 业务记录的团队成员列表 */
    List<CrmTeamMember> listMembers(int bizType, long bizId);

    /** 业务删除时联动清理团队 */
    void deleteByBiz(int bizType, long bizId);

    /** 用户参与（任意级别）的某类业务记录 id 集合：数据可见范围 */
    List<Long> listMemberBizIds(int bizType, long userId);
}
