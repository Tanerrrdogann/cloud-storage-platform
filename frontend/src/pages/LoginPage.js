import React, { useState } from 'react';

import API from '../services/api';

import { useNavigate, Link } from 'react-router-dom';



function LoginPage() {

    const [user, setUser] = useState("");    

    const [pass, setPass] = useState("");

    const navigate = useNavigate();
    const isDemoMode = process.env.REACT_APP_DEMO_MODE === 'true';
    const demoAccounts = [
        { label: 'Demo User', username: 'demo.user', password: 'Demo12345!' },
        { label: 'Demo Moderator', username: 'demo.moderator', password: 'Demo12345!' },
        { label: 'Demo Admin', username: 'demo.admin', password: 'Demo12345!' }
    ];

    

    const handleLogin = async () => {
        try {
            const res = await API.post('/auth/login', {
                usernameOrEmail: user,
                password: pass
            });
        
            localStorage.setItem("accessToken", res.data.accessToken);
            localStorage.setItem("refreshToken", res.data.refreshToken);
            localStorage.setItem("role", res.data.role); 
            localStorage.setItem("username", user);

            const role = res.data.role;
            if (role === "ROLE_ADMIN") {
                navigate("/admin");
            } else if (role === "ROLE_MODERATOR") {
                navigate("/moderator");
            } else {
                navigate("/user");
            }
        } catch (err) {
            const errorMsg = err.response?.data?.message || "Login failed. Please check your credentials.";
            alert("Error: " + errorMsg);
        }
    };

    return (
        <div style={styles.container}>
            <div style={styles.card}>
                <div style={styles.header}>
                    <h1 style={styles.title}>Sign in</h1>
                    <p style={styles.subtitle}>Access your secure cloud storage workspace.</p>
                </div>

                <div style={styles.inputGroup}>
                    <input 
                        type="text" 
                        placeholder="Username or email" 
                        value={user}
                        onChange={(e) => setUser(e.target.value)} 
                        style={styles.input}
                    />
                </div>

                <div style={styles.inputGroup}>
                    <input 
                        type="password" 
                        placeholder="Password" 
                        value={pass}
                        onChange={(e) => setPass(e.target.value)} 
                        style={styles.input}
                    />
                </div>

                {isDemoMode && (
                    <div style={styles.demoPanel}>
                        {demoAccounts.map((account) => (
                            <button
                                key={account.username}
                                type="button"
                                style={styles.demoButton}
                                onClick={() => {
                                    setUser(account.username);
                                    setPass(account.password);
                                }}
                            >
                                {account.label}
                            </button>
                        ))}
                    </div>
                )}

                <button onClick={handleLogin} style={styles.button}>
                    Sign in
                </button>
                
                {!isDemoMode && (
                    <div style={styles.footer}>
                        <Link to="/register" style={styles.link}>Create account</Link>
                        <span style={styles.separator}>|</span>
                        <Link to="/forgot-password" style={styles.link}>Forgot password</Link>
                    </div>
                )}
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
        padding: '24px',
        boxSizing: 'border-box',
    },
    card: {
        backgroundColor: '#1e1e1e',
        padding: '40px',
        borderRadius: '18px',
        boxShadow: '0 24px 70px rgba(0, 0, 0, 0.45)',
        width: '100%',
        maxWidth: '400px',
        border: '1px solid #333333',
    },
    header: {
        marginBottom: '30px',
        textAlign: 'center',
    },
    title: {
        color: '#ffffff',
        fontSize: '28px',
        margin: '0 0 10px 0',
        fontWeight: '700',
    },
    subtitle: {
        color: '#c9c9c9',
        fontSize: '14px',
        margin: '0',
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
        marginBottom: '20px',
    },
    demoPanel: {
        display: 'grid',
        gridTemplateColumns: '1fr',
        gap: '8px',
        marginBottom: '18px',
    },
    demoButton: {
        padding: '10px',
        borderRadius: '10px',
        border: '1px solid rgba(216, 216, 216, 0.28)',
        backgroundColor: '#242424',
        color: '#e6e6e6',
        fontWeight: '700',
        cursor: 'pointer',
    },
    footer: {
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
        marginTop: '10px',
        fontSize: '14px',
    },
    link: {
        textDecoration: 'none',
        color: '#bdbdbd',
        transition: 'color 0.3s ease',
    },
    separator: {
        margin: '0 15px',
        color: '#555555',
    }
};

export default LoginPage;
