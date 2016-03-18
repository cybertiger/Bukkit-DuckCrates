DROP TABLE IF EXISTS claimed;
CREATE TABLE claimed (
        uuid varchar(36) NOT NULL,
        crate_type varchar(200) NOT NULL,
        amount int NOT NULL,
        PRIMARY KEY (uuid, crate_type)
);

DROP TABLE IF EXISTS last_daily;
CREATE TABLE last_daily (
        uuid varchar(36) PRIMARY KEY NOT NULL,
        claim_time BIGINT NOT NULL
);

DROP TABLE IF EXISTS version;
CREATE TABLE version (
        id INT PRIMARY KEY NOT NULL
);
INSERT INTO version VALUES(1);
