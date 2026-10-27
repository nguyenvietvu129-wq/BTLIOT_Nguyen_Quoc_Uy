// frontend/js/active-history.js

const API_BASE = 'http://localhost:8081/api';

let currentPage = 1;
let currentLimit = 10;
let currentSortKey = 'executedAt';
let currentSortDir = 'DESC';
let currentDeviceName = '';
let currentUsername = '';
let currentAction = '';
let currentStatus = '';
let currentExecutedAt = '';

function formatDateTime(dt) {
    if (!dt) return '--';
    if (typeof dt === 'string') {
        return dt.replace('T', ' ').substring(0, 19);
    }
    if (Array.isArray(dt)) {
        const pad = n => String(n).padStart(2, '0');
        return `${dt[0]}-${pad(dt[1])}-${pad(dt[2])} ${pad(dt[3] || 0)}:${pad(dt[4] || 0)}:${pad(dt[5] || 0)}`;
    }
    return String(dt);
}

function onPageSizeChange() {
    currentLimit = parseInt(document.getElementById('pageSizeSelect').value, 10);
    currentPage = 1;
    loadHistory();
}

function sortBy(key) {
    if (currentSortKey === key) {
        currentSortDir = currentSortDir === 'DESC' ? 'ASC' : 'DESC';
    } else {
        currentSortKey = key;
        currentSortDir = 'DESC';
    }
    currentPage = 1;
    loadHistory();
}

function doSearch() {
    const deviceNameEl = document.getElementById('searchDeviceName');
    const usernameEl = document.getElementById('searchUsername');
    const actionEl = document.getElementById('searchAction');
    const statusEl = document.getElementById('searchStatus');
    const executedAtEl = document.getElementById('searchExecutedAt');

    currentDeviceName = deviceNameEl ? deviceNameEl.value.trim() : '';
    currentUsername   = usernameEl ? usernameEl.value.trim() : '';
    currentAction     = actionEl ? actionEl.value.trim() : '';
    currentStatus     = statusEl ? statusEl.value.trim() : '';
    currentExecutedAt = executedAtEl ? executedAtEl.value.trim() : '';

    currentPage = 1;
    loadHistory();
}

function resetSearch() {
    const ids = ['searchDeviceName', 'searchUsername', 'searchAction', 'searchStatus', 'searchExecutedAt'];
    ids.forEach(id => {
        const el = document.getElementById(id);
        if (el) el.value = '';
    });

    currentDeviceName = '';
    currentUsername = '';
    currentAction = '';
    currentStatus = '';
    currentExecutedAt = '';
    currentPage = 1;
    loadHistory();
}

let historyRequest = 0;
let historyTimer;
let hasPendingHistory = false;

function escapeHtml(value) {
    return String(value).replace(/[&<>"']/g, c => ({'&':'&amp;', '<':'&lt;', '>':'&gt;', '"':'&quot;', "'":'&#39;'}[c]));
}

async function loadHistory(quiet = false) {
    clearTimeout(historyTimer);
    const requestId = ++historyRequest;
    const tbody = document.getElementById('historyTableBody');
    if (!tbody) return;

    if (!quiet) tbody.innerHTML = '<tr><td colspan="6" style="text-align:center;color:#4a5a6a;">Đang tải...</td></tr>';

    try {
        const params = new URLSearchParams({
            page: currentPage,
            limit: currentLimit,
            sortKey: currentSortKey,
            sortDir: currentSortDir
        });

        if (currentDeviceName) params.append('deviceName', currentDeviceName);
        if (currentUsername) params.append('username', currentUsername);
        if (currentAction) params.append('action', currentAction);
        if (currentStatus) params.append('status', currentStatus);
        if (currentExecutedAt) params.append('executedAt', currentExecutedAt);

        const response = await fetch(`${API_BASE}/activehistories/search?${params.toString()}`, {
            credentials: 'include'
        });

        if (requestId !== historyRequest) return;
        if (!response.ok) {
            tbody.innerHTML = `<tr><td colspan="6" style="text-align:center;color:#f85149;">Lỗi khi tải lịch sử (HTTP ${response.status})</td></tr>`;
            return;
        }

        const result = await response.json();
        if (requestId !== historyRequest) return;
        const pageData = result.data || [];
        hasPendingHistory = pageData.some(row => row.status === 'LOADING');
        const pagination = result.pagination || { total: 0, totalPages: 1, page: 1, limit: currentLimit };

        const total = pagination.total;
        const totalPages = pagination.totalPages || 1;
        currentPage = pagination.page;

        tbody.innerHTML = '';
        if (pageData.length > 0) {
            const startIdx = (currentPage - 1) * currentLimit;
            pageData.forEach((row, index) => {
                const stt = startIdx + index + 1;
                const deviceName = escapeHtml(row.deviceName || 'N/A');
                const username   = escapeHtml(row.username || 'N/A');
                const action     = escapeHtml(row.action || '--');
                const status     = escapeHtml(row.status || '--');
                const executedAt = formatDateTime(row.executedAt);

                const isOn = action.toUpperCase() === 'ON';
                const statusClass = ({LOADING: 'badge-loading', SUCCESS: 'badge-success', FAILURE: 'badge-failure'})[status] || 'badge-legacy';
                const tr = document.createElement('tr');
                tr.innerHTML = `
                    <td>${stt}</td>
                    <td>${deviceName}</td>
                    <td>${username}</td>
                    <td><span class="badge ${isOn ? 'badge-on' : 'badge-off'}">${action}</span></td>
                    <td><span class="badge ${statusClass}">${status === 'LOADING' ? '<span class="status-spinner" aria-hidden="true"></span> ' : ''}${status}</span></td>
                `;
                tr.appendChild(createTimeCell(executedAt));
                tbody.appendChild(tr);
            });
        } else {
            tbody.innerHTML = '<tr><td colspan="6" style="text-align:center;color:#4a5a6a;">Không tìm thấy lịch sử</td></tr>';
        }

        renderPagination(total, totalPages, currentPage, currentLimit);

    } catch (error) {
        if (requestId !== historyRequest) return;
        console.error('Lỗi loadHistory:', error);
        tbody.innerHTML = '<tr><td colspan="6" style="text-align:center;color:#f85149;">Không thể kết nối đến máy chủ Backend!</td></tr>';
    } finally {
        if (requestId === historyRequest && !document.hidden) {
            historyTimer = setTimeout(() => loadHistory(true), hasPendingHistory ? 1000 : 5000);
        }
    }
}

function renderPagination(total, totalPages, page, limit) {
    const recordCountEl = document.getElementById('recordCount');
    if (recordCountEl) recordCountEl.textContent = `${total} bản ghi`;

    const paginationInfoEl = document.getElementById('paginationInfo');
    const startDisplay = total > 0 ? (page - 1) * limit + 1 : 0;
    const endDisplay = Math.min(page * limit, total);
    if (paginationInfoEl) {
        paginationInfoEl.textContent = `Hiển thị ${startDisplay}–${endDisplay} trên tổng số ${total}`;
    }

    const controls = document.getElementById('paginationControls');
    if (!controls) return;
    controls.innerHTML = '';

    if (totalPages <= 1) return;

    // Nút Trước
    const prevBtn = document.createElement('button');
    prevBtn.innerHTML = '&laquo;';
    prevBtn.disabled = page <= 1;
    prevBtn.onclick = () => { if (currentPage > 1) { currentPage--; loadHistory(); } };
    controls.appendChild(prevBtn);

    const startPage = Math.max(1, page - 2);
    const endPage = Math.min(totalPages, page + 2);

    if (startPage > 1) {
        const firstBtn = document.createElement('button');
        firstBtn.textContent = '1';
        firstBtn.onclick = () => { currentPage = 1; loadHistory(); };
        controls.appendChild(firstBtn);
        if (startPage > 2) {
            const span = document.createElement('span');
            span.textContent = '...';
            span.style.padding = '0 6px';
            controls.appendChild(span);
        }
    }

    for (let i = startPage; i <= endPage; i++) {
        const btn = document.createElement('button');
        btn.textContent = i;
        btn.className = i === page ? 'active' : '';
        btn.onclick = () => { currentPage = i; loadHistory(); };
        controls.appendChild(btn);
    }

    if (endPage < totalPages) {
        if (endPage < totalPages - 1) {
            const span = document.createElement('span');
            span.textContent = '...';
            span.style.padding = '0 6px';
            controls.appendChild(span);
        }
        const lastBtn = document.createElement('button');
        lastBtn.textContent = totalPages;
        lastBtn.onclick = () => { currentPage = totalPages; loadHistory(); };
        controls.appendChild(lastBtn);
    }

    // Nút Sau
    const nextBtn = document.createElement('button');
    nextBtn.innerHTML = '&raquo;';
    nextBtn.disabled = page >= totalPages;
    nextBtn.onclick = () => { if (currentPage < totalPages) { currentPage++; loadHistory(); } };
    controls.appendChild(nextBtn);
}

document.addEventListener('DOMContentLoaded', () => {
    const executedAtEl = document.getElementById('searchExecutedAt');
    if (executedAtEl) {
        executedAtEl.addEventListener('keydown', e => {
            if (e.key === 'Enter') doSearch();
        });
    }
    loadHistory();
});

// Poll slowly while idle to discover commands created in another tab.
document.addEventListener('visibilitychange', () => {
    clearTimeout(historyTimer);
    if (!document.hidden) loadHistory(true);
});
window.addEventListener('pagehide', () => clearTimeout(historyTimer));
