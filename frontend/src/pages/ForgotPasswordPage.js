import React, { useState } from 'react';

import API from '../services/api';

import { Link } from 'react-router-dom';



function ForgotPasswordPage() {

    const [email, setEmail] = useState("");

    const [isSent, setIsSent] = useState(false);

    

    const handleForgot = async () => {

        try {

            await API.post('/auth/forgot-password', { email: email });

            setIsSent(true);

        } catch (err) {

            const errorMsg = err.response?.data?.message || "Invalid data or request format.";

            alert("Request failed: " + errorMsg);

        }

    };  

    if (isSent) {
        return (
            <div style={styles.container}>
                <div style={styles.card}>
                    <div style={styles.header}>
                        <div style={styles.successIcon}>OK</div>
                        <h2 style={styles.successTitle}>Email sent</h2>
                        <p style={styles.subtitle}>
                            Password reset instructions were sent to <strong style={{color: '#0f2742'}}>{email}</strong>.
                        </p>
                    </div>
                    
                    <div style={styles.warningBox}>
                        <span style={styles.warningIcon}>i</span>
                        <p style={styles.warningText}>
                            The password reset link is valid for <strong>30 minutes</strong>.
                        </p>
                    </div>

                    <Link to="/" style={{ textDecoration: 'none' }}>
                        <button style={styles.buttonSecondary}>Back to sign in</button>
                    </Link>
                </div>
            </div>
        );
    }

    return (
        <div style={styles.container}>
            <div style={styles.card}>
                <div style={styles.header}>
                    <h2 style={styles.title}>Forgot password</h2>
                    <p style={styles.subtitle}>
                        Enter your account email and we will send you a reset link.
                    </p>
                </div>

                <div style={styles.inputGroup}>
                    <input 
                        type="email" 
                        placeholder="Your email address" 
                        onChange={(e) => setEmail(e.target.value)} 
                        style={styles.input}
                    />
                </div>

                <button onClick={handleForgot} style={styles.button}>
                    Send reset link
                </button>

                <div style={styles.footer}>
                    <Link to="/" style={styles.link}>Back to sign in</Link>
                </div>
            </div>
        </div>
    );
}

const styles = {
    container: {
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
        minHeight: '100vh',
        backgroundColor: '#f4f8fd',
        fontFamily: "'Inter', sans-serif",
        padding: '20px',
    },
    card: {
        backgroundColor: '#ffffff',
        padding: '40px',
        borderRadius: '16px',
        boxShadow: '0 24px 60px rgba(15, 23, 42, 0.11)',
        width: '100%',
        maxWidth: '420px',
        border: '1px solid #dbe7f3',
        textAlign: 'center',
    },
    header: {
        marginBottom: '30px',
    },
    title: {
        color: '#0f2742',
        fontSize: '24px',
        margin: '0 0 10px 0',
        fontWeight: '700',
    },
    successTitle: {
        color: '#10b981',
        fontSize: '24px',
        margin: '10px 0',
        fontWeight: '700',
    },
    subtitle: {
        color: '#64748b',
        fontSize: '15px',
        lineHeight: '1.5',
        margin: '0',
    },
    successIcon: {
        width: '60px',
        height: '60px',
        backgroundColor: '#10b98122',
        color: '#10b981',
        borderRadius: '50%',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        fontSize: '30px',
        margin: '0 auto 15px',
        border: '2px solid #10b981',
    },
    inputGroup: {
        marginBottom: '20px',
    },
    input: {
        width: '100%',
        padding: '12px 16px',
        borderRadius: '8px',
        border: '1px solid #dbe7f3',
        backgroundColor: '#ffffff',
        color: '#102033',
        fontSize: '16px',
        boxSizing: 'border-box',
        outline: 'none',
        transition: 'border-color 0.3s ease',
    },
    button: {
        width: '100%',
        padding: '12px',
        borderRadius: '8px',
        border: 'none',
        backgroundColor: '#38bdf8',
        color: 'white',
        fontSize: '16px',
        fontWeight: '600',
        cursor: 'pointer',
        transition: 'all 0.3s ease',
    },
    buttonSecondary: {
        width: '100%',
        padding: '12px',
        borderRadius: '8px',
        border: '1px solid #bae6fd',
        backgroundColor: '#eef7ff',
        color: '#075985',
        fontSize: '16px',
        fontWeight: '600',
        cursor: 'pointer',
        marginTop: '10px',
    },
    warningBox: {
        backgroundColor: '#e0f2fe',
        border: '1px solid #bae6fd',
        borderRadius: '8px',
        padding: '12px',
        display: 'flex',
        alignItems: 'center',
        gap: '10px',
        marginBottom: '25px',
        textAlign: 'left',
    },
    warningIcon: {
        fontSize: '20px',
    },
    warningText: {
        color: '#075985',
        fontSize: '13px',
        margin: '0',
    },
    footer: {
        marginTop: '20px',
    },
    link: {
        textDecoration: 'none',
        color: '#0284c7',
        fontSize: '14px',
        transition: 'color 0.3s',
    }
};

export default ForgotPasswordPage;
