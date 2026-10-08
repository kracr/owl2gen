import json, statistics as st

def median_iqr(values):
    if not values: return None,None,None
    sv=sorted(values); n=len(sv)
    med=st.median(sv)
    def pct(p):
        idx=p*(n-1); lo=int(idx); hi=min(lo+1,n-1); frac=idx-lo
        return sv[lo]*(1-frac)+sv[hi]*frac
    return med, pct(0.25), pct(0.75)

d = json.load(open('rigorous_basic_results.json'))
VARIANTS = ['UNIFORM_RANDOM','CHAIN','BALANCED_TREE','PREFERENTIAL']

print("=== Table 3 equivalent: Scalability (consistency + median time) ===")
for pc, req in [(2,128),(5,320),(8,512)]:
    for var in VARIANTS:
        runs=[d[f'scaling_{pc}_{var}_{s}'] for s in range(1,11)]
        statuses=[r.get('verif_status', r.get('status')) for r in runs]
        n_ok = statuses.count('CONSISTENT')
        times=[r['elapsed_s'] for r in runs if r.get('verif_status')=='CONSISTENT']
        med,q1,q3=median_iqr(times)
        if med is not None:
            print(f'{req:>4} {var:<15} {n_ok:>2}/10  {med:.2f}s [{q1:.2f},{q3:.2f}]')
        else:
            print(f'{req:>4} {var:<15} {n_ok:>2}/10  N/A')

print()
print("=== Table 2 equivalent: Entity-Selection Strategies (512 requested, structural medians) ===")
for var in VARIANTS:
    runs=[d[f'scaling_8_{var}_{s}'] for s in range(1,11)]
    consistent=[r for r in runs if r.get('verif_status')=='CONSISTENT']
    n=len(consistent)
    line=f'{var:<15} n={n}/10 '
    for m in ['hierarchy_depth','branching','tangledness','avg_degree','clustering','inferred_ratio']:
        vals=[r[m] for r in consistent if r.get(m) is not None]
        med,_,_=median_iqr(vals)
        line += f'{m}={med if med is None else round(med,3)} '
    print(line)

print()
print("=== Table 1 equivalent: Structural Variation (320 requested, Uniform Random, mean/std/min/max) ===")
runs=[d[f'scaling_5_UNIFORM_RANDOM_{s}'] for s in range(1,11)]
consistent=[r for r in runs if r.get('verif_status')=='CONSISTENT']
print(f'n consistent = {len(consistent)}/10')
for m, label in [('axiomCount','Final axiom count'),('hierarchy_depth','Hierarchy depth'),('branching','Avg branching'),
                 ('tangledness','Tangledness'),('nesting_max_depth','Max nesting depth'),('avg_degree','Avg degree'),
                 ('clustering','Clustering'),('inferred_ratio','Inferred/asserted')]:
    vals=[r[m] for r in consistent if r.get(m) is not None]
    if vals:
        print(f'{label:<20} mean={st.mean(vals):.3f} std={st.pstdev(vals):.3f} min={min(vals):.3f} max={max(vals):.3f}')
