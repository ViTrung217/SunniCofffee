(() => {
  'use strict';
  const root = document.querySelector('[data-hero-carousel]');
  if (!root) return;
  const slides = [...root.querySelectorAll('.hero-slide')];
  if (slides.length < 2) return;
  const dots = [...root.querySelectorAll('[data-slide-index]')];
  const pause = root.querySelector('[data-slide-pause]');
  const media = window.matchMedia('(prefers-reduced-motion: reduce)');
  const progress = document.createElement('span');
  progress.className = 'hero-slide-progress';
  progress.setAttribute('aria-hidden', 'true');
  const bar = document.createElement('span');
  progress.append(bar);
  root.append(progress);
  let current = 0, paused = false, hovered = false, focused = false, visible = true, timer, progressAnimation;
  const reduced = () => media.matches || document.documentElement.classList.contains('reduce-motion');
  const stop = () => {
    clearTimeout(timer);
    timer = undefined;
    progressAnimation?.cancel();
    progressAnimation = undefined;
  };
  function schedule() {
    stop();
    const playing = !paused && !hovered && !focused && visible && !document.hidden && !reduced();
    root.classList.toggle('is-paused', !playing);
    if (playing) {
      timer = setTimeout(() => show(current + 1, false), 6000);
      if (bar.animate) progressAnimation = bar.animate([{transform:'scaleX(0)'}, {transform:'scaleX(1)'}], {duration:6000, easing:'linear', fill:'forwards'});
    }
    if (reduced()) slides.forEach(slide => slide.classList.remove('is-entering'));
    pause.disabled = reduced();
    pause.textContent = paused || reduced() ? '▶' : 'Ⅱ';
    pause.setAttribute('aria-pressed', String(paused || reduced()));
    pause.setAttribute('aria-label', reduced() ? 'Tự chuyển ảnh đã tắt theo thiết lập giảm chuyển động' : paused ? 'Bật tự chuyển ảnh' : 'Tạm dừng tự chuyển ảnh');
  }
  function show(index, manual = true) {
    current = (index + slides.length) % slides.length;
    slides.forEach((slide, i) => {
      slide.hidden = i !== current;
      slide.classList.toggle('is-entering', i === current && !reduced());
    });
    dots.forEach((dot, i) => dot.setAttribute('aria-pressed', String(i === current)));
    if (manual) root.querySelector('[data-slide-status]').textContent = `Ảnh ${current + 1} / ${slides.length}`;
    schedule();
  }
  root.querySelector('.hero-slider-controls').hidden = false;
  root.querySelector('[data-slide-prev]').addEventListener('click', () => show(current - 1));
  root.querySelector('[data-slide-next]').addEventListener('click', () => show(current + 1));
  dots.forEach((dot, i) => dot.addEventListener('click', () => show(i)));
  pause.addEventListener('click', () => { paused = !paused; schedule(); });
  root.addEventListener('pointerenter', event => { if (event.pointerType === 'mouse') { hovered = true; schedule(); } });
  root.addEventListener('pointerleave', event => { if (event.pointerType === 'mouse') { hovered = false; schedule(); } });
  root.addEventListener('focusin', () => { focused = true; schedule(); });
  root.addEventListener('focusout', event => { focused = root.contains(event.relatedTarget); schedule(); });
  root.addEventListener('keydown', event => {
    if (event.key === 'ArrowLeft' || event.key === 'ArrowRight') { event.preventDefault(); show(current + (event.key === 'ArrowLeft' ? -1 : 1)); }
  });
  let touchStart;
  root.addEventListener('pointerdown', event => {
    touchStart = event.isPrimary && event.pointerType === 'touch' && !event.target.closest('button,a') ? {x:event.clientX, y:event.clientY} : undefined;
  });
  root.addEventListener('pointerup', event => {
    if (!touchStart) return;
    const dx = event.clientX - touchStart.x, dy = event.clientY - touchStart.y;
    touchStart = undefined;
    if (Math.abs(dx) > 55 && Math.abs(dx) > Math.abs(dy) * 1.5) show(current + (dx < 0 ? 1 : -1));
  });
  root.addEventListener('pointercancel', () => { touchStart = undefined; });
  if ('IntersectionObserver' in window) new IntersectionObserver(([entry]) => {
    visible = entry.isIntersecting;
    schedule();
  }, {threshold:0.15}).observe(root);
  media.addEventListener('change', schedule);
  document.addEventListener('suni:motionchange', schedule);
  document.addEventListener('visibilitychange', schedule);
  window.addEventListener('pagehide', stop);
  window.addEventListener('pageshow', schedule);
  show(0, false);
})();
