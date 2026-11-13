(() => {
    const select = document.getElementById('event-session');
    if (!select) return;
    const button = document.querySelector('.reg-apply-btn');
    const state = document.getElementById('session-apply-state');
    const format = value => value ? value.slice(0, 16).replace('T', ' ').replaceAll('-', '.') : '모집 기간 미정';
    const update = () => {
        const option = select.selectedOptions[0];
        const selected = Boolean(select.value);
        document.getElementById('session-apply-start').textContent = selected ? format(option.dataset.applyStart) : '회차를 선택해주세요.';
        document.getElementById('session-apply-end').textContent = selected ? format(option.dataset.applyEnd) : '회차를 선택해주세요.';
        const canApply = selected && option.dataset.canApply === 'true';
        button.disabled = !canApply;
        state.hidden = !selected;
        state.textContent = selected ? (canApply ? '신청 가능한 회차입니다.' : option.dataset.closedReason || '현재 신청할 수 없는 회차입니다.') : '';
    };
    select.addEventListener('change', update);
    update();
})();
