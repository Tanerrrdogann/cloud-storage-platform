import React from 'react';
import '../styles/dashboard.css';

function CloudIcon() {
    return (
        <svg className="cloud-logo" viewBox="0 0 64 64" aria-hidden="true">
            <path d="M23 48h27c7 0 12-5 12-12s-5-12-12-12h-1C47 14 39 7 29 7 18 7 10 15 9 26 4 28 1 32 1 37c0 6 5 11 12 11h10Z" />
            <path d="M22 35h20M32 25v20" />
        </svg>
    );
}

function DashboardLayout({ eyebrow, title, description, role, navItems = [], activeNavItem, onNavItemClick, actions, children }) {
    return (
        <div className="dashboard-shell">
            <aside className="dashboard-sidebar">
                <div className="dashboard-brand">
                    <div className="dashboard-brand-mark"><CloudIcon /></div>
                    <div>
                        <h2 className="dashboard-brand-title">Cloud Storage</h2>
                        <p className="dashboard-brand-subtitle">Platform Control Center</p>
                    </div>
                </div>

                <div className="dashboard-nav">
                    <div className="dashboard-nav-item">{role}</div>
                    {navItems.map((item) => (
                        <button
                            key={item}
                            className={`dashboard-nav-item dashboard-nav-button ${activeNavItem === item ? 'dashboard-nav-active' : ''}`}
                            onClick={() => onNavItemClick?.(item)}
                            type="button"
                        >
                            {item}
                        </button>
                    ))}
                </div>
            </aside>

            <main className="dashboard-content">
                <div className="dashboard-topbar">
                    <div>
                        <p className="dashboard-eyebrow">{eyebrow}</p>
                        <h1 className="dashboard-title">{title}</h1>
                        {description && <p className="dashboard-description">{description}</p>}
                    </div>
                    {actions && <div className="dashboard-actions">{actions}</div>}
                </div>

                <div className="dashboard-grid">
                    {children}
                </div>
            </main>
        </div>
    );
}

export default DashboardLayout;
