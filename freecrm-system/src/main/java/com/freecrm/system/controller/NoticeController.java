/*
 * 码域CRM（MaoyuCRM）— CRM + OA + EAM 一体化系统
 * Copyright (c) 2026 合肥市码域科技 (Maoyu Technology, Hefei)
 *
 * 本程序是自由软件：您可以根据自由软件基金会发布的 GNU Affero 通用公共许可证
 * （第 3 版或任意后续版本）的条款重新分发和/或修改它。
 * 传播本程序时须保留本版权声明；商业闭源使用请联系作者获取商业授权。
 * 详见仓库根目录 LICENSE（AGPL-3.0）。
 */
package com.freecrm.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.freecrm.common.result.R;
import com.freecrm.system.entity.SysNotice;
import com.freecrm.system.service.NoticeService;
import org.springframework.web.bind.annotation.*;

/** 站内信：未读数 / 我的消息 / 标记已读 */
@RestController
@RequestMapping("/api/notice")
public class NoticeController {

    private final NoticeService noticeService;

    public NoticeController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    @GetMapping("/unread-count")
    public R<Long> unreadCount() {
        return R.ok(noticeService.unreadCount());
    }

    @GetMapping("/page")
    public R<Page<SysNotice>> page(@RequestParam(defaultValue = "1") long pageNo,
                                   @RequestParam(defaultValue = "10") long pageSize,
                                   @RequestParam(required = false) Boolean unreadOnly) {
        return R.ok(noticeService.myNotices(pageNo, pageSize, unreadOnly));
    }

    @PutMapping("/read")
    public R<Void> markRead(@RequestParam(required = false) Long id) {
        noticeService.markRead(id);
        return R.ok();
    }
}
