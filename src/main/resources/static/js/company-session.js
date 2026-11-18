(() => {
    document.querySelectorAll('[data-delete-form]').forEach(form => form.addEventListener('submit', async event => {
        event.preventDefault();
        if (!confirm(form.dataset.confirm)) return;
        const button = form.querySelector('[type="submit"]'), error = form.querySelector('[data-delete-error]');
        button.disabled = true; error.hidden = true;
        try {
            const response = await fetch(form.action, {method:'POST', body:new FormData(form)});
            if (!response.ok || !response.redirected || new URL(response.url).pathname !== '/events') throw new Error();
            location.assign('/events');
        } catch (_) {
            error.textContent = '삭제하지 못했습니다. 연결된 회차·이미지·신청·결제 내역 또는 로그인 상태를 확인해 주세요.';
            error.hidden = false;
        } finally { button.disabled = false; }
    }));
    document.querySelectorAll('[data-session-create-form]').forEach(form => {
        const eventId = form.dataset.eventId || location.pathname.match(/^\/events\/sessions\/(?:list|status)\/(\d+)$/)?.[1];
        if (!eventId) return;
        form.action = '/events/sessions/save/' + encodeURIComponent(eventId);
        const details = form.closest('[data-session-create]'); details.hidden = false;
        if (new URLSearchParams(location.search).has('add')) details.open = true;
        const field = name => form.elements.namedItem(name);
        const updateFee = () => { const fee = field('fee'); fee.readOnly = field('pricingType').value === 'FREE'; fee.min = fee.readOnly ? '0' : '1'; if (fee.readOnly) fee.value = '0'; };
        field('pricingType').addEventListener('change', updateFee);
        for (const [start, end] of [['startsAt','endsAt'],['applyStartAt','applyEndAt']]) {
            const validate = () => field(end).setCustomValidity(field(start).value && field(end).value && field(start).value > field(end).value ? '종료 일시는 시작 일시보다 빠를 수 없습니다.' : '');
            field(start).addEventListener('input', validate); field(end).addEventListener('input', validate);
        }
        form.addEventListener('submit', event => {
            if (!field('name').value.trim()) { event.preventDefault(); field('name').focus(); return; }
            field('minimumCheckAt').value = field('startsAt').value;
        });
    });
    const formatDate = value => value ? String(value).replace('T', ' ').slice(0, 16).replaceAll('-', '.') : '—';
    document.querySelectorAll('[data-session-date]').forEach(el => { el.textContent = formatDate(el.textContent.trim()); });
    document.querySelectorAll('[data-session-point]').forEach(el => { el.textContent = Number(el.textContent.trim()).toLocaleString('ko-KR') + ' P'; });
    document.querySelectorAll('[data-session-capacity]').forEach(el => { const n = Number(el.textContent.trim()); el.textContent = n === 2147483647 ? '제한 없음' : n.toLocaleString('ko-KR') + '명'; });
    const states = {SCHEDULED:'진행 예정',IN_PROGRESS:'진행 중',COMPLETED:'종료',CANCELLED:'취소'};
    document.querySelectorAll('[data-session-status]').forEach(el => { el.textContent = states[el.textContent.trim()] || el.textContent; });
    const picker = document.querySelector('[data-session-picker]');
    if (picker) {
        const links = picker.querySelectorAll('[data-session-link]');
        if (links.length === 1 && !new URLSearchParams(location.search).has('select') && !new URLSearchParams(location.search).has('add')) location.replace(links[0].getAttribute('href'));
    }
})();
