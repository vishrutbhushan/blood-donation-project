-- ============================================================
-- ENUM type
-- ============================================================
CREATE TYPE request_status AS ENUM ('ACTIVE', 'FULFILLED', 'NO_RESPONSE');

-- ============================================================
-- 1. search
-- ============================================================
CREATE TABLE search (
    search_id   UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    city        VARCHAR(100) NOT NULL,
    blood_group VARCHAR(3)   NOT NULL,
    lat         DECIMAL(9,6),
    lng         DECIMAL(9,6),
    searched_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- ============================================================
-- 2. otp
-- ============================================================
CREATE TABLE otp (
    otp_id      UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    phone       VARCHAR(15) NOT NULL,
    otp_code    VARCHAR(6)  NOT NULL,
    expiry_time TIMESTAMPTZ NOT NULL,
    is_verified BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_otp_phone_verified UNIQUE (phone, is_verified)
);

-- ============================================================
-- 3. user  (quoted because it's a reserved word in PostgreSQL)
-- ============================================================
CREATE TABLE "user" (
    user_id    UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name       VARCHAR(100) NOT NULL,
    phone      VARCHAR(15)  NOT NULL,
    pincode    VARCHAR(10),
    blood_group VARCHAR(3),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_user_phone UNIQUE (phone)
);

-- ============================================================
-- 4. request
-- ============================================================
CREATE TABLE request (
    req_id        UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    search_fk     UUID           NOT NULL REFERENCES search(search_id)  ON DELETE RESTRICT,
    user_fk       UUID           NOT NULL REFERENCES "user"(user_id)    ON DELETE RESTRICT,
    parent_req_fk UUID                    REFERENCES request(req_id)    ON DELETE SET NULL,
    status        request_status NOT NULL DEFAULT 'ACTIVE',
    created_at    TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_request_created_at   ON request(created_at);
CREATE INDEX idx_request_user_created ON request(user_fk, created_at);

-- ============================================================
-- 5. response
-- ============================================================
CREATE TABLE response (
    response_id  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    request_fk   UUID        NOT NULL REFERENCES request(req_id)   ON DELETE CASCADE,
    donor_fk     UUID        NOT NULL REFERENCES "user"(user_id)   ON DELETE RESTRICT,
    replied      CHAR(1)     NOT NULL CHECK (replied IN ('Y', 'N')),
    responded_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
