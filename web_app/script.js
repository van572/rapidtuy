document.addEventListener('DOMContentLoaded', () => {
    const originSelect = document.getElementById('origin');
    const destinationSelect = document.getElementById('destination');
    const farePrice = document.getElementById('farePrice');
    const estimatedTime = document.getElementById('estimatedTime');
    const requestWhatsappBtn = document.getElementById('requestWhatsappBtn');

    // Matrix of estimated prices ($ USD)
    const fares = {
        'charallave_centro': { 'charallave_centro': 2.0, 'ferro_cua': 4.0, 'ocumare_centro': 6.0, 'santa_teresa': 7.0, 'yare': 5.0, 'tamanaco_tuy': 3.0, 'las_brisas': 3.5, 'pinto_salinas': 4.5 },
        'ferro_cua': { 'charallave_centro': 4.0, 'ferro_cua': 2.0, 'ocumare_centro': 5.0, 'santa_teresa': 6.5, 'yare': 4.5, 'tamanaco_tuy': 4.5, 'las_brisas': 5.0, 'pinto_salinas': 3.0 },
        'ocumare_centro': { 'charallave_centro': 6.0, 'ferro_cua': 5.0, 'ocumare_centro': 2.0, 'santa_teresa': 5.5, 'yare': 4.0, 'tamanaco_tuy': 6.0, 'las_brisas': 7.0, 'pinto_salinas': 5.5 },
        'santa_teresa': { 'charallave_centro': 7.0, 'ferro_cua': 6.5, 'ocumare_centro': 5.5, 'santa_teresa': 2.0, 'yare': 3.5, 'tamanaco_tuy': 7.0, 'las_brisas': 8.0, 'pinto_salinas': 7.0 },
        'yare': { 'charallave_centro': 5.0, 'ferro_cua': 4.5, 'ocumare_centro': 4.0, 'santa_teresa': 3.5, 'yare': 2.0, 'tamanaco_tuy': 5.0, 'las_brisas': 6.0, 'pinto_salinas': 5.0 },
        'tamanaco_tuy': { 'charallave_centro': 3.0, 'ferro_cua': 4.5, 'ocumare_centro': 6.0, 'santa_teresa': 7.0, 'yare': 5.0, 'tamanaco_tuy': 2.0, 'las_brisas': 3.0, 'pinto_salinas': 5.0 },
        'las_brisas': { 'charallave_centro': 3.5, 'ferro_cua': 5.0, 'ocumare_centro': 7.0, 'santa_teresa': 8.0, 'yare': 6.0, 'tamanaco_tuy': 3.0, 'las_brisas': 2.0, 'pinto_salinas': 5.5 },
        'pinto_salinas': { 'charallave_centro': 4.5, 'ferro_cua': 3.0, 'ocumare_centro': 5.5, 'santa_teresa': 7.0, 'yare': 5.0, 'tamanaco_tuy': 5.0, 'las_brisas': 5.5, 'pinto_salinas': 2.0 }
    };

    const zoneNames = {
        'charallave_centro': 'Charallave Centro / Ferrocarril',
        'ferro_cua': 'Estación Ferrocarril Cúa',
        'ocumare_centro': 'Ocumare del Tuy Centro',
        'santa_teresa': 'Santa Teresa del Tuy Centro',
        'yare': 'San Antonio de Yare Plaza',
        'tamanaco_tuy': 'C.C. Tamanaco Tuy',
        'las_brisas': 'Las Brisas / Charallave Norte',
        'pinto_salinas': 'Pinto Salinas / Cúa Sur'
    };

    function calculateFare() {
        const origin = originSelect.value;
        const dest = destinationSelect.value;

        let price = 3.0;
        let timeMin = 10;

        if (fares[origin] && fares[origin][dest]) {
            price = fares[origin][dest];
            timeMin = price === 2.0 ? 8 : Math.round(price * 4);
        }

        farePrice.textContent = `$${price.toFixed(2)}`;
        estimatedTime.textContent = `⏱️ Tiempo est.: ~${timeMin} min`;

        const textMessage = encodeURIComponent(`Hola RapidTuy, deseo solicitar una moto desde ${zoneNames[origin]} hasta ${zoneNames[dest]}. Tarifa estimada: $${price.toFixed(2)}.`);
        requestWhatsappBtn.href = `https://wa.me/584261215060?text=${textMessage}`;
    }

    if (originSelect && destinationSelect) {
        originSelect.addEventListener('change', calculateFare);
        destinationSelect.addEventListener('change', calculateFare);
        calculateFare();
    }

    // Live GPS Location Handler for Origen and Destino
    const btnGpsOrigin = document.getElementById('btnGpsOrigin');
    const btnGpsDest = document.getElementById('btnGpsDest');

    function applyGpsLocation(targetSelect, targetBtn, key, isOrigin) {
        if (!navigator.geolocation) {
            alert('Su navegador no soporta geolocalización GPS.');
            return;
        }
        targetBtn.textContent = '📡 Conectando con satélite GPS...';
        navigator.geolocation.getCurrentPosition(
            (pos) => {
                const lat = pos.coords.latitude.toFixed(4);
                const lng = pos.coords.longitude.toFixed(4);
                const gpsLabel = `📍 GPS en Vivo (${lat}, ${lng} - Valles del Tuy)`;
                
                let opt = Array.from(targetSelect.options).find(o => o.value === key);
                if (!opt) {
                    opt = document.createElement('option');
                    opt.value = key;
                    targetSelect.prepend(opt);
                }
                opt.textContent = gpsLabel;
                opt.selected = true;
                
                zoneNames[key] = gpsLabel;
                if (!fares[key]) fares[key] = fares['charallave_centro'];
                
                targetBtn.textContent = `✅ GPS Capturado (${lat}, ${lng})`;
                calculateFare();
            },
            (err) => {
                // Fallback simulation for live demo if permission denied or unavailable
                const mockLat = (10.2394 + (Math.random() * 0.01 - 0.005)).toFixed(4);
                const mockLng = (-66.8612 + (Math.random() * 0.01 - 0.005)).toFixed(4);
                const gpsLabel = `📍 GPS en Vivo (${mockLat}, ${mockLng} - Valles del Tuy)`;
                
                let opt = Array.from(targetSelect.options).find(o => o.value === key);
                if (!opt) {
                    opt = document.createElement('option');
                    opt.value = key;
                    targetSelect.prepend(opt);
                }
                opt.textContent = gpsLabel;
                opt.selected = true;
                
                zoneNames[key] = gpsLabel;
                if (!fares[key]) fares[key] = fares['charallave_centro'];
                
                targetBtn.textContent = `✅ GPS Simulado (${mockLat}, ${mockLng})`;
                calculateFare();
            },
            { enableHighAccuracy: true, timeout: 8000 }
        );
    }

    if (btnGpsOrigin) {
        btnGpsOrigin.addEventListener('click', () => applyGpsLocation(originSelect, btnGpsOrigin, 'gps_location_orig', true));
    }
    if (btnGpsDest) {
        btnGpsDest.addEventListener('click', () => applyGpsLocation(destinationSelect, btnGpsDest, 'gps_location_dest', false));
    }
});
