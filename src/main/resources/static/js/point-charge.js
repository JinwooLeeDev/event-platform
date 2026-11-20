(() => {
    const input = document.getElementById('chargeAmount');
    if (!input) return;
    const pay = document.getElementById('chargePayBtn');
    const methods = [...document.querySelectorAll('[data-payment-provider]')];
    const amounts = [...document.querySelectorAll('.amount-btn')];
    const balance = document.getElementById('myPoint');
    const providerNames = {naver: '네이버페이', kakao: '카카오페이'};
    let provider = 'naver';
    let paying = false;
    balance.textContent = Number(balance.textContent.trim()).toLocaleString('ko-KR') + 'P';

    function updateAmount() {
        const amount = Number(input.value);
        const formatted = (Number.isSafeInteger(amount) && amount > 0 ? amount : 0).toLocaleString('ko-KR');
        document.getElementById('expectedPoint').textContent = formatted + 'P';
        document.getElementById('chargeTotal').textContent = formatted + '원';
        amounts.forEach(button => {
            const active = Number(button.dataset.amount) === amount;
            button.classList.toggle('active', active);
            button.setAttribute('aria-pressed', String(active));
        });
    }
    methods.forEach(button => button.addEventListener('click', () => {
        if (paying) return;
        provider = button.dataset.paymentProvider;
        methods.forEach(method => method.setAttribute('aria-pressed', String(method === button)));
        document.getElementById('selectedPaymentMethod').textContent = providerNames[provider];
    }));
    amounts.forEach(button => button.addEventListener('click', () => {
        if (paying) return;
        input.value = button.dataset.amount;
        updateAmount();
    }));
    input.addEventListener('input', updateAmount);

    function setBusy(value) {
        paying = value;
        [pay, input, ...methods, ...amounts].forEach(element => { element.disabled = value; });
        pay.textContent = value ? '결제 처리 중…' : '결제하기';
        pay.setAttribute('aria-busy', String(value));
    }
    async function post(url, body) {
        const response = await fetch(url, {
            method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify(body)
        });
        if (!response.ok) throw new Error('결제 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.');
        return response.json();
    }
    pay.addEventListener('click', async () => {
        if (paying) return;
        const amount = Number(input.value);
        if (!Number.isSafeInteger(amount) || amount < 1 || !input.validity.valid) {
            alert('충전할 금액을 1원 이상의 정수로 입력해주세요.');
            input.focus();
            return;
        }
        // Capture the provider once, so prepare and complete always use the same payment channel.
        const endpoint = '/api/' + provider + '/payment/';
        setBusy(true);
        try {
            if (!window.PortOne) throw new Error('결제창을 불러오지 못했습니다. 새로고침 후 다시 시도해주세요.');
            const prepared = await post(endpoint + 'prepare', {amount});
            const payment = await window.PortOne.requestPayment({
                storeId: prepared.storeId,
                channelKey: prepared.channelKey,
                paymentId: prepared.paymentId,
                orderName: 'POINT',
                totalAmount: prepared.amount,
                currency: 'CURRENCY_KRW',
                payMethod: 'EASY_PAY'
            });
            if (!payment || payment.code !== undefined) {
                alert('결제가 취소되었거나 중단되었습니다.');
                return;
            }
            const completed = await post(endpoint + 'complete', {paymentId: payment.paymentId});
            balance.textContent = Number(completed.currentPoint).toLocaleString('ko-KR') + 'P';
            alert('충전이 완료되었습니다.');
            window.location.href = completed.redirectUrl;
        } catch (error) {
            alert(error.message || '결제에 실패했습니다. 잠시 후 다시 시도해주세요.');
        } finally {
            setBusy(false);
        }
    });
})();
