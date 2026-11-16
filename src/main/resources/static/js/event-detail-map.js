(() => {
    const container = document.getElementById('event-map');
    const status = document.getElementById('event-map-status');
    if (!container || !status) return;

    let authenticationFailed = false;
    const fail = (message, code = 'MAP_ERROR') => {
        container.hidden = true;
        status.hidden = false;
        status.textContent = message;
        console.error('[행사 지도]', code, message);
    };
    const address = container.dataset.address.trim();
    const clientId = container.dataset.clientId.trim();
    if (!address) {
        fail('등록된 행사 주소가 없습니다.');
        return;
    }
    if (!clientId) {
        fail('지도 설정을 확인 중입니다. 표시된 주소를 참고해주세요.');
        return;
    }

    window.navermap_authFailure = () => {
        authenticationFailed = true;
        fail('지도 인증에 실패했습니다. 표시된 주소를 참고해주세요.', 'AUTH_FAILED');
    };
    const script = document.createElement('script');
    script.src = 'https://oapi.map.naver.com/openapi/v3/maps.js?ncpKeyId='
        + encodeURIComponent(clientId) + '&submodules=geocoder&callback=initEventDetailMap';
    script.onerror = () => fail('지도를 불러올 수 없습니다. 잠시 후 다시 시도해주세요.');
    // SDK의 load 이벤트보다 늦게 준비될 수 있는 geocoder 모듈까지 기다립니다.
    window.initEventDetailMap = () => {
        if (authenticationFailed) return;
        const maps = window.naver?.maps;
        if (!maps?.Map) {
            fail('지도 기능을 초기화하지 못했습니다. 표시된 주소를 참고해주세요.', 'SDK_UNAVAILABLE');
            return;
        }
        if (!maps?.Service?.geocode) {
            fail('주소 검색 기능을 불러오지 못했습니다. 표시된 주소를 참고해주세요.', 'GEOCODER_UNAVAILABLE');
            return;
        }
        maps.Service.geocode({ query: address }, (result, response) => {
            if (result !== maps.Service.Status.OK) {
                fail('행사 위치를 조회할 수 없습니다. 표시된 주소를 참고해주세요.');
                return;
            }
            const location = response?.v2?.addresses?.[0];
            if (!location || !Number.isFinite(Number(location.y)) || !Number.isFinite(Number(location.x))) {
                fail('주소에 해당하는 위치를 찾지 못했습니다. 표시된 주소를 참고해주세요.');
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
