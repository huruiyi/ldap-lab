-- Application tables: users, permission groups, and membership.
-- Compatible with both PostgreSQL and H2.

CREATE TABLE IF NOT EXISTS app_users (
    username        VARCHAR(50)  NOT NULL PRIMARY KEY,
    password        VARCHAR(100) NOT NULL,
    display_name    VARCHAR(100) DEFAULT NULL,
    enabled         BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS permission_groups (
    name        VARCHAR(50)  NOT NULL PRIMARY KEY,
    description VARCHAR(200) DEFAULT NULL
);

CREATE TABLE IF NOT EXISTS group_members (
    group_name VARCHAR(50) NOT NULL REFERENCES permission_groups(name) ON DELETE CASCADE,
    username   VARCHAR(50) NOT NULL REFERENCES app_users(username) ON DELETE CASCADE,
    PRIMARY KEY (group_name, username)
);
