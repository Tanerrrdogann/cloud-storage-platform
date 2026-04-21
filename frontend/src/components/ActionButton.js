import React from 'react';

function ActionButton({ children, variant = 'primary', className = '', ...props }) {
    return (
        <button className={`button button-${variant} ${className}`} {...props}>
            {children}
        </button>
    );
}

export default ActionButton;
