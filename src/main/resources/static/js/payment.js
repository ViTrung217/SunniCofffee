(() => {
  'use strict';
  const form = document.getElementById('payment-check');
  if (!form) return;
  const box = document.getElementById('payment-box');
  const status = document.getElementById('payment-status');
  const button = form.querySelector('button');
  const csrf = form.querySelector('input[type=hidden]');
  let busy = false, automatic = box.dataset.auto === 'true';
  const started = Date.now();
  async function check() {
    if (busy || !csrf) return;
    busy = true; button.disabled = true;
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), 12000);
    try {
      const body = new URLSearchParams(); body.set(csrf.name, csrf.value);
      const response = await fetch(form.action, { method: 'POST', body, credentials: 'same-origin', cache: 'no-store', signal: controller.signal });
      if (response.redirected || response.status === 401 || response.status === 403) {
        automatic = false; status.textContent = 'Phiên đã hết hạn. Hãy tải lại trang và đăng nhập.'; return;
      }
      const data = await response.json();
      if (!response.ok) throw new Error(data.message || 'Chưa kiểm tra được thanh toán. Vui lòng thử lại sau.');
      if (data.status === 'PAID' && box.dataset.successUrl) {
        automatic = false; window.location.replace(box.dataset.successUrl); return;
      }
      status.textContent = data.label;
      document.getElementById('payment-received').textContent = 'Đã ghi nhận: ' + new Intl.NumberFormat('vi-VN').format(data.received) + ' ₫';
      document.getElementById('payment-qr').hidden = !data.payable;
      automatic = data.payable;
      if (data.status === 'PAID') status.textContent += ' — Bạn có thể quay lại chi tiết đơn hàng.';
      if (data.status === 'REVIEW' || data.status === 'REFUND_REQUIRED') status.textContent += ' — Vui lòng liên hệ cửa hàng, không chuyển thêm tiền.';
    } catch (error) {
      status.textContent = error.name === 'AbortError' ? 'Kiểm tra đang chậm. Không chuyển lại tiền; vui lòng thử kiểm tra sau.' : error.message;
    } finally { clearTimeout(timeout); busy = false; button.disabled = false; }
  }
  form.addEventListener('submit', event => { event.preventDefault(); check(); });
  if (automatic) check();
  const timer = setInterval(() => {
    if (Date.now() - started >= 20 * 60 * 1000) { clearInterval(timer); return; }
    if (automatic && !document.hidden) check();
  }, 15000);
  window.addEventListener('pagehide', () => clearInterval(timer));
})();
