import React, { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import API from '../services/api';

function VerifyEmailPage() {
    const [status, setStatus] = useState("loading");
    const [searchParams] = useSearchParams();
    const navigate = useNavigate();
    const token = searchParams.get("token");
    console.log("Verification token:", token);

    useEffect(() => {
        const verify = async () => {
        if (!token) {
            setStatus("error");
            return;
        }

        try {
            const response = await API.post(`/auth/verify?token=${token}`);

            if (response.status === 200) {
                setStatus("success");
            } else {
                setStatus("error");
            }
        } catch (err) {
            console.error("Verification error:", err);
            setStatus("error");
        }
    };

        verify();
    }, [token]);

    return (
        <div style={styles.container}>
            <div style={styles.card}>
                {status === "loading" && (
                    <div style={styles.stateWrapper}>
                        <div style={styles.spinner}></div>
                        <h2 style={styles.title}>Verifying...</h2>
                        <p style={styles.subtitle}>Your account is being activated. Please wait.</p>
                    </div>
                )}

                {status === "success" && (
                    <div style={styles.stateWrapper}>
                        <div style={styles.successIcon}>OK</div>
                        <h2 style={styles.successTitle}>Account verified</h2>
                        <p style={styles.subtitle}>Your account is active. You can sign in now.</p>
                        <button onClick={() => navigate("/")} style={styles.button}>
                            Sign in
                        </button>
                    </div>
                )}

                {status === "error" && (
                    <div style={styles.stateWrapper}>
                        <div style={styles.errorIcon}>!</div>
                        <h2 style={styles.errorTitle}>Verification failed</h2>
                        <p style={styles.subtitle}>The token is invalid, expired or malformed.</p>
                        <button onClick={() => navigate("/register")} style={styles.buttonSecondary}>
                            Register again
                        </button>
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
        padding: '20px',
    },
    card: {
        backgroundColor: '#1e1e1e',
        padding: '40px',
        borderRadius: '18px',
        boxShadow: '0 24px 70px rgba(0, 0, 0, 0.45)',
        width: '100%',
        maxWidth: '400px',
        border: '1px solid #333333',
        textAlign: 'center',
    },
    stateWrapper: {
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
    },
    title: {
        color: '#ffffff',
        fontSize: '24px',
        margin: '20px 0 10px 0',
    },
    successTitle: {
        color: '#d8d8d8',
        fontSize: '24px',
        margin: '20px 0 10px 0',
        fontWeight: '700',
    },
    errorTitle: {
        color: '#d8d8d8',
        fontSize: '24px',
        margin: '20px 0 10px 0',
        fontWeight: '700',
    },
    subtitle: {
        color: '#c9c9c9',
        fontSize: '15px',
        lineHeight: '1.5',
        marginBottom: '30px',
    },
    spinner: {
        width: '50px',
        height: '50px',
        border: '4px solid #303030',
        borderTop: '4px solid #bdbdbd',
        borderRadius: '50%',
        animation: 'spin 1s linear infinite',
    },
    successIcon: {
        width: '70px',
        height: '70px',
        backgroundColor: '#303030',
        color: '#d8d8d8',
        borderRadius: '50%',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        fontSize: '35px',
        border: '2px solid #555555',
    },
    errorIcon: {
        width: '70px',
        height: '70px',
        backgroundColor: '#303030',
        color: '#d8d8d8',
        borderRadius: '50%',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        fontSize: '35px',
        border: '2px solid #555555',
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
        transition: 'background-color 0.15s ease',
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
    }
};

export default VerifyEmailPage;
