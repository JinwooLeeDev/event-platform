(() => {
    const browser = document.querySelector('[data-admin-cancel-browser]');
    if (!browser) return;
    const list = browser.querySelector('[data-admin-cancel-list]');
    const tabs = [...browser.querySelectorAll('[data-cancel-status]')];
    const loading = browser.querySelector('[data-cancel-loading]');
    const error = browser.querySelector('[data-cancel-load-error]');
    const errorMessage = browser.querySelector('[data-cancel-error-message]');
    let controller;
    let requestId = 0;
    let retryTarget;

    async function load(url, pushHistory) {
        const id = ++requestId;
        controller?.abort();
        controller = new AbortController();
        retryTarget = {url, pushHistory};
        list.setAttribute('aria-busy', 'true');
        loading.hidden = false;
        error.hidden = true;
        try {
            const response = await fetch(url, {
                signal: controller.signal,
                credentials: 'same-origin',
                headers: {'Accept': 'text/html'}
            });
            if (!response.ok) {
                throw new Error(response.status === 401 || response.status === 403
                    ? '관리자 로그인 상태를 확인한 후 다시 시도해주세요.'
                    : '취소 신청 내역을 불러오지 못했습니다. 다시 시도해주세요.');
            }
            const html = await response.text();
            if (id !== requestId) return;
            const page = new DOMParser().parseFromString(html, 'text/html');
            const incoming = page.querySelector('[data-admin-cancel-list]');
            if (!incoming) throw new Error('목록을 확인할 수 없습니다. 관리자 로그인 상태를 확인해주세요.');
            // Keep navigation and its keyboard focus; replace only the request list.
            list.replaceChildren(...incoming.childNodes);
            list.querySelectorAll('.admin-badge').forEach(badge => {
                const label = badge.textContent.trim();
                badge.dataset.tone = label.includes('거절') ? 'danger'
                    : label.includes('대기') ? 'warning' : 'success';
            });
            const status = new URL(url, window.location.href).searchParams.get('status') || 'PENDING';
            tabs.forEach(tab => {
                if (tab.dataset.cancelStatus === status) tab.setAttribute('aria-current', 'page');
                else tab.removeAttribute('aria-current');
            });
            if (pushHistory) window.history.pushState(null, '', url);
        } catch (failure) {
            if (id !== requestId || failure.name === 'AbortError') return;
            errorMessage.textContent = failure.message;
            error.hidden = false;
        } finally {
            if (id === requestId) {
                list.setAttribute('aria-busy', 'false');
                loading.hidden = true;
            }
        }
    }

    browser.addEventListener('click', event => {
        const link = event.target.closest('a[data-cancel-status]');
        if (!link || event.defaultPrevented || event.button !== 0 || event.ctrlKey || event.metaKey || event.shiftKey || event.altKey) return;
        event.preventDefault();
        load(link.href, true);
    });
    browser.querySelector('[data-cancel-retry]').addEventListener('click', () => {
        if (retryTarget) load(retryTarget.url, retryTarget.pushHistory);
    });
    window.addEventListener('popstate', () => load(window.location.href, false));
})();
