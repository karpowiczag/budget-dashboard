import { useState } from "react";
import { CashflowSankeyChart } from "../components/charts/CashflowSankeyChart.jsx";
import { CategoryTrendChart } from "../components/charts/CategoryTrendChart.jsx";
import { SavingsWaterfallChart } from "../components/charts/SavingsWaterfallChart.jsx";
import { ReportDataTable } from "../components/tables/ReportDataTable.jsx";
import { Panel } from "../components/ui/Panel.jsx";
import { money } from "../domain/formatters.js";
import { selectNetWorth } from "../domain/budgetSelectors.js";

const KIND_OPTIONS = [
  { value: "CHECKING", label: "Rachunek bieżący" },
  { value: "SAVINGS", label: "Oszczędnościowe" },
  { value: "CASH", label: "Gotówka" },
  { value: "OTHER", label: "Inne aktywo" },
];

function AccountEditorRow({ account, onSave, saving }) {
  const [kind, setKind] = useState(account.kind);
  const [liquid, setLiquid] = useState(account.liquid);
  const [anchorBalance, setAnchorBalance] = useState(account.anchorBalance != null ? String(account.anchorBalance) : "");
  const [anchorDate, setAnchorDate] = useState(account.anchorDate || "");
  const canSave = anchorBalance !== "" && !Number.isNaN(Number(anchorBalance)) && anchorDate !== "" && !saving;
  return (
    <tr>
      <td>{account.accountKey}</td>
      <td>
        <select value={kind} onChange={(event) => setKind(event.target.value)} aria-label={`Typ konta ${account.accountKey}`}>
          {KIND_OPTIONS.map((option) => (
            <option key={option.value} value={option.value}>{option.label}</option>
          ))}
        </select>
      </td>
      <td className="num">
        <input
          type="number"
          step="0.01"
          value={anchorBalance}
          placeholder="np. 12000"
          aria-label={`Saldo otwarcia ${account.accountKey}`}
          onChange={(event) => setAnchorBalance(event.target.value)}
        />
      </td>
      <td>
        <input
          type="date"
          value={anchorDate}
          aria-label={`Data salda otwarcia ${account.accountKey}`}
          onChange={(event) => setAnchorDate(event.target.value)}
        />
      </td>
      <td>
        <label className="nwCheckbox">
          <input type="checkbox" checked={liquid} onChange={(event) => setLiquid(event.target.checked)} /> płynne
        </label>
      </td>
      <td className="num">
        {account.configured ? money(account.derivedBalance) : <span className="nwMuted">ustaw saldo</span>}
      </td>
      <td>
        <button
          type="button"
          className="primaryButton"
          disabled={!canSave}
          onClick={() => onSave(account.accountKey, {
            name: account.name || account.accountKey,
            kind,
            liquid,
            excludeFromNetWorth: account.excludeFromNetWorth,
            anchorBalance: Number(anchorBalance),
            anchorDate,
          })}
        >
          {saving ? "Zapisuję..." : "Zapisz"}
        </button>
      </td>
    </tr>
  );
}

export function WealthView({ wealthDashboard, netWorth, onSaveAccount, savingAccount = false, netWorthStatus, onInspect }) {
  const dashboard = wealthDashboard || {};
  const nw = selectNetWorth(netWorth);
  return (
    <section className="viewStack">
      <Panel title="Fundusz awaryjny i salda kont">
        {!nw ? (
          <p className="nwMuted">Wczytuję salda kont…</p>
        ) : (
          <>
            <div className="nwSummary">
              <div>
                <span className="nwMuted">Płynne środki (wyliczone)</span>
                <strong>{money(nw.liquidTotal)}</strong>
              </div>
              <div>
                <span className="nwMuted">Fundusz awaryjny — cel komfortowy</span>
                <strong>{money(nw.emergencyFundComfort)}</strong>
                <span className="nwMuted">minimum {money(nw.emergencyFundMin)}</span>
              </div>
              <div>
                <span className="nwMuted">Pokrycie celu</span>
                <strong className={nw.comfortReached ? "good" : nw.minReached ? "" : "warn"}>{nw.progressPercent}%</strong>
              </div>
            </div>
            <div className="nwProgressTrack" role="img" aria-label={`Pokrycie funduszu awaryjnego ${nw.progressPercent}%`}>
              <div className="nwProgressFill" style={{ width: `${nw.progressWidth}%` }} />
            </div>
            <p className="nwMuted">
              Salda wyliczane z importowanych przepływów: saldo otwarcia plus suma transakcji po dacie otwarcia.
              {" "}Skonfigurowane konta: {nw.configuredCount} z {nw.accounts.length}.
            </p>
            {netWorthStatus ? (
              <div className={`dataQualityBanner ${netWorthStatus.type === "error" ? "warn" : "neutral"}`}>
                <span>{netWorthStatus.message}</span>
              </div>
            ) : null}
            <div className="nwTableScroll">
              <table className="nwTable">
                <thead>
                  <tr>
                    <th>Konto (z importu)</th>
                    <th>Typ</th>
                    <th className="num">Saldo otwarcia</th>
                    <th>Data otwarcia</th>
                    <th>Płynność</th>
                    <th className="num">Aktualne saldo</th>
                    <th aria-label="Akcje" />
                  </tr>
                </thead>
                <tbody>
                  {nw.accounts.map((account) => (
                    <AccountEditorRow key={account.accountKey} account={account} onSave={onSaveAccount} saving={savingAccount} />
                  ))}
                  {nw.accounts.length === 0 ? (
                    <tr><td colSpan={7} className="nwMuted">Brak kont w importowanych transakcjach.</td></tr>
                  ) : null}
                </tbody>
              </table>
            </div>
          </>
        )}
      </Panel>

      <Panel title="Przepływy majątkowe">
        <div className="dataQualityBanner neutral">
          <strong>Salda wyliczane, inwestycje osobno</strong>
          <span>Powyżej: płynne salda kont (saldo otwarcia + przepływy). Poniżej: sumy ruchów majątkowych z importu. Wartość inwestycji (MyFund) i pełna wartość netto dochodzą w kolejnym etapie.</span>
        </div>
      </Panel>

      <section className="gridTwo">
        <Panel title="Sankey: gdzie przesuwamy pieniądze">
          <CashflowSankeyChart
            data={dashboard.sankey}
            onSelect={(entry) => {
              const row = entry?.payload || entry;
              if (row?.filter) {
                onInspect?.({ title: `Transakcje: ${row.target || row.name}`, filters: row.filter, useTimeScope: false });
              }
            }}
          />
        </Panel>
        <Panel title="Wkład w majątek i dług">
          <SavingsWaterfallChart data={dashboard.waterfall || []} />
        </Panel>
      </section>

      <Panel title="Trend miesięczny przepływów majątkowych">
        <CategoryTrendChart
          data={dashboard.monthlyTrend || []}
          onSelect={(entry) => {
            const row = entry?.payload || entry;
            onInspect?.({ title: `Transakcje: majątek ${row.month}`, filters: row.filter || { flow: "financial", month: row.monthKey }, useTimeScope: false });
          }}
        />
      </Panel>

      <Panel title="Kategorie przepływów majątkowych">
        <ReportDataTable
          exportName="majątek-przepływy"
          rows={dashboard.flows || []}
          onRowClick={(row) => onInspect?.({ title: `Transakcje: ${row.category}`, filters: { category: row.category }, useTimeScope: false })}
          columns={[
            { key: "category", header: "Kategoria" },
            { key: "outgoing", header: "Wypływy (brutto)", className: "num", render: (row) => money(row.outgoing) },
            { key: "count", header: "Transakcje", className: "num" },
          ]}
        />
      </Panel>
    </section>
  );
}
