document.addEventListener('DOMContentLoaded', () => {
    const originSelect = document.getElementById('origin');
    const destinationSelect = document.getElementById('destination');
    const farePrice = document.getElementById('farePrice');
    const estimatedTime = document.getElementById('estimatedTime');
    const requestWhatsappBtn = document.getElementById('requestWhatsappBtn');

    // Matrix of estimated prices ($ USD)
    const fares = {
        'charallave_centro': { 'charallave_centro': 2.0, 'ferro_cua': 4.0, 'ocumare_centro': 6.0, 'santa_teresa': 7.0, 'yare': 5.0 },
        'ferro_cua': { 'charallave_centro': 4.0, 'ferro_cua': 2.0, 'ocumare_centro': 5.0, 'santa_teresa': 6.5, 'yare': 4.5 },
        'ocumare_centro': { 'charallave_centro': 6.0, 'ferro_cua': 5.0, 'ocumare_centro': 2.0, 'santa_teresa': 5.5, 'yare': 4.0 },
        'santa_teresa': { 'charallave_centro': 7.0, 'ferro_cua': 6.5, 'ocumare_centro': 5.5, 'santa_teresa': 2.0, 'yare': 3.5 },
        'yare': { 'charallave_centro': 5.0, 'ferro_cua': 4.5, 'ocumare_centro': 4.0, 'santa_teresa': 3.5, 'yare': 2.0 }
    };

    const zoneNames = {
        'charallave_centro': 'Charallave Centro',
        'ferro_cua': 'Estación Ferrocarril Cúa',
        'ocumare_centro': 'Ocumare del Tuy Centro',
        'santa_teresa': 'Santa Teresa del Tuy',
        'yare': 'San Antonio de Yare'
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
});
