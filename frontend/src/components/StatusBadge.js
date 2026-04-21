import React from 'react';

const statusClass = (value) => {
    const normalized = String(value || '').toUpperCase();

    if (['ACTIVE', 'SUCCESS', 'PROCESSED', 'COMPLETED', 'ROLE_USER'].includes(normalized)) {
        return 'badge-success';
    }
    if (['PENDING', 'PROCESSING', 'STARTED', 'ROLE_MODERATOR'].includes(normalized)) {
        return 'badge-warning';
    }
    if (['FAILED', 'DELETED', 'GC_PENDING', 'ROLE_ADMIN'].includes(normalized)) {
        return 'badge-danger';
    }

    return 'badge-info';
};

function StatusBadge({ value }) {
    return <span className={`badge ${statusClass(value)}`}>{value || '-'}</span>;
}

export default StatusBadge;
