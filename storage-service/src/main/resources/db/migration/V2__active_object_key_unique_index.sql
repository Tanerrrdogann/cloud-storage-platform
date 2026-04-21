ALTER TABLE stored_objects
    DROP CONSTRAINT IF EXISTS uk_stored_objects_owner_object_key;

CREATE UNIQUE INDEX IF NOT EXISTS uk_stored_objects_owner_active_object_key
    ON stored_objects (owner_id, object_key)
    WHERE status = 'ACTIVE';
