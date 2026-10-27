// frontend/js/data-sensor.js

const API_BASE = 'http://localhost:8081/api';

let currentPage = 1;
let currentLimit = 10;
let currentSortKey = 'measuredAt';
let currentSortDir = 'DESC';
let currentSensorName = '';
let currentMeasuredAt = '';
let currentSearchValue = '';

const searchTypeConfig = {
    temperature: { label: 'Giá trị nhiệt độ', placeholder: 'Nhập giá trị nhiệt độ (ví dụ: 28.5)...' },
    humidity: { label: 'Giá trị độ ẩm', placeholder: 'Nhập giá trị độ ẩm (ví dụ: 65.2)...' },
    light: { label: 'Giá trị ánh sáng', placeholder: 'Nhập giá trị ánh sáng (ví dụ: 450)...' },
    time: { label: 'Thời gian đo', placeholder: 'Nhập thời gian (ví dụ: 2026-10-06 hoặc 19:30)...' }
};

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

function onSearchTypeChange() {
    const type = document.getElementById('searchType').value;
    const config = searchTypeConfig[type] || searchTypeConfig.temperature;
    const labelEl = document.getElementById('searchValueLabel');
    const inputEl = document.getElementById('searchValue');
    if (labelEl) labelEl.textContent = config.label;
    if (inputEl) {
        inputEl.placeholder = config.placeholder;
        inputEl.value = '';
    }
}

function onPageSizeChange() {
    currentLimit = parseInt(document.getElementById('pageSizeSelect').value, 10);
    currentPage = 1;
    loadData();
}

function sortBy(key) {
    if (currentSortKey === key) {
        currentSortDir = currentSortDir === 'DESC' ? 'ASC' : 'DESC';
    } else {
        currentSortKey = key;
        currentSortDir = 'DESC';
    }
    currentPage = 1;
    loadData();
}

function doSearch() {
    const type = document.getElementById('searchType').value;
    const value = document.getElementById('searchValue').value.trim();

    currentSensorName = '';
    currentMeasuredAt = '';
    currentSearchValue = '';

    if (type === 'time') {
        currentMeasuredAt = value;
    } else {
        const sensorNames = {
            temperature: 'nhiệt độ',
            humidity: 'độ ẩm',
            light: 'ánh sáng'
        };
        currentSensorName = sensorNames[type] || '';
        currentSearchValue = value;
    }

    currentPage = 1;
    loadData();
}

function resetSearch() {
    document.getElementById('searchType').value = 'temperature';
    document.getElementById('searchValue').value = '';
    onSearchTypeChange();
    currentSensorName = '';
    currentMeasuredAt = '';
    currentSearchValue = '';
    currentPage = 1;
    loadData();
}

async function loadData() {
    const tbody = document.getElementById('sensorTableBody');
    if (!tbody) return;

    tbody.innerHTML = '<tr><td colspan="4" style="text-align:center;color:#4a5a6a;">Đang tải...</td></tr>';

    try {
        const params = new URLSearchParams({
            page: currentPage,
            limit: currentLimit,
            sortKey: currentSortKey,
            sortDir: currentSortDir
        });

        if (currentSensorName) params.append('sensorName', currentSensorName);
        if (currentMeasuredAt) params.append('measuredAt', currentMeasuredAt);
        if (currentSearchValue) params.append('value', currentSearchValue);

        const response = await fetch(`${API_BASE}/datasensors/search?${params.toString()}`, {
            credentials: 'include'
        });

        if (!response.ok) {
            tbody.innerHTML = `<tr><td colspan="4" style="text-align:center;color:#f85149;">Lỗi khi tải dữ liệu (HTTP ${response.status})</td></tr>`;
            return;
        }

        const result = await response.json();
        const pageData = result.data || [];
        const pagination = result.pagination || { total: 0, totalPages: 1, page: 1, limit: currentLimit };

        const total = pagination.total;
        const totalPages = pagination.totalPages || 1;
        currentPage = pagination.page;

        tbody.innerHTML = '';
        if (pageData.length > 0) {
            const startIdx = (currentPage - 1) * currentLimit;
            pageData.forEach((row, index) => {
                const stt = startIdx + index + 1;
                const sensorName = row.sensorName || 'N/A';
                const unit = row.unit || '';
                const value = row.value != null ? Number(row.value).toFixed(2) : '--';
                const displayValue = unit ? `${value} ${unit}` : value;
                const measuredAt = formatDateTime(row.measuredAt);

                const tr = document.createElement('tr');
                tr.innerHTML = `
                    <td>${stt}</td>
                    <td>${sensorName}</td>
                    <td>${displayValue}</td>
                `;
                tr.appendChild(createTimeCell(measuredAt));
                tbody.appendChild(tr);
            });
        } else {
            tbody.innerHTML = '<tr><td colspan="4" style="text-align:center;color:#4a5a6a;">Không tìm thấy dữ liệu</td></tr>';
        }

        renderPagination(total, totalPages, currentPage, currentLimit);

    } catch (error) {
        console.error('Lỗi loadData:', error);
        tbody.innerHTML = '<tr><td colspan="4" style="text-align:center;color:#f85149;">Không thể kết nối đến máy chủ Backend!</td></tr>';
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
    prevBtn.onclick = () => { if (currentPage > 1) { currentPage--; loadData(); } };
    controls.appendChild(prevBtn);

    const startPage = Math.max(1, page - 2);
    const endPage = Math.min(totalPages, page + 2);

    if (startPage > 1) {
        const firstBtn = document.createElement('button');
        firstBtn.textContent = '1';
        firstBtn.onclick = () => { currentPage = 1; loadData(); };
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
        btn.onclick = () => { currentPage = i; loadData(); };
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
        lastBtn.onclick = () => { currentPage = totalPages; loadData(); };
        controls.appendChild(lastBtn);
    }

    // Nút Sau
    const nextBtn = document.createElement('button');
    nextBtn.innerHTML = '&raquo;';
    nextBtn.disabled = page >= totalPages;
    nextBtn.onclick = () => { if (currentPage < totalPages) { currentPage++; loadData(); } };
    controls.appendChild(nextBtn);
}

document.addEventListener('DOMContentLoaded', () => {
    onSearchTypeChange();
    const searchValue = document.getElementById('searchValue');
    if (searchValue) {
        searchValue.addEventListener('keydown', e => {
            if (e.key === 'Enter') doSearch();
        });
    }
    loadData();
});
