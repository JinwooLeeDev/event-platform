(() => {
    const select = document.getElementById('event-session');
    if (!select) return;
    const button = document.querySelector('.reg-apply-btn');
    const state = document.getElementById('session-apply-state');
    const format = value => value ? value.slice(0, 16).replace('T', ' ').replaceAll('-', '.') : BusanI18n.t("common.ui0080");
    const update = () => {
        const option = select.selectedOptions[0];
        const selected = Boolean(select.value);
        document.getElementById('session-apply-start').textContent = selected ? format(option.dataset.applyStart) : BusanI18n.t("common.ui0081");
        document.getElementById('session-apply-end').textContent = selected ? format(option.dataset.applyEnd) : BusanI18n.t("common.ui0081");
        const canApply = selected && option.dataset.canApply === 'true';
        button.disabled = !canApply;
        state.hidden = !selected;
        state.textContent = selected ? (canApply ? BusanI18n.t("common.ui0082") : option.dataset.closedReason || BusanI18n.t("event.ui0023")) : '';
    };
    select.addEventListener('change', update);
    update();
})();
