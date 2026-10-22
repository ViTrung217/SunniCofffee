(() => {
  const root = document.querySelector('[data-hero-carousel]');
  if (!root) return;
  const slides = [...root.querySelectorAll('.hero-slide')];
  if (slides.length < 2) return;
  const dots = [...root.querySelectorAll('[data-slide-index]')];
  const pause = root.querySelector('[data-slide-pause]');
  const media = window.matchMedia('(prefers-reduced-motion: reduce)');
  let current = 0, paused = false, hovered = false, focused = false, timer;
  const reduced = () => media.matches || document.documentElement.classList.contains('reduce-motion');
  const stop = () => { clearInterval(timer); timer = undefined; };
  function schedule() {
    stop();
    if (!paused && !hovered && !focused && !document.hidden && !reduced()) timer = setInterval(() => show(current + 1, false), 6000);
    pause.disabled = reduced();
    pause.textContent = paused || reduced() ? '▶' : 'Ⅱ';
    pause.setAttribute('aria-pressed', String(paused || reduced()));
    pause.setAttribute('aria-label', reduced() ? 'Tự chuyển ảnh đã tắt theo thiết lập giảm chuyển động' : paused ? 'Bật tự chuyển ảnh' : 'Tạm dừng tự chuyển ảnh');
  }
  function show(index, manual = true) {
    current = (index + slides.length) % slides.length;
    slides.forEach((slide, i) => slide.hidden = i !== current);
    dots.forEach((dot, i) => dot.setAttribute('aria-pressed', String(i === current)));
    if (!reduced() && slides[current].animate) slides[current].animate([{opacity:.25, transform:'translateX(12px)'}, {opacity:1, transform:'translateX(0)'}], {duration:450, easing:'ease-out'});
    if (manual) {
      root.querySelector('[data-slide-status]').textContent = `Ảnh ${current + 1} / ${slides.length}`;
      schedule();
    }
  }
  root.querySelector('.hero-slider-controls').hidden = false;
  root.querySelector('[data-slide-prev]').addEventListener('click', () => show(current - 1));
  root.querySelector('[data-slide-next]').addEventListener('click', () => show(current + 1));
  dots.forEach((dot, i) => dot.addEventListener('click', () => show(i)));
  pause.addEventListener('click', () => { paused = !paused; schedule(); });
  root.addEventListener('mouseenter', () => { hovered = true; schedule(); });
  root.addEventListener('mouseleave', () => { hovered = false; schedule(); });
  root.addEventListener('focusin', () => { focused = true; schedule(); });
  root.addEventListener('focusout', event => { focused = root.contains(event.relatedTarget); schedule(); });
  root.addEventListener('keydown', event => {
    if (event.key === 'ArrowLeft' || event.key === 'ArrowRight') { event.preventDefault(); show(current + (event.key === 'ArrowLeft' ? -1 : 1)); }
  });
  media.addEventListener('change', schedule);
  new MutationObserver(schedule).observe(document.documentElement, {attributes:true, attributeFilter:['class']});
  document.addEventListener('visibilitychange', schedule);
  window.addEventListener('pagehide', stop);
  window.addEventListener('pageshow', schedule);
  schedule();
})();
