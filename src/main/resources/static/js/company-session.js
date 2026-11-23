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
            error.textContent = BusanI18n.t("common.ui0068");
            error.hidden = false;
        } finally { button.disabled = false; }
    }));
    document.querySelectorAll('[data-session-create-form]').forEach(form => {
        const eventId = form.dataset.eventId || location.pathname.match(/^\/events\/sessions\/(?:list|status)\/(\d+)$/)?.[1];
        if (!eventId) return;
        form.action = '/events/sessions/save/' + encodeURIComponent(eventId);
        const details = form.closest('[data-session-create]');
        const field = name => form.elements.namedItem(name);
        const updateFee = () => { const fee = field('fee'); fee.readOnly = field('pricingType').value === 'FREE'; fee.min = fee.readOnly ? '0' : '1'; if (fee.readOnly) fee.value = '0'; };
        field('pricingType').addEventListener('change', updateFee);
        for (const [start, end] of [['startsAt','endsAt'],['applyStartAt','applyEndAt']]) {
            const validate = () => field(end).setCustomValidity(field(start).value && field(end).value && field(start).value > field(end).value ? BusanI18n.t("event.ui0169") : '');
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
    document.querySelectorAll('[data-session-capacity]').forEach(el => { const n = Number(el.textContent.trim()); el.textContent = n === 2147483647 ? BusanI18n.t("event.ui0155") : n.toLocaleString('ko-KR') + BusanI18n.t("admin.ui0067"); });
    const states = {SCHEDULED:BusanI18n.t("common.ui0069"),IN_PROGRESS:BusanI18n.t("common.ui0070"),COMPLETED:BusanI18n.t("company.ui0042"),CANCELLED:BusanI18n.t("company.ui0043")};
    document.querySelectorAll('[data-session-status]').forEach(el => { el.textContent = states[el.textContent.trim()] || el.textContent; });
})();
