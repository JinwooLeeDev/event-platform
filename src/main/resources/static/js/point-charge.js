(() => {
    const input = document.getElementById('chargeAmount');
    if (!input) return;
    const pay = document.getElementById('chargePayBtn');
    const methods = [...document.querySelectorAll('[data-payment-provider]')];
    const amounts = [...document.querySelectorAll('.amount-btn')];
    const balance = document.getElementById('myPoint');
    const providerNames = {naver: BusanI18n.t("payment.ui0018"), kakao: BusanI18n.t("payment.ui0020")};
    let provider = 'naver';
    let paying = false;
    balance.textContent = Number(balance.textContent.trim()).toLocaleString('ko-KR') + 'P';

    function updateAmount() {
        const amount = Number(input.value);
        const formatted = (Number.isSafeInteger(amount) && amount > 0 ? amount : 0).toLocaleString('ko-KR');
        document.getElementById('expectedPoint').textContent = formatted + 'P';
        document.getElementById('chargeTotal').textContent = formatted + BusanI18n.t("payment.ui0009");
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
        pay.textContent = value ? BusanI18n.t("common.ui0101") : BusanI18n.t("payment.ui0031");
        pay.setAttribute('aria-busy', String(value));
    }
    async function post(url, body) {
        const response = await fetch(url, {
            method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify(body)
        });
        if (!response.ok) throw new Error(BusanI18n.t("common.ui0102"));
        return response.json();
    }
    pay.addEventListener('click', async () => {
        if (paying) return;
        const amount = Number(input.value);
        if (!Number.isSafeInteger(amount) || amount < 1 || !input.validity.valid) {
            alert(BusanI18n.t("common.ui0103"));
            input.focus();
            return;
        }
        // Capture the provider once, so prepare and complete always use the same payment channel.
        const endpoint = '/api/' + provider + '/payment/';
        setBusy(true);
        try {
            if (!window.PortOne) throw new Error(BusanI18n.t("common.ui0104"));
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
                alert(BusanI18n.t("common.ui0105"));
                return;
            }
            const completed = await post(endpoint + 'complete', {paymentId: payment.paymentId});
            balance.textContent = Number(completed.currentPoint).toLocaleString('ko-KR') + 'P';
            alert(BusanI18n.t("common.ui0106"));
            window.location.href = completed.redirectUrl;
        } catch (error) {
            alert(error.message || BusanI18n.t("common.ui0107"));
        } finally {
            setBusy(false);
        }
    });
})();
