import React, { useState } from 'react';

import API from '../services/api';

import { useNavigate, Link } from 'react-router-dom';



function LoginPage() {

    const [user, setUser] = useState("");    

    const [pass, setPass] = useState("");

    const navigate = useNavigate();

    

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
                        onChange={(e) => setUser(e.target.value)} 
                        style={styles.input}
                    />
                </div>

                <div style={styles.inputGroup}>
                    <input 
                        type="password" 
                        placeholder="Password" 
                        onChange={(e) => setPass(e.target.value)} 
                        style={styles.input}
                    />
                </div>

                <button onClick={handleLogin} style={styles.button}>
                    Sign in
                </button>
                
                <div style={styles.footer}>
                    <Link to="/register" style={styles.link}>Create account</Link>
                    <span style={styles.separator}>|</span>
                    <Link to="/forgot-password" style={styles.link}>Forgot password</Link>
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
    },
    card: {
        backgroundColor: '#ffffff', 
        padding: '40px',
        borderRadius: '16px',
        boxShadow: '0 24px 60px rgba(15, 23, 42, 0.11)',
        width: '100%',
        maxWidth: '400px',
        border: '1px solid #dbe7f3',
    },
    header: {
        marginBottom: '30px',
        textAlign: 'center',
    },
    title: {
        color: '#0f2742',
        fontSize: '28px',
        margin: '0 0 10px 0',
        fontWeight: '700',
    },
    subtitle: {
        color: '#64748b',
        fontSize: '14px',
        margin: '0',
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
        transition: 'background-color 0.3s ease, transform 0.2s',
        marginBottom: '20px',
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
        color: '#0284c7',
        transition: 'color 0.3s ease',
    },
    separator: {
        margin: '0 15px',
        color: '#dbe7f3',
    }
};

export default LoginPage;
