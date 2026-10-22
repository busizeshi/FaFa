CREATE TABLE pet_breed (
    id VARCHAR(64) PRIMARY KEY,
    species VARCHAR(16) NOT NULL,
    name VARCHAR(80) NOT NULL,
    status VARCHAR(16) NOT NULL,
    version BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT uk_breed_species_name UNIQUE (species, name),
    INDEX idx_breed_species_status (species, status, id)
);
INSERT INTO pet_breed(id,species,name,status) VALUES
('CAT_UNKNOWN','CAT','未知品种','ACTIVE'),('CAT_MIXED','CAT','混种猫','ACTIVE'),
('DOG_UNKNOWN','DOG','未知品种','ACTIVE'),('DOG_MIXED','DOG','混种犬','ACTIVE'),
('OTHER_UNKNOWN','OTHER','其他或未知','ACTIVE');
CREATE TABLE pet (
    id VARCHAR(36) PRIMARY KEY,
    owner_user_id VARCHAR(36) NOT NULL,
    created_by VARCHAR(36) NOT NULL,
    nickname VARCHAR(30) NOT NULL,
    species VARCHAR(16) NOT NULL,
    breed_id VARCHAR(64),
    sex VARCHAR(16) NOT NULL,
    neutered_status VARCHAR(16) NOT NULL,
    birth_date DATE,
    estimated_birth_month VARCHAR(7),
    arrived_on DATE,
    status VARCHAR(24) NOT NULL,
    version BIGINT NOT NULL DEFAULT 1,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_pet_owner FOREIGN KEY (owner_user_id) REFERENCES user_account(id),
    CONSTRAINT fk_pet_breed FOREIGN KEY (breed_id) REFERENCES pet_breed(id),
    INDEX idx_pet_owner_status (owner_user_id,status,id)
);
CREATE TABLE user_preferences (
    user_id VARCHAR(36) PRIMARY KEY,
    current_pet_id VARCHAR(36),
    default_pet_id VARCHAR(36),
    version BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT fk_preferences_user FOREIGN KEY (user_id) REFERENCES user_account(id)
);
CREATE TABLE pet_deletion_request (
    id VARCHAR(36) PRIMARY KEY,
    pet_id VARCHAR(36) NOT NULL,
    actor_id VARCHAR(36) NOT NULL,
    previous_status VARCHAR(24) NOT NULL,
    state VARCHAR(24) NOT NULL,
    delete_after DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_deletion_pet FOREIGN KEY (pet_id) REFERENCES pet(id),
    INDEX idx_pet_deletion_state_due (state,delete_after,pet_id)
);
CREATE TABLE idempotency_request (
    actor_id VARCHAR(36) NOT NULL,
    operation VARCHAR(64) NOT NULL,
    request_key VARCHAR(128) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    response_json LONGTEXT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (actor_id,operation,request_key)
);
