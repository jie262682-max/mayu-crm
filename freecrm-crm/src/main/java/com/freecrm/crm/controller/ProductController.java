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
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.freecrm.common.exception.BizException;
import com.freecrm.common.result.R;
import com.freecrm.crm.entity.CrmProduct;
import com.freecrm.crm.entity.CrmProductCategory;
import com.freecrm.crm.mapper.CrmProductCategoryMapper;
import com.freecrm.crm.mapper.CrmProductMapper;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 产品 + 产品分类
 */
@RestController
@RequestMapping("/api/crm/product")
public class ProductController {

    private final CrmProductMapper productMapper;
    private final CrmProductCategoryMapper categoryMapper;

    public ProductController(CrmProductMapper productMapper, CrmProductCategoryMapper categoryMapper) {
        this.productMapper = productMapper;
        this.categoryMapper = categoryMapper;
    }

    // ---- 产品 ----

    @GetMapping("/page")
    @SaCheckPermission("crm:product:list")
    public R<Page<CrmProduct>> page(@RequestParam(defaultValue = "1") long pageNo,
                                    @RequestParam(defaultValue = "10") long pageSize,
                                    @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<CrmProduct> qw = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            qw.like(CrmProduct::getName, keyword).or().like(CrmProduct::getNo, keyword);
        }
        return R.ok(productMapper.selectPage(new Page<>(pageNo, pageSize), qw.orderByDesc(CrmProduct::getId)));
    }

    /** 下拉选择用 */
    @GetMapping("/simple-list")
    public R<List<CrmProduct>> simpleList() {
        return R.ok(productMapper.selectList(new LambdaQueryWrapper<CrmProduct>()
                .eq(CrmProduct::getStatus, 1)));
    }

    @PostMapping("/save")
    @SaCheckPermission("crm:product:save")
    public R<Long> save(@RequestBody CrmProduct product) {
        Long dup = productMapper.selectCount(new LambdaQueryWrapper<CrmProduct>()
                .eq(CrmProduct::getNo, product.getNo())
                .ne(product.getId() != null, CrmProduct::getId, product.getId()));
        if (dup != null && dup > 0) {
            throw new BizException("产品编码已存在");
        }
        if (product.getId() == null) {
            productMapper.insert(product);
        } else {
            productMapper.updateById(product);
        }
        return R.ok(product.getId());
    }

    @DeleteMapping("/delete")
    @SaCheckPermission("crm:product:delete")
    public R<Void> delete(@RequestParam Long id) {
        productMapper.deleteById(id);
        return R.ok();
    }

    // ---- 分类 ----

    @GetMapping("/category/list")
    public R<List<CrmProductCategory>> listCategories() {
        return R.ok(categoryMapper.selectList(new LambdaQueryWrapper<CrmProductCategory>()
                .orderByAsc(CrmProductCategory::getSort)));
    }

    @PostMapping("/category/save")
    @SaCheckPermission("crm:product:save")
    public R<Long> saveCategory(@RequestBody CrmProductCategory category) {
        if (category.getId() == null) {
            categoryMapper.insert(category);
        } else {
            categoryMapper.updateById(category);
        }
        return R.ok(category.getId());
    }

    @DeleteMapping("/category/delete")
    @SaCheckPermission("crm:product:save")
    public R<Void> deleteCategory(@RequestParam Long id) {
        Long used = productMapper.selectCount(new LambdaQueryWrapper<CrmProduct>()
                .eq(CrmProduct::getCategoryId, id));
        if (used != null && used > 0) {
            throw new BizException("分类下有产品，不能删除");
        }
        categoryMapper.deleteById(id);
        return R.ok();
    }
}
