CREATE TABLE IF NOT EXISTS processed_messages (
    id BIGSERIAL PRIMARY KEY,
    idempotency_key VARCHAR(200) NOT NULL,
    event_id VARCHAR(100) NOT NULL,
    processed_at TIMESTAMP NOT NULL,
    consumer_name VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL,
    CONSTRAINT uk_processed_message_idempotency_key UNIQUE (idempotency_key)
);

CREATE INDEX IF NOT EXISTS idx_processed_messages_status
    ON processed_messages(status);

CREATE INDEX IF NOT EXISTS idx_processed_messages_consumer_name
    ON processed_messages(consumer_name);
