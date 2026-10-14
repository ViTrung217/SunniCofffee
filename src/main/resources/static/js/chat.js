(() => {
  'use strict';
  const form = document.getElementById('chat-form');
  if (!form) return;
  const input = document.getElementById('chat-input');
  const messages = document.getElementById('chat-messages');
  const status = document.getElementById('chat-status');
  const send = document.getElementById('chat-send');
  const clear = document.getElementById('chat-clear');
  const greeting = messages.firstElementChild.cloneNode(true);
  let pending = false;
  let conversationId = crypto.randomUUID();
  function add(role, text, sources = [], searchSuggestionsHtml = null) {
    const article = document.createElement('article');
    article.className = `chat-message ${role}`;
    const name = document.createElement('strong');
    name.textContent = role === 'user' ? 'Bạn' : 'SuniCoffee';
    const content = document.createElement('p');
    content.textContent = text; // Never parse user/model content as HTML or Markdown.
    article.append(name, content);
    if (sources.length) {
      const list = document.createElement('ul'); list.className = 'chat-sources';
      sources.forEach(source => {
        let url;
        try { url = new URL(source.url, window.location.origin); } catch (_) { return; }
        if (!(url.protocol === 'https:' || (url.origin === window.location.origin && url.pathname.startsWith('/')))) return;
        const item = document.createElement('li'); const a = document.createElement('a');
        a.href = url.href; a.textContent = source.title; a.target = '_blank'; a.rel = 'noopener noreferrer';
        item.append(a); list.append(item);
      });
      article.append(list);
    }
    if (role === 'assistant' && typeof searchSuggestionsHtml === 'string' && searchSuggestionsHtml.length <= 50000) {
      // Provider Search Suggestions stay isolated: no scripts, same-origin access, forms or top navigation.
      const suggestions = document.createElement('iframe');
      suggestions.title = 'Gợi ý tìm kiếm Google'; suggestions.className = 'chat-search-suggestions';
      suggestions.setAttribute('sandbox', 'allow-popups allow-popups-to-escape-sandbox');
      suggestions.referrerPolicy = 'no-referrer';
      suggestions.srcdoc = '<meta charset="utf-8"><meta http-equiv="Content-Security-Policy" content="default-src \'none\'; style-src \'unsafe-inline\'; img-src https: data:; base-uri \'none\'; form-action \'none\'">' + searchSuggestionsHtml;
      article.append(suggestions);
    }
    messages.append(article);
    while (messages.children.length > 60) messages.firstElementChild.remove();
    messages.scrollTop = messages.scrollHeight;
  }
  form.addEventListener('submit', async event => {
    event.preventDefault();
    const message = input.value.trim();
    if (pending || !message || message.length > 1500) return;
    pending = true; send.disabled = true; clear.disabled = true;
    add('user', message); input.value = ''; status.textContent = 'Đang tìm câu trả lời…';
    const controller = new AbortController(); const timeout = setTimeout(() => controller.abort(), 75000);
    try {
      const csrf = form.querySelector('input[name="_csrf"]');
      const response = await fetch(form.action, {
        method: 'POST', credentials: 'same-origin', cache: 'no-store', signal: controller.signal,
        headers: { 'Content-Type': 'application/json', 'Accept': 'application/json', 'X-CSRF-TOKEN': csrf?.value || '' },
        body: JSON.stringify({message, conversationId})
      });
      if (response.redirected || response.status === 401 || response.status === 403) {
        messages.replaceChildren(greeting.cloneNode(true));
        conversationId = crypto.randomUUID();
        add('assistant', 'Phiên làm việc đã thay đổi. Vui lòng tải lại trang và đăng nhập lại nếu cần tra cứu đơn.');
      } else if (response.ok || response.status === 429) {
        const reply = await response.json();
        if (typeof reply.answer !== 'string' || !Array.isArray(reply.sources)) throw new Error('Invalid chat response');
        add('assistant', reply.answer, reply.sources, reply.searchSuggestionsHtml);
      } else { add('assistant', 'Chưa gửi được tin nhắn. Vui lòng thử lại sau.'); }
    } catch (_) { add('assistant', 'Kết nối bị gián đoạn hoặc phản hồi quá lâu. Vui lòng gửi lại câu hỏi.'); }
    finally { clearTimeout(timeout); pending = false; send.disabled = false; clear.disabled = false; status.textContent = ''; if (document.hasFocus()) input.focus(); }
  });
  document.querySelectorAll('[data-question]').forEach(button => button.addEventListener('click', () => {
    input.value = button.dataset.question; input.focus();
  }));
  clear.addEventListener('click', () => { conversationId = crypto.randomUUID(); messages.replaceChildren(greeting.cloneNode(true)); input.value = ''; input.focus(); });
  document.addEventListener('keydown', event => {
    if (event.key === 'Escape' && window.parent !== window) {
      window.parent.postMessage({type: 'suni-chat-close'}, window.location.origin);
    }
  });
  // Avoid restoring private order replies when returning via the browser back/forward cache.
  window.addEventListener('pageshow', event => { if (event.persisted) window.location.reload(); });
})();
