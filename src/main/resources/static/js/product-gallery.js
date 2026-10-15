(() => {
  const main = document.getElementById('product-main-image');
  if (!main) return;
  const reduced = () => window.matchMedia('(prefers-reduced-motion: reduce)').matches || document.documentElement.classList.contains('reduce-motion');
  document.querySelectorAll('[data-gallery-image]').forEach(link => {
    link.addEventListener('click', event => {
      if (event.ctrlKey || event.metaKey || event.shiftKey || event.altKey) return;
      event.preventDefault();
      main.src = link.href;
      if (!reduced() && main.animate) main.animate([{opacity:.45}, {opacity:1}], {duration:220});
      document.querySelectorAll('[data-gallery-image]').forEach(item => item.removeAttribute('aria-current'));
      link.setAttribute('aria-current', 'true');
    });
  });
  const quantity = document.getElementById('product-quantity');
  if (quantity) {
    const buttons = document.querySelectorAll('[data-quantity-step]');
    const sync = () => {
      const value = quantity.valueAsNumber;
      buttons.forEach(button => {
        button.disabled = Number(button.dataset.quantityStep) < 0 ? value <= Number(quantity.min) : value >= Number(quantity.max);
      });
    };
    buttons.forEach(button => {
      button.hidden = false;
      button.addEventListener('click', () => {
        const value = Number.isFinite(quantity.valueAsNumber) ? quantity.valueAsNumber : Number(quantity.min);
        quantity.value = Math.min(Number(quantity.max), Math.max(Number(quantity.min), value + Number(button.dataset.quantityStep)));
        quantity.dispatchEvent(new Event('input', {bubbles:true}));
      });
    });
    quantity.addEventListener('input', sync);
    sync();
  }
  const dialog = document.querySelector('.product-lightbox');
  const zoom = document.querySelector('[data-image-zoom]');
  if (dialog && zoom && typeof dialog.showModal === 'function') {
    zoom.hidden = false;
    zoom.addEventListener('click', () => {
      const image = dialog.querySelector('img');
      image.src = main.src; image.alt = main.alt;
      dialog.showModal();
    });
    dialog.addEventListener('click', event => { if (event.target === dialog) dialog.close(); });
    dialog.addEventListener('close', () => zoom.focus());
  }
})();
