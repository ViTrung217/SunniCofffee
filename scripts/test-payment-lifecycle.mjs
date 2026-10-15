import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import vm from 'node:vm';

const code = await readFile(new URL('../src/main/resources/static/js/payment.js', import.meta.url), 'utf8');
async function mount(paymentStatus = 'UNPAID') {
  const events = {}, intervals = new Map();
  let reloads = 0, redirected = null;
  const nodes = {
    'payment-check': { action: '/orders/1/payment/check', querySelector: s => s === 'button' ? {} : { name: '_csrf', value: 'test' }, addEventListener() {} },
    'payment-box': { dataset: { auto: 'true', successUrl: '/orders/1/payment/success' } },
    'payment-status': {}, 'payment-received': {}, 'payment-qr': {}
  };
  vm.runInNewContext(code, {
    document: { getElementById: id => nodes[id], hidden: false },
    window: { addEventListener: (key, fn) => events[key] = fn,
      location: { reload: () => reloads++, replace: url => redirected = url } },
    fetch: async () => ({ ok: true, status: 200, json: async () => ({
      status: paymentStatus, label: paymentStatus, received: 100000, payable: paymentStatus === 'UNPAID'
    }) }),
    URLSearchParams, AbortController, Intl, Date, setTimeout, clearTimeout,
    setInterval: fn => { intervals.set(1, fn); return 1; },
    clearInterval: id => intervals.delete(id)
  });
  await new Promise(resolve => setTimeout(resolve, 0));
  return { events, intervals, reloads: () => reloads, redirected: () => redirected };
}
const page = await mount();
assert.equal(page.intervals.size, 1);
page.events.pageshow({ persisted: false });
assert.equal(page.reloads(), 0, 'ordinary load must not reload in a loop');
page.events.pagehide({ persisted: true });
assert.equal(page.intervals.size, 0);
page.events.pageshow({ persisted: true });
assert.equal(page.reloads(), 1, 'cached page must reload authoritative state and initialize polling');
for (const status of ['PAID', 'UNPAID', 'REVIEW', 'REFUND_REQUIRED']) {
  const result = await mount(status);
  assert.equal(result.redirected(), status === 'PAID' ? '/orders/1/payment/success' : null);
}
console.log('PASS: payment cached-page recovery, ordinary load, and redirects for four payment states');
