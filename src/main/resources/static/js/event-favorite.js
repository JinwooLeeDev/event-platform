(() => {
    const groups = new Map();
    const page = document.querySelector('[data-favorites-page]');
    const labels = {PUBLISHED:'공개 중',PENDING_REVIEW:'심사 대기',FEEDBACK_REQUIRED:'보완 요청',PAID_READY:'공개 전',COMPLETED:'종료',CANCELLED:'취소'};
    document.querySelectorAll('[data-favorite-event-status]').forEach(el => { el.textContent = labels[el.textContent.trim()] || el.textContent; });
    const message = (group, text = '') => group.buttons.forEach(button => {
        const element = button.closest('[data-favorite-control]').querySelector('[data-favorite-message]');
        element.textContent = text; element.hidden = !text;
    });
    const busy = (group, value) => { group.busy = value; group.buttons.forEach(button => { button.disabled = value; }); };
    function apply(group, data) {
        const favorite = data.favorite ?? data.isFavorite;

        if (typeof favorite !== 'boolean') {
            throw new Error('좋아요 상태를 확인하지 못했습니다. 다시 눌러 주세요.');
        }

        group.state = { favorite };

        group.buttons.forEach(button => {
            button.setAttribute('aria-pressed', String(favorite));
            button.setAttribute('aria-label', favorite ? '찜 취소' : '찜');

            if (page && !favorite) {
                button.closest('[data-favorite-item]')?.remove();
            }
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
            const error = new Error('좋아요를 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.');
            error.status = response.status; throw error;
        }
        if (!data) throw new Error('좋아요 상태를 확인하지 못했습니다. 다시 눌러 주세요.');
        return data;
    }
    async function load(group) {
        if (group.busy) return;
        busy(group, true); message(group);
        try { apply(group, await request(group.id)); }
        catch (_) { message(group, '좋아요를 불러오지 못했습니다. 하트를 눌러 다시 시도해 주세요.'); }
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
                if (button.dataset.favoriteCompany === 'true') { message(group, '개인 회원만 좋아요를 등록할 수 있습니다.'); return; }
                busy(group, true); message(group);
                try {
                    if (!group.state) apply(group, await request(id));
                    apply(group, await request(id, group.state.favorite ? 'DELETE' : 'POST'));
                } catch (error) {
                    if (error.status === 401) {
                        message(group, '좋아요를 변경하려면 개인 계정으로 로그인해 주세요.');
                        if (window.BusanLogin) window.BusanLogin.open();
                        else if (confirm('로그인이 필요합니다. 로그인 화면으로 이동할까요?')) location.assign('/login');
                    } else if (error.status === 400 || error.status === 404) {
                        try { apply(group, await request(id)); message(group, '변경된 좋아요 상태를 반영했습니다. 다시 확인해 주세요.'); }
                        catch (_) { message(group, '좋아요를 처리하지 못했습니다. 다시 시도해 주세요.'); }
                    } else { message(group, '좋아요를 처리하지 못했습니다. 연결 상태를 확인하고 다시 시도해 주세요.'); }
                } finally { busy(group, false); }
            });
        });
        addedGroups.forEach(load);
    }
    initialize();
    window.addEventListener('busan:events-updated', initialize);
    window.addEventListener('pageshow', event => { if (event.persisted) groups.forEach(load); });
})();
