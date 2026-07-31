DROP TABLE IF EXISTS upload_file;
DROP TABLE IF EXISTS blocked_extension;

CREATE TABLE blocked_extension (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    extension        VARCHAR(20)  NOT NULL,
    -- Computed column: H2 doesn't support function-based indexes directly,
    -- so case-insensitive uniqueness is enforced via a unique index on this.
    extension_lower  VARCHAR(20)  AS (LOWER(extension)),
    type             VARCHAR(10)  NOT NULL,
    is_blocked       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Case-insensitive uniqueness enforced at the DB level (not just in the app)
-- so two concurrent inserts of e.g. "sh" and "SH" can't both succeed.
CREATE UNIQUE INDEX uq_blocked_extension_lower ON blocked_extension (extension_lower);

CREATE TABLE upload_file (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    original_filename   VARCHAR(255) NOT NULL,
    stored_filename     VARCHAR(255) NOT NULL,
    detected_extension  VARCHAR(20),
    status              VARCHAR(10)  NOT NULL,
    reject_reason       VARCHAR(255),
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
