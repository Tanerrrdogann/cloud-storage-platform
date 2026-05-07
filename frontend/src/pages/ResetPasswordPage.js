import React, { useState } from 'react';

import API from '../services/api';

import { useNavigate, useSearchParams } from 'react-router-dom';



function ResetPasswordPage() {

    const [newPassword, setNewPassword] = useState(""); 

    const [searchParams] = useSearchParams();

    const navigate = useNavigate();



    const token = searchParams.get("token");

    

    const handleReset = async () => {

        if (!token) {

            alert("Token was not found.");

            return;

        }



        if (newPassword.length < 6) {

            alert("Password must be at least 6 characters.");

            return;

        }



        try {

            await API.post('/auth/reset-password', {

                token: token,

                newPassword: newPassword

            });

            

            alert("Your password was changed successfully.");

            navigate("/");

        } catch (err) {

            const errorMsg = err.response?.data?.message || "Invalid token or request format.";

            alert("Request failed: " + errorMsg);

        }

    };    

    return (
        <div style={styles.container}>
            <div style={styles.card}>
                <div style={styles.header}>
                    <h2 style={styles.title}>Set new password</h2>
                    <p style={styles.subtitle}>
                        Choose a strong password with at least 6 characters.
                    </p>
                </div>

                <div style={styles.inputGroup}>
                    <input 
                        type="password" 
                        placeholder="New password" 
                        onChange={(e) => setNewPassword(e.target.value)} 
                        style={styles.input}
                    />
                </div>

                <button onClick={handleReset} style={styles.button}>
                    Update password
                </button>

                {!token && (
                    <div style={styles.errorBox}>
                        A valid reset token was not found.
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
    header: {
        marginBottom: '30px',
    },
    title: {
        color: '#ffffff',
        fontSize: '24px',
        margin: '0 0 10px 0',
        fontWeight: '700',
    },
    subtitle: {
        color: '#c9c9c9',
        fontSize: '14px',
        lineHeight: '1.5',
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
        transition: 'background-color 0.15s ease',
    },
    errorBox: {
        marginTop: '20px',
        padding: '10px',
        backgroundColor: '#2b1d1f',
        color: '#b91c1c',
        borderRadius: '10px',
        fontSize: '13px',
        border: '1px solid #fecaca',
    }
};

export default ResetPasswordPage;
