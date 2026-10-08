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
NON_REASONING = ['hierarchy_depth','branching','tangledness','avg_degree','clustering']

print("=== TABLE: Entity-Selection Strategies (512 requested) -- ALL runs for structural metrics, CONSISTENT-only for ratio ===")
for var in VARIANTS:
    runs=[d[f'scaling_8_{var}_{s}'] for s in range(1,11)]
    all_completed = [r for r in runs if r.get('status')=='COMPLETED']
    consistent = [r for r in runs if r.get('verif_status')=='CONSISTENT']
    n_verified = len(consistent)
    n_completed = len(all_completed)
    line = f'{var:<15} verified={n_verified}/10 completed={n_completed}/10  '
    for m in NON_REASONING:
        vals=[r[m] for r in all_completed if r.get(m) is not None]
        med,q1,q3 = median_iqr(vals)
        line += f'{m}={med:.3f} ' if med is not None else f'{m}=N/A '
    ratio_vals = [r['inferred_ratio'] for r in consistent if r.get('inferred_ratio') is not None]
    rmed,_,_ = median_iqr(ratio_vals)
    line += f'ratio={rmed:.3f}(n={len(ratio_vals)})' if rmed is not None else 'ratio=N/A'
    print(line)
