// Shared time-cell control for sensor data and activity history.
function createTimeCell(value) {
    const cell = document.createElement('td');
    const content = document.createElement('span');
    content.className = 'time-cell-content';
    const time = document.createElement('span');
    time.textContent = value;
    const button = document.createElement('button');
    button.type = 'button';
    button.className = 'copy-time-button';
    button.textContent = 'Sao chép';
    button.setAttribute('aria-label', `Sao chép thời gian ${value}`);
    button.setAttribute('aria-live', 'polite');
    button.disabled = value === '--';
    button.addEventListener('click', async () => {
        button.disabled = true;
        try {
            if (navigator.clipboard && window.isSecureContext) {
                await navigator.clipboard.writeText(value);
            } else {
                // Support pages served over HTTP where Clipboard API is unavailable.
                const input = document.createElement('textarea');
                input.value = value;
                input.style.position = 'fixed';
                input.style.opacity = '0';
                document.body.appendChild(input);
                try {
                    input.select();
                    if (!document.execCommand('copy')) throw new Error('Copy failed');
                } finally {
                    input.remove();
                    button.focus();
                }
            }
            button.textContent = 'Đã chép';
        } catch (error) {
            button.textContent = 'Thử lại';
            button.title = 'Không thể sao chép. Nhấn để thử lại.';
        } finally {
            button.disabled = false;
            setTimeout(() => {
                button.textContent = 'Sao chép';
                button.removeAttribute('title');
            }, 2000);
        }
    });
    content.append(time, button);
    cell.appendChild(content);
    return cell;
}
