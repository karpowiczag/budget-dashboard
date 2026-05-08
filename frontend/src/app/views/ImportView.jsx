import { RefreshCw, Upload } from "lucide-react";
import { Panel } from "../components/ui/Panel.jsx";

export function ImportView({ activeYear, importHealth, importRuns = [], onRebuild, onUpload, rebuilding, uploading, importStatus }) {
  const disabled = uploading || rebuilding;
  return (
    <section className="viewStack">
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
              <strong>{displayImportName(run)}</strong>
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

function formatDateTime(value) {
  if (!value) return "";
  return new Intl.DateTimeFormat("pl-PL", {
    dateStyle: "short",
    timeStyle: "short",
  }).format(new Date(value));
}

function displayImportName(run) {
  const rawName = String(run?.inputCsv || "");
  const rawMessage = String(run?.message || "");
  if (/local rebuild|local data|csv files/i.test(`${rawName} ${rawMessage}`)) {
    return "Lokalny rebuild";
  }
  return rawName.split(/[\\/]/).pop() || "Import CSV";
}
