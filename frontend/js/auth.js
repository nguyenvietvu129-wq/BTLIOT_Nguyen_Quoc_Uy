// frontend/js/auth.js
const AUTH_API_BASE = 'http://localhost:8081';

async function checkAuth() {
    // Không chạy checkAuth nếu đang ở trang login
    if (window.location.pathname.endsWith('login.html')) {
        return true;
    }

    try {
        const response = await fetch(`${AUTH_API_BASE}/api/me`, {
            credentials: 'include'
        });
        const data = response.ok ? await response.json() : null;
        if (!response.ok || data?.authenticated !== true) {
            window.location.href = 'login.html';
            return false;
        }
        return true;
    } catch (error) {
        console.error('Lỗi kiểm tra session:', error);
        window.location.href = 'login.html';
        return false;
    }
}

// Mọi trang tải auth.js đều là trang yêu cầu đăng nhập.
document.addEventListener('DOMContentLoaded', checkAuth);

async function logout() {
    try {
        await fetch(`${AUTH_API_BASE}/api/logout`, {
            method: 'POST',
            credentials: 'include'
        });
    } catch (error) {
        console.error('Lỗi đăng xuất:', error);
    } finally {
        window.location.href = 'login.html';
    }
}
