import { useState } from "react";
import { CashflowSankeyChart } from "../components/charts/CashflowSankeyChart.jsx";
import { CategoryTrendChart } from "../components/charts/CategoryTrendChart.jsx";
import { SavingsWaterfallChart } from "../components/charts/SavingsWaterfallChart.jsx";
import { ReportDataTable } from "../components/tables/ReportDataTable.jsx";
import { Panel } from "../components/ui/Panel.jsx";
import { money, percent } from "../domain/formatters.js";
import { selectDebtPayoff, selectNetWorth } from "../domain/budgetSelectors.js";

const KIND_OPTIONS = [
  { value: "CHECKING", label: "Rachunek bieżący" },
  { value: "SAVINGS", label: "Oszczędnościowe" },
  { value: "CASH", label: "Gotówka" },
  { value: "OTHER", label: "Inne aktywo" },
];

const LIABILITY_KIND_OPTIONS = [
  { value: "MORTGAGE", label: "Hipoteka" },
  { value: "AUTO", label: "Kredyt auto" },
  { value: "CONSUMER", label: "Kredyt/pożyczka" },
  { value: "STUDENT", label: "Studencki" },
  { value: "OTHER", label: "Inne" },
];

function slug(value) {
  return (value || "")
    .toLowerCase()
    .normalize("NFD")
    .replace(/[̀-ͯ]/g, "")
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "")
    .slice(0, 60) || "zobowiazanie";
}

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
        <input type="number" step="0.01" value={anchorBalance} placeholder="np. 12000"
          aria-label={`Saldo otwarcia ${account.accountKey}`} onChange={(event) => setAnchorBalance(event.target.value)} />
      </td>
      <td>
        <input type="date" value={anchorDate} aria-label={`Data salda otwarcia ${account.accountKey}`}
          onChange={(event) => setAnchorDate(event.target.value)} />
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
        <button type="button" className="primaryButton" disabled={!canSave}
          onClick={() => onSave(account.accountKey, {
            name: account.name || account.accountKey,
            kind,
            liquid,
            excludeFromNetWorth: account.excludeFromNetWorth,
            anchorBalance: Number(anchorBalance),
            anchorDate,
          })}>
          {saving ? "Zapisuję..." : "Zapisz"}
        </button>
      </td>
    </tr>
  );
}

function LiabilityEditorRow({ liability, onSave, onDelete, saving }) {
  const [kind, setKind] = useState(liability.kind);
  const [principal, setPrincipal] = useState(String(liability.currentPrincipal ?? ""));
  const [rate, setRate] = useState(liability.annualInterestRate != null ? String(liability.annualInterestRate * 100) : "");
  const [payment, setPayment] = useState(liability.monthlyPayment != null ? String(liability.monthlyPayment) : "");
  const canSave = principal !== "" && !Number.isNaN(Number(principal)) && !saving;
  return (
    <tr>
      <td>{liability.name}</td>
      <td>
        <select value={kind} onChange={(event) => setKind(event.target.value)} aria-label={`Typ zobowiązania ${liability.name}`}>
          {LIABILITY_KIND_OPTIONS.map((option) => (
            <option key={option.value} value={option.value}>{option.label}</option>
          ))}
        </select>
      </td>
      <td className="num">
        <input type="number" step="0.01" value={principal} aria-label={`Kapitał ${liability.name}`}
          onChange={(event) => setPrincipal(event.target.value)} />
      </td>
      <td className="num">
        <input type="number" step="0.01" value={rate} placeholder="%" aria-label={`Oprocentowanie ${liability.name}`}
          onChange={(event) => setRate(event.target.value)} />
      </td>
      <td className="num">
        <input type="number" step="0.01" value={payment} placeholder="rata" aria-label={`Rata ${liability.name}`}
          onChange={(event) => setPayment(event.target.value)} />
      </td>
      <td>
        <button type="button" className="primaryButton" disabled={!canSave}
          onClick={() => onSave(liability.liabilityKey, {
            name: liability.name,
            kind,
            currentPrincipal: Number(principal),
            annualInterestRate: rate === "" ? null : Number(rate) / 100,
            monthlyPayment: payment === "" ? null : Number(payment),
            asOf: liability.asOf || null,
          })}>
          {saving ? "..." : "Zapisz"}
        </button>
        <button type="button" className="nwGhost" disabled={saving} onClick={() => onDelete(liability.liabilityKey)}>Usuń</button>
      </td>
    </tr>
  );
}

function NewLiabilityForm({ onSave, saving }) {
  const [name, setName] = useState("");
  const [kind, setKind] = useState("MORTGAGE");
  const [principal, setPrincipal] = useState("");
  const canAdd = name.trim() !== "" && principal !== "" && !Number.isNaN(Number(principal)) && !saving;
  return (
    <div className="nwNewLiability">
      <input value={name} placeholder="Nazwa (np. Hipoteka)" aria-label="Nazwa nowego zobowiązania" onChange={(event) => setName(event.target.value)} />
      <select value={kind} onChange={(event) => setKind(event.target.value)} aria-label="Typ nowego zobowiązania">
        {LIABILITY_KIND_OPTIONS.map((option) => (
          <option key={option.value} value={option.value}>{option.label}</option>
        ))}
      </select>
      <input type="number" step="0.01" value={principal} placeholder="Kapitał do spłaty" aria-label="Kapitał nowego zobowiązania" onChange={(event) => setPrincipal(event.target.value)} />
      <button type="button" className="primaryButton" disabled={!canAdd}
        onClick={() => {
          onSave(slug(name), { name: name.trim(), kind, currentPrincipal: Number(principal), annualInterestRate: null, monthlyPayment: null, asOf: null });
          setName("");
          setPrincipal("");
        }}>
        {saving ? "Dodaję..." : "Dodaj zobowiązanie"}
      </button>
    </div>
  );
}

function formatMonths(months) {
  if (months == null) return "—";
  const years = Math.floor(months / 12);
  const rest = months % 12;
  if (years === 0) return `${rest} mies.`;
  if (rest === 0) return `${years} lat`;
  return `${years} lat ${rest} mies.`;
}

function DebtPayoffPanel({ liabilities }) {
  const [extra, setExtra] = useState("0");
  const plan = selectDebtPayoff({ liabilities, extraMonthly: Number(extra) || 0 });
  if (!plan) return null;
  const summarise = (strategy) => `${formatMonths(strategy.months)} · odsetki ${money(strategy.totalInterest)}`;
  return (
    <Panel title="Spłata długu: lawina vs kula śnieżna">
      <div className="nwNewLiability">
        <label className="nwInlineField">
          Dodatkowa nadpłata / mies.
          <input type="number" step="50" value={extra} aria-label="Dodatkowa nadpłata miesięczna" onChange={(event) => setExtra(event.target.value)} />
        </label>
        <span className="nwMuted">Budżet na dług: {money(plan.monthlyBudget)} / mies. (raty {money(plan.totalMinPayment)} + nadpłata)</span>
      </div>
      {!plan.feasible ? (
        <div className="dataQualityBanner warn">
          <span>Przy obecnych ratach dług nie zostanie spłacony — zwiększ nadpłatę lub raty miesięczne.</span>
        </div>
      ) : null}
      <div className="nwSummary">
        <div>
          <span className="nwMuted">Lawina (od najwyższego oproc.)</span>
          <strong className={plan.recommended === "avalanche" ? "good" : ""}>{summarise(plan.avalanche)}</strong>
          <span className="nwMuted">kolejność: {plan.avalanche.order.join(" → ")}</span>
        </div>
        <div>
          <span className="nwMuted">Kula śnieżna (od najmniejszego salda)</span>
          <strong className={plan.recommended === "snowball" ? "good" : ""}>{summarise(plan.snowball)}</strong>
          <span className="nwMuted">kolejność: {plan.snowball.order.join(" → ")}</span>
        </div>
        <div>
          <span className="nwMuted">Rekomendacja</span>
          <strong className="good">{plan.recommended === "avalanche" ? "Lawina" : "Kula śnieżna"}</strong>
          {plan.interestSaved > 0 ? <span className="nwMuted">lawina oszczędza {money(plan.interestSaved)} odsetek</span> : null}
        </div>
      </div>
      <p className="nwMuted">Nadpłacać dług czy inwestować? Punkt odniesienia rynkowy: {percent(plan.referenceReturn)} rocznie.</p>
      <ul className="nwOverpayList">
        {plan.overpayVsInvest.map((row) => (
          <li key={row.name}>
            <strong>{row.name}</strong> — {percent(row.rate)}: {row.verdict === "overpay" ? "nadpłacaj (zwrot pewny, wyższy niż rynek)" : "inwestowanie może dać więcej"}
          </li>
        ))}
      </ul>
      {plan.missingPayments.length ? (
        <p className="nwMuted">Ustaw ratę miesięczną, aby uwzględnić w planie: {plan.missingPayments.join(", ")}.</p>
      ) : null}
    </Panel>
  );
}

export function WealthView({ wealthDashboard, netWorth, onSaveAccount, onSaveLiability, onDeleteLiability, savingAccount = false, savingLiability = false, netWorthStatus, onInspect }) {
  const dashboard = wealthDashboard || {};
  const nw = selectNetWorth(netWorth);
  return (
    <section className="viewStack">
      <Panel title="Wartość netto">
        {!nw ? (
          <p className="nwMuted">Wczytuję bilans…</p>
        ) : (
          <div className="nwSummary">
            <div>
              <span className="nwMuted">Wartość netto</span>
              <strong className={nw.netWorth < 0 ? "warn" : "good"}>{money(nw.netWorth)}</strong>
              <span className="nwMuted">aktywa − zobowiązania</span>
            </div>
            <div>
              <span className="nwMuted">Płynne środki</span>
              <strong>{money(nw.liquidTotal)}</strong>
            </div>
            <div>
              <span className="nwMuted">Inwestycje (MyFund)</span>
              <strong>{money(nw.investedAssets)}</strong>
              {nw.investedAssets === 0 ? <span className="nwMuted">ustaw raporty w module FIRE</span> : null}
            </div>
            <div>
              <span className="nwMuted">Zobowiązania</span>
              <strong className={nw.totalLiabilities > 0 ? "warn" : ""}>{money(nw.totalLiabilities)}</strong>
            </div>
          </div>
        )}
        <div className="dataQualityBanner neutral">
          <strong>Salda i inwestycje liczone osobno</strong>
          <span>Płynne salda = saldo otwarcia + przepływy z importu. Inwestycje pochodzą z raportów MyFund (moduł FIRE). Net worth = płynne + inwestycje − zobowiązania.</span>
        </div>
      </Panel>

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

      <Panel title="Zobowiązania (kredyty, pożyczki)">
        {!nw ? (
          <p className="nwMuted">Wczytuję zobowiązania…</p>
        ) : (
          <>
            <div className="nwTableScroll">
              <table className="nwTable">
                <thead>
                  <tr>
                    <th>Nazwa</th>
                    <th>Typ</th>
                    <th className="num">Kapitał</th>
                    <th className="num">Oproc. % rocznie</th>
                    <th className="num">Rata mies.</th>
                    <th aria-label="Akcje" />
                  </tr>
                </thead>
                <tbody>
                  {nw.liabilities.map((liability) => (
                    <LiabilityEditorRow key={liability.liabilityKey} liability={liability} onSave={onSaveLiability} onDelete={onDeleteLiability} saving={savingLiability} />
                  ))}
                  {nw.liabilities.length === 0 ? (
                    <tr><td colSpan={6} className="nwMuted">Brak zobowiązań. Dodaj kredyt lub pożyczkę poniżej.</td></tr>
                  ) : null}
                </tbody>
              </table>
            </div>
            <NewLiabilityForm onSave={onSaveLiability} saving={savingLiability} />
          </>
        )}
      </Panel>

      {nw && nw.liabilities.length > 0 ? <DebtPayoffPanel liabilities={nw.liabilities} /> : null}

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
