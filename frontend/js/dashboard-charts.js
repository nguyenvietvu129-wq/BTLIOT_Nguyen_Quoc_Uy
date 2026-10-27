// let lightChart;
// let climateChart;

// function initChart() {
//     const labels = Array.from({ length: 20 }, (_, index) => index);
//     const common = {
//         responsive: true,
//         maintainAspectRatio: false,
//         plugins: { legend: { labels: { color: '#333' } } },
//         scales: {
//             x: { ticks: { color: '#555', maxRotation: 45, minRotation: 45 }, grid: { color: '#eee' } },
//             y: { ticks: { color: '#555' }, grid: { color: '#eee' } }
//         }
//     };

//     lightChart = new Chart(document.getElementById('lightChart'), {
//         type: 'bar',
//         data: { labels, datasets: [{ label: 'Ánh sáng (lux)', data: [], backgroundColor: '#888' }] },
//         options: {
//             ...common,
//             scales: { ...common.scales, y: { ...common.scales.y, min: 0, max: 200, title: { display: true, text: 'lux' } } }
//         }
//     });

//     climateChart = new Chart(document.getElementById('climateChart'), {
//         type: 'bar',
//         data: { labels, datasets: [
//             { label: 'Nhiệt độ (°C)', data: [], backgroundColor: '#555', yAxisID: 'temperature' },
//             { label: 'Độ ẩm (%)', data: [], backgroundColor: '#aaa', yAxisID: 'humidity' }
//         ] },
//         options: {
//             ...common,
//             scales: {
//                 x: common.scales.x,
//                 temperature: { type: 'linear', position: 'left', min: 0, max: 100, title: { display: true, text: '°C' }, ticks: { color: '#555' }, grid: { color: '#eee' } },
//                 humidity: { type: 'linear', position: 'right', min: 0, max: 100, title: { display: true, text: '%' }, ticks: { color: '#555' }, grid: { drawOnChartArea: false } }
//             }
//         }
//     });
// }

// function updateChart(chartData) {
//     const points = chartData || [];
//     const sensorValue = (terms, fallback, max) => {
//         const point = points.find(item => terms.some(term => (item.sensorName || '').toLowerCase().includes(term)));
//         const value = point && Number.isFinite(Number(point.value)) ? Number(point.value) : fallback;
//         return Math.max(0, Math.min(max, value));
//     };
//     const light = sensorValue(['ánh sáng', 'light'], 100, 200);
//     const temperature = sensorValue(['nhiệt độ', 'temperature'], 25, 100);
//     const humidity = sensorValue(['độ ẩm', 'humidity'], 55, 100);
//     const now = Date.now();
//     const labels = Array.from({ length: 20 }, (_, index) =>
//         new Date(now - (19 - index) * 2000).toLocaleTimeString('vi-VN', { hour12: false }));
//     const values = (base, max, amplitude) => Array.from({ length: 20 }, (_, index) => {
//         const variation = Math.sin((index + now / 2000) * 0.72) * amplitude + (Math.random() - 0.5) * amplitude * 0.6;
//         return Math.max(0, Math.min(max, Number((base + variation).toFixed(1))));
//     });

//     lightChart.data.labels = labels;
//     lightChart.data.datasets[0].data = values(light, 200, 20);
//     lightChart.update();
//     climateChart.data.labels = labels;
//     climateChart.data.datasets[0].data = values(temperature, 100, 3);
//     climateChart.data.datasets[1].data = values(humidity, 100, 5);
//     climateChart.update();
// }
