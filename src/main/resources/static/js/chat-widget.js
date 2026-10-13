(() => {
  'use strict';
  const root = document.getElementById('chat-widget');
  if (!root) return;
  const toggle = document.getElementById('chat-widget-toggle');
  const panel = document.getElementById('chat-widget-panel');
  const close = document.getElementById('chat-widget-close');
  const frame = document.getElementById('chat-widget-frame');
  let opened = false;
  function setOpen(value) {
    opened = value; panel.hidden = !value;
    toggle.setAttribute('aria-expanded', String(value));
    toggle.setAttribute('aria-label', value ? 'Thu gọn trò chuyện với SuniCoffee' : 'Mở trò chuyện với SuniCoffee');
    if (value) {
      if (!frame.getAttribute('src')) frame.src = frame.dataset.src;
      close.focus();
    } else toggle.focus();
  }
  toggle.hidden = false;
  toggle.addEventListener('click', () => setOpen(!opened));
  close.addEventListener('click', () => setOpen(false));
  root.addEventListener('keydown', event => {
    if (event.key === 'Escape' && opened) { event.preventDefault(); setOpen(false); }
  });
  window.addEventListener('message', event => {
    if (event.origin !== window.location.origin || event.source !== frame.contentWindow) return;
    if (event.data?.type === 'suni-chat-close' && opened) setOpen(false);
  });
  // Never retain a private order reply through the back/forward cache or a changed login.
  window.addEventListener('pageshow', event => {
    if (event.persisted) { frame.removeAttribute('src'); setOpen(false); }
  });
})();
