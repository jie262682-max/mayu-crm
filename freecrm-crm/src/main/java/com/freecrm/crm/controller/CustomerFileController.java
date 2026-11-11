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
import com.freecrm.common.exception.BizException;
import com.freecrm.common.log.OpLog;
import com.freecrm.common.result.R;
import com.freecrm.crm.entity.CrmCustomerFile;
import com.freecrm.crm.mapper.CrmCustomerFileMapper;
import com.freecrm.crm.mapper.CrmCustomerMapper;
import com.freecrm.crm.service.TeamService;
import com.freecrm.crm.service.impl.TeamServiceImpl;
import com.freecrm.crm.service.impl.LeadServiceImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;
import java.util.UUID;

/**
 * 客户资料附件：多文件上传 / 列表 / 下载（带权限校验）/ 删除
 * 存储目录：file.upload-dir（默认 ./uploads/customer，生产随 jar 工作目录）
 */
@RestController
@RequestMapping("/api/crm/customer/file")
public class CustomerFileController {

    @Value("${file.upload-dir:uploads/customer}")
    private String uploadDir;

    private final CrmCustomerFileMapper fileMapper;
    private final CrmCustomerMapper customerMapper;
    private final TeamService teamService;

    public CustomerFileController(CrmCustomerFileMapper fileMapper, CrmCustomerMapper customerMapper,
                                  TeamService teamService) {
        this.fileMapper = fileMapper;
        this.customerMapper = customerMapper;
        this.teamService = teamService;
    }

    /** 多文件上传 */
    @PostMapping("/upload")
    @SaCheckPermission("crm:customer:save")
    @OpLog(module = "客户资料", action = "上传资料")
    public R<Integer> upload(@RequestParam Long customerId,
                             @RequestParam(value = "followUpId", required = false) Long followUpId,
                             @RequestParam("files") MultipartFile[] files) throws IOException {
        checkWrite(customerId);
        if (files == null || files.length == 0) {
            throw new BizException("请选择文件");
        }
        File dir = new File(uploadDir, String.valueOf(customerId));
        if (!dir.exists() && !dir.mkdirs()) {
            throw new BizException("创建存储目录失败：" + dir.getAbsolutePath());
        }
        for (MultipartFile f : files) {
            if (f.isEmpty()) {
                continue;
            }
            String original = f.getOriginalFilename() == null ? "file" : f.getOriginalFilename();
            String stored = UUID.randomUUID().toString().replace("-", "") +
                    (original.lastIndexOf('.') >= 0 ? original.substring(original.lastIndexOf('.')) : "");
            File target = new File(dir, stored);
            f.transferTo(target);
            CrmCustomerFile row = new CrmCustomerFile();
            row.setCustomerId(customerId);
            row.setFileName(original);
            row.setFilePath(customerId + "/" + stored);
            row.setFileSize(f.getSize());
            row.setContentType(f.getContentType());
            row.setFollowUpId(followUpId);
            fileMapper.insert(row);
        }
        return R.ok(files.length);
    }

    @GetMapping("/list")
    @SaCheckPermission("crm:customer:list")
    public R<List<CrmCustomerFile>> list(@RequestParam Long customerId) {
        checkRead(customerId);
        return R.ok(fileMapper.selectList(new LambdaQueryWrapper<CrmCustomerFile>()
                .eq(CrmCustomerFile::getCustomerId, customerId)
                .orderByDesc(CrmCustomerFile::getId)));
    }

    /** 下载（文件名中文 RFC5987 编码） */
    @GetMapping("/download")
    @SaCheckPermission("crm:customer:list")
    public ResponseEntity<FileSystemResource> download(@RequestParam Long id) throws IOException {
        CrmCustomerFile f = fileMapper.selectById(id);
        if (f == null) {
            throw new BizException("文件不存在");
        }
        checkRead(f.getCustomerId());
        File target = new File(uploadDir, f.getFilePath());
        if (!target.exists()) {
            throw new BizException("文件已丢失：" + f.getFileName());
        }
        String encoded = URLEncoder.encode(f.getFileName(), "UTF-8").replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=utf-8''" + encoded)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(target.length())
                .body(new FileSystemResource(target));
    }

    @DeleteMapping("/delete")
    @SaCheckPermission("crm:customer:save")
    @OpLog(module = "客户资料", action = "删除资料")
    public R<Void> delete(@RequestParam Long id) {
        CrmCustomerFile f = fileMapper.selectById(id);
        if (f == null) {
            throw new BizException("文件不存在");
        }
        checkWrite(f.getCustomerId());
        fileMapper.deleteById(id);
        new File(uploadDir, f.getFilePath()).delete();
        return R.ok();
    }

    private void checkRead(Long customerId) {
        teamService.checkPermission(LeadServiceImpl.BIZ_CUSTOMER, customerId, TeamServiceImpl.READ);
    }

    private void checkWrite(Long customerId) {
        teamService.checkPermission(LeadServiceImpl.BIZ_CUSTOMER, customerId, TeamServiceImpl.WRITE);
    }
}
