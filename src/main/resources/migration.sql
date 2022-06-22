CREATE TABLE IF NOT EXISTS shopunit
(
    Id       varchar(36)  primary key,
    Name     varchar(200) NOT NULL,
    Type     varchar(8)   NOT NULL,
    Price    bigint       NULL,
    Date     timestamp    NOT NULL,
    ParentId varchar(36)  NULL
);

CREATE INDEX CONCURRENTLY IF NOT EXISTS "index_id_and_parentid_on_shopunit"
    on shopunit using btree (id, parentId);

CREATE INDEX CONCURRENTLY IF NOT EXISTS "date_and_type_on_shopunit"
    on shopunit using btree (date, type);