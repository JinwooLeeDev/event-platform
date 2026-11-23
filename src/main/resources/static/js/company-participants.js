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
    const labels = {CONFIRMED:BusanI18n.t("community.ui0027"), PENDING:BusanI18n.t("company.ui0034"), WAITING:BusanI18n.t("event.ui0190")};
    let participants = [], loading = false, rejecting = false, selected = null;
    const formatDate = value => value ? String(value).replace('T', ' ').slice(0, 16).replaceAll('-', '.') : '—';
    // ★ [f/42] 출석 열 — 출석: "출석 11. 27 오후 1:42" / 참가 확정 미출석: "미출석" / 대기·승인 대기: "—"
    const attendanceText = p => p.attended ? BusanI18n.t("common.extra023") + (p.attendedAtText || '') : (p.status === 'CONFIRMED' ? BusanI18n.t("common.ui0050") : '—');

    function render() {
        const keyword = search.value.trim().toLocaleLowerCase('ko-KR');
        const rows = participants.filter(p => (!filter.value || p.status === filter.value)
            && (!keyword || String(p.displayName || '').toLocaleLowerCase('ko-KR').includes(keyword) || String(p.registrationId).includes(keyword)));
        body.replaceChildren();
        rows.forEach(p => {
            const tr = document.createElement('tr');
            [p.registrationId, p.displayName, labels[p.status], formatDate(p.createdAt), attendanceText(p)].forEach(value => {   // ★ [f/42] 출석 열 추가
                const td = document.createElement('td'); td.textContent = value == null ? '—' : String(value); tr.appendChild(td);
            });
            const action = document.createElement('td'), button = document.createElement('button');
            button.type = 'button'; button.className = 'participant-reject-button'; button.textContent = BusanI18n.t("company.ui0019");
            button.setAttribute('aria-label', BusanI18n.t("common.ui0051", [p.displayName || BusanI18n.t("community.ui0054"), p.registrationId]));
            // ★ [f/42] 회차가 시작된 신청은 반려 불가 (서버 rejectable — reject() 의 시작 시각 검사와 같은 기준)
            const rejectable = p.rejectable !== false;
            if (!rejectable) button.title = BusanI18n.t("common.ui0052");
            button.disabled = loading || rejecting || !rejectable;
            button.addEventListener('click', () => {
                if (loading || rejecting || !rejectable) return;
                selected = p;
                target.textContent = BusanI18n.t("common.ui0053", [p.displayName || BusanI18n.t("community.ui0054"), p.registrationId]);
                dialogError.hidden = true;
                dialog.showModal();
            });
            action.appendChild(button); tr.appendChild(action); body.appendChild(tr);
        });
        if (!rows.length) {
            const tr = document.createElement('tr'), td = document.createElement('td'); td.colSpan = 6;   // ★ [f/42] 열 5 → 6
            td.textContent = participants.length ? BusanI18n.t("common.ui0054") : BusanI18n.t("common.ui0055");
            tr.appendChild(td); body.appendChild(tr);
        }
        const count = status => participants.filter(p => p.status === status).length;
        const attended = participants.filter(p => p.attended).length;   // ★ [f/42] 출석 인원
        summary.textContent = BusanI18n.t("common.ui0056", [participants.length, count('CONFIRMED'), count('PENDING'), count('WAITING'), attended, rows.length]);
    }

    async function load(afterReject = false) {
        if (loading) return;
        loading = true; refresh.disabled = true; search.disabled = true; filter.disabled = true;
        error.hidden = true; login.hidden = true; table.setAttribute('aria-busy', 'true');
        render(); summary.textContent = BusanI18n.t("event.ui0191");
        try {
            const response = await fetch('/api/registrations/participants/' + encodeURIComponent(panel.dataset.participants), {headers:{Accept:'application/json'}, credentials:'same-origin'});
            if (response.status === 401 || response.redirected) { login.hidden = false; throw new Error(BusanI18n.t("common.ui0057")); }
            if (response.status === 403) throw new Error(BusanI18n.t("common.ui0058"));
            if (!response.ok) throw new Error(BusanI18n.t("common.ui0059"));
            const data = await response.json();
            if (!Array.isArray(data)) throw new Error(BusanI18n.t("common.ui0060"));
            participants = data.filter(p => Object.hasOwn(labels, p.status));
            const occupied = document.querySelector('[data-session-occupied]');
            if (occupied) occupied.textContent = participants.filter(p => p.status !== 'WAITING').length.toLocaleString('ko-KR') + BusanI18n.t("admin.ui0067");
        } catch (failure) {
            error.textContent = afterReject ? BusanI18n.t("common.ui0061")
                : failure instanceof TypeError ? BusanI18n.t("common.ui0062") : failure.message;
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
        confirmButton.textContent = BusanI18n.t("common.ui0063"); dialogError.hidden = true; notice.hidden = true;
        try {
            const response = await fetch('/api/registrations/reject/' + encodeURIComponent(participant.registrationId), {
                method:'POST', headers:{Accept:'application/json'}, credentials:'same-origin'
            });
            if (response.status === 401 || response.redirected) { login.hidden = false; throw new Error(BusanI18n.t("common.ui0064")); }
            const data = await response.json().catch(() => ({}));
            if (!response.ok) throw new Error(data.message || BusanI18n.t("common.ui0065"));
            participants = participants.filter(p => p.registrationId !== participant.registrationId);
            dialog.close(); selected = null;
            notice.textContent = BusanI18n.t("common.ui0066", [participant.displayName || BusanI18n.t("community.ui0054")]); notice.hidden = false;
            await load(true);
            refresh.focus();
        } catch (failure) {
            dialogError.textContent = failure instanceof TypeError ? BusanI18n.t("common.ui0067") : failure.message;
            dialogError.hidden = false;
        } finally {
            rejecting = false; confirmButton.disabled = false; closeButton.disabled = false; refresh.disabled = false;
            confirmButton.textContent = BusanI18n.t("company.ui0088"); render();
        }
    });
    closeButton.addEventListener('click', () => dialog.close());
    dialog.addEventListener('cancel', event => { if (rejecting) event.preventDefault(); });
    dialog.addEventListener('close', () => { selected = null; });
    search.addEventListener('input', render); filter.addEventListener('change', render);
    refresh.addEventListener('click', () => { if (!rejecting) load(); });
    load();
})();
