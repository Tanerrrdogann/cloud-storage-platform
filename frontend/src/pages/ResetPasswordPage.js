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
        fontSize: '24px',
        margin: '0 0 10px 0',
        fontWeight: '700',
    },
    subtitle: {
        color: '#64748b',
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
    },
    errorBox: {
        marginTop: '20px',
        padding: '10px',
        backgroundColor: '#fee2e2',
        color: '#b91c1c',
        borderRadius: '8px',
        fontSize: '13px',
        border: '1px solid #fecaca',
    }
};

export default ResetPasswordPage;
