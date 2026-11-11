# 第三方组件与来源声明

## 一、源代码原创性

本仓库全部业务代码（freecrm-common / freecrm-system / freecrm-crm / freecrm-oa / freecrm-eam /
freecrm-server / freecrm-web 页面 / freecrm-app 页面）均为本项目从零编写，未复制任何第三方
CRM/OA/ERP 系统的源代码文件。功能与业务流程（线索-客户-商机-合同-回款、审批流、资产管理等）
属于行业通用业务模式，实现均为独立设计。

## 二、脚手架来源（MIT 许可，可自由使用与修改）

| 部分 | 来源 | 许可证 |
|------|------|--------|
| freecrm-app 工程骨架 | dcloudio/uni-preset-vue（vite 分支）官方模板，其中的 main.js / App.vue / uni.scss / index.html 为模板自带文件，业务页面为本项目编写 | MIT |

## 三、开源依赖（均在最终发行物中分发，许可允许商用与再分发）

### 后端（Maven）
| 依赖 | 版本 | 许可证 |
|------|------|--------|
| Spring Boot | 2.7.18 | Apache License 2.0 |
| MyBatis-Plus | 3.5.7 | Apache License 2.0 |
| Sa-Token | 1.38.0 | Apache License 2.0（部分 MIT） |
| Hutool | 5.8.27 | MulanPSL-2.0（免费商用） |
| Lombok | 1.18.32 | MIT |
| EasyExcel | 3.3.4 | Apache License 2.0 |
| 阿里云短信 SDK dysmsapi20170525 | 3.0.0 | Apache License 2.0 |
| MySQL Connector/J | 8.0.33 | GPL-2.0 with FOSS Exception（作为未修改的独立组件使用） |

### 前端（npm/pnpm）
| 依赖 | 许可证 |
|------|--------|
| Vue 3 / vue-router / pinia | MIT |
| Element Plus | MIT |
| Axios | MIT |
| ECharts | Apache License 2.0 |
| uni-app 运行时（@dcloudio/*） | Apache License 2.0 |

以上组件的许可证均允许商业使用、修改与再分发；各许可证原文随依赖包分发，
Apache-2.0 要求保留原作者版权声明（随 jar/npm 包内 LICENSE 文件保留）。

## 四、数据与设计

- 数据库表结构（sys_* / crm_* / oa_* / eam_*）为本项目自行设计，非任何商业产品的建表脚本
- 演示数据（sql/freecrm-demo-data.sql）为虚构，与任何真实企业无关

## 五、商标提示

产品品牌为「码域CRM」（合肥市码域科技）。仓库/模块名 freecrm 仅为工程代号（目录与 Maven artifactId），
不构成对外品牌使用；对外展示名称已在界面层统一为「码域CRM」。
