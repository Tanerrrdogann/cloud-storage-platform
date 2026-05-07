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
                            Password reset instructions were sent to <strong style={{color: '#ffffff'}}>{email}</strong>.
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
        background: 'radial-gradient(circle at 18% 10%, rgba(216, 216, 216, 0.34), transparent 30%), #121212',
        fontFamily: "'Inter', sans-serif",
        padding: '20px',
    },
    card: {
        backgroundColor: '#1e1e1e',
        padding: '40px',
        borderRadius: '18px',
        boxShadow: '0 24px 70px rgba(0, 0, 0, 0.45)',
        width: '100%',
        maxWidth: '420px',
        border: '1px solid #333333',
        textAlign: 'center',
    },
    header: {
        marginBottom: '30px',
    },
    title: {
        color: '#ffffff',
        fontSize: '24px',
        margin: '0 0 10px 0',
        fontWeight: '700',
    },
    successTitle: {
        color: '#d8d8d8',
        fontSize: '24px',
        margin: '10px 0',
        fontWeight: '700',
    },
    subtitle: {
        color: '#c9c9c9',
        fontSize: '15px',
        lineHeight: '1.5',
        margin: '0',
    },
    successIcon: {
        width: '60px',
        height: '60px',
        backgroundColor: '#303030',
        color: '#d8d8d8',
        borderRadius: '50%',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        fontSize: '30px',
        margin: '0 auto 15px',
        border: '2px solid #555555',
    },
    inputGroup: {
        marginBottom: '20px',
    },
    input: {
        width: '100%',
        padding: '12px 16px',
        borderRadius: '10px',
        border: '1px solid #333333',
        backgroundColor: '#242424',
        color: '#f0f0f0',
        fontSize: '16px',
        boxSizing: 'border-box',
        outline: 'none',
        transition: 'border-color 0.15s ease, box-shadow 0.15s ease',
    },
    button: {
        width: '100%',
        padding: '12px',
        borderRadius: '10px',
        border: '1px solid rgba(216,216,216,0.08)',
        backgroundColor: '#d6d6d6',
        color: '#121212',
        fontSize: '16px',
        fontWeight: '600',
        cursor: 'pointer',
        transition: 'background-color 0.15s ease, transform 0.15s ease',
    },
    buttonSecondary: {
        width: '100%',
        padding: '12px',
        borderRadius: '10px',
        border: '1px solid #444444',
        backgroundColor: '#303030',
        color: '#eeeeee',
        fontSize: '16px',
        fontWeight: '600',
        cursor: 'pointer',
        marginTop: '10px',
    },
    warningBox: {
        backgroundColor: '#242424',
        border: '1px solid rgba(216, 216, 216, 0.28)',
        borderRadius: '10px',
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
        color: '#e6e6e6',
        fontSize: '13px',
        margin: '0',
    },
    footer: {
        marginTop: '20px',
    },
    link: {
        textDecoration: 'none',
        color: '#bdbdbd',
        fontSize: '14px',
        transition: 'color 0.3s',
    }
};

export default ForgotPasswordPage;
