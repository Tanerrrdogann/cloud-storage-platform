import React from 'react';

function FileIcon({ type = 'file' }) {
    const normalized = String(type).toLowerCase();

    if (normalized === 'folder') {
        return (
            <svg className="file-icon folder-icon" viewBox="0 0 64 64" aria-hidden="true">
                <path d="M7 18c0-4 3-7 7-7h13l6 7h17c4 0 7 3 7 7v4H7V18Z" />
                <path d="M7 26h50v22c0 4-3 7-7 7H14c-4 0-7-3-7-7V26Z" />
            </svg>
        );
    }

    const label = {
        image: 'IMG',
        pdf: 'PDF',
        video: 'VID',
        archive: 'ZIP',
        document: 'DOC',
        file: 'FILE'
    }[normalized] || 'FILE';

    return (
        <svg className={`file-icon file-icon-${normalized}`} viewBox="0 0 64 64" aria-hidden="true">
            <path className="file-icon-page" d="M15 6h24l10 10v39a4 4 0 0 1-4 4H15a4 4 0 0 1-4-4V10a4 4 0 0 1 4-4Z" />
            <path className="file-icon-fold" d="M39 6v10h10" />
            <rect className="file-icon-label" x="16" y="35" width="32" height="14" rx="4" />
            <text x="32" y="45" textAnchor="middle" fontSize="8" fontWeight="900">{label}</text>
        </svg>
    );
}

export default FileIcon;
