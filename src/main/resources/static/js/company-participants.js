(() => {
    const panel = document.querySelector('[data-participants]');
    if (!panel) return;
    const find = selector => panel.querySelector(selector);
    const search = find('[data-participant-search]'), filter = find('[data-participant-filter]');
    const refresh = find('[data-participant-refresh]'), body = find('[data-participant-rows]');
    const summary = find('[data-participant-summary]'), error = find('[data-participant-error]');
    const login = find('[data-participant-login]'), table = find('table');
    const notice = find('[data-participant-notice]');
    const dialog = document.querySelector('[data-participant-reject-dialog]');
    const target = dialog.querySelector('[data-reject-target]');
    const confirmButton = dialog.querySelector('[data-reject-confirm]');
    const dialogError = dialog.querySelector('[data-reject-error]');
    const closeButton = dialog.querySelector('[data-reject-close]');
    const labels = {CONFIRMED:'참가 확정', PENDING:'승인 대기', WAITING:'대기'};
    let participants = [], loading = false, rejecting = false, selected = null;
    const formatDate = value => value ? String(value).replace('T', ' ').slice(0, 16).replaceAll('-', '.') : '—';

    function render() {
        const keyword = search.value.trim().toLocaleLowerCase('ko-KR');
        const rows = participants.filter(p => (!filter.value || p.status === filter.value)
            && (!keyword || String(p.displayName || '').toLocaleLowerCase('ko-KR').includes(keyword) || String(p.registrationId).includes(keyword)));
        body.replaceChildren();
        rows.forEach(p => {
            const tr = document.createElement('tr');
            [p.registrationId, p.displayName, labels[p.status], formatDate(p.createdAt)].forEach(value => {
                const td = document.createElement('td'); td.textContent = value == null ? '—' : String(value); tr.appendChild(td);
            });
            const action = document.createElement('td'), button = document.createElement('button');
            button.type = 'button'; button.className = 'participant-reject-button'; button.textContent = '반려';
            button.setAttribute('aria-label', `${p.displayName || '참가자'} · 신청 ${p.registrationId} 반려`);
            button.disabled = loading || rejecting;
            button.addEventListener('click', () => {
                if (loading || rejecting) return;
                selected = p;
                target.textContent = `${p.displayName || '참가자'} · 신청 번호 ${p.registrationId}`;
                dialogError.hidden = true;
                dialog.showModal();
            });
            action.appendChild(button); tr.appendChild(action); body.appendChild(tr);
        });
        if (!rows.length) {
            const tr = document.createElement('tr'), td = document.createElement('td'); td.colSpan = 5;
            td.textContent = participants.length ? '검색 조건에 맞는 참가자가 없습니다.' : '확정 또는 대기 중인 참가자가 없습니다.';
            tr.appendChild(td); body.appendChild(tr);
        }
        const count = status => participants.filter(p => p.status === status).length;
        summary.textContent = `전체 ${participants.length}명 · 확정 ${count('CONFIRMED')}명 · 승인 대기 ${count('PENDING')}명 · 대기 ${count('WAITING')}명 · 조회 결과 ${rows.length}명`;
    }

    async function load(afterReject = false) {
        if (loading) return;
        loading = true; refresh.disabled = true; search.disabled = true; filter.disabled = true;
        error.hidden = true; login.hidden = true; table.setAttribute('aria-busy', 'true');
        render(); summary.textContent = '참가자 목록을 불러오는 중입니다.';
        try {
            const response = await fetch('/api/registrations/participants/' + encodeURIComponent(panel.dataset.participants), {headers:{Accept:'application/json'}, credentials:'same-origin'});
            if (response.status === 401 || response.redirected) { login.hidden = false; throw new Error('로그인이 필요합니다. 기업 계정으로 로그인해주세요.'); }
            if (response.status === 403) throw new Error('이 회차를 주최한 기업만 참가자를 관리할 수 있습니다.');
            if (!response.ok) throw new Error('참가자 목록을 불러오지 못했습니다. 새로고침으로 다시 시도해주세요.');
            const data = await response.json();
            if (!Array.isArray(data)) throw new Error('참가자 목록 응답을 확인할 수 없습니다.');
            participants = data.filter(p => Object.hasOwn(labels, p.status));
            const occupied = document.querySelector('[data-session-occupied]');
            if (occupied) occupied.textContent = participants.filter(p => p.status !== 'WAITING').length.toLocaleString('ko-KR') + '명';
        } catch (failure) {
            error.textContent = afterReject ? '반려는 완료되었지만 최신 목록을 불러오지 못했습니다. 새로고침해주세요.'
                : failure instanceof TypeError ? '네트워크 연결을 확인하고 다시 시도해주세요.' : failure.message;
            error.hidden = false;
        } finally {
            loading = false; refresh.disabled = false; search.disabled = false; filter.disabled = false;
            table.setAttribute('aria-busy', 'false'); render();
        }
    }

    confirmButton.addEventListener('click', async () => {
        if (!selected || rejecting || loading) return;
        const participant = selected;
        rejecting = true; confirmButton.disabled = true; closeButton.disabled = true; refresh.disabled = true;
        confirmButton.textContent = '반려 처리 중…'; dialogError.hidden = true; notice.hidden = true;
        try {
            const response = await fetch('/api/registrations/reject/' + encodeURIComponent(participant.registrationId), {
                method:'POST', headers:{Accept:'application/json'}, credentials:'same-origin'
            });
            if (response.status === 401 || response.redirected) { login.hidden = false; throw new Error('기업 계정으로 다시 로그인해주세요.'); }
            const data = await response.json().catch(() => ({}));
            if (!response.ok) throw new Error(data.message || '반려하지 못했습니다. 현재 신청 상태와 권한을 확인해주세요.');
            participants = participants.filter(p => p.registrationId !== participant.registrationId);
            dialog.close(); selected = null;
            notice.textContent = `${participant.displayName || '참가자'}의 신청을 반려했습니다.`; notice.hidden = false;
            await load(true);
            refresh.focus();
        } catch (failure) {
            dialogError.textContent = failure instanceof TypeError ? '통신 중 오류가 발생했습니다. 목록을 새로고침해 처리 여부를 확인해주세요.' : failure.message;
            dialogError.hidden = false;
        } finally {
            rejecting = false; confirmButton.disabled = false; closeButton.disabled = false; refresh.disabled = false;
            confirmButton.textContent = '신청 반려'; render();
        }
    });
    closeButton.addEventListener('click', () => dialog.close());
    dialog.addEventListener('cancel', event => { if (rejecting) event.preventDefault(); });
    dialog.addEventListener('close', () => { selected = null; });
    search.addEventListener('input', render); filter.addEventListener('change', render);
    refresh.addEventListener('click', () => { if (!rejecting) load(); });
    load();
})();
