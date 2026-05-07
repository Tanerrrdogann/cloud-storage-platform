import axios from 'axios';

const basePath = process.env.REACT_APP_PUBLIC_BASE_PATH || '';

const API = axios.create({
    baseURL: process.env.REACT_APP_API_BASE_URL || `${basePath}/api/v1`,
    headers: {
        'Content-Type': 'application/json'
    }
});

API.interceptors.request.use((config) => {
    const token = localStorage.getItem('accessToken');
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
}, (error) => {
    return Promise.reject(error);
});

API.interceptors.response.use(
    (response) => response,
    (error) => {
        if (error.response && error.response.status === 401) {
            if (window.location.pathname !== `${basePath}/`) {
                localStorage.clear();
                window.location.href = `${basePath}/`;
            }
        }
        return Promise.reject(error);
    }
);

export const authApi = {
    me: () => API.get('/auth/me'),
    logout: (refreshToken) => API.post('/auth/logout', { refreshToken })
};

export const adminApi = {
    users: () => API.get('/admin/dashboard'),
    changeRole: (userId, newRole) => API.post(`/admin/change-role?userId=${userId}&newRole=${newRole}`),
    suspendUser: (userId, days) => API.get(`/admin/suspend?userId=${userId}&days=${days}`),
    permanentDelete: (userId) => API.delete(`/admin/permanent-delete?userId=${userId}`),
    asyncSummary: () => API.get('/admin/async-summary')
};

export const moderatorApi = {
    users: () => API.get('/moderator/dashboard'),
    suspendUser: (userId, days) => API.get(`/moderator/suspend?userId=${userId}&days=${days}`),
    activitySummary: () => API.get('/moderator/activity-summary')
};

export const storageApi = {
    myObjects: () => API.get('/objects/my'),
    allObjects: () => API.get('/objects/admin/all'),
    replicationTasks: () => API.get('/objects/admin/replication-tasks'),
    walEntries: () => API.get('/objects/admin/wal'),
    sagas: () => API.get('/objects/admin/sagas'),
    uploadObject: (objectKey, file) => {
        const formData = new FormData();
        formData.append('objectKey', objectKey);
        formData.append('file', file);

        return API.post('/objects', formData, {
            headers: {
                'Content-Type': 'multipart/form-data'
            }
        });
    },
    metadata: (objectKey) => API.get(`/objects/metadata?objectKey=${encodeURIComponent(objectKey)}`),
    downloadObject: (objectKey) => API.get(`/objects/download?objectKey=${encodeURIComponent(objectKey)}`, {
        responseType: 'blob'
    }),
    deleteObject: (objectKey) => API.delete(`/objects?objectKey=${encodeURIComponent(objectKey)}`),
    deleteFolder: (folderKey, recursive = false) => API.delete(`/objects/folders?folderKey=${encodeURIComponent(folderKey)}&recursive=${recursive}`),
    createFolder: (folderKey) => API.post('/objects/folders', { folderKey }),
    previewOperation: (operationType, sourceObjectKey, targetObjectKey) => API.post('/objects/operations/preview', {
        operationType,
        sourceObjectKey,
        targetObjectKey
    }),
    moveObject: (oldObjectKey, newObjectKey, fileConflictPolicy = 'FAIL') => API.patch('/objects/move', {
        oldObjectKey,
        newObjectKey,
        fileConflictPolicy
    }),
    copyObject: (sourceObjectKey, targetObjectKey, fileConflictPolicy = 'FAIL') => API.post('/objects/copy', {
        sourceObjectKey,
        targetObjectKey,
        fileConflictPolicy
    })
};

export default API;
