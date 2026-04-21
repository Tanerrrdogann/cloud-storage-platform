import React, { useState } from 'react';

import API from '../services/api';

import { Link } from 'react-router-dom';



function RegisterPage() {

    const [formData, setFormData] = useState({ username: '', email: '', password: ''});    

    const [status, setStatus] = useState(false);

    const [loading, setLoading] = useState(false);

    

    const handleRegister = async () => {

        setLoading(true);

        try {

            await API.post('/auth/register', formData);

            setStatus(true);

        } catch (err) {

            const errorMsg = err.response?.data?.message || "Server is not reachable";

            alert("Registration error: " + errorMsg);

        } finally {

            setLoading(false);

        }

    };    

    if (status) {
        return (
            <div style={styles.container}>
                <div style={styles.card}>
                    <div style={styles.header}>
                        <div style={styles.successIcon}>OK</div>
                        <h2 style={styles.successTitle}>Registration complete</h2>
                        <p style={styles.subtitle}>A verification link was sent to your email address.</p>
                    </div>

                    <div style={styles.warningBox}>
                        <span style={styles.warningIcon}>i</span>
                        <p style={styles.warningText}>
                            <strong>Heads up:</strong> The verification link is valid for <strong>30 minutes</strong>.
                        </p>
                    </div>

                    <p style={styles.infoText}>Please check your inbox and spam folder.</p>

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
                    <h2 style={styles.title}>Create account</h2>
                    <p style={styles.subtitle}>Join the secure cloud storage workspace.</p>
                </div>

                <div style={styles.inputGroup}>
                    <input 
                        type="text" 
                        placeholder="Username" 
                        style={styles.input}
                        onChange={e => setFormData({...formData, username: e.target.value})} 
                    />
                </div>

                <div style={styles.inputGroup}>
                    <input 
                        type="email" 
                        placeholder="Email" 
                        style={styles.input}
                        onChange={e => setFormData({...formData, email: e.target.value})} 
                    />
                </div>

                <div style={styles.inputGroup}>
                    <input 
                        type="password" 
                        placeholder="Password" 
                        style={styles.input}
                        onChange={e => setFormData({...formData, password: e.target.value})} 
                    />
                </div>

                <button 
                    onClick={handleRegister} 
                    disabled={loading}
                    style={loading ? {...styles.button, opacity: 0.7} : styles.button}
                >
                    {loading ? "Creating..." : "Create account"}
                </button>

                <div style={styles.footer}>
                    <Link to="/" style={styles.link}>Already have an account? Sign in</Link>
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
        maxWidth: '400px',
        border: '1px solid #dbe7f3',
        textAlign: 'center',
    },
    header: {
        marginBottom: '30px',
    },
    title: {
        color: '#0f2742',
        fontSize: '26px',
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
        fontSize: '14px',
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
        marginBottom: '15px',
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
        transition: 'all 0.3s ease',
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
        transition: 'background-color 0.3s ease',
        marginTop: '10px',
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
    },
    warningBox: {
        backgroundColor: '#e0f2fe',
        border: '1px solid #bae6fd',
        borderRadius: '8px',
        padding: '12px',
        display: 'flex',
        alignItems: 'center',
        gap: '10px',
        marginBottom: '15px',
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
    infoText: {
        color: '#64748b',
        fontSize: '13px',
        marginBottom: '20px',
    },
    footer: {
        marginTop: '20px',
    },
    link: {
        textDecoration: 'none',
        color: '#0284c7',
        fontSize: '14px',
    }
};

export default RegisterPage;
