#!/usr/bin/env python3
"""Small local read-only HTTP load smoke; not a production capacity benchmark."""
import concurrent.futures, json, time, urllib.request, argparse, statistics
p=argparse.ArgumentParser();p.add_argument('--port',type=int,default=18085);p.add_argument('--requests',type=int,default=100);p.add_argument('--workers',type=int,default=10);args=p.parse_args()
if not (1 <= args.requests <= 1000 and 1 <= args.workers <= 20): p.error('At most 1000 requests and 20 workers')
url=f'http://127.0.0.1:{args.port}/api/products'
def fetch(_):
    start=time.perf_counter()
    try:
        with urllib.request.urlopen(url,timeout=10) as response:
            data=json.load(response)
            ok=response.status==200 and isinstance(data.get('items'),list)
    except Exception:
        ok=False
    return ok,(time.perf_counter()-start)*1000
fetch(0)
start=time.perf_counter()
with concurrent.futures.ThreadPoolExecutor(max_workers=args.workers) as pool: results=list(pool.map(fetch,range(args.requests)))
values=sorted(ms for _,ms in results)
print(json.dumps({'url':url,'requests':args.requests,'workers':args.workers,'success':sum(ok for ok,_ in results),'elapsed_seconds':round(time.perf_counter()-start,3),'median_ms':round(statistics.median(values),2),'p95_ms':round(values[max(0,int(len(values)*.95)-1)],2),'max_ms':round(max(values),2),'scope':'local read-only API smoke, not sustained capacity'},indent=2))
raise SystemExit(0 if all(ok for ok,_ in results) else 1)
