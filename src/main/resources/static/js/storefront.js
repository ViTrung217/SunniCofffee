/* Progressive enhancement: all content and navigation work without JavaScript. */
(() => {
  'use strict';
  const media = window.matchMedia('(prefers-reduced-motion: reduce)');
  const key = 'suni-reduce-motion';
  let manual = false;
  try { manual = localStorage.getItem(key) === 'true'; } catch (_) { /* Storage is optional. */ }
  const toggles = document.querySelectorAll('[data-motion-toggle]');
  let observer;
  const animations = new Set();
  const reduced = () => media.matches || manual;
  const sync = () => {
    document.documentElement.classList.toggle('reduce-motion', reduced());
    toggles.forEach(toggle => {
      toggle.hidden = false;
      toggle.disabled = media.matches;
      toggle.setAttribute('aria-pressed', String(reduced()));
      toggle.textContent = media.matches ? 'Đã giảm chuyển động theo thiết bị' : manual ? 'Bật chuyển động' : 'Tắt chuyển động';
    });
    if (reduced()) {
      observer?.disconnect();
      animations.forEach(animation => animation.cancel());
      animations.clear();
    }
    document.dispatchEvent(new CustomEvent('suni:motionchange'));
  };
  sync();
  toggles.forEach(toggle => toggle.addEventListener('click', () => {
    manual = !manual;
    try { localStorage.setItem(key, String(manual)); } catch (_) { /* Preference works this visit. */ }
    sync();
    if (!reduced()) observe();
  }));
  media.addEventListener('change', () => { sync(); if (!reduced()) observe(); });
  window.addEventListener('storage', event => {
    if (event.key !== key && event.key !== null) return;
    manual = event.newValue === 'true';
    sync();
    if (!reduced()) observe();
  });
  function observe() {
    if (reduced() || !('IntersectionObserver' in window) || !Element.prototype.animate) return;
    observer?.disconnect();
    observer = new IntersectionObserver(entries => {
      let stagger = 0;
      entries.forEach(entry => {
        if (!entry.isIntersecting) return;
        const element = entry.target;
        observer.unobserve(element);
        if (element.dataset.revealed) return;
        element.dataset.revealed = 'true';
        const animation = element.animate([
          { opacity: .25, transform: 'translateY(20px)' },
          { opacity: 1, transform: 'translateY(0)' }
        ], { duration: 650, delay: Math.min(stagger++ * 55, 220), easing: 'cubic-bezier(.22,1,.36,1)', fill: 'none' });
        animations.add(animation);
        animation.finished.then(() => animations.delete(animation)).catch(() => animations.delete(animation));
      });
    }, { threshold: 0.06 });
    // Avoid nested reveals: a panel and its children should not animate twice.
    const candidates = [...document.querySelectorAll('[data-reveal], .category-tile, .metric-grid > .panel, main > .panel, main > .table-wrap')];
    candidates.filter(element => !candidates.some(parent => parent !== element && parent.contains(element)))
      .filter(element => !element.dataset.revealed).forEach(element => observer.observe(element));
  }
  observe();
  const accountMenu = document.querySelector('.account-menu');
  if (accountMenu) {
    document.addEventListener('click', event => { if (!accountMenu.contains(event.target)) accountMenu.open = false; });
    document.addEventListener('keydown', event => {
      if (event.key === 'Escape' && accountMenu.open) {
        accountMenu.open = false;
        accountMenu.querySelector('summary').focus();
      }
    });
  }
  const path = window.location.pathname.replace(/\/$/, '') || '/';
  document.querySelectorAll('.main-navigation > a, .account-dropdown > a, .tabs > a').forEach(link => {
    const target = new URL(link.href).pathname.replace(/\/$/, '') || '/';
    if (target === path || (target !== '/' && path.startsWith(target + '/'))) link.setAttribute('aria-current', 'page');
  });
  // A keyboard user can scroll wide management tables without trapping focus.
  document.querySelectorAll('.table-wrap').forEach(table => {
    table.tabIndex = 0;
    table.setAttribute('role', 'region');
    table.setAttribute('aria-label', 'Bảng dữ liệu, có thể cuộn ngang');
  });
  document.querySelectorAll('input[type=password]').forEach((input, index) => {
    const wrapper = document.createElement('span');
    wrapper.className = 'password-control';
    input.before(wrapper);
    wrapper.append(input);
    if (!input.id) input.id = `suni-password-${index}`;
    const button = document.createElement('button');
    button.type = 'button';
    button.className = 'password-toggle';
    button.textContent = 'Hiện';
    button.setAttribute('aria-label', 'Hiện mật khẩu');
    button.setAttribute('aria-controls', input.id);
    button.setAttribute('aria-pressed', 'false');
    button.addEventListener('click', () => {
      const visible = input.type === 'password';
      input.type = visible ? 'text' : 'password';
      button.textContent = visible ? 'Ẩn' : 'Hiện';
      button.setAttribute('aria-label', visible ? 'Ẩn mật khẩu' : 'Hiện mật khẩu');
      button.setAttribute('aria-pressed', String(visible));
    });
    wrapper.append(button);
    // Never restore a visible password when returning from the back-forward cache.
    window.addEventListener('pagehide', () => {
      input.type = 'password';
      button.textContent = 'Hiện';
      button.setAttribute('aria-label', 'Hiện mật khẩu');
      button.setAttribute('aria-pressed', 'false');
    });
  });
  const backTop = document.querySelector('[data-back-top]');
  const header = document.querySelector('.header');
  if (backTop && header && 'IntersectionObserver' in window) {
    new IntersectionObserver(([entry]) => { backTop.hidden = entry.isIntersecting; }).observe(header);
  }
})();
