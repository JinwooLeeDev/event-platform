(() => {
    const container = document.getElementById('event-map');
    const status = document.getElementById('event-map-status');
    if (!container || !status) return;

    let authenticationFailed = false;
    const fail = (message, code = 'MAP_ERROR') => {
        container.hidden = true;
        status.hidden = false;
        status.textContent = message;
        console.error(BusanI18n.t("common.ui0071"), code, message);
    };
    const address = container.dataset.address.trim();
    const clientId = container.dataset.clientId.trim();
    if (!address) {
        fail(BusanI18n.t("common.ui0072"));
        return;
    }
    if (!clientId) {
        fail(BusanI18n.t("common.ui0073"));
        return;
    }

    window.navermap_authFailure = () => {
        authenticationFailed = true;
        fail(BusanI18n.t("common.ui0074"), 'AUTH_FAILED');
    };
    const script = document.createElement('script');
    script.src = 'https://oapi.map.naver.com/openapi/v3/maps.js?ncpKeyId='
        + encodeURIComponent(clientId) + '&submodules=geocoder&callback=initEventDetailMap';
    script.onerror = () => fail(BusanI18n.t("common.ui0075"));
    // SDK의 load 이벤트보다 늦게 준비될 수 있는 geocoder 모듈까지 기다립니다.
    window.initEventDetailMap = () => {
        if (authenticationFailed) return;
        const maps = window.naver?.maps;
        if (!maps?.Map) {
            fail(BusanI18n.t("common.ui0076"), 'SDK_UNAVAILABLE');
            return;
        }
        if (!maps?.Service?.geocode) {
            fail(BusanI18n.t("common.ui0077"), 'GEOCODER_UNAVAILABLE');
            return;
        }
        maps.Service.geocode({ query: address }, (result, response) => {
            if (result !== maps.Service.Status.OK) {
                fail(BusanI18n.t("common.ui0078"));
                return;
            }
            const location = response?.v2?.addresses?.[0];
            if (!location || !Number.isFinite(Number(location.y)) || !Number.isFinite(Number(location.x))) {
                fail(BusanI18n.t("common.ui0079"));
                return;
            }
            const position = new maps.LatLng(Number(location.y), Number(location.x));
            container.hidden = false;
            status.hidden = true;
            const map = new maps.Map(container, { center: position, zoom: 16, zoomControl: true });
            new maps.Marker({ map, position, title: container.dataset.title });
        });
    };
    document.head.appendChild(script);
})();
