import React, { useCallback, useEffect, useState } from 'react';
import DashboardLayout from '../components/DashboardLayout';
import StatCard from '../components/StatCard';
import DashboardSection from '../components/DashboardSection';
import DataTable from '../components/DataTable';
import ActionButton from '../components/ActionButton';
import StatusBadge from '../components/StatusBadge';
import { moderatorApi } from '../services/api';

const GRAFANA_URL = 'http://localhost:3000/d/cloud-storage-platform-overview/cloud-storage-platform-overview';
const isDemoMode = process.env.REACT_APP_DEMO_MODE === 'true';
const basePath = process.env.REACT_APP_PUBLIC_BASE_PATH || '';

function ModeratorPage() {
    const [users, setUsers] = useState([]);
    const [activity, setActivity] = useState(null);
    const [activeNavItem, setActiveNavItem] = useState('User Moderation');
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    const fetchDashboard = useCallback(async () => {
        setError('');
        try {
            const [usersRes, activityRes] = await Promise.all([
                moderatorApi.users(),
                moderatorApi.activitySummary()
            ]);

            setUsers(usersRes.data || []);
            setActivity(activityRes.data || null);
        } catch (err) {
            setError(err.response?.data?.message || 'Moderator dashboard data could not be loaded.');
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        fetchDashboard();
    }, [fetchDashboard]);

    const handleLogout = () => {
        localStorage.clear();
        window.location.href = `${basePath}/`;
    };

    const suspendUser = async (userId, days) => {
        if (isDemoMode) {
            alert('Demo moderation is read-only. Restarting the demo restores the same sample accounts.');
            return;
        }

        try {
            await moderatorApi.suspendUser(userId, days);
            await fetchDashboard();
        } catch (err) {
            alert(err.response?.data?.message || 'Action denied.');
        }
    };

    const manageableUsers = users.filter((user) => user.role === 'ROLE_USER');

    const userColumns = [
        { key: 'id', header: 'ID' },
        { key: 'username', header: 'Username' },
        { key: 'email', header: 'Email' },
        { key: 'enabled', header: 'Status', render: (row) => <StatusBadge value={row.enabled ? 'ACTIVE' : 'PENDING'} /> },
        { key: 'lastLoginIp', header: 'Last IP', render: (row) => row.lastLoginIp || <span className="muted">-</span> },
        { key: 'actions', header: 'Moderation', render: (row) => isDemoMode ? <span className="muted">Read-only demo</span> : (
            <div className="dashboard-actions" style={{ justifyContent: 'flex-start' }}>
                <ActionButton variant="warning" onClick={() => suspendUser(row.id, 5)}>Suspend 5 days</ActionButton>
                <ActionButton variant="success" onClick={() => suspendUser(row.id, 0)}>Unsuspend</ActionButton>
            </div>
        ) }
    ];

    const recentColumns = [
        { key: 'username', header: 'Username' },
        { key: 'email', header: 'Email' },
        { key: 'role', header: 'Role', render: (row) => <StatusBadge value={row.role} /> },
        { key: 'enabled', header: 'Status', render: (row) => <StatusBadge value={row.enabled ? 'ACTIVE' : 'PENDING'} /> }
    ];

    return (
        <DashboardLayout
            eyebrow="ROLE_MODERATOR"
            title="Moderation Console"
            description="A focused operator view for account safety, user moderation and recent platform activity."
            role="MODERATOR"
            navItems={isDemoMode ? ['User Moderation', 'Recent Activity'] : ['User Moderation', 'Recent Activity', 'Observability']}
            activeNavItem={activeNavItem}
            onNavItemClick={(item) => {
                setActiveNavItem(item);
                if (item === 'Observability') {
                    window.open(GRAFANA_URL, '_blank', 'noopener,noreferrer');
                    return;
                }
                const target = item === 'Recent Activity' ? 'Recent Activity' : 'User Moderation';
                document.querySelector(`[data-section-title="${target}"]`)?.scrollIntoView({ behavior: 'smooth', block: 'start' });
            }}
            actions={(
                <>
                    {!isDemoMode && <a className="button button-primary" href={GRAFANA_URL} target="_blank" rel="noreferrer">Open Grafana</a>}
                    <ActionButton variant="ghost" onClick={fetchDashboard}>Refresh</ActionButton>
                    <ActionButton variant="danger" onClick={handleLogout}>Sign out</ActionButton>
                </>
            )}
        >
            {error && (
                <DashboardSection title="Notice" className="span-12">
                    <div className="notice">{error}</div>
                </DashboardSection>
            )}

            <StatCard label="Total Users" value={loading ? '...' : activity?.totalUsers || users.length} hint="Non-deleted accounts" />
            <StatCard label="Active Users" value={activity?.activeUsers || 0} hint="Enabled accounts" />
            <StatCard label="Suspended" value={activity?.suspendedUsers || 0} hint="Currently suspended accounts" />
            <StatCard label="Locked" value={activity?.lockedUsers || 0} hint="Brute-force locks" />

            <DashboardSection
                title="User Moderation"
                subtitle="Moderators can only suspend or unsuspend ROLE_USER accounts."
                className="span-12"
            >
                <DataTable columns={userColumns} rows={manageableUsers} emptyText="No manageable users found." />
            </DashboardSection>

            <DashboardSection
                title="Recent Activity"
                subtitle="Read-only activity summary; deeper operational tables stay in the admin panel."
                className="span-12"
            >
                <DataTable columns={recentColumns} rows={activity?.recentUsers || []} emptyText="No recent activity yet." />
            </DashboardSection>
        </DashboardLayout>
    );
}

export default ModeratorPage;
