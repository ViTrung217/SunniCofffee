// Behavioral checks for autoplay, accessibility preferences and page lifecycle.
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import vm from 'node:vm';
const code = await readFile(new URL('../src/main/resources/static/js/hero-carousel.js', import.meta.url), 'utf8');
function mount({ reduced = false } = {}) {
  function element() {
    const listeners = {}, attrs = {}, classes = new Set();
    return {listeners, attrs, hidden:false, disabled:false, textContent:'',
      classList: {contains:k => classes.has(k),remove:k => classes.delete(k),toggle(k,v) { if(v) classes.add(k); else classes.delete(k); }},
      setAttribute(k,v) { attrs[k]=v; }, addEventListener(k,fn) { listeners[k]=fn; }, append() {},
      animate() { return { cancel() {} }; }
    };
  }
  const slides = Array.from({length:4}, element), dots = Array.from({length:4},element);
  const nodes = Object.fromEntries(['[data-slide-pause]', '.hero-slider-controls', '[data-slide-prev]', '[data-slide-next]', '[data-slide-status]'].map(k=>[k,element()]));
  const root = element(); root.querySelector = k => nodes[k]; root.querySelectorAll = k => k === '.hero-slide' ? slides : dots;
  root.contains = target => Object.values(nodes).includes(target) || dots.includes(target);
  const html = element(); const doc = {hidden:false, documentElement:html, listeners:{}, querySelector:()=>root,createElement:element,addEventListener(k,fn){this.listeners[k]=fn;}};
  const media = {matches:reduced,listeners:{},addEventListener(k,fn){this.listeners[k]=fn;}};
  const win = {listeners:{},matchMedia:()=>media,addEventListener(k,fn){this.listeners[k]=fn;}};
  let visibility, id=0;const timers = new Map();
  const IntersectionObserver = class {constructor(fn){visibility=fn;} observe() {}};
  win.IntersectionObserver = IntersectionObserver;
  vm.runInNewContext(code, {document:doc, window:win, IntersectionObserver,
    setTimeout:fn=>{timers.set(++id,fn);return id;},clearTimeout:id=>timers.delete(id)});
  return {root,slides,dots,nodes,doc,html,media,win,timers, visibility:yes=>visibility([{isIntersecting:yes}]),
    advance() {const [key,fn]=timers.entries().next().value;timers.delete(key);fn();}};
}
const p = mount();
assert.equal(p.timers.size,1,'one autoplay timer');
assert.equal(p.slides.filter(s=>!s.hidden).length,1,'one visible slide');
p.advance();assert.equal(p.dots[1].attrs['aria-pressed'],'true','autoplay advances');
assert.equal(p.nodes['[data-slide-status]'].textContent,'','autoplay does not spam live region');
p.root.listeners.pointerenter({pointerType:'mouse'});assert.equal(p.timers.size,0,'hover pauses');
p.root.listeners.pointerleave({pointerType:'mouse'});assert.equal(p.timers.size,1);
p.root.listeners.focusin();assert.equal(p.timers.size,0,'keyboard focus pauses');
p.nodes['[data-slide-next]'].listeners.click();assert.equal(p.dots[2].attrs['aria-pressed'],'true');
assert.equal(p.nodes['[data-slide-status]'].textContent,'Ảnh 3 / 4');
p.root.listeners.focusout({relatedTarget:null});assert.equal(p.timers.size,1);
p.visibility(false);assert.equal(p.timers.size,0,'offscreen pauses');
p.visibility(true);assert.equal(p.timers.size,1);
p.doc.hidden=true;p.doc.listeners.visibilitychange();assert.equal(p.timers.size,0,'hidden tab pauses');
p.doc.hidden=false;p.doc.listeners.visibilitychange();assert.equal(p.timers.size,1);
p.nodes['[data-slide-pause]'].listeners.click();assert.equal(p.timers.size,0,'explicit pause');
p.visibility(false);p.visibility(true);assert.equal(p.timers.size,0,'explicit pause survives visibility changes');
p.nodes['[data-slide-pause]'].listeners.click();assert.equal(p.timers.size,1);
p.html.classList.toggle('reduce-motion',true);p.doc.listeners['suni:motionchange']();
assert.equal(p.timers.size,0,'manual reduced motion cancels autoplay');
assert.equal(p.nodes['[data-slide-pause]'].disabled,true);
assert.ok(p.slides.every(s=>!s.classList.contains('is-entering')),'photo animations removed');
p.nodes['[data-slide-next]'].listeners.click();assert.equal(p.dots[3].attrs['aria-pressed'],'true','manual navigation remains available');
p.html.classList.toggle('reduce-motion',false);p.doc.listeners['suni:motionchange']();assert.equal(p.timers.size,1);
p.win.listeners.pagehide();assert.equal(p.timers.size,0,'pagehide cancels timer');
p.win.listeners.pageshow();assert.equal(p.timers.size,1,'back-forward restore starts one timer');
p.win.listeners.pageshow();assert.equal(p.timers.size,1,'repeated pageshow never duplicates timer');
let prevented=false;p.root.listeners.keydown({key:'ArrowRight',preventDefault(){prevented=true;}});
assert.equal(prevented,true);assert.equal(p.dots[0].attrs['aria-pressed'],'true','arrow key wraps');
const target={closest:()=>null};
p.root.listeners.pointerdown({isPrimary:true,pointerType:'touch',clientX:250,clientY:10,target});
p.root.listeners.pointerup({clientX:100,clientY:20});assert.equal(p.dots[1].attrs['aria-pressed'],'true','horizontal swipe advances');
p.root.listeners.pointerdown({isPrimary:true,pointerType:'touch',clientX:250,clientY:10,target});
p.root.listeners.pointerup({clientX:230,clientY:150});assert.equal(p.dots[1].attrs['aria-pressed'],'true','vertical scroll does not advance');
const r=mount({reduced:true});assert.equal(r.timers.size,0,'OS reduced motion disables autoplay on initial load');
r.nodes['[data-slide-next]'].listeners.click();assert.equal(r.dots[1].attrs['aria-pressed'],'true');
console.log('PASS: carousel autoplay, hover/focus, offscreen/hidden tab, manual/OS reduced motion, swipe, keyboard, BFCache lifecycle');
