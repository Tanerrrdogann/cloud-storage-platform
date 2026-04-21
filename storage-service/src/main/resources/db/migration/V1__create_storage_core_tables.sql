CREATE TABLE object_blobs (
    id UUID PRIMARY KEY,
    content_hash VARCHAR(128) NOT NULL UNIQUE,
    storage_path VARCHAR(1024) NOT NULL,
    encrypted_size_bytes BIGINT NOT NULL,
    original_size_bytes BIGINT NOT NULL,
    encryption_algorithm VARCHAR(64) NOT NULL,
    encryption_iv VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    reference_count BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE stored_objects (
    id UUID PRIMARY KEY,
    owner_id VARCHAR(128) NOT NULL,
    object_key VARCHAR(512) NOT NULL,
    file_name VARCHAR(512) NOT NULL,
    content_type VARCHAR(255),
    size_bytes BIGINT NOT NULL,
    content_hash VARCHAR(128) NOT NULL,
    blob_id UUID NOT NULL,
    status VARCHAR(32) NOT NULL,
    version BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_stored_objects_blob
        FOREIGN KEY (blob_id)
        REFERENCES object_blobs (id),

    CONSTRAINT uk_stored_objects_owner_object_key
        UNIQUE (owner_id, object_key)
);

CREATE TABLE object_references (
    id UUID PRIMARY KEY,
    object_id UUID NOT NULL,
    blob_id UUID NOT NULL,
    owner_id VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_object_references_object
        FOREIGN KEY (object_id)
        REFERENCES stored_objects (id),

    CONSTRAINT fk_object_references_blob
        FOREIGN KEY (blob_id)
        REFERENCES object_blobs (id),

    CONSTRAINT uk_object_references_object_blob
        UNIQUE (object_id, blob_id)
);

CREATE TABLE storage_wal (
    id UUID PRIMARY KEY,
    operation_id VARCHAR(128) NOT NULL UNIQUE,
    operation_type VARCHAR(64) NOT NULL,
    object_id UUID,
    blob_id UUID,
    owner_id VARCHAR(128),
    object_key VARCHAR(512),
    status VARCHAR(32) NOT NULL,
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE replication_tasks (
    id UUID PRIMARY KEY,
    object_id UUID NOT NULL,
    blob_id UUID NOT NULL,
    target VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    retry_count INTEGER NOT NULL DEFAULT 0,
    next_retry_at TIMESTAMP WITH TIME ZONE,
    last_error TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_replication_tasks_object
        FOREIGN KEY (object_id)
        REFERENCES stored_objects (id),

    CONSTRAINT fk_replication_tasks_blob
        FOREIGN KEY (blob_id)
        REFERENCES object_blobs (id)
);

CREATE TABLE storage_sagas (
    id UUID PRIMARY KEY,
    saga_type VARCHAR(64) NOT NULL,
    aggregate_id VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    current_step VARCHAR(64) NOT NULL,
    compensation_required BOOLEAN NOT NULL DEFAULT FALSE,
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_object_blobs_content_hash
    ON object_blobs (content_hash);

CREATE INDEX idx_object_blobs_status
    ON object_blobs (status);

CREATE INDEX idx_stored_objects_owner_id
    ON stored_objects (owner_id);

CREATE INDEX idx_stored_objects_content_hash
    ON stored_objects (content_hash);

CREATE INDEX idx_stored_objects_status
    ON stored_objects (status);

CREATE INDEX idx_object_references_blob_id
    ON object_references (blob_id);

CREATE INDEX idx_storage_wal_status
    ON storage_wal (status);

CREATE INDEX idx_storage_wal_operation_type
    ON storage_wal (operation_type);

CREATE INDEX idx_replication_tasks_status
    ON replication_tasks (status);

CREATE INDEX idx_replication_tasks_next_retry_at
    ON replication_tasks (next_retry_at);

CREATE INDEX idx_storage_sagas_status
    ON storage_sagas (status);

CREATE INDEX idx_storage_sagas_aggregate_id
    ON storage_sagas (aggregate_id);
