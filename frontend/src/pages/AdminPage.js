import React, { useCallback, useEffect, useState } from 'react';
import DashboardLayout from '../components/DashboardLayout';
import StatCard from '../components/StatCard';
import DashboardSection from '../components/DashboardSection';
import DataTable from '../components/DataTable';
import ActionButton from '../components/ActionButton';
import StatusBadge from '../components/StatusBadge';
import { adminApi, storageApi } from '../services/api';

const GRAFANA_URL = 'http://localhost:3000/d/cloud-storage-platform-overview/cloud-storage-platform-overview';

function AdminPage() {
    const [users, setUsers] = useState([]);
    const [objects, setObjects] = useState([]);
    const [replicationTasks, setReplicationTasks] = useState([]);
    const [walEntries, setWalEntries] = useState([]);
    const [sagas, setSagas] = useState([]);
    const [asyncSummary, setAsyncSummary] = useState(null);
    const [activeTab, setActiveTab] = useState('users');
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    const fetchDashboard = useCallback(async () => {
        setError('');
        try {
            const [usersRes, asyncRes, objectsRes, replicationRes, walRes, sagasRes] = await Promise.all([
                adminApi.users(),
                adminApi.asyncSummary(),
                storageApi.allObjects(),
                storageApi.replicationTasks(),
                storageApi.walEntries(),
                storageApi.sagas()
            ]);

            setUsers(usersRes.data || []);
            setAsyncSummary(asyncRes.data || null);
            setObjects(objectsRes.data || []);
            setReplicationTasks(replicationRes.data || []);
            setWalEntries(walRes.data || []);
            setSagas(sagasRes.data || []);
        } catch (err) {
            setError(err.response?.data?.message || 'Admin dashboard data could not be loaded.');
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        fetchDashboard();
    }, [fetchDashboard]);

    const handleLogout = () => {
        localStorage.clear();
        window.location.href = '/';
    };

    const changeRole = async (userId, newRole) => {
        try {
            await adminApi.changeRole(userId, newRole);
            await fetchDashboard();
        } catch (err) {
            alert(err.response?.data?.message || 'Role could not be updated.');
        }
    };

    const suspendUser = async (userId, days) => {
        try {
            await adminApi.suspendUser(userId, days);
            await fetchDashboard();
        } catch (err) {
            alert(err.response?.data?.message || 'Moderation action failed.');
        }
    };

    const permanentDelete = async (userId) => {
        if (!window.confirm('Permanently delete this user?')) {
            return;
        }

        try {
            await adminApi.permanentDelete(userId);
            await fetchDashboard();
        } catch (err) {
            alert(err.response?.data?.message || 'Delete failed.');
        }
    };

    const activeUsers = users.filter((user) => user.enabled).length;
    const moderatorUsers = users.filter((user) => user.role === 'ROLE_MODERATOR').length;
    const dlqRisk = replicationTasks.filter((task) => task.status === 'FAILED').length;
    const storageUsed = objects.reduce((sum, object) => sum + Number(object.sizeBytes || 0), 0);
    const signedInUsers = users.filter((user) => user.lastLoginIp).length;
    const tabs = [
        ['users', 'Users'],
        ['storage', 'Storage'],
        ['async', 'Async Engine'],
        ['replication', 'Replication'],
        ['wal', 'WAL + Saga'],
        ['observability', 'Observability']
    ];

    const userColumns = [
        { key: 'id', header: 'ID' },
        { key: 'username', header: 'Username' },
        { key: 'email', header: 'Email' },
        { key: 'role', header: 'Role', render: (row) => <StatusBadge value={row.role} /> },
        { key: 'enabled', header: 'Status', render: (row) => <StatusBadge value={row.enabled ? 'ACTIVE' : 'PENDING'} /> },
        { key: 'actions', header: 'Actions', render: (row) => row.role === 'ROLE_ADMIN' ? <span className="muted">Protected</span> : (
            <div className="dashboard-actions" style={{ justifyContent: 'flex-start' }}>
                <ActionButton variant="warning" onClick={() => changeRole(row.id, 'ROLE_MODERATOR')}>Mod</ActionButton>
                <ActionButton variant="primary" onClick={() => changeRole(row.id, 'ROLE_USER')}>User</ActionButton>
                <ActionButton variant="ghost" onClick={() => suspendUser(row.id, 5)}>Suspend</ActionButton>
                <ActionButton variant="success" onClick={() => suspendUser(row.id, 0)}>Unsuspend</ActionButton>
                <ActionButton variant="danger" onClick={() => permanentDelete(row.id)}>Delete</ActionButton>
            </div>
        ) }
    ];

    const objectColumns = [
        { key: 'ownerId', header: 'Owner' },
        { key: 'objectKey', header: 'Object Key', render: (row) => <span className="mono">{row.objectKey}</span> },
        { key: 'sizeBytes', header: 'Size', render: (row) => `${Number(row.sizeBytes || 0).toLocaleString('en-US')} B` },
        { key: 'contentHash', header: 'Hash', render: (row) => <span className="mono">{String(row.contentHash || '').slice(0, 16)}...</span> },
        { key: 'status', header: 'Status', render: (row) => <StatusBadge value={row.status} /> }
    ];

    const replicationColumns = [
        { key: 'objectKey', header: 'Object' },
        { key: 'target', header: 'Target' },
        { key: 'status', header: 'Status', render: (row) => <StatusBadge value={row.status} /> },
        { key: 'retryCount', header: 'Retry' },
        { key: 'lastError', header: 'Last Error', render: (row) => row.lastError || <span className="muted">-</span> }
    ];

    const walColumns = [
        { key: 'operationId', header: 'Operation', render: (row) => <span className="mono">{String(row.operationId).slice(0, 18)}...</span> },
        { key: 'operationType', header: 'Type' },
        { key: 'objectKey', header: 'Object' },
        { key: 'status', header: 'Status', render: (row) => <StatusBadge value={row.status} /> },
        { key: 'errorMessage', header: 'Error', render: (row) => row.errorMessage || <span className="muted">-</span> }
    ];

    const sagaColumns = [
        { key: 'sagaType', header: 'Type' },
        { key: 'aggregateId', header: 'Aggregate' },
        { key: 'currentStep', header: 'Step' },
        { key: 'status', header: 'Status', render: (row) => <StatusBadge value={row.status} /> },
        { key: 'compensationRequired', header: 'Compensation', render: (row) => row.compensationRequired ? <StatusBadge value="REQUIRED" /> : <span className="muted">No</span> }
    ];

    const asyncColumns = [
        { key: 'name', header: 'Event / Consumer' },
        { key: 'count', header: 'Count' }
    ];

    return (
        <DashboardLayout
            eyebrow="ROLE_ADMIN"
            title="Platform Control Center"
            description="A clear operator view for users, storage inventory, async events, replication, WAL and saga state."
            role="ADMIN"
            navItems={tabs.map(([, label]) => label)}
            activeNavItem={tabs.find(([key]) => key === activeTab)?.[1]}
            onNavItemClick={(item) => {
                const found = tabs.find(([, label]) => label === item);
                if (found) setActiveTab(found[0]);
            }}
            actions={(
                <>
                    <a className="button button-primary" href={GRAFANA_URL} target="_blank" rel="noreferrer">Open Grafana</a>
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

            <StatCard label="Total Users" value={loading ? '...' : users.length} hint={`${activeUsers} active accounts`} />
            <StatCard label="Files Uploaded" value={objects.length} hint={`${(storageUsed / 1024 / 1024).toFixed(1)} MB stored`} />
            <StatCard label="Async Processed" value={asyncSummary?.processedMessages || 0} hint={`${asyncSummary?.failedMessages || 0} failed`} />
            <StatCard label="Replication Failures" value={dlqRisk} hint={`${moderatorUsers} moderator accounts`} />

            <DashboardSection title="Operational insights" subtitle="Readable indicators for a quick portfolio/demo review." className="span-12">
                <div className="insight-list">
                    <div className="insight">{signedInUsers} users have signed in at least once.</div>
                    <div className="insight">{objects.length} files are tracked in the storage inventory.</div>
                    <div className="insight">{asyncSummary?.processedMessages || 0} async messages were processed with idempotency protection.</div>
                    <div className="insight">{dlqRisk} replication task(s) currently need attention.</div>
                </div>
            </DashboardSection>

            <DashboardSection title="Dashboard sections" subtitle="Switch between operational views without overwhelming the page." className="span-12">
                <div className="tabs">
                    {tabs.map(([key, label]) => (
                        <button
                            key={key}
                            className={`tab ${activeTab === key ? 'tab-active' : ''}`}
                            onClick={() => setActiveTab(key)}
                        >
                            {label}
                        </button>
                    ))}
                </div>
            </DashboardSection>

            {activeTab === 'users' && <DashboardSection title="User management" subtitle="RBAC, suspension and permanent-delete controls." className="span-12">
                <DataTable columns={userColumns} rows={users} />
            </DashboardSection>}

            {activeTab === 'async' && <DashboardSection title="Async event distribution" subtitle="Real processed-message summary from the idempotency table." className="span-6">
                <DataTable columns={asyncColumns} rows={asyncSummary?.byEventType || []} emptyText="No event records yet." />
            </DashboardSection>}

            {activeTab === 'async' && <DashboardSection title="Consumer distribution" subtitle="Processed message count by consumer." className="span-6">
                <DataTable columns={asyncColumns} rows={asyncSummary?.byConsumer || []} emptyText="No consumer records yet." />
            </DashboardSection>}

            {activeTab === 'storage' && <DashboardSection title="Storage object inventory" subtitle="Object metadata, ownership and deduplication hash view." className="span-12">
                <DataTable columns={objectColumns} rows={objects} emptyText="No storage objects yet." />
            </DashboardSection>}

            {activeTab === 'replication' && <DashboardSection title="Replication tasks" subtitle="MinIO/S3 replication state and retry visibility." className="span-12">
                <DataTable columns={replicationColumns} rows={replicationTasks} emptyText="No replication tasks yet." />
            </DashboardSection>}

            {activeTab === 'wal' && <DashboardSection title="WAL entries" subtitle="Write-Ahead Log operation history." className="span-6">
                <DataTable columns={walColumns} rows={walEntries} emptyText="No WAL entries yet." />
            </DashboardSection>}

            {activeTab === 'wal' && <DashboardSection title="Saga state" subtitle="Distributed transaction and compensation tracking." className="span-6">
                <DataTable columns={sagaColumns} rows={sagas} emptyText="No saga entries yet." />
            </DashboardSection>}

            {activeTab === 'observability' && <DashboardSection title="Observability" subtitle="Deep metrics, traces and logs stay in Grafana." className="span-12">
                <div className="insight-list">
                    <div className="insight">Gateway, auth, storage and RabbitMQ metrics are scraped by Prometheus.</div>
                    <div className="insight">Tempo traces and Loki logs can be explored from the Grafana dashboard.</div>
                    <a className="button button-primary" href={GRAFANA_URL} target="_blank" rel="noreferrer">Open Grafana dashboard</a>
                </div>
            </DashboardSection>}
        </DashboardLayout>
    );
}

export default AdminPage;
