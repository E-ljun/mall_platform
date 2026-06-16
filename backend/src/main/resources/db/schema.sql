-- 智能商品详情与营销文案生成系统 - 初始化脚本
CREATE DATABASE IF NOT EXISTS mall_content DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE mall_content;

CREATE TABLE sys_user (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    username      VARCHAR(64)  NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    nickname      VARCHAR(64)  NOT NULL DEFAULT '',
    role          VARCHAR(16)  NOT NULL DEFAULT 'USER' COMMENT 'USER | ADMIN',
    status        TINYINT      NOT NULL DEFAULT 1 COMMENT '1=启用 0=禁用',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted       TINYINT      NOT NULL DEFAULT 0
) COMMENT '系统用户';

CREATE TABLE product_category (
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    name       VARCHAR(64) NOT NULL,
    parent_id  BIGINT      DEFAULT 0,
    sort_order INT         NOT NULL DEFAULT 0,
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted    TINYINT     NOT NULL DEFAULT 0
) COMMENT '商品分类';

CREATE TABLE product (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT         NOT NULL COMMENT '创建人',
    name            VARCHAR(128)   NOT NULL,
    price           DECIMAL(10, 2) NOT NULL DEFAULT 0,
    category_id     BIGINT         DEFAULT NULL,
    stock           INT            NOT NULL DEFAULT 0,
    status          VARCHAR(16)    NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT|ON_SHELF|OFF_SHELF',
    main_image_url  VARCHAR(512)   DEFAULT NULL,
    short_title     VARCHAR(32)    DEFAULT NULL COMMENT 'AI 精炼标题 ≤15字',
    selling_points  JSON           DEFAULT NULL COMMENT '3-5条卖点',
    detail_content  TEXT           DEFAULT NULL COMMENT '150-300字详情',
    keywords        VARCHAR(512)   DEFAULT NULL COMMENT '用户补充关键词',
    ai_analysis_raw JSON           DEFAULT NULL COMMENT '多模态分析原始结果',
    created_at      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT        NOT NULL DEFAULT 0,
    INDEX idx_product_user (user_id),
    INDEX idx_product_category (category_id),
    INDEX idx_product_status (status),
    INDEX idx_product_name (name)
) COMMENT '商品';

CREATE TABLE product_image (
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_id BIGINT       NOT NULL,
    file_path  VARCHAR(512) NOT NULL,
    file_name  VARCHAR(255) NOT NULL,
    mime_type  VARCHAR(64)  NOT NULL,
    file_size  BIGINT       NOT NULL DEFAULT 0,
    sort_order INT          NOT NULL DEFAULT 0,
    is_main    TINYINT      NOT NULL DEFAULT 0,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted    TINYINT      NOT NULL DEFAULT 0,
    INDEX idx_image_product (product_id)
) COMMENT '商品图片';

CREATE TABLE marketing_copy (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_id   BIGINT       NOT NULL,
    user_id      BIGINT       NOT NULL,
    platform     VARCHAR(16)  NOT NULL COMMENT 'XIAOHONGSHU|TAOBAO|DOUYIN',
    variant_no   INT          NOT NULL DEFAULT 1 COMMENT '同平台备选版本 1-3',
    title        VARCHAR(256) DEFAULT NULL,
    content      TEXT         NOT NULL,
    hashtags     JSON         DEFAULT NULL,
    is_favorite  TINYINT      NOT NULL DEFAULT 0,
    is_draft     TINYINT      NOT NULL DEFAULT 1,
    source       VARCHAR(16)  NOT NULL DEFAULT 'AI' COMMENT 'AI|MANUAL',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted      TINYINT      NOT NULL DEFAULT 0,
    INDEX idx_copy_product (product_id),
    INDEX idx_copy_platform (platform)
) COMMENT '营销文案';

CREATE TABLE copy_library (
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id    BIGINT      NOT NULL,
    title      VARCHAR(128) NOT NULL,
    platform   VARCHAR(16) NOT NULL,
    content    TEXT        NOT NULL,
    tags       JSON        DEFAULT NULL,
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted    TINYINT     NOT NULL DEFAULT 0,
    INDEX idx_library_user (user_id)
) COMMENT '文案库（加分项）';

CREATE TABLE ai_generation_log (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id       BIGINT       NOT NULL,
    product_id    BIGINT       DEFAULT NULL,
    task_type     VARCHAR(32)  NOT NULL COMMENT 'PRODUCT_DESC|MARKETING_COPY|DETAIL_IMAGE',
    platform      VARCHAR(16)  DEFAULT NULL,
    model         VARCHAR(64)  DEFAULT NULL,
    status        VARCHAR(16)  NOT NULL COMMENT 'SUCCESS|FAILED|RETRY',
    input_summary VARCHAR(512) DEFAULT NULL,
    error_message TEXT         DEFAULT NULL,
    duration_ms   INT          DEFAULT NULL,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) COMMENT 'AI 生成日志';

INSERT INTO product_category (name, sort_order) VALUES
('数码家电', 1),
('美妆个护', 2),
('服饰鞋包', 3),
('食品生鲜', 4),
('家居日用', 5);
