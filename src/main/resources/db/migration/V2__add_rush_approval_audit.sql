ALTER TABLE quote_requests
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0,
    ADD CONSTRAINT quote_requests_version_check CHECK (version >= 0),
    ADD CONSTRAINT quote_requests_rush_fee_precision_check CHECK (
        rush_fee_amount IS NULL OR (
            rush_fee_amount BETWEEN 0 AND 9999999999.99
            AND scale(rush_fee_amount) <= 2
        )
    );

CREATE TABLE quote_rush_approval_audit (
    reference_number UUID NOT NULL REFERENCES quote_requests (reference_number),
    old_version BIGINT NOT NULL,
    new_version BIGINT NOT NULL,
    actor TEXT NOT NULL,
    old_rush_approved BOOLEAN NOT NULL,
    new_rush_approved BOOLEAN NOT NULL,
    old_rush_fee_amount NUMERIC(12, 2),
    new_rush_fee_amount NUMERIC(12, 2),
    changed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (reference_number, new_version),
    CONSTRAINT quote_rush_audit_actor_check CHECK (btrim(actor) <> ''),
    CONSTRAINT quote_rush_audit_version_check CHECK (
        old_version >= 0 AND new_version = old_version + 1
    ),
    CONSTRAINT quote_rush_audit_effective_change_check CHECK (
        old_rush_approved IS DISTINCT FROM new_rush_approved
        OR old_rush_fee_amount IS DISTINCT FROM new_rush_fee_amount
    )
);
