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
    const panel = document.querySelector('[data-participants]');
    if (!panel) return;
    const search = panel.querySelector('[data-participant-search]'), filter = panel.querySelector('[data-participant-filter]');
    const refresh = panel.querySelector('[data-participant-refresh]'), body = panel.querySelector('[data-participant-rows]');
    const summary = panel.querySelector('[data-participant-summary]'), error = panel.querySelector('[data-participant-error]');
    const login = panel.querySelector('[data-participant-login]'), table = panel.querySelector('table');
    const labels = {CONFIRMED:'참가 확정',PENDING:'승인 대기',WAITING:'대기',CANCELLED:'취소',REJECTED:'반려'};
    let participants = [], loading = false;
    function render() {
        const keyword = search.value.trim().toLocaleLowerCase('ko-KR');
        const rows = participants.filter(p => (!filter.value || p.status === filter.value)
            && (!keyword || String(p.displayName || '').toLocaleLowerCase('ko-KR').includes(keyword) || String(p.registrationId).includes(keyword)));
        body.replaceChildren();
        rows.forEach(p => {
            const tr = document.createElement('tr');
            [p.registrationId, p.displayName, labels[p.status] || p.statusDescription || p.status, formatDate(p.createdAt)].forEach(value => {
                const td = document.createElement('td'); td.textContent = value == null ? '—' : String(value); tr.appendChild(td);
            });
            body.appendChild(tr);
        });
        if (!rows.length) {
            const tr = document.createElement('tr'), td = document.createElement('td'); td.colSpan = 4;
            td.textContent = participants.length ? '검색 조건에 맞는 신청 내역이 없습니다.' : '아직 참가 신청 내역이 없습니다.';
            tr.appendChild(td); body.appendChild(tr);
        }
        const count = status => participants.filter(p => p.status === status).length;
        summary.textContent = `전체 ${participants.length}건 · 확정 ${count('CONFIRMED')}건 · 대기 ${count('WAITING')}건 · 조회 결과 ${rows.length}건`;
    }
    async function load() {
        if (loading) return;
        loading = true; refresh.disabled = true; search.disabled = true; filter.disabled = true;
        error.hidden = true; login.hidden = true; table.setAttribute('aria-busy', 'true');
        summary.textContent = '참가자 목록을 불러오는 중입니다.'; body.replaceChildren();
        try {
            const response = await fetch('/api/registrations/participants/' + encodeURIComponent(panel.dataset.participants), {headers:{Accept:'application/json'}});
            if (response.status === 401) { login.hidden = false; throw new Error('로그인이 필요합니다. 기업 계정으로 로그인해 주세요.'); }
            if (response.status === 403) throw new Error('이 회차를 주최한 기업만 참가자 목록을 조회할 수 있습니다.');
            if (!response.ok) throw new Error('참가자 목록을 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.');
            const data = await response.json();
            if (!Array.isArray(data)) throw new Error('참가자 목록 응답을 확인할 수 없습니다. 다시 시도해 주세요.');
            participants = data; search.disabled = false; filter.disabled = false; render();
        } catch (failure) {
            participants = []; summary.textContent = '참가자 목록 조회 실패';
            error.textContent = failure instanceof TypeError ? '네트워크 연결을 확인하고 새로고침해 주세요.' : failure.message;
            error.hidden = false;
        } finally { loading = false; refresh.disabled = false; table.setAttribute('aria-busy', 'false'); }
    }
    search.addEventListener('input', render); filter.addEventListener('change', render); refresh.addEventListener('click', load);
    load();
})();
