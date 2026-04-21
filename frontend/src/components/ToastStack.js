import React from 'react';

function ToastStack({ toasts }) {
    if (!toasts.length) {
        return null;
    }

    return (
        <div className="toast-stack">
            {toasts.map((toast) => (
                <div key={toast.id} className={`toast toast-${toast.type || 'info'}`}>
                    {toast.message}
                </div>
            ))}
        </div>
    );
}

export default ToastStack;
