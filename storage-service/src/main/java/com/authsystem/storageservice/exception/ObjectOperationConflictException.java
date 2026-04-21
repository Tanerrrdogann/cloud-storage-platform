package com.authsystem.storageservice.exception;

import com.authsystem.storageservice.dto.ObjectOperationConflict;

import java.util.List;

public class ObjectOperationConflictException extends RuntimeException {

    private final List<ObjectOperationConflict> conflicts;

    public ObjectOperationConflictException(List<ObjectOperationConflict> conflicts) {
        super("Object operation has file conflicts");
        this.conflicts = conflicts;
    }

    public List<ObjectOperationConflict> getConflicts() {
        return conflicts;
    }
}
