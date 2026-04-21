import React from 'react';

function DashboardSection({ title, subtitle, action, className = 'span-12', children }) {
    return (
        <section className={`dashboard-section ${className}`} data-section-title={title}>
            <div className="section-header">
                <div>
                    <h2 className="section-title">{title}</h2>
                    {subtitle && <p className="section-subtitle">{subtitle}</p>}
                </div>
                {action}
            </div>
            <div className="section-body">
                {children}
            </div>
        </section>
    );
}

export default DashboardSection;
