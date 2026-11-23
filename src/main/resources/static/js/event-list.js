(() => {
    const page = document.querySelector('.event-list-page');
    if (!page) return;
    const content = page.querySelector('.list-content');
    const feedback = document.getElementById('list-feedback');
    const searchForm = document.querySelector('.header-search');
    let currentURL = new URL(location.href);
    let controller;
    let revision = 0;

    function syncControls() {
        const category = currentURL.searchParams.get('categoryId') || '';
        const keyword = currentURL.searchParams.get('keyword') || '';
        page.querySelectorAll('.list-category').forEach(button => {
            const selected = button.value === category;
            button.classList.toggle('is-selected', selected);
            if (selected) button.setAttribute('aria-current', 'true');
            else button.removeAttribute('aria-current');
        });
        page.querySelectorAll('.list-sidebar [name="keyword"]').forEach(input => { input.value = keyword; });
        if (searchForm) {
            searchForm.querySelector('[name="keyword"]').value = keyword;
            let field = searchForm.querySelector('[name="categoryId"]');
            if (!field) {
                field = document.createElement('input');
                field.type = 'hidden'; field.name = 'categoryId'; searchForm.append(field);
            }
            field.value = category;
            field.disabled = !category;
        }
    }
    function renderPagination() {
        const nav = content.querySelector('#list-pagination');
        if (!nav) return;
        const current = Number(nav.dataset.current), total = Number(nav.dataset.total);
        nav.replaceChildren();
        nav.hidden = !Number.isInteger(total) || total < 1;
        if (nav.hidden) return;
        function item(label, number, selected = false) {
            const valid = number >= 1 && number <= total;
            const element = document.createElement(valid ? 'a' : 'span');
            element.textContent = label;
            if (valid) {
                const url = new URL(currentURL);
                url.searchParams.set('page', String(number));
                url.searchParams.set('size', nav.dataset.size);
                element.href = url.pathname + url.search;
                if (selected) element.setAttribute('aria-current', 'page');
                const originalLabel = BusanI18n.source(label);
                element.setAttribute('aria-label', originalLabel === '이전' ? BusanI18n.t('common.pagination.previous')
                    : originalLabel === '다음' ? BusanI18n.t('common.pagination.next') : BusanI18n.t('common.pagination.page', [number]));
            } else element.setAttribute('aria-disabled', 'true');
            nav.append(element);
        }
        item(BusanI18n.t("common.ui0095"), current - 1);
        const start = Math.max(1, Math.min(current - 2, total - 4));
        for (let number = start; number <= Math.min(total, start + 4); number++) item(String(number), number, number === current);
        item(BusanI18n.t("common.ui0096"), current + 1);
    }
    async function navigate(url, push = true) {
        controller?.abort();
        controller = new AbortController();
        const requestRevision = ++revision;
        content.setAttribute('aria-busy', 'true');
        feedback.textContent = BusanI18n.t("common.ui0097");
        feedback.hidden = false;
        try {
            const response = await fetch(url, {signal: controller.signal, headers: {Accept: 'text/html'}});
            if (!response.ok) throw new Error(BusanI18n.t("common.ui0098"));
            const parsed = new DOMParser().parseFromString(await response.text(), 'text/html');
            const next = parsed.querySelector('.event-list-page .list-content');
            if (!next) throw new Error(BusanI18n.t("common.ui0099"));
            if (requestRevision !== revision) return;
            content.replaceChildren(...document.importNode(next, true).childNodes);
            currentURL = new URL(url);
            if (push && location.href !== currentURL.href) history.pushState(null, '', currentURL.pathname + currentURL.search);
            syncControls();
            renderPagination();
            window.dispatchEvent(new CustomEvent('busan:events-updated'));
            feedback.hidden = true;
        } catch (error) {
            if (requestRevision !== revision || error.name === 'AbortError') return;
            if (!push) { location.reload(); return; }
            feedback.textContent = BusanI18n.t("common.ui0100");
            feedback.hidden = false;
        } finally {
            if (requestRevision === revision) content.setAttribute('aria-busy', 'false');
        }
    }
    function submit(event) {
        const form = event.target;
        if (!form.matches('.list-sidebar form, .header-search')) return;
        event.preventDefault();
        const url = new URL(form.action, location.href);
        url.search = new URLSearchParams(new FormData(form, event.submitter)).toString();
        url.searchParams.set('page', '1');
        url.searchParams.set('size', content.querySelector('#list-pagination')?.dataset.size || '9');
        navigate(url);
    }
    page.addEventListener('submit', submit);
    searchForm?.addEventListener('submit', submit);
    content.addEventListener('click', event => {
        if (event.button !== 0 || event.ctrlKey || event.metaKey || event.shiftKey || event.altKey) return;
        const link = event.target.closest('.list-pagination a, .main-events__empty .list-button');
        if (!link) return;
        event.preventDefault();
        navigate(new URL(link.href));
    });
    window.addEventListener('popstate', () => navigate(new URL(location.href), false));
    syncControls();
    renderPagination();
})();
