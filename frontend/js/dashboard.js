// frontend/js/dashboard.js

const API_BASE = 'http://localhost:8081/api';
let sensorChart = null;

// Màu sắc từng cảm biến
const SENSOR_COLORS = {
    temperature: {
        border: '#ff5722',
        background: 'rgba(255, 87, 34, 0.12)'
    },
    humidity: {
        border: '#06b6d4',
        background: 'rgba(6, 182, 212, 0.12)'
    },
    light: {
        border: '#f4b400',
        background: 'rgba(244, 180, 0, 0.14)'
    },
    default: {
        border: '#06b6d4',
        background: 'rgba(71, 85, 105, 0.12)'
    }
};

// Chuẩn hóa tên cảm biến
function normalizeSensorName(name) {
    if (!name) return '';

    const normalized = String(name)
        .toLowerCase()
        .normalize('NFD')
        .replace(/[\u0300-\u036f]/g, '');

    if (
        normalized.includes('nhiet do') ||
        normalized.includes('temperature')
    ) {
        return 'temperature';
    }

    if (
        normalized.includes('do am') ||
        normalized.includes('humidity')
    ) {
        return 'humidity';
    }

    if (
        normalized.includes('anh sang') ||
        normalized.includes('light')
    ) {
        return 'light';
    }

    return normalized;
}

// Lấy màu sắc theo cảm biến
function getSensorColor(name) {
    const normalized = normalizeSensorName(name);

    if (normalized.includes('temperature') || normalized.includes('nhiet')) {
        return SENSOR_COLORS.temperature;
    }
    if (normalized.includes('humidity') || normalized.includes('do am')) {
        return SENSOR_COLORS.humidity;
    }
    if (normalized.includes('light') || normalized.includes('anh sang')) {
        return SENSOR_COLORS.light;
    }

    return SENSOR_COLORS.default;
}

// Biểu tượng cảm biến
function getEmojiForSensor(name) {
    if (!name) return '📡';

    const n = name.toLowerCase();

    if (n.includes('nhiệt') || n.includes('temperature')) {
        return '🌡️';
    }

    if (n.includes('ẩm') || n.includes('humidity')) {
        return '💧';
    }

    if (n.includes('ánh sáng') || n.includes('light')) {
        return '☀️';
    }

    return '📡';
}

function formatTimeKey(t) {
    if (!t) return '';
    if (typeof t === 'string') return t;
    if (Array.isArray(t)) {
        const pad = n => String(n).padStart(2, '0');
        return `${t[0]}-${pad(t[1])}-${pad(t[2])}T${pad(t[3] || 0)}:${pad(t[4] || 0)}:${pad(t[5] || 0)}`;
    }
    return String(t);
}

function formatTimeDisplay(t) {
    if (!t) return '';
    if (typeof t === 'string') {
        const part = t.includes('T') ? t.split('T')[1] : (t.split(' ')[1] || t);
        return part.substring(0, 8);
    }
    if (Array.isArray(t)) {
        const pad = n => String(n).padStart(2, '0');
        return `${pad(t[3] || 0)}:${pad(t[4] || 0)}:${pad(t[5] || 0)}`;
    }
    return String(t);
}

// Khởi tạo biểu đồ
function initChart() {
    const canvas = document.getElementById('sensorChart');
    if (!canvas) return;

    const ctx = canvas.getContext('2d');

    sensorChart = new Chart(ctx, {
        type: 'line',
        data: {
            labels: [],
            datasets: []
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            interaction: {
                mode: 'index',
                intersect: false
            },
            plugins: {
                legend: {
                    labels: {
                        color: '#374151',
                        font: {
                            size: 12,
                            family: 'Arial, sans-serif'
                        },
                        padding: 16,
                        usePointStyle: true,
                        pointStyleWidth: 10
                    }
                },
                tooltip: {
                    enabled: true,
                    mode: 'index',
                    intersect: false,
                    backgroundColor: 'rgba(17, 24, 39, 0.94)',
                    titleColor: '#ffffff',
                    bodyColor: '#e5e7eb',
                    borderColor: '#d1d5db',
                    borderWidth: 1,
                    padding: 12,
                    cornerRadius: 0,
                    callbacks: {
                        labelColor: function(context) {
                            const color = context.dataset.borderColor;
                            return {
                                borderColor: color,
                                backgroundColor: color,
                                borderWidth: 2
                            };
                        },
                        label: function(context) {
                            const label = context.dataset.label || '';
                            const value = context.parsed.y;
                            if (value === null || value === undefined) return null;

                            const sensorType = normalizeSensorName(label);
                            let unit = '';
                            if (sensorType === 'temperature') unit = ' °C';
                            else if (sensorType === 'humidity') unit = ' %';
                            else if (sensorType === 'light') unit = ' Lux';

                            return `  ${label}: ${Number(value).toFixed(2)}${unit}`;
                        }
                    }
                }
            },
            scales: {
                x: {
                    ticks: {
                        color: '#4b5563',
                        font: { size: 11 },
                        maxRotation: 90,
                        minRotation: 0
                    },
                    grid: {
                        color: 'rgba(148, 163, 184, 0.25)'
                    }
                },
                y: {
                    type: 'linear',
                    position: 'left',
                    min: 0,
                    max: 100,
                    title: {
                        display: true,
                        text: 'Nhiệt độ (°C) / Độ ẩm (%)',
                        color: '#4b5563'
                    },
                    ticks: {
                        color: '#4b5563',
                        font: { size: 11 }
                    },
                    grid: {
                        color: 'rgba(148, 163, 184, 0.25)'
                    }
                },
                y2: {
                    type: 'linear',
                    position: 'right',
                    min: 0,
                    max: 1000,
                    title: {
                        display: true,
                        text: 'Ánh sáng (Lux)',
                        color: '#4b5563'
                    },
                    ticks: {
                        color: '#4b5563',
                        font: { size: 11 }
                    },
                    grid: {
                        drawOnChartArea: false
                    }
                }
            }
        }
    });
}

// Cập nhật biểu đồ
function updateChart(chartData) {
    if (!sensorChart || !chartData || chartData.length === 0) return;

    const grouped = {};
    chartData.forEach(pt => {
        const name = pt.sensorName || 'Cảm biến';
        if (!grouped[name]) {
            grouped[name] = [];
        }
        grouped[name].push({
            value: pt.value,
            timeKey: formatTimeKey(pt.measuredAt),
            timeDisplay: formatTimeDisplay(pt.measuredAt)
        });
    });

    const allTimeKeys = [
        ...new Set(chartData.map(pt => formatTimeKey(pt.measuredAt)))
    ].sort().slice(-5);

    const labels = allTimeKeys.map(k => formatTimeDisplay(k));

    const datasets = Object.entries(grouped).map(([name, points]) => {
        const dataMap = {};
        points.forEach(pt => {
            dataMap[pt.timeKey] = pt.value;
        });

        const values = allTimeKeys.map(k => dataMap[k] != null ? dataMap[k] : null);
        const palette = getSensorColor(name);
        const sensorType = normalizeSensorName(name);

        return {
            label: name,
            data: values,
            yAxisID: sensorType === 'light' ? 'y2' : 'y',
            borderColor: palette.border,
            backgroundColor: palette.background,
            borderWidth: 2.5,
            pointRadius: 3,
            pointHoverRadius: 6,
            pointBackgroundColor: palette.border,
            pointBorderColor: '#ffffff',
            pointBorderWidth: 2,
            tension: 0.3,
            fill: false,
            spanGaps: true
        };
    });

    sensorChart.data.labels = labels;
    sensorChart.data.datasets = datasets;
    sensorChart.update();
}

// Hiển thị KPI cards
function renderKPI(latestSensors) {
    const kpiGrid = document.getElementById('kpiGrid');
    if (!kpiGrid || !latestSensors || latestSensors.length === 0) return;

    kpiGrid.innerHTML = '';
    latestSensors.forEach(sensor => {
        const value = sensor.value != null
            ? Number(sensor.value).toFixed(2)
            : '--';
        const unit = sensor.unit || '';
        const emoji = getEmojiForSensor(sensor.sensorName);
        const palette = getSensorColor(sensor.sensorName);

        const card = document.createElement('div');
        card.className = 'kpi-card';
        card.style.borderTop = `3px solid ${palette.border}`;

        card.innerHTML = `
            <div class="kpi-label">
                ${emoji} ${(sensor.sensorName || 'SENSOR').toUpperCase()}
            </div>
            <div class="kpi-value">
                <span>${value}</span>
                <span class="kpi-unit">${unit}</span>
            </div>
        `;
        kpiGrid.appendChild(card);
    });
}

// Hiển thị danh sách thiết bị
function renderDevices(devices) {
    const container = document.getElementById('deviceControls');
    if (!container || !devices) return;

    // Không render lại thiết bị nếu có switch đang trong trạng thái loading
    const anyLoading = container.querySelector('.switch[data-loading="true"]');
    if (anyLoading) return;

    container.innerHTML = '';
    devices.forEach(device => {
        const div = document.createElement('div');
        div.className = 'device-item';
        const isOn = (device.currentStatus || '').toUpperCase() === 'ON';

        div.innerHTML = `
            <div class="device-info">
                <div class="name">${device.name}</div>
                <div class="gpio">
                    ${device.type || 'Device'} · ID: ${device.id}
                </div>
            </div>

            <div class="device-status">
                <div
                    class="device-light ${isOn ? 'on' : 'off'}"
                    aria-label="${isOn ? 'Bật' : 'Tắt'}">
                    💡
                </div>

                <div
                    class="switch ${isOn ? 'active' : ''}"
                    data-device-id="${device.id}"
                    onclick="toggleDevice(${device.id})">
                    <div class="knob"></div>
                </div>
            </div>
        `;

        container.appendChild(div);
    });
}

// Tải Dashboard từ Backend API
async function loadDashboard() {
    try {
        const response = await fetch(`${API_BASE}/dashboard/summary`, {
            credentials: 'include'
        });

        if (!response.ok) {
            console.error('Không thể lấy dữ liệu dashboard (HTTP ' + response.status + ')');
            return;
        }

        const data = await response.json();
        renderKPI(data.latestSensors);
        updateChart(data.chartData);
        renderDevices(data.devices);
    } catch (error) {
        console.error('Lỗi khi tải dữ liệu dashboard:', error);
    }
}

// Điều khiển thiết bị ON / OFF
async function toggleDevice(deviceId) {
    const switchEl = document.querySelector(`.switch[data-device-id="${deviceId}"]`);
    if (!switchEl) return;
    if (switchEl.dataset.loading === 'true') return;

    const isCurrentlyOn = switchEl.classList.contains('active');
    const newState = isCurrentlyOn ? 'OFF' : 'ON';

    const originalHTML = switchEl.innerHTML;
    switchEl.dataset.loading = 'true';
    switchEl.classList.add('loading');
    switchEl.innerHTML = '<span class="loading-text" style="font-size:9px;color:#fff;line-height:26px;display:block;text-align:center;">...</span>';
    switchEl.style.pointerEvents = 'none';

    try {
        const response = await fetch(`${API_BASE}/devices/${deviceId}/state`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include',
            body: JSON.stringify({ state: newState })
        });

        const data = await response.json();

        if (response.status !== 202 || !data.historyId) {
            throw new Error(data.message || 'Không thể tiếp nhận lệnh điều khiển');
        }
        let result;
        // HTTP 202 only confirms acceptance; keep the switch at its confirmed state.
        while (true) {
            await new Promise(resolve => setTimeout(resolve, 1000));
            const poll = await fetch(`${API_BASE}/activehistories/${data.historyId}`, { credentials: 'include' });
            if (!poll.ok) throw new Error('Chưa lấy được kết quả lệnh. Hãy xem Active History.');
            result = await poll.json();
            if (result.status !== 'LOADING') break;
        }
        if (result.status === 'FAILURE') {
            alert('Điều khiển thất bại hoặc chưa nhận được xác nhận ESP32. Kiểm tra Active History và kết nối thiết bị.');
        }
        const isOn = result.state === 'ON';
        switchEl.classList.toggle('active', isOn);
        const indicator = switchEl.closest('.device-item')?.querySelector('.device-light');
        if (indicator) {
            indicator.classList.toggle('on', isOn);
            indicator.classList.toggle('off', !isOn);
            indicator.setAttribute('aria-label', isOn ? 'Bật' : 'Tắt');
        }
    } catch (error) {
        console.error('Lỗi toggleDevice:', error);
        alert(error.message || 'Không thể kết nối đến máy chủ Backend!');
    } finally {
        switchEl.innerHTML = originalHTML;
        switchEl.dataset.loading = 'false';
        switchEl.classList.remove('loading');
        switchEl.style.pointerEvents = '';
        await loadDashboard();
    }
}

// Khởi chạy Dashboard
document.addEventListener('DOMContentLoaded', () => {
    initChart();
    loadDashboard();
    // Tự động làm mới dữ liệu sau mỗi 5 giây
    setInterval(loadDashboard, 5000);
});
