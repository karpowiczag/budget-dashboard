import {
  AlertTriangle,
  ArrowDownRight,
  ArrowUpRight,
  Banknote,
  ShieldCheck,
  WalletCards,
} from "lucide-react";
import { money, percent } from "../../domain/formatters.js";
import { Kpi } from "../ui/Kpi.jsx";

export function KpiStrip({ activeMonths, kpis, wants }) {
  return (
    <section className="kpiGrid">
      <Kpi icon={Banknote} label="Dochód" value={money(kpis.income)} detail="tylko rozpoznane pensje" tone="good" />
      <Kpi icon={WalletCards} label="Wydatki analizowane" value={money(kpis.spend)} detail={`${money(kpis.spend / activeMonths)} / mies.`} />
      <Kpi icon={ArrowUpRight} label="Nadwyżka operacyjna" value={money(kpis.operatingSurplus)} detail={percent(kpis.savingsRate)} tone="good" />
      <Kpi icon={ShieldCheck} label="Inwestycje/nadpłaty" value={money(kpis.realSavingsOutgoing)} detail={`${money(kpis.realSavingsOutgoing / activeMonths)} / mies.`} tone="good" />
      <Kpi icon={AlertTriangle} label="Do sprawdzenia" value={money(kpis.toCheckAmount)} detail={`${kpis.toCheck} transakcji`} tone={kpis.toCheckAmount ? "warn" : "good"} />
      <Kpi icon={ArrowDownRight} label="Zachcianki" value={money(wants)} detail={`${money(wants / activeMonths)} / mies.`} tone="warn" />
    </section>
  );
}
