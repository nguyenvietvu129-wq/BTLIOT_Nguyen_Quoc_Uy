// frontend/js/login.js
const API_BASE = 'http://localhost:8081/api';

async function hasAuthenticatedSession() {
    try {
        const response = await fetch(`${API_BASE}/me`, { credentials: 'include' });
        if (!response.ok) return false;
        const data = await response.json();
        return data.authenticated === true;
    } catch (e) {
        return false;
    }
}

document.addEventListener('DOMContentLoaded', async () => {
    const loginForm = document.getElementById('loginForm');
    const errorMessage = document.getElementById('errorMessage');

    if (window.location.protocol === 'file:') {
        errorMessage.textContent = 'Hãy mở frontend bằng Live Server tại http://localhost:5500, không mở trực tiếp file HTML.';
        errorMessage.style.display = 'block';
        return;
    }

    // Nếu đã đăng nhập trước đó, chuyển tiếp vào dashboard
    if (await hasAuthenticatedSession()) {
        window.location.href = 'dashboard.html';
        return;
    }

    if (loginForm) {
        loginForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            errorMessage.style.display = 'none';

            const username = document.getElementById('username').value.trim();
            const password = document.getElementById('password').value.trim();

            if (!username || !password) {
                errorMessage.textContent = 'Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu';
                errorMessage.style.display = 'block';
                return;
            }

            try {
                const response = await fetch(`${API_BASE}/login`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    credentials: 'include',
                    body: JSON.stringify({ username, password })
                });

                const data = await response.json();

                if (response.ok && data.success) {
                    window.location.href = 'dashboard.html';
                } else {
                    errorMessage.textContent = data.message || 'Tên đăng nhập hoặc mật khẩu không đúng.';
                    errorMessage.style.display = 'block';
                }
            } catch (error) {
                console.error('Lỗi đăng nhập:', error);
                errorMessage.textContent = 'Không thể kết nối đến máy chủ Backend (port 8081)!';
                errorMessage.style.display = 'block';
            }
        });
    }
});
