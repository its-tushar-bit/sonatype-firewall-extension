-- SaaS Compatible

CREATE TABLE IF NOT EXISTS announcement_banner
(
    announcement_banner_id varchar(50)              NOT NULL,
    enabled                boolean                  NOT NULL DEFAULT false,
    window_id              varchar(200),
    display_from           timestamp with time zone,
    display_until          timestamp with time zone,
    message                text,
    severity               varchar(20)              NOT NULL DEFAULT 'info',
    updated_at             timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT announcement_banner_pk PRIMARY KEY (announcement_banner_id)
);

INSERT INTO announcement_banner (announcement_banner_id, enabled)
SELECT 'announcement-banner', false
WHERE NOT EXISTS (SELECT 1 FROM announcement_banner WHERE announcement_banner_id = 'announcement-banner');
