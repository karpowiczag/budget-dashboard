import { RefreshCw, Upload } from "lucide-react";
import { Panel } from "../components/ui/Panel.jsx";
import { money } from "../domain/formatters.js";

export function ImportView({ activeYear, importHealth, importRuns = [], onRebuild, onUpload, rebuilding, uploading, importStatus }) {
  const health = importHealth || { cards: [], latestRun: null };
  const disabled = uploading || rebuilding;
  return (
    <section className="viewStack">
      <Panel title="Stan danych">
        <div className="importHealth">
          {(health.cards || []).map((card) => (
            <div key={card.label}>
              <span>{card.label}</span>
              <strong>{formatHealthValue(card)}</strong>
              <p>{card.amount != null ? `${money(card.amount)} · ${card.detail}` : card.detail}</p>
            </div>
          ))}
        </div>
      </Panel>

      <Panel title="Import transakcji CSV">
        <div className="importBox">
          <div>
            <strong>Dodaj eksport bankowy</strong>
            <p>Plik CSV jest analizowany po stronie serwera i nie jest przechowywany po imporcie.</p>
          </div>
          <label className={`uploadButton ${disabled ? "disabled" : ""}`}>
            <Upload size={18} />
            <span>{uploading ? "Importuję..." : "Wybierz CSV"}</span>
            <input type="file" accept=".csv,text/csv" disabled={disabled} onChange={(event) => onUpload(event.target.files?.[0])} />
          </label>
        </div>
        {onRebuild && activeYear && (
          <div className="importBox rebuildBox">
            <div>
              <strong>Przelicz lokalne dane {activeYear}</strong>
              <p>Użyj po zmianie reguł kategorii, żeby stare transakcje dostały nowe kategorie.</p>
            </div>
            <button type="button" className="secondaryButton" disabled={disabled} onClick={() => onRebuild(activeYear)}>
              <RefreshCw size={18} />
              {rebuilding ? "Przeliczam..." : "Przebuduj rok"}
            </button>
          </div>
        )}
        {importStatus && <div className={`importStatus ${importStatus.type}`}>{importStatus.message}</div>}
      </Panel>

      <Panel title="Historia importów">
        <div className="importRuns">
          {(importRuns || []).slice(0, 8).map((run) => (
            <div className={`importRun ${run.status}`} key={run.id}>
              <span>{run.year || "brak roku"}</span>
              <strong>{run.inputCsv}</strong>
              <p>{run.status} · duplikaty {run.duplicatesRemoved || 0} · {formatDateTime(run.createdAt)}</p>
              {run.message && <em>{run.message}</em>}
            </div>
          ))}
          {!importRuns?.length && <div className="empty">Brak historii importów.</div>}
        </div>
      </Panel>
    </section>
  );
}

function formatHealthValue(card) {
  if (card.amount != null) return card.value;
  return typeof card.value === "number" ? card.value.toLocaleString("pl-PL") : card.value;
}

function formatDateTime(value) {
  if (!value) return "";
  return new Intl.DateTimeFormat("pl-PL", {
    dateStyle: "short",
    timeStyle: "short",
  }).format(new Date(value));
}
