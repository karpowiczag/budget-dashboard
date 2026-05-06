import { CalendarDays, CircleDollarSign, Filter, Search, SlidersHorizontal } from "lucide-react";
import { TransactionsTable } from "../components/tables/TransactionsTable.jsx";
import { Panel } from "../components/ui/Panel.jsx";
import { money } from "../domain/formatters.js";

export function TransactionsView({
  activeTimeLabel,
  bucket,
  buckets,
  drillFilteredTransactions,
  filteredTransactions,
  query,
  visibleSpend,
  onBucketChange,
  onQueryChange,
}) {
  return (
    <Panel
      title="Transakcje"
      className="wide"
      action={
        <div className="panelActions">
          <label className="field">
            <Search size={16} />
            <input value={query} onChange={(event) => onQueryChange(event.target.value)} placeholder="Szukaj sprzedawcy, opisu, kategorii" />
          </label>
          <label className="select">
            <Filter size={16} />
            <select value={bucket} onChange={(event) => onBucketChange(event.target.value)}>
              {buckets.map((option) => (
                <option key={option}>{option}</option>
              ))}
            </select>
          </label>
        </div>
      }
    >
      <div className="tableMeta">
        <span>
          <SlidersHorizontal size={15} /> Widoczne: {filteredTransactions.length}
        </span>
        <span>Zakres po filtrach: {drillFilteredTransactions.length}</span>
        <span>
          <CircleDollarSign size={15} /> Suma wydatków widocznych: {money(visibleSpend)}
        </span>
        <span>
          <CalendarDays size={15} /> {activeTimeLabel}
        </span>
      </div>
      <TransactionsTable transactions={filteredTransactions} />
    </Panel>
  );
}
