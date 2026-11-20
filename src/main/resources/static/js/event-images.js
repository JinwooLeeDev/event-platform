(() => {
    function showFallback(image) {
        image.hidden = true;
        image.closest('.event-card__visual')?.classList.add('is-image-missing');
    }
    // Capture image errors, including cards inserted by asynchronous category changes.
    document.addEventListener('error', event => {
        if (event.target.matches?.('.event-card__image')) showFallback(event.target);
    }, true);
    document.querySelectorAll('.event-card__image').forEach(image => {
        if (image.complete && image.naturalWidth === 0) showFallback(image);
    });
})();
