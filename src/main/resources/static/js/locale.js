(() => {
    const widget = document.querySelector('.bp-locale');
    const dialog = document.getElementById('locale-modal');
    if (!widget || !dialog || dialog.dataset.ready) return;
    dialog.dataset.ready = 'true';
    document.documentElement.lang = widget.dataset.locale;
    let opener;
    function open() {
        opener = document.activeElement;
        if (!dialog.open) dialog.showModal();
    }
    document.querySelectorAll('[data-locale-open]').forEach(button => button.addEventListener('click', open));
    dialog.querySelector('[data-locale-close]').addEventListener('click', () => dialog.close());
    dialog.addEventListener('close', () => {
        if (opener instanceof HTMLElement && opener.isConnected) opener.focus();
        document.dispatchEvent(new Event('busan:locale-ready'));
    });
    if (widget.dataset.firstVisit === 'true') open();
    else document.dispatchEvent(new Event('busan:locale-ready'));
})();
