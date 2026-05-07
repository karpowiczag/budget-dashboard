import {
  AlertTriangle,
  ArrowDownRight,
  ArrowUpRight,
  Banknote,
  PiggyBank,
  ShieldCheck,
  WalletCards,
} from "lucide-react";
import { money, percent } from "../../domain/formatters.js";
import { Kpi } from "../ui/Kpi.jsx";

export function KpiStrip({ activeMonths, financialFlows, kpis, wants, onInspect }) {
  const investmentTotal = Number(financialFlows?.investmentTotal || 0);
  const savingsAccountTotal = Number(financialFlows?.savingsAccountTotal || 0);
  const savingsAccountInflows = Number(financialFlows?.savingsAccountInflows || 0);
  const savingsAccountOutflows = Number(financialFlows?.savingsAccountOutflows || 0);
  const loanOverpaymentTotal = Number(financialFlows?.loanOverpaymentTotal || 0);
  return (
    <section className="kpiGrid">
      <Kpi
        icon={Banknote}
        label="Dochód"
        value={money(kpis.income)}
        detail="tylko rozpoznane pensje"
        tone="good"
        onInspect={() => onInspect?.({ title: "Transakcje: dochód", filters: { flow: "income" }, useTimeScope: false })}
      />
      <Kpi
        icon={WalletCards}
        label="Wydatki analizowane"
        value={money(kpis.spend)}
        detail={`${money(kpis.spend / activeMonths)} / mies.`}
        onInspect={() => onInspect?.({ title: "Transakcje: wydatki analizowane", filters: { flow: "spend" }, useTimeScope: false })}
      />
      <Kpi icon={ArrowUpRight} label="Nadwyżka operacyjna" value={money(kpis.operatingSurplus)} detail={percent(kpis.savingsRate)} tone="good" />
      <Kpi
        icon={ShieldCheck}
        label="Inwestycje"
        value={money(investmentTotal)}
        detail={`${money(investmentTotal / activeMonths)} / mies.`}
        tone="good"
        onInspect={() => onInspect?.({ title: "Transakcje: inwestycje", filters: { category: "Oszczędności i inwestycje" }, useTimeScope: false })}
      />
      <Kpi
        icon={PiggyBank}
        label="Konto oszczędnościowe"
        value={money(savingsAccountTotal)}
        detail={`wpływy ${money(savingsAccountInflows)} · wydatki ${money(savingsAccountOutflows)}`}
        tone="good"
        onInspect={() => onInspect?.({ title: "Transakcje: konto oszczędnościowe", filters: { category: "Konto oszczędnościowe" }, useTimeScope: false })}
      />
      <Kpi
        icon={ShieldCheck}
        label="Nadpłaty kredytu"
        value={money(loanOverpaymentTotal)}
        detail={`${money(loanOverpaymentTotal / activeMonths)} / mies.`}
        tone="good"
        onInspect={() => onInspect?.({ title: "Transakcje: nadpłaty kredytu", filters: { category: "Nadpłata kredytu" }, useTimeScope: false })}
      />
      <Kpi
        icon={AlertTriangle}
        label="Do sprawdzenia"
        value={money(kpis.toCheckAmount)}
        detail={`${kpis.toCheck} transakcji`}
        tone={kpis.toCheckAmount ? "warn" : "good"}
        onInspect={() => onInspect?.({ title: "Transakcje: do sprawdzenia", filters: { category: "Do sprawdzenia" }, useTimeScope: false })}
      />
      <Kpi
        icon={ArrowDownRight}
        label="Nieobowiązkowe"
        value={money(wants)}
        detail={`${money(wants / activeMonths)} / mies.`}
        tone="warn"
        onInspect={() => onInspect?.({ title: "Transakcje: nieobowiązkowe", filters: { bucket: "Nieobowiązkowe" }, useTimeScope: false })}
      />
    </section>
  );
}
