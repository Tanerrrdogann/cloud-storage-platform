import React from 'react';

function StatCard({ label, value, hint, className = 'span-3' }) {
    return (
        <div className={`dashboard-card ${className}`}>
            <p className="stat-label">{label}</p>
            <div className="stat-value">{value}</div>
            {hint && <p className="stat-hint">{hint}</p>}
        </div>
    );
}

export default StatCard;
