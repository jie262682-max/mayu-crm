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

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.freecrm.system.entity.SysNotice;
import com.freecrm.system.mapper.SysNoticeMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/** 站内信：发送 / 我的消息 / 未读数 / 已读 */
@Service
public class NoticeService {

    private final SysNoticeMapper noticeMapper;

    public NoticeService(SysNoticeMapper noticeMapper) {
        this.noticeMapper = noticeMapper;
    }

    /** 发送站内信（bizType/bizId 供前端跳转业务单） */
    public void send(long userId, String title, String content, int type, Integer bizType, Long bizId) {
        SysNotice n = new SysNotice();
        n.setUserId(userId);
        n.setTitle(title);
        n.setContent(content);
        n.setType(type);
        n.setBizType(bizType);
        n.setBizId(bizId);
        n.setReadFlag(0);
        noticeMapper.insert(n);
    }

    public Page<SysNotice> myNotices(long pageNo, long pageSize, Boolean unreadOnly) {
        long userId = StpUtil.getLoginIdAsLong();
        return noticeMapper.selectPage(new Page<>(pageNo, pageSize), new LambdaQueryWrapper<SysNotice>()
                .eq(SysNotice::getUserId, userId)
                .eq(Boolean.TRUE.equals(unreadOnly), SysNotice::getReadFlag, 0)
                .orderByDesc(SysNotice::getId));
    }

    public long unreadCount() {
        long userId = StpUtil.getLoginIdAsLong();
        return noticeMapper.selectCount(new LambdaQueryWrapper<SysNotice>()
                .eq(SysNotice::getUserId, userId)
                .eq(SysNotice::getReadFlag, 0));
    }

    public void markRead(Long id) {
        long userId = StpUtil.getLoginIdAsLong();
        if (id == null) {
            noticeMapper.update(null, new LambdaUpdateWrapper<SysNotice>()
                    .eq(SysNotice::getUserId, userId).eq(SysNotice::getReadFlag, 0)
                    .set(SysNotice::getReadFlag, 1));
        } else {
            noticeMapper.update(null, new LambdaUpdateWrapper<SysNotice>()
                    .eq(SysNotice::getId, id).eq(SysNotice::getUserId, userId)
                    .set(SysNotice::getReadFlag, 1));
        }
    }

    /** 给一批用户发（审批人多人在用） */
    public void sendBatch(List<Long> userIds, String title, String content, int type, Integer bizType, Long bizId) {
        for (Long uid : userIds) {
            send(uid, title, content, type, bizType, bizId);
        }
    }
}
