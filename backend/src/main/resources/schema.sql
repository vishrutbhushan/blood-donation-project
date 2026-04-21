CREATE TABLE users (
    user_id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    abha_id VARCHAR(14) UNIQUE,
    phone VARCHAR(15) UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE searches (
    search_id SERIAL PRIMARY KEY,
    user_id INT REFERENCES users(user_id) ON DELETE CASCADE,
    hospital_pincode VARCHAR(10),
    blood_group VARCHAR(20),
    blood_component VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE requests (
    request_id SERIAL PRIMARY KEY,
    search_id INT REFERENCES searches(search_id) ON DELETE CASCADE,
    
    blood_group VARCHAR(20) NOT NULL,
    component VARCHAR(20) NOT NULL,
    
    units_requested INT DEFAULT 1,
    
    number_of_donors_contacted INT DEFAULT 0,
    
    status VARCHAR(20) CHECK (status IN ('ACTIVE', 'EXPIRED', 'CLOSED')) DEFAULT 'ACTIVE',
    
    parent_request_id INT,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP DEFAULT (CURRENT_TIMESTAMP + INTERVAL '24 hours'),
    last_notified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (parent_request_id) REFERENCES requests(request_id) ON DELETE SET NULL
);

CREATE TABLE responses (
    response_id SERIAL PRIMARY KEY,
    
    request_id INT REFERENCES requests(request_id) ON DELETE CASCADE,
    
    donor_id VARCHAR(100),  -- from external source (RedCross/WHO)
    
    donor_name VARCHAR(100),
    abha_id VARCHAR(14),
    phone_number VARCHAR(15),
    
    blood_group VARCHAR(20),
    location TEXT,
    
    response_status VARCHAR(10) CHECK (response_status IN ('YES', 'NO')),
    
    responded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE blood_group_lookup (
    blood_group_code VARCHAR(20) PRIMARY KEY,
    sort_order INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE blood_component_lookup (
    blood_component_name VARCHAR(20) PRIMARY KEY,
    sort_order INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

INSERT INTO blood_group_lookup (blood_group_code, sort_order) VALUES
    ('A+', 1),
    ('A-', 2),
    ('B+', 3),
    ('B-', 4),
    ('AB+', 5),
    ('AB-', 6),
    ('O+', 7),
    ('O-', 8),
    ('BOMBAY', 9),
    ('RH NULL', 10)
ON CONFLICT (blood_group_code) DO NOTHING;

INSERT INTO blood_component_lookup (blood_component_name, sort_order) VALUES
    ('Whole Blood', 1),
    ('Packed RBC', 2),
    ('Fresh Frozen Plasma', 3),
    ('Plasma', 4),
    ('Platelets', 5),
    ('Cryoprecipitate', 6)
ON CONFLICT (blood_component_name) DO NOTHING;

ALTER TABLE searches
    ADD CONSTRAINT fk_searches_blood_group
    FOREIGN KEY (blood_group) REFERENCES blood_group_lookup(blood_group_code);

ALTER TABLE searches
    ADD CONSTRAINT fk_searches_blood_component
    FOREIGN KEY (blood_component) REFERENCES blood_component_lookup(blood_component_name);

ALTER TABLE requests
    ADD CONSTRAINT fk_requests_blood_group
    FOREIGN KEY (blood_group) REFERENCES blood_group_lookup(blood_group_code);

ALTER TABLE requests
    ADD CONSTRAINT fk_requests_component
    FOREIGN KEY (component) REFERENCES blood_component_lookup(blood_component_name);

CREATE INDEX IF NOT EXISTS idx_users_abha_id ON users(abha_id);
CREATE INDEX IF NOT EXISTS idx_searches_user_id ON searches(user_id);
CREATE INDEX IF NOT EXISTS idx_searches_blood_group ON searches(blood_group);
CREATE INDEX IF NOT EXISTS idx_searches_blood_component ON searches(blood_component);
CREATE INDEX IF NOT EXISTS idx_requests_search_id ON requests(search_id);
CREATE INDEX IF NOT EXISTS idx_requests_blood_group ON requests(blood_group);
CREATE INDEX IF NOT EXISTS idx_requests_component ON requests(component);
CREATE INDEX IF NOT EXISTS idx_requests_status_expiry ON requests(status, expires_at);
CREATE INDEX IF NOT EXISTS idx_responses_request_id ON responses(request_id);
CREATE INDEX IF NOT EXISTS idx_responses_phone_status ON responses(phone_number, response_status);
CREATE INDEX IF NOT EXISTS idx_responses_status_responded_at ON responses(response_status, responded_at);
CREATE INDEX IF NOT EXISTS idx_blood_group_lookup_order ON blood_group_lookup(sort_order, blood_group_code);
CREATE INDEX IF NOT EXISTS idx_blood_component_lookup_order ON blood_component_lookup(sort_order, blood_component_name);