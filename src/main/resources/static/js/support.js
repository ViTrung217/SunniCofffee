(() => {
  'use strict';
  const root = document.getElementById('support-chat');
  if (!root) return;
  const endpoint = root.dataset.endpoint;
  const admin = root.dataset.admin === 'true';
  const embedded = root.dataset.embedded === 'true';
  const messages = document.getElementById('support-messages');
  const notice = document.getElementById('support-notice');
  const state = document.getElementById('support-state');
  const input = document.getElementById('support-input');
  const send = document.getElementById('support-send');
  const older = document.getElementById('support-older');
  const toggle = document.getElementById('support-toggle-state');
  const csrf = document.querySelector('meta[name="_csrf"]').content;
  const known = new Map();
  let cursor = 0, readCursor = 0, loading = false, sending = false, closed = false, stopped = false;
  let active = !embedded, initial = true, pendingRequest = null, timer;
  const visible = () => active && !document.hidden && !stopped;
  function controls() { send.disabled = sending || stopped || (admin && closed); input.disabled = stopped || (admin && closed); }
  function expired() {
    stopped = true; clearTimeout(timer); known.clear(); messages.replaceChildren(); controls();
    notice.textContent = 'Phiên đăng nhập đã thay đổi. Vui lòng tải lại trang và đăng nhập lại.';
  }
  async function request(path, body) {
    const controller = new AbortController(); const timeout = setTimeout(() => controller.abort(), 12000);
    try {
      const response = await fetch(endpoint + path, {method: body ? 'POST' : 'GET', credentials: 'same-origin', cache: 'no-store', signal: controller.signal,
        headers: {'Accept':'application/json', ...(body ? {'Content-Type':'application/json','X-CSRF-TOKEN':csrf} : {})}, ...(body ? {body:JSON.stringify(body)} : {})});
      if (response.redirected || response.status === 401 || response.status === 403) { expired(); throw new Error('Phiên đăng nhập đã thay đổi.'); }
      if (!response.ok) throw new Error(response.status === 429 ? 'Bạn gửi tin hơi nhanh. Vui lòng đợi một phút.' : response.status === 409 ? 'Hội thoại đã thay đổi. Tải lại tin mới; nếu đã đóng, hãy mở lại trước khi trả lời.' : 'Chưa thực hiện được. Nội dung vẫn được giữ để bạn thử lại.');
      const raw = await response.text(); return raw ? JSON.parse(raw) : null;
    } finally { clearTimeout(timeout); }
  }
  function render(rows, prepend = false) {
    let changed = false;
    rows.forEach(row => { if (!known.has(row.id)) { known.set(row.id, row); changed = true; } });
    if (!changed && !initial) return;
    const previousHeight = messages.scrollHeight, previousTop = messages.scrollTop;
    const atBottom = previousHeight - previousTop - messages.clientHeight < 100;
    messages.replaceChildren();
    if (!known.size) { const p=document.createElement('p'); p.className='muted'; p.textContent='Chưa có tin nhắn. Gửi nội dung để bắt đầu trao đổi với cửa hàng.'; messages.append(p); }
    [...known.values()].sort((a,b) => a.id-b.id).forEach(row => {
      const article=document.createElement('article'); article.className='support-message' + (row.fromAdmin === admin ? ' mine' : '');
      const name=document.createElement('strong'); name.textContent=row.fromAdmin ? 'Nhân viên · ' + row.senderName : row.senderName;
      const content=document.createElement('p'); content.textContent=row.content;
      const time=document.createElement('time'); time.dateTime=row.createdAt+'+07:00'; time.textContent=new Date(row.createdAt+'+07:00').toLocaleString('vi-VN');
      article.append(name,content,time); messages.append(article);
    });
    if (prepend) messages.scrollTop=previousTop + messages.scrollHeight-previousHeight;
    else if (atBottom || initial) messages.scrollTop=messages.scrollHeight;
  }
  function updateState(conversation) {
    closed=Boolean(conversation?.closed);
    state.textContent=closed ? (admin ? 'Đã đóng hội thoại' : 'Đã đóng. Gửi tin mới để tiếp tục trao đổi.') : 'Đang mở · Tin nhắn được lưu trong tài khoản';
    if (toggle) { toggle.disabled=false; toggle.textContent=closed ? 'Mở lại hội thoại' : 'Đóng hội thoại'; }
    controls();
  }
  async function refresh(before = 0) {
    if (loading || !visible()) return;
    loading=true; older.disabled=true;
    try {
      const data=await request('/messages' + (before ? '?before='+before : '?after='+cursor));
      if (stopped) return;
      updateState(data.conversation); render(data.messages, before>0);
      if (initial || before) older.hidden=!data.hasOlder;
      if (!before) data.messages.forEach(row => { cursor=Math.max(cursor,row.id); });
      initial=false;
      if (visible() && cursor>readCursor) { await request('/read',{throughId:cursor}); readCursor=cursor; }
      if (data.hasMore && !before) { clearTimeout(timer); timer=setTimeout(() => refresh(), 50); }
    } catch (error) { if (!stopped) notice.textContent=error.name==='AbortError' ? 'Kết nối chậm; đang chờ để thử tải lại tin nhắn.' : error.message; }
    finally { loading=false; older.disabled=false; }
  }
  document.getElementById('support-form').addEventListener('submit',async event => {
    event.preventDefault(); const content=input.value.trim();
    if (!content || content.length>1500 || sending || stopped || (admin && closed)) return;
    if (!pendingRequest || pendingRequest.content!==content) pendingRequest={content,requestToken:crypto.randomUUID()};
    sending=true; controls(); notice.textContent='Đang gửi…';
    try {
      await request('/messages',pendingRequest);
      input.value=''; pendingRequest=null; notice.textContent='Đã gửi.';
      await refresh();
    } catch(error) { if (!stopped) notice.textContent=error.name==='AbortError' ? 'Chưa xác nhận gửi thành công. Bạn có thể bấm Gửi lại; hệ thống chống gửi trùng.' : error.message; }
    finally { sending=false; controls(); }
  });
  older.addEventListener('click',() => { if (known.size) refresh(Math.min(...known.keys())); });
  toggle?.addEventListener('click', async () => {
    toggle.disabled=true;
    try { await request('/state',{closed:!closed}); await refresh(); notice.textContent='Đã cập nhật trạng thái hội thoại.'; }
    catch(error) { if(!stopped) notice.textContent=error.message; }
    finally { if(!stopped) toggle.disabled=false; }
  });
  function schedule() { clearTimeout(timer); if(stopped) return; timer=setTimeout(async () => { await refresh(); schedule(); },3000); }
  document.addEventListener('visibilitychange',() => { if(visible()) refresh(); });
  window.addEventListener('message',event => {
    if(!embedded || event.source!==window.parent || event.origin!==window.location.origin || event.data?.type!=='suni-support-visible') return;
    active=event.data.visible===true; if(active) refresh();
  });
  document.addEventListener('keydown',event => { if(embedded && event.key==='Escape') window.parent.postMessage({type:'suni-chat-close'},window.location.origin); });
  window.addEventListener('pageshow',event => { if(event.persisted) window.location.reload(); });
  window.addEventListener('pagehide',() => { stopped=true; clearTimeout(timer); known.clear(); messages.replaceChildren(); });
  if(embedded) window.parent.postMessage({type:'suni-support-ready'},window.location.origin);
  refresh(); schedule();
})();
