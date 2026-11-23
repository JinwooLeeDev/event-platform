(() => {
    const groups = new Map();
    const page = document.querySelector('[data-favorites-page]');
    const labels = {PUBLISHED:BusanI18n.t("company.ui0033"),PENDING_REVIEW:BusanI18n.t("common.ui0083"),FEEDBACK_REQUIRED:BusanI18n.t("admin.ui0056"),PAID_READY:BusanI18n.t("common.ui0084"),COMPLETED:BusanI18n.t("company.ui0042"),CANCELLED:BusanI18n.t("company.ui0043")};
    document.querySelectorAll('[data-favorite-event-status]').forEach(el => { el.textContent = labels[el.textContent.trim()] || el.textContent; });
    const message = (group, text = '') => group.buttons.forEach(button => {
        const element = button.closest('[data-favorite-control]').querySelector('[data-favorite-message]');
        element.textContent = text; element.hidden = !text;
    });
    const busy = (group, value) => { group.busy = value; group.buttons.forEach(button => { button.disabled = value; }); };
    function apply(group, data) {
        const favorite = data.favorite ?? data.isFavorite;
        const count = Number(data.favoriteCount);
        if (typeof favorite !== 'boolean' || !Number.isFinite(count) || count < 0) throw new Error(BusanI18n.t("common.ui0085"));
        group.state = {favorite, count};
        group.buttons.forEach(button => {
            button.setAttribute('aria-pressed', String(favorite));
            button.setAttribute('aria-label', favorite ? BusanI18n.t("common.ui0086") : BusanI18n.t("common.ui0036"));
            button.querySelector('[data-favorite-count]').textContent = count.toLocaleString('ko-KR');
            if (page && !favorite) button.closest('[data-favorite-item]')?.remove();
        });
        if (page) {
            const total = page.querySelectorAll('[data-favorite-item]').length;
            page.querySelector('[data-favorite-total]').textContent = total.toLocaleString('ko-KR');
            page.querySelector('[data-favorites-empty]').hidden = total > 0;
        }
    }
    async function request(id, method = 'GET') {
        const response = await fetch('/events/favorite/' + encodeURIComponent(id), {method, headers:{Accept:'application/json'}});
        const data = await response.json().catch(() => null);
        if (!response.ok) {
            const error = new Error(BusanI18n.t("common.ui0087"));
            error.status = response.status; throw error;
        }
        if (!data) throw new Error(BusanI18n.t("common.ui0085"));
        return data;
    }
    async function load(group) {
        if (group.busy) return;
        busy(group, true); message(group);
        try { apply(group, await request(group.id)); }
        catch (_) { message(group, BusanI18n.t("common.ui0088")); }
        finally { busy(group, false); }
    }
    const initialized = new WeakSet();
    function initialize() {
        groups.forEach((group, id) => {
            group.buttons = group.buttons.filter(button => button.isConnected !== false);
            if (!group.buttons.length) groups.delete(id);
        });
        const addedGroups = new Set();
        document.querySelectorAll('[data-favorite-event]').forEach(button => {
            if (initialized.has(button)) return;
            initialized.add(button);
            const id = button.dataset.favoriteEvent;
            if (!groups.has(id)) groups.set(id, {id, buttons:[], state:null, busy:false});
            const group = groups.get(id); group.buttons.push(button);
            addedGroups.add(group);
            button.addEventListener('click', async event => {
                event.preventDefault(); event.stopPropagation();
                if (group.busy) return;
                if (button.dataset.favoriteCompany === 'true') { message(group, BusanI18n.t("common.ui0089")); return; }
                busy(group, true); message(group);
                try {
                    if (!group.state) apply(group, await request(id));
                    apply(group, await request(id, group.state.favorite ? 'DELETE' : 'POST'));
                } catch (error) {
                    if (error.status === 401) {
                        message(group, BusanI18n.t("common.ui0090"));
                        if (window.BusanLogin) window.BusanLogin.open();
                        else if (confirm(BusanI18n.t("common.ui0091"))) location.assign('/login');
                    } else if (error.status === 400 || error.status === 404) {
                        try { apply(group, await request(id)); message(group, BusanI18n.t("common.ui0092")); }
                        catch (_) { message(group, BusanI18n.t("common.ui0093")); }
                    } else { message(group, BusanI18n.t("common.ui0094")); }
                } finally { busy(group, false); }
            });
        });
        addedGroups.forEach(load);
    }
    initialize();
    window.addEventListener('busan:events-updated', initialize);
    window.addEventListener('pageshow', event => { if (event.persisted) groups.forEach(load); });
})();
