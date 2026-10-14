(() => {
  'use strict';
  const root = document.getElementById('chat-widget');
  if (!root) return;
  const toggle = document.getElementById('chat-widget-toggle');
  const panel = document.getElementById('chat-widget-panel');
  const close = document.getElementById('chat-widget-close');
  const frame = document.getElementById('chat-widget-frame');
  const supportFrame = document.getElementById('support-widget-frame');
  let opened = false, mode = 'ai';
  function notifySupport() {
    if (supportFrame?.getAttribute('src')) supportFrame.contentWindow.postMessage({type:'suni-support-visible',visible:opened && mode === 'support'},window.location.origin);
  }
  function loadChannel() {
    const selected = mode === 'support' ? supportFrame : frame;
    if (opened && selected && !selected.getAttribute('src')) selected.src = selected.dataset.src;
    frame.hidden = mode !== 'ai'; if (supportFrame) supportFrame.hidden = mode !== 'support';
    document.getElementById('chat-widget-title').textContent = mode === 'support' ? 'Nhân viên SuniCoffee' : 'Trợ lý SuniCoffee';
    root.querySelectorAll('[data-chat-mode]').forEach(button => button.setAttribute('aria-pressed',String(button.dataset.chatMode === mode)));
    notifySupport();
  }
  root.querySelectorAll('[data-chat-mode]').forEach(button => button.addEventListener('click',() => { mode = button.dataset.chatMode; loadChannel(); }));
  function setOpen(value) {
    opened = value; panel.hidden = !value;
    toggle.setAttribute('aria-expanded', String(value));
    toggle.setAttribute('aria-label', value ? 'Thu gọn trò chuyện với SuniCoffee' : 'Mở trò chuyện với SuniCoffee');
    if (value) {
      loadChannel();
      close.focus();
    } else { notifySupport(); toggle.focus(); }
  }
  toggle.hidden = false;
  toggle.addEventListener('click', () => setOpen(!opened));
  close.addEventListener('click', () => setOpen(false));
  root.addEventListener('keydown', event => {
    if (event.key === 'Escape' && opened) { event.preventDefault(); setOpen(false); }
  });
  window.addEventListener('message', event => {
    if (event.origin !== window.location.origin || (event.source !== frame.contentWindow && event.source !== supportFrame?.contentWindow)) return;
    if (event.data?.type === 'suni-support-ready') notifySupport();
    if (event.data?.type === 'suni-chat-close' && opened) setOpen(false);
  });
  // Never retain a private order reply through the back/forward cache or a changed login.
  window.addEventListener('pageshow', event => {
    if (event.persisted) { frame.removeAttribute('src'); supportFrame?.removeAttribute('src'); setOpen(false); }
  });
})();
