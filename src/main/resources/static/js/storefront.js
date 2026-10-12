/* Progressive enhancement: all content and navigation work without JavaScript. */
(() => {
  'use strict';
  const media = window.matchMedia('(prefers-reduced-motion: reduce)');
  const key = 'suni-reduce-motion';
  let manual = false;
  try { manual = localStorage.getItem(key) === 'true'; } catch (_) { /* Storage may be unavailable. */ }
  const toggle = document.querySelector('[data-motion-toggle]');
  let observer;
  const animations = new Set();
  const reduced = () => media.matches || manual;
  const sync = () => {
    document.documentElement.classList.toggle('reduce-motion', reduced());
    if (toggle) {
      toggle.hidden = false;
      toggle.disabled = media.matches;
      toggle.setAttribute('aria-pressed', String(reduced()));
      toggle.textContent = media.matches ? 'Đã giảm chuyển động theo thiết bị' : manual ? 'Bật chuyển động' : 'Tắt chuyển động';
    }
    if (reduced()) {
      observer?.disconnect();
      animations.forEach(animation => animation.cancel());
      animations.clear();
    }
  };
  sync();
  toggle?.addEventListener('click', () => {
    manual = !manual;
    try { localStorage.setItem(key, String(manual)); } catch (_) { /* Preference still works this visit. */ }
    sync();
    if (!reduced()) observe();
  });
  media.addEventListener('change', () => { sync(); if (!reduced()) observe(); });
  function observe() {
    if (reduced() || !('IntersectionObserver' in window) || !Element.prototype.animate) return;
    observer?.disconnect();
    observer = new IntersectionObserver(entries => {
      entries.forEach(entry => {
        if (!entry.isIntersecting) return;
        const element = entry.target;
        observer.unobserve(element);
        if (element.dataset.revealed) return;
        element.dataset.revealed = 'true';
        const animation = element.animate([
          { opacity: 0.35, transform: 'translateY(22px)' },
          { opacity: 1, transform: 'translateY(0)' }
        ], { duration: 580, easing: 'cubic-bezier(.2,.65,.3,1)', fill: 'none' });
        animations.add(animation);
        animation.finished.then(() => animations.delete(animation)).catch(() => animations.delete(animation));
      });
    }, { threshold: 0.08 });
    document.querySelectorAll('[data-reveal]:not([data-revealed])').forEach(element => observer.observe(element));
  }
  observe();
  const path = window.location.pathname.replace(/\/$/, '') || '/';
  document.querySelectorAll('.nav nav > a').forEach(link => {
    const target = new URL(link.href).pathname.replace(/\/$/, '') || '/';
    if (target === path || (target === '/products' && path.startsWith('/products/'))) link.setAttribute('aria-current', 'page');
  });
})();
