import json, statistics as st

def median_iqr(values):
    if not values: return None,None,None
    sv=sorted(values); n=len(sv)
    med=st.median(sv)
    def pct(p):
        idx=p*(n-1); lo=int(idx); hi=min(lo+1,n-1); frac=idx-lo
        return sv[lo]*(1-frac)+sv[hi]*frac
    return med, pct(0.25), pct(0.75)

d = json.load(open('rigorous_reference_results.json'))
REF_METRICS = json.load(open('reference_comparison_summary.json'))
REFS = [('prov-o','PROV-O'),('ssn','SSN'),('sosa','SOSA'),('pizza','Pizza'),('goslim_generic','GO slim'),('wine','Wine')]
VARIANTS = [('UNIFORM_RANDOM','Uniform Random'),('CHAIN','Chain'),('BALANCED_TREE','Balanced Tree'),('PREFERENTIAL','Preferential')]
HELD_OUT = ['tangledness','avg_degree','clustering','inferred_ratio']

lines = []
for refkey, refname in REFS:
    rv = REF_METRICS[refkey]['reference']
    lines.append(f"{refname} & \\textit{{Reference}} & --- & {rv['hierarchy_depth']:.0f} & {rv['branching']:.2f} & {rv['tangledness']:.3f} & {rv['avg_degree']:.2f} & {rv['clustering']:.3f} & {rv['inferred_ratio']:.3f} \\\\")
    for vkey, vname in VARIANTS:
        runs=[d[f'{refkey}_{vkey}_{s}'] for s in range(1,11)]
        consistent=[r for r in runs if r.get('verif_status')=='CONSISTENT']
        n=len(consistent)
        if n==0:
            lines.append(f"        & {vname} & 0  & --- & --- & --- & --- & --- & --- \\\\")
            continue
        cells=[]
        for m in ['hierarchy_depth','branching','tangledness','avg_degree','clustering','inferred_ratio']:
            vals=[r[m] for r in consistent if r.get(m) is not None]
            med,q1,q3 = median_iqr(vals)
            inrange = (q1 <= rv[m] <= q3) if m in HELD_OUT else False
            txt = f"{med:.2f}"
            if inrange: txt = "\\textbf{" + txt + "}"
            cells.append(txt)
        lines.append(f"        & {vname} & {n} & " + " & ".join(cells) + " \\\\")
    lines.append("\\midrule")

print("\n".join(lines))
