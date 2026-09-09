CREATE DATABASE IF NOT EXISTS liveticket
DEFAULT CHARACTER SET utf8mb4
COLLATE utf8mb4_0900_ai_ci;

USE liveticket;

CREATE TABLE lt_user (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(32) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    nickname VARCHAR(32) NOT NULL,
    avatar_url VARCHAR(255) DEFAULT NULL,
    city_code VARCHAR(20) DEFAULT NULL,
    longitude DECIMAL(10,6) DEFAULT NULL,
    latitude DECIMAL(10,6) DEFAULT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB;

CREATE TABLE lt_event (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(128) NOT NULL,
    artist VARCHAR(128) NOT NULL,
    category VARCHAR(32) NOT NULL,
    description TEXT NOT NULL,
    city_code VARCHAR(20) NOT NULL,
    venue_name VARCHAR(128) NOT NULL,
    address VARCHAR(255) NOT NULL,
    longitude DECIMAL(10,6) NOT NULL,
    latitude DECIMAL(10,6) NOT NULL,
    cover_url VARCHAR(255) NOT NULL,
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    sale_start_time DATETIME NOT NULL,
    sale_end_time DATETIME NOT NULL,
    status TINYINT NOT NULL,
    heat_score BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_city_status (city_code, status),
    KEY idx_category_status (category, status),
    KEY idx_start_time (start_time),
    KEY idx_heat_score (heat_score)
) ENGINE=InnoDB;

CREATE TABLE lt_ticket_sku (
    id BIGINT NOT NULL AUTO_INCREMENT,
    event_id BIGINT NOT NULL,
    sku_name VARCHAR(64) NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    total_stock INT NOT NULL,
    available_stock INT NOT NULL,
    per_user_limit INT NOT NULL DEFAULT 1,
    status TINYINT NOT NULL DEFAULT 1,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_event_id (event_id)
) ENGINE=InnoDB;

CREATE TABLE lt_order (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_no VARCHAR(32) NOT NULL,
    user_id BIGINT NOT NULL,
    event_id BIGINT NOT NULL,
    ticket_sku_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    status TINYINT NOT NULL,
    source TINYINT NOT NULL,
    purchase_guard VARCHAR(80) DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    paid_at DATETIME DEFAULT NULL,
    cancelled_at DATETIME DEFAULT NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    UNIQUE KEY uk_purchase_guard (purchase_guard),
    KEY idx_user_status_created (user_id, status, created_at),
    KEY idx_ticket_sku_id (ticket_sku_id)
) ENGINE=InnoDB;

CREATE TABLE lt_follow (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    follow_user_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_follow (user_id, follow_user_id),
    KEY idx_follow_user (follow_user_id)
) ENGINE=InnoDB;

CREATE TABLE lt_post (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    content VARCHAR(1000) NOT NULL,
    event_id BIGINT DEFAULT NULL,
    image_url VARCHAR(255) DEFAULT NULL,
    like_count INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_user_created (user_id, created_at)
) ENGINE=InnoDB;

CREATE TABLE lt_post_like (
    id BIGINT NOT NULL AUTO_INCREMENT,
    post_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_post_user (post_id, user_id),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB;

CREATE TABLE lt_checkin (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    event_id BIGINT NOT NULL,
    checkin_date DATE NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_event (user_id, event_id),
    KEY idx_user_date (user_id, checkin_date)
) ENGINE=InnoDB;

CREATE TABLE lt_mq_consume_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    message_id VARCHAR(64) NOT NULL,
    business_type VARCHAR(32) NOT NULL,
    business_id VARCHAR(64) NOT NULL,
    consume_status TINYINT NOT NULL DEFAULT 0,
    retry_count INT NOT NULL DEFAULT 0,
    last_error VARCHAR(500) DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_message_id (message_id)
) ENGINE=InnoDB;
