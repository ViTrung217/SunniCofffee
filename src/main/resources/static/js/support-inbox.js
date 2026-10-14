(() => {
  'use strict';
  const root=document.getElementById('support-inbox'); if(!root) return;
  const body=document.getElementById('support-threads'), status=document.getElementById('inbox-status');
  let stopped=false, timer;
  async function update() {
    if(document.hidden || stopped) return;
    const controller=new AbortController(), timeout=setTimeout(() => controller.abort(),10000);
    try {
      const params=new URLSearchParams({q:root.dataset.query,state:root.dataset.state,page:root.dataset.page});
      const response=await fetch(root.dataset.endpoint+'?'+params,{credentials:'same-origin',cache:'no-store',headers:{Accept:'application/json'},signal:controller.signal});
      if(response.redirected || response.status===401 || response.status===403) { stopped=true;body.replaceChildren();status.textContent='Phiên đăng nhập đã thay đổi. Vui lòng tải lại trang.';return; }
      if(!response.ok) throw new Error();
      const data=await response.json(); if(stopped) return; body.replaceChildren();
      data.conversations.forEach(c => {
        const tr=document.createElement('tr'), customer=document.createElement('td'), a=document.createElement('a'), email=document.createElement('small');
        a.href=root.dataset.base+c.id;a.textContent=c.customerName;email.textContent=c.customerEmail;customer.append(a,document.createElement('br'),email);tr.append(customer);
        [c.preview,c.unread,c.closed?'Đã đóng':'Đang mở',new Date(c.updatedAt+'+07:00').toLocaleString('vi-VN')].forEach(value => {const td=document.createElement('td');td.textContent=value;tr.append(td);});
        body.append(tr);
      });
      if(!data.conversations.length) {const tr=document.createElement('tr'),td=document.createElement('td');td.colSpan=5;td.textContent='Chưa có hội thoại phù hợp.';tr.append(td);body.append(tr);}
      document.getElementById('inbox-next').hidden=!data.hasMore;
      status.textContent='Đã cập nhật hộp thư · '+new Date().toLocaleTimeString('vi-VN');
    } catch(_) { status.textContent='Chưa cập nhật được hộp thư. Sẽ thử lại tự động.'; }
    finally { clearTimeout(timeout); }
  }
  function schedule() {timer=setTimeout(async () => { await update();if(!stopped) schedule(); },5000);}
  window.addEventListener('pagehide',() => {stopped=true;clearTimeout(timer);body.replaceChildren();});
  window.addEventListener('pageshow',event => {if(event.persisted) window.location.reload();});
  schedule();
})();
