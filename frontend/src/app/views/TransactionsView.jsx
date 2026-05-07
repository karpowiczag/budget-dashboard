import { useState } from "react";
import { ArrowUpDown, CalendarDays, CircleDollarSign, Filter, RotateCcw, Search, SlidersHorizontal } from "lucide-react";
import { DataQualityChart } from "../components/charts/DataQualityChart.jsx";
import { TransactionsTable } from "../components/tables/TransactionsTable.jsx";
import { Panel } from "../components/ui/Panel.jsx";
import { money } from "../domain/formatters.js";

export function TransactionsView({
  activeTimeLabel,
  bucket,
  buckets,
  drillFilteredTransactions,
  dataQualityChart,
  filterOptions,
  filteredTransactions,
  presets,
  query,
  transactionFilters,
  transactionPage,
  visibleSpend,
  yearTransactionTotal,
  onInspect,
  onBucketChange,
  onFilterChange,
  onPageChange,
  onPreset,
  onQueryChange,
  onResetFilters,
  onShowFullYear,
}) {
  const [advancedOpen, setAdvancedOpen] = useState(false);
  const totalItems = transactionPage?.totalItems || filteredTransactions.length;
  const page = transactionPage?.page || 0;
  const totalPages = transactionPage?.totalPages || 0;
  const options = filterOptions || {};
  const filters = transactionFilters || {};
  const yearlyTotal = Number(yearTransactionTotal || 0);
  const isScopedBelowYear = yearlyTotal > 0 && totalItems < yearlyTotal;

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
          <button type="button" className="secondaryButton" onClick={onResetFilters}>
            <RotateCcw size={16} /> Wyczyść
          </button>
        </div>
      }
    >
      <div className="dataQualityBanner">
        <strong>Audyt danych</strong>
        <span>Niska pewność, do sprawdzenia, korekty i podejrzane kwoty.</span>
      </div>
      <DataQualityChart data={dataQualityChart} onInspect={onInspect} />

      {!!(presets || []).length && (
        <div className="presetBar" aria-label="Szybkie filtry transakcji">
          {presets.map((preset) => (
            <button type="button" className="presetChip" key={preset.label} onClick={() => onPreset?.(preset)}>
              {preset.label}
            </button>
          ))}
        </div>
      )}

      <div className="transactionFilters primaryFilters">
        <OptionSelect
          icon={Filter}
          label="Przepływ"
          value={filters.flow}
          options={options.flows || []}
          onChange={(value) => onFilterChange("flow", value)}
        />
        <OptionSelect
          label="Koszyk"
          value={bucket}
          options={(buckets || []).map((option) => ({ value: option, label: option }))}
          onChange={onBucketChange}
        />
        <OptionSelect
          label="Kategoria"
          value={filters.category}
          options={toOptions(options.categories)}
          onChange={(value) => onFilterChange("category", value)}
        />
        <OptionSelect
          icon={ArrowUpDown}
          label="Sortowanie"
          value={filters.sort}
          options={options.sort || []}
          onChange={(value) => onFilterChange("sort", value)}
        />
        <OptionSelect
          label="Na stronie"
          value={String(filters.pageSize)}
          options={(options.pageSizes || [50]).map((value) => ({ value: String(value), label: String(value) }))}
          onChange={(value) => onFilterChange("pageSize", Number(value))}
        />
      </div>

      <div className="advancedFilters">
        <button type="button" className="secondaryButton" onClick={() => setAdvancedOpen((open) => !open)}>
          <SlidersHorizontal size={16} /> {advancedOpen ? "Ukryj filtry zaawansowane" : "Filtry zaawansowane"}
        </button>
        {advancedOpen && (
          <div className="transactionFilters">
            <OptionSelect
              label="Obszar"
              value={filters.area}
              options={toOptions(options.areas)}
              onChange={(value) => onFilterChange("area", value)}
            />
            <OptionSelect
              label="Grupa"
              value={filters.group}
              options={toOptions(options.groups)}
              onChange={(value) => onFilterChange("group", value)}
            />
            <OptionSelect
              label="Podkategoria"
              value={filters.subcategory}
              options={normalizeOptions(options.subcategories)}
              onChange={(value) => onFilterChange("subcategory", value)}
            />
            <OptionSelect
              label="Stałość"
              value={filters.fixedness}
              options={toOptions(options.fixedness)}
              onChange={(value) => onFilterChange("fixedness", value)}
            />
            <OptionSelect
              label="Pewność"
              value={filters.confidence}
              options={toOptions(options.confidence)}
              onChange={(value) => onFilterChange("confidence", value)}
            />
          </div>
        )}
      </div>

      <div className="tableMeta">
        <span>
          <CalendarDays size={15} /> Zakres: {activeTimeLabel}
        </span>
        <span>
          <SlidersHorizontal size={15} /> W zakresie: {totalItems}{yearlyTotal ? ` z ${yearlyTotal} transakcji roku` : ""}
        </span>
        <span>Widoczna strona: {filteredTransactions.length}</span>
        <span>
          <CircleDollarSign size={15} /> Suma wydatków na stronie: {money(visibleSpend)}
        </span>
        {isScopedBelowYear && onShowFullYear && (
          <button type="button" className="inlineMetaButton" onClick={onShowFullYear}>
            Pokaż cały rok
          </button>
        )}
      </div>
      <TransactionsTable
        transactions={filteredTransactions}
        sort={filters.sort}
        onSort={(nextSort) => onFilterChange("sort", nextSort)}
      />
      <div className="pager">
        <button disabled={page <= 0} onClick={() => onPageChange(page - 1)}>Poprzednia</button>
        <span>
          Strona {totalPages ? page + 1 : 0} / {totalPages}
        </span>
        <button disabled={!totalPages || page >= totalPages - 1} onClick={() => onPageChange(page + 1)}>Następna</button>
      </div>
    </Panel>
  );
}

function OptionSelect({ icon: Icon, label, value, options, onChange }) {
  return (
    <label className="filterControl">
      <span>{Icon && <Icon size={14} />} {label}</span>
      <select value={value ?? "Wszystkie"} onChange={(event) => onChange(event.target.value)} aria-label={label}>
        {normalizeOptions(options).map((option) => (
          <option key={`${option.value}-${option.label}`} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
    </label>
  );
}

function toOptions(values = []) {
  return values.map((value) => ({ value, label: value }));
}

function normalizeOptions(options = []) {
  return options.map((option) => (typeof option === "string" ? { value: option, label: option } : option));
}
