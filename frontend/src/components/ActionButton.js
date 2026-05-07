import React from 'react';

function ActionButton({ children, variant = 'primary', className = '', ...props }) {
    const isLogout = String(children).toLowerCase() === 'sign out';
    const resolvedClassName = `${className} ${isLogout ? 'button-logout' : ''}`.trim();

    return (
        <button className={`button button-${variant} ${resolvedClassName}`} {...props}>
            {children}
        </button>
    );
}

export default ActionButton;
