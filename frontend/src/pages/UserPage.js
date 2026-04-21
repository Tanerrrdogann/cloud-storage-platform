import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import DashboardLayout from '../components/DashboardLayout';
import StatCard from '../components/StatCard';
import DashboardSection from '../components/DashboardSection';
import ActionButton from '../components/ActionButton';
import EmptyState from '../components/EmptyState';
import ToastStack from '../components/ToastStack';
import FileIcon from '../components/FileIcon';
import { authApi, storageApi } from '../services/api';

const FOLDER_MARKER = '.folder';
const DRIVE_ENTRY_MIME = 'application/x-cloud-drive-entry';

const joinPath = (...parts) => parts
    .filter(Boolean)
    .join('/')
    .replace(/\/+/g, '/')
    .replace(/^\/|\/$/g, '');

const parentPath = (path) => {
    const normalized = path.replace(/^\/|\/$/g, '');
    const slashIndex = normalized.lastIndexOf('/');
    return slashIndex === -1 ? '' : normalized.slice(0, slashIndex);
};

const baseName = (path) => path.split('/').filter(Boolean).pop() || path;
const splitName = (name) => {
    const dotIndex = name.lastIndexOf('.');
    if (dotIndex <= 0) return [name, ''];
    return [name.slice(0, dotIndex), name.slice(dotIndex)];
};
const folderMarkerKey = (path) => joinPath(path, FOLDER_MARKER);
const isFolderMarker = (key) => key === FOLDER_MARKER || key.endsWith(`/${FOLDER_MARKER}`);
const folderPathFromMarker = (key) => key.replace(new RegExp(`/?${FOLDER_MARKER}$`), '').replace(/\/$/g, '');
const parseDraggedEntry = (dataTransfer) => {
    const rawValue = dataTransfer.getData(DRIVE_ENTRY_MIME) || dataTransfer.getData('application/json');
    if (!rawValue) return null;

    try {
        return JSON.parse(rawValue);
    } catch {
        return null;
    }
};
const formatBytes = (bytes) => {
    const value = Number(bytes || 0);
    if (value < 1024) return `${value} B`;
    if (value < 1024 * 1024) return `${(value / 1024).toFixed(1)} KB`;
    if (value < 1024 * 1024 * 1024) return `${(value / 1024 / 1024).toFixed(1)} MB`;
    return `${(value / 1024 / 1024 / 1024).toFixed(1)} GB`;
};

const fileType = (fileName = '', contentType = '') => {
    const lower = fileName.toLowerCase();
    if (contentType.startsWith('image/') || /\.(png|jpg|jpeg|gif|webp|svg)$/.test(lower)) return 'image';
    if (lower.endsWith('.pdf')) return 'pdf';
    if (/\.(zip|rar|7z|tar|gz)$/.test(lower)) return 'archive';
    if (/\.(doc|docx|txt|md|rtf)$/.test(lower)) return 'document';
    if (/\.(mp4|mov|avi|mkv)$/.test(lower)) return 'video';
    return 'file';
};

function UserPage() {
    const [user, setUser] = useState(null);
    const [objects, setObjects] = useState([]);
    const [currentPath, setCurrentPath] = useState('');
    const [loading, setLoading] = useState(true);
    const [uploading, setUploading] = useState(false);
    const [menuOpen, setMenuOpen] = useState(false);
    const [contextMenu, setContextMenu] = useState(null);
    const [moveModalEntry, setMoveModalEntry] = useState(null);
    const [manualMovePath, setManualMovePath] = useState('');
    const [dragActive, setDragActive] = useState(false);
    const [dragHint, setDragHint] = useState('');
    const [activeNavItem, setActiveNavItem] = useState('My Drive');
    const [clipboard, setClipboard] = useState(null);
    const [conflictDialog, setConflictDialog] = useState(null);
    const [deleteConfirmEntry, setDeleteConfirmEntry] = useState(null);
    const [toasts, setToasts] = useState([]);
    const fileInputRef = useRef(null);
    const navigate = useNavigate();

    const notify = useCallback((message, type = 'info') => {
        const id = `${Date.now()}-${Math.random()}`;
        setToasts((items) => [...items, { id, message, type }]);
        window.setTimeout(() => {
            setToasts((items) => items.filter((item) => item.id !== id));
        }, 3400);
    }, []);

    const handleLogout = useCallback(() => {
        localStorage.clear();
        navigate('/');
    }, [navigate]);

    const fetchDashboard = useCallback(async () => {
        try {
            const [profileRes, objectsRes] = await Promise.all([
                authApi.me(),
                storageApi.myObjects()
            ]);
            setUser(profileRes.data);
            setObjects(objectsRes.data || []);
        } catch (err) {
            notify(err.response?.data?.message || 'Drive data could not be loaded.', 'error');
            if (err.response?.status === 401) {
                handleLogout();
            }
        } finally {
            setLoading(false);
        }
    }, [handleLogout, notify]);

    useEffect(() => {
        fetchDashboard();
    }, [fetchDashboard]);

    const driveEntries = useMemo(() => {
        const folders = new Map();
        const files = [];
        const prefix = currentPath ? `${currentPath}/` : '';

        objects
            .filter((object) => object.status === 'ACTIVE')
            .forEach((object) => {
                const key = object.objectKey || '';

                if (isFolderMarker(key)) {
                    const folderPath = folderPathFromMarker(key);
                    if (parentPath(folderPath) === currentPath) {
                        folders.set(folderPath, {
                            type: 'folder',
                            key: folderPath,
                            markerKey: key,
                            name: baseName(folderPath),
                            markerExists: true
                        });
                    }
                    return;
                }

                if (currentPath && !key.startsWith(prefix)) {
                    return;
                }

                const relative = currentPath ? key.slice(prefix.length) : key;
                if (!relative || relative.startsWith('/')) {
                    return;
                }

                const [firstSegment] = relative.split('/');
                if (relative.includes('/')) {
                    const folderPath = joinPath(currentPath, firstSegment);
                    folders.set(folderPath, {
                        type: 'folder',
                        key: folderPath,
                        markerKey: folderMarkerKey(folderPath),
                        name: firstSegment,
                        markerExists: folders.get(folderPath)?.markerExists || false
                    });
                    return;
                }

                files.push({
                    type: 'file',
                    key,
                    name: object.fileName || baseName(key),
                    sizeBytes: object.sizeBytes,
                    contentType: object.contentType,
                    updatedAt: object.updatedAt,
                    contentHash: object.contentHash
                });
            });

        return [
            ...Array.from(folders.values()).sort((a, b) => a.name.localeCompare(b.name)),
            ...files.sort((a, b) => a.name.localeCompare(b.name))
        ];
    }, [objects, currentPath]);

    const uploadFiles = async (files, targetPath = currentPath) => {
        const selectedFiles = Array.from(files || []);
        if (!selectedFiles.length) return;

        setUploading(true);
        try {
            for (const selectedFile of selectedFiles) {
                await storageApi.uploadObject(joinPath(targetPath, selectedFile.name), selectedFile);
            }
            if (fileInputRef.current) {
                fileInputRef.current.value = '';
            }
            notify(`${selectedFiles.length} file(s) uploaded.`, 'success');
            await fetchDashboard();
        } catch (err) {
            notify(err.response?.data?.message || 'Upload failed.', 'error');
        } finally {
            setUploading(false);
        }
    };

    const createFolder = async () => {
        const name = window.prompt('Folder name');
        if (!name || !name.trim()) return;

        try {
            await storageApi.createFolder(joinPath(currentPath, name.trim()));
            notify('Folder created.', 'success');
            await fetchDashboard();
        } catch (err) {
            notify(err.response?.data?.message || 'Folder could not be created.', 'error');
        }
    };

    const downloadFile = async (entry) => {
        try {
            const res = await storageApi.downloadObject(entry.key);
            const url = window.URL.createObjectURL(new Blob([res.data]));
            const link = document.createElement('a');
            link.href = url;
            link.setAttribute('download', entry.name);
            document.body.appendChild(link);
            link.click();
            link.remove();
            window.URL.revokeObjectURL(url);
        } catch (err) {
            notify(err.response?.data?.message || 'Download failed.', 'error');
        }
    };

    const folderHasChildren = (folderPath) => {
        const prefix = `${folderPath}/`;
        return objects.some((object) => object.status === 'ACTIVE'
            && object.objectKey.startsWith(prefix)
            && object.objectKey !== folderMarkerKey(folderPath));
    };

    const errorMessage = (err, fallback) => (
        err.response?.data?.message
        || err.response?.data?.blockedReason
        || err.response?.data?.error
        || fallback
    );

    const deleteFolderRecursive = async (entry) => {
        try {
            await storageApi.deleteFolder(entry.key, true);
            notify('Folder and contents deleted.', 'success');
            setDeleteConfirmEntry(null);
            await fetchDashboard();
        } catch (err) {
            notify(errorMessage(err, 'Folder delete failed.'), 'error');
        }
    };

    const deleteEntry = async (entry) => {
        setContextMenu(null);
        try {
            if (entry.type === 'folder') {
                if (folderHasChildren(entry.key)) {
                    setDeleteConfirmEntry(entry);
                    return;
                }
                await storageApi.deleteFolder(entry.key, false);
                notify('Folder deleted.', 'success');
            } else {
                await storageApi.deleteObject(entry.key);
                notify('File deleted.', 'success');
            }
            await fetchDashboard();
        } catch (err) {
            notify(errorMessage(err, 'Delete failed.'), 'error');
        }
    };

    const targetKeyForEntry = (entry, targetPath, targetName = entry?.name) => {
        if (!entry) return '';
        const normalizedTarget = joinPath(targetPath, targetName);
        return entry.type === 'folder' ? folderMarkerKey(normalizedTarget) : normalizedTarget;
    };

    const targetExists = (entry, targetPath, targetName = entry?.name) => {
        const targetKey = targetKeyForEntry(entry, targetPath, targetName);
        if (entry?.type === 'folder') {
            const folderPath = folderPathFromMarker(targetKey);
            return objects.some((object) => object.status === 'ACTIVE'
                && object.objectKey.startsWith(`${folderPath}/`));
        }

        return objects.some((object) => object.status === 'ACTIVE' && object.objectKey === targetKey);
    };

    const sourceKeyForEntry = (entry) => (entry?.type === 'folder' ? entry.markerKey : entry?.key);

    const copyNameForAttempt = (name, attempt, type) => {
        const suffix = attempt === 1 ? ' (copy)' : ` (copy ${attempt})`;
        if (type === 'folder') return `${name}${suffix}`;

        const [stem, extension] = splitName(name);
        return `${stem}${suffix}${extension}`;
    };

    const availableCopyName = (entry, targetPath) => {
        for (let attempt = 1; attempt < 100; attempt += 1) {
            const nextName = copyNameForAttempt(entry.name, attempt, entry.type);
            if (!targetExists(entry, targetPath, nextName)) {
                return nextName;
            }
        }

        return copyNameForAttempt(entry.name, Date.now(), entry.type);
    };

    const folderWouldMoveIntoItself = (entry, targetPath) => (
        entry?.type === 'folder'
        && targetPath
        && (targetPath === entry.key || targetPath.startsWith(`${entry.key}/`))
    );

    const clipboardLabel = clipboard
        ? `${clipboard.mode === 'cut' ? 'Cut' : 'Copied'}: ${clipboard.entry.name}`
        : '';

    const operationSuccessMessage = (mode, entry, targetPath, preview) => {
        if (mode === 'copy') {
            return entry.type === 'folder' ? 'Folder copied and merged safely.' : 'File copied.';
        }
        if (preview?.folderMerges?.length) {
            return 'Folder moved and merged safely.';
        }
        return targetPath ? `Moved to ${targetPath}.` : 'Moved to My Drive.';
    };

    const executeObjectOperation = async ({
        mode,
        entry,
        targetPath,
        targetName = entry?.name,
        fileConflictPolicy = 'FAIL',
        skipPreview = false,
        clearClipboardOnSuccess = false
    }) => {
        if (!entry) return false;
        if (folderWouldMoveIntoItself(entry, targetPath)) {
            notify(`A folder cannot be ${mode === 'copy' ? 'copied' : 'moved'} into itself.`, 'error');
            return false;
        }

        const sourceKey = sourceKeyForEntry(entry);
        const targetKey = targetKeyForEntry(entry, targetPath, targetName);

        if (mode === 'move' && sourceKey === targetKey) {
            notify('Already here.', 'info');
            setMoveModalEntry(null);
            if (clearClipboardOnSuccess) setClipboard(null);
            return true;
        }

        let preview = null;
        try {
            if (!skipPreview) {
                const previewRes = await storageApi.previewOperation(mode, sourceKey, targetKey);
                preview = previewRes.data;

                if (preview?.blockedReason) {
                    notify(preview.blockedReason, 'error');
                    return false;
                }

                if (preview?.fileConflicts?.length && fileConflictPolicy === 'FAIL') {
                    setConflictDialog({
                        mode,
                        entry,
                        targetPath,
                        targetName,
                        preview,
                        clearClipboardOnSuccess
                    });
                    return false;
                }
            }

            if (mode === 'copy') {
                await storageApi.copyObject(sourceKey, targetKey, fileConflictPolicy);
            } else {
                await storageApi.moveObject(sourceKey, targetKey, fileConflictPolicy);
            }

            notify(operationSuccessMessage(mode, entry, targetPath, preview), 'success');
            setMoveModalEntry(null);
            if (clearClipboardOnSuccess) setClipboard(null);
            await fetchDashboard();
            return true;
        } catch (err) {
            if (err.response?.status === 409 && fileConflictPolicy === 'FAIL') {
                setConflictDialog({
                    mode,
                    entry,
                    targetPath,
                    targetName,
                    preview: err.response.data,
                    clearClipboardOnSuccess
                });
                return false;
            }

            notify(errorMessage(err, `${mode === 'copy' ? 'Copy' : 'Move'} failed.`), 'error');
            return false;
        }
    };

    const resolveConflictDialog = async (fileConflictPolicy) => {
        if (!conflictDialog) return;
        const pendingOperation = conflictDialog;
        setConflictDialog(null);
        await executeObjectOperation({
            ...pendingOperation,
            fileConflictPolicy,
            skipPreview: true
        });
    };

    const renameEntry = async (entry) => {
        setContextMenu(null);
        const nextName = window.prompt('New name', entry.name);
        if (!nextName || !nextName.trim() || nextName === entry.name) return;

        await executeObjectOperation({
            mode: 'move',
            entry,
            targetPath: parentPath(entry.key),
            targetName: nextName.trim()
        });
    };

    const moveEntryToFolder = async (entry, folder) => {
        if (!entry || !folder || folder.type !== 'folder') return false;
        return executeObjectOperation({
            mode: 'move',
            entry,
            targetPath: folder.key
        });
    };

    const moveEntryToPath = async (entry, targetPath, options = {}) => {
        if (!entry) return false;
        return executeObjectOperation({
            mode: 'move',
            entry,
            targetPath,
            clearClipboardOnSuccess: Boolean(options.clearClipboardOnSuccess)
        });
    };

    const copyEntryToPath = async (entry, targetPath) => {
        if (!entry) return false;

        const sourceParentPath = parentPath(entry.key);
        const sameParentCopy = sourceParentPath === targetPath;
        const shouldAutoCopyName = sameParentCopy;
        const targetName = shouldAutoCopyName ? availableCopyName(entry, targetPath) : entry.name;

        return executeObjectOperation({
            mode: 'copy',
            entry,
            targetPath,
            targetName,
            fileConflictPolicy: shouldAutoCopyName ? 'KEEP_BOTH' : 'FAIL'
        });
    };

    const pasteEntryToPath = async (targetPath) => {
        setContextMenu(null);
        if (!clipboard) {
            notify('Clipboard is empty.', 'info');
            return;
        }

        if (clipboard.mode === 'copy') {
            await copyEntryToPath(clipboard.entry, targetPath);
            return;
        }

        await moveEntryToPath(clipboard.entry, targetPath, { clearClipboardOnSuccess: true });
    };

    const copyEntry = (entry) => {
        setClipboard({ mode: 'copy', entry });
        notify(`${entry.name} copied.`, 'success');
        setContextMenu(null);
    };

    const cutEntry = (entry) => {
        setClipboard({ mode: 'cut', entry });
        notify(`${entry.name} cut. Choose a folder and paste.`, 'success');
        setContextMenu(null);
    };

    const handleDropToPath = (event, targetPath) => {
        event.preventDefault();
        event.stopPropagation();
        setDragActive(false);
        setDragHint('');

        const source = parseDraggedEntry(event.dataTransfer);
        if (source) {
            moveEntryToPath(source, targetPath);
            return;
        }

        uploadFiles(event.dataTransfer.files, targetPath);
    };

    const handleDropToFolder = (event, folder) => {
        if (folder.type !== 'folder') return;

        event.preventDefault();
        event.stopPropagation();
        setDragActive(false);
        setDragHint('');

        const source = parseDraggedEntry(event.dataTransfer);
        if (source) {
            moveEntryToFolder(source, folder);
            return;
        }

        uploadFiles(event.dataTransfer.files, folder.key);
    };

    const manualMoveEntry = async () => {
        if (!moveModalEntry) return;
        const normalizedPath = manualMovePath.trim().replace(/^\/|\/$/g, '');
        await moveEntryToPath(moveModalEntry, normalizedPath);
        setManualMovePath('');
    };

    const openContextMenu = (event, entry = null) => {
        event.preventDefault();
        event.stopPropagation();
        setMenuOpen(false);
        setContextMenu({
            x: event.clientX,
            y: event.clientY,
            entry
        });
    };

    const closeMenus = () => {
        setMenuOpen(false);
        setContextMenu(null);
    };

    const breadcrumbs = currentPath ? currentPath.split('/') : [];
    const files = objects.filter((object) => object.status === 'ACTIVE' && !isFolderMarker(object.objectKey));
    const folders = objects.filter((object) => object.status === 'ACTIVE' && isFolderMarker(object.objectKey));
    const totalBytes = files.reduce((sum, object) => sum + Number(object.sizeBytes || 0), 0);
    const recentUploads = files.slice().sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt)).slice(0, 3);
    const folderTargets = useMemo(() => {
        const paths = new Set(['']);
        objects
            .filter((object) => object.status === 'ACTIVE')
            .forEach((object) => {
                const key = object.objectKey || '';
                if (isFolderMarker(key)) {
                    paths.add(folderPathFromMarker(key));
                    return;
                }

                const segments = key.split('/').filter(Boolean);
                for (let index = 1; index < segments.length; index += 1) {
                    paths.add(segments.slice(0, index).join('/'));
                }
            });

        return Array.from(paths).sort((a, b) => a.localeCompare(b));
    }, [objects]);

    if (loading) {
        return (
            <DashboardLayout eyebrow="Cloud Drive" title="Loading your drive" role="USER">
                <DashboardSection title="Loading" className="span-12">
                    <EmptyState text="Preparing your secure workspace..." />
                </DashboardSection>
            </DashboardLayout>
        );
    }

    return (
        <div onClick={closeMenus}>
            <DashboardLayout
                eyebrow="Cloud Drive"
                title={`${user?.username || 'My'} Drive`}
                description="Upload, organize, move and download encrypted objects without touching technical object keys."
                role="USER"
                navItems={['My Drive', 'Workspace', 'Recent uploads']}
                activeNavItem={activeNavItem}
                onNavItemClick={(item) => {
                    setActiveNavItem(item);
                    if (item === 'My Drive') {
                        setCurrentPath('');
                        document.querySelector('[data-section-title="Workspace"]')?.scrollIntoView({ behavior: 'smooth' });
                    }
                    if (item === 'Workspace') document.querySelector('[data-section-title="Workspace"]')?.scrollIntoView({ behavior: 'smooth' });
                    if (item === 'Recent uploads') document.querySelector('[data-section-title="Recent uploads"]')?.scrollIntoView({ behavior: 'smooth' });
                }}
                actions={(
                    <>
                        <div className="plus-menu" onClick={(event) => event.stopPropagation()}>
                            <ActionButton
                                variant="primary"
                                className="plus-button"
                                onClick={() => setMenuOpen((value) => !value)}
                                disabled={uploading}
                            >
                                +
                            </ActionButton>
                            {menuOpen && (
                                <div className="menu-popover">
                                    <button className="menu-item" onClick={() => fileInputRef.current?.click()}>Upload files</button>
                                    <button className="menu-item" onClick={createFolder}>New folder</button>
                                </div>
                            )}
                        </div>
                        <ActionButton variant="ghost" onClick={fetchDashboard}>Refresh</ActionButton>
                        <ActionButton variant="danger" onClick={handleLogout}>Sign out</ActionButton>
                    </>
                )}
            >
                <input
                    ref={fileInputRef}
                    type="file"
                    multiple
                    hidden
                    onChange={(event) => uploadFiles(event.target.files)}
                />

                <StatCard label="Files" value={files.length} hint="Encrypted objects" />
                <StatCard label="Folders" value={folders.length} hint="Virtual folders" />
                <StatCard label="Storage used" value={formatBytes(totalBytes)} hint="Original file size" />
                <StatCard label="Upload limit" value="250 MB" hint="Per request demo limit" />

                <DashboardSection title="Workspace" subtitle="Right-click anywhere to create folders or manage files." className="span-8">
                    <div className="drive-toolbar">
                        <div className="drive-breadcrumb">
                            <button
                                className="breadcrumb-button"
                                onClick={() => setCurrentPath('')}
                                onDragOver={(event) => {
                                    event.preventDefault();
                                    setDragHint('Move to My Drive');
                                }}
                                onDragLeave={() => setDragHint('')}
                                onDrop={(event) => {
                                    handleDropToPath(event, '');
                                }}
                            >
                                My Drive
                            </button>
                            {breadcrumbs.map((part, index) => {
                                const path = breadcrumbs.slice(0, index + 1).join('/');
                                return (
                                    <React.Fragment key={path}>
                                        <span>/</span>
                                        <button
                                            className="breadcrumb-button"
                                            onClick={() => setCurrentPath(path)}
                                            onDragOver={(event) => {
                                                event.preventDefault();
                                                setDragHint(`Move to ${path}`);
                                            }}
                                            onDragLeave={() => setDragHint('')}
                                            onDrop={(event) => {
                                                handleDropToPath(event, path);
                                            }}
                                        >
                                            {part}
                                        </button>
                                    </React.Fragment>
                                );
                            })}
                        </div>
                        <div className="drive-toolbar-actions">
                            {clipboardLabel && <span className="clipboard-pill">{clipboardLabel}</span>}
                            {currentPath && (
                                <button
                                    className="breadcrumb-button drop-target-button"
                                    onDragOver={(event) => {
                                        event.preventDefault();
                                        setDragHint(`Move to ${parentPath(currentPath) || 'My Drive'}`);
                                    }}
                                    onDragLeave={() => setDragHint('')}
                                    onDrop={(event) => handleDropToPath(event, parentPath(currentPath))}
                                    onClick={() => setCurrentPath(parentPath(currentPath))}
                                >
                                    Up one level
                                </button>
                            )}
                            {clipboard && (
                                <button
                                    className="breadcrumb-button drop-target-button"
                                    onClick={() => pasteEntryToPath(currentPath)}
                                >
                                    Paste here
                                </button>
                            )}
                            <span className="muted">{uploading ? 'Uploading...' : `${driveEntries.length} item(s)`}</span>
                        </div>
                    </div>

                    <div
                        className={`drop-zone ${dragActive ? 'drop-zone-active' : ''}`}
                        onContextMenu={(event) => openContextMenu(event)}
                        onDragOver={(event) => {
                            event.preventDefault();
                            setDragActive(true);
                            setDragHint(`Move to ${currentPath || 'My Drive'}`);
                        }}
                        onDragLeave={() => {
                            setDragActive(false);
                            setDragHint('');
                        }}
                        onDrop={(event) => {
                            handleDropToPath(event, currentPath);
                        }}
                    >
                        {driveEntries.length === 0 ? (
                            <EmptyState text="Drop your first file here or use the + button." />
                        ) : (
                            <div className="drive-grid">
                                {driveEntries.map((entry) => (
                                    <div
                                        key={`${entry.type}-${entry.key}`}
                                        className="drive-item"
                                        draggable
                                        onDragStart={(event) => {
                                            event.stopPropagation();
                                            event.dataTransfer.effectAllowed = 'move';
                                            event.dataTransfer.setData(DRIVE_ENTRY_MIME, JSON.stringify(entry));
                                            event.dataTransfer.setData('application/json', JSON.stringify(entry));
                                        }}
                                        onDragOver={(event) => {
                                            if (entry.type === 'folder') {
                                                event.preventDefault();
                                                setDragHint(`Move to ${entry.key}`);
                                            }
                                        }}
                                        onDragLeave={() => setDragHint('')}
                                        onDrop={(event) => {
                                            handleDropToFolder(event, entry);
                                        }}
                                        onContextMenu={(event) => openContextMenu(event, entry)}
                                        onDoubleClick={() => entry.type === 'folder' ? setCurrentPath(entry.key) : downloadFile(entry)}
                                    >
                                        <div>
                                            <div className="drive-icon">
                                                <FileIcon type={entry.type === 'folder' ? 'folder' : fileType(entry.name, entry.contentType)} />
                                            </div>
                                            <p className="drive-name">{entry.name}</p>
                                        </div>
                                        <p className="drive-meta">
                                            {entry.type === 'folder' ? 'Folder' : formatBytes(entry.sizeBytes)}
                                        </p>
                                    </div>
                                ))}
                            </div>
                        )}
                        {dragHint && <div className="drop-hint">{dragHint}</div>}
                    </div>
                </DashboardSection>

                <DashboardSection title="Recent uploads" subtitle="Latest encrypted objects in your drive." className="span-4">
                    <div className="insight-list">
                        {recentUploads.length ? recentUploads.map((object) => (
                            <div className="insight" key={object.id}>
                                {baseName(object.objectKey)} uploaded as <span className="mono">{object.objectKey}</span>
                            </div>
                        )) : <EmptyState text="No uploads yet." />}
                    </div>
                </DashboardSection>

                {contextMenu && (
                    <div
                        className="context-menu"
                        style={{ left: contextMenu.x, top: contextMenu.y }}
                        onClick={(event) => event.stopPropagation()}
                    >
                        {!contextMenu.entry && (
                            <>
                                <button className="menu-item" onClick={() => fileInputRef.current?.click()}>Upload here</button>
                                <button className="menu-item" onClick={createFolder}>New folder</button>
                                {clipboard && <button className="menu-item" onClick={() => pasteEntryToPath(currentPath)}>Paste here</button>}
                            </>
                        )}
                        {contextMenu.entry?.type === 'folder' && (
                            <>
                                <button className="menu-item" onClick={() => setCurrentPath(contextMenu.entry.key)}>Open folder</button>
                                <button className="menu-item" onClick={() => copyEntry(contextMenu.entry)}>Copy</button>
                                <button className="menu-item" onClick={() => cutEntry(contextMenu.entry)}>Cut</button>
                                {clipboard && <button className="menu-item" onClick={() => pasteEntryToPath(contextMenu.entry.key)}>Paste inside</button>}
                                {clipboard && <button className="menu-item" onClick={() => pasteEntryToPath(parentPath(contextMenu.entry.key))}>Paste here</button>}
                                <button className="menu-item" onClick={() => renameEntry(contextMenu.entry)}>Rename</button>
                                <button className="menu-item" onClick={() => {
                                    setMoveModalEntry(contextMenu.entry);
                                    setContextMenu(null);
                                }}>Move to...</button>
                                <button className="menu-item" onClick={() => deleteEntry(contextMenu.entry)}>Delete</button>
                            </>
                        )}
                        {contextMenu.entry?.type === 'file' && (
                            <>
                                <button className="menu-item" onClick={() => copyEntry(contextMenu.entry)}>Copy</button>
                                <button className="menu-item" onClick={() => cutEntry(contextMenu.entry)}>Cut</button>
                                {clipboard && <button className="menu-item" onClick={() => pasteEntryToPath(parentPath(contextMenu.entry.key))}>Paste here</button>}
                                <button className="menu-item" onClick={() => renameEntry(contextMenu.entry)}>Rename / Move</button>
                                <button className="menu-item" onClick={() => {
                                    setMoveModalEntry(contextMenu.entry);
                                    setContextMenu(null);
                                }}>Move to...</button>
                                <button className="menu-item" onClick={() => deleteEntry(contextMenu.entry)}>Delete</button>
                                <button className="menu-item" onClick={() => downloadFile(contextMenu.entry)}>Download</button>
                            </>
                        )}
                    </div>
                )}

                {moveModalEntry && (
                    <div className="modal-backdrop" onClick={() => setMoveModalEntry(null)}>
                        <div className="modal-card" onClick={(event) => event.stopPropagation()}>
                            <div className="section-header">
                                <div>
                                    <h2 className="section-title">Move {moveModalEntry.type === 'folder' ? 'folder' : 'file'}</h2>
                                    <p className="section-subtitle">
                                        Choose a folder, My Drive, or type a target path. Folders move together with their contents.
                                    </p>
                                </div>
                                <ActionButton variant="ghost" onClick={() => setMoveModalEntry(null)}>Close</ActionButton>
                            </div>
                            <div className="section-body">
                                <div className="move-target-list">
                                    {folderTargets.map((targetPath) => (
                                        <button
                                            key={targetPath || 'root'}
                                            className="move-target"
                                            disabled={folderWouldMoveIntoItself(moveModalEntry, targetPath)}
                                            onClick={() => moveEntryToPath(moveModalEntry, targetPath)}
                                        >
                                            <FileIcon type="folder" />
                                            <span>{targetPath || 'My Drive'}</span>
                                        </button>
                                    ))}
                                </div>

                                <div className="manual-move">
                                    <label>
                                        <span className="field-label">Custom target folder</span>
                                        <input
                                            className="input"
                                            value={manualMovePath}
                                            onChange={(event) => setManualMovePath(event.target.value)}
                                            placeholder="Example: images/wallpapers"
                                        />
                                    </label>
                                    <ActionButton variant="primary" onClick={manualMoveEntry}>Move here</ActionButton>
                                </div>
                            </div>
                        </div>
                    </div>
                )}

                {conflictDialog && (
                    <div className="modal-backdrop" onClick={() => setConflictDialog(null)}>
                        <div className="modal-card conflict-card" onClick={(event) => event.stopPropagation()}>
                            <div className="section-header">
                                <div>
                                    <h2 className="section-title">Some files already exist</h2>
                                    <p className="section-subtitle">
                                        Folders with the same name will be merged. Choose what to do with matching files.
                                    </p>
                                </div>
                                <ActionButton variant="ghost" onClick={() => setConflictDialog(null)}>Cancel</ActionButton>
                            </div>
                            <div className="section-body">
                                {conflictDialog.preview?.folderMerges?.length > 0 && (
                                    <div className="merge-note">
                                        Folder merge detected. Existing folders will stay, and new contents will be added.
                                    </div>
                                )}
                                <div className="conflict-list">
                                    {(conflictDialog.preview?.fileConflicts || []).slice(0, 6).map((conflict) => (
                                        <div className="conflict-row" key={`${conflict.sourceObjectKey}-${conflict.targetObjectKey}`}>
                                            <span className="mono">{conflict.targetObjectKey}</span>
                                            <span>already exists</span>
                                        </div>
                                    ))}
                                    {(conflictDialog.preview?.fileConflicts || []).length > 6 && (
                                        <div className="conflict-row">
                                            + {(conflictDialog.preview?.fileConflicts || []).length - 6} more file conflict(s)
                                        </div>
                                    )}
                                </div>
                                <div className="modal-actions">
                                    <ActionButton variant="primary" onClick={() => resolveConflictDialog('REPLACE')}>
                                        Replace existing files
                                    </ActionButton>
                                    <ActionButton variant="secondary" onClick={() => resolveConflictDialog('KEEP_BOTH')}>
                                        Keep both
                                    </ActionButton>
                                    <ActionButton variant="ghost" onClick={() => setConflictDialog(null)}>
                                        Cancel
                                    </ActionButton>
                                </div>
                            </div>
                        </div>
                    </div>
                )}

                {deleteConfirmEntry && (
                    <div className="modal-backdrop" onClick={() => setDeleteConfirmEntry(null)}>
                        <div className="modal-card conflict-card" onClick={(event) => event.stopPropagation()}>
                            <div className="section-header">
                                <div>
                                    <h2 className="section-title">Delete folder and contents?</h2>
                                    <p className="section-subtitle">
                                        This folder is not empty. All files and nested folders inside it will be deleted.
                                    </p>
                                </div>
                                <ActionButton variant="ghost" onClick={() => setDeleteConfirmEntry(null)}>Cancel</ActionButton>
                            </div>
                            <div className="section-body">
                                <div className="merge-note danger-note">
                                    {deleteConfirmEntry.name} will be removed recursively.
                                </div>
                                <div className="modal-actions">
                                    <ActionButton variant="danger" onClick={() => deleteFolderRecursive(deleteConfirmEntry)}>
                                        Delete everything
                                    </ActionButton>
                                    <ActionButton variant="ghost" onClick={() => setDeleteConfirmEntry(null)}>
                                        Keep folder
                                    </ActionButton>
                                </div>
                            </div>
                        </div>
                    </div>
                )}
            </DashboardLayout>
            <ToastStack toasts={toasts} />
        </div>
    );
}

export default UserPage;
