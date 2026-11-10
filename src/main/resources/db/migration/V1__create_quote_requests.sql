CREATE TABLE quote_requests (
                                reference_number UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                first_name VARCHAR(100) NOT NULL CHECK (btrim(first_name) <> ''),
                                last_name VARCHAR(100) NOT NULL CHECK (btrim(last_name) <> ''),
                                phone VARCHAR(40) NOT NULL CHECK (btrim(phone) <> ''),
                                email VARCHAR(254) NOT NULL CHECK (btrim(email) <> ''),
                                product_id BIGINT NOT NULL CHECK (product_id > 0),
                                quantity INTEGER NOT NULL CHECK (quantity > 0),
                                fulfillment_method VARCHAR(12) NOT NULL
                                    CHECK (fulfillment_method IN ('PICKUP', 'MAILING', 'DELIVERY')),

                                company_name VARCHAR(150),
                                needed_by DATE,
                                product_options JSONB NOT NULL DEFAULT '{}'::jsonb
        CHECK (jsonb_typeof(product_options) = 'object'),
                                special_instructions VARCHAR(2000),
                                rush_requested BOOLEAN NOT NULL DEFAULT FALSE,
                                rush_requested_completion_date DATE,
                                rush_notes VARCHAR(1000),
                                marketing_opt_in BOOLEAN NOT NULL DEFAULT FALSE,

                                status VARCHAR(24) NOT NULL DEFAULT 'NEW',
                                rush_approved BOOLEAN NOT NULL DEFAULT FALSE,
                                rush_fee_amount NUMERIC,
                                created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                CONSTRAINT quote_requests_rush_details_check CHECK (
                                    rush_requested
                                        OR (rush_requested_completion_date IS NULL AND rush_notes IS NULL)
                                    ),
                                CONSTRAINT quote_requests_rush_approval_check CHECK (
                                    (NOT rush_approved AND rush_fee_amount IS NULL)
                                        OR (
                                        rush_requested AND rush_approved
                                            AND rush_fee_amount IS NOT NULL AND rush_fee_amount >= 0
                                        )
                                    )
);