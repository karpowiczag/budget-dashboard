import { Upload } from "lucide-react";
import { Panel } from "../components/ui/Panel.jsx";

export function ImportView({ onUpload, uploading, importStatus }) {
  return (
    <Panel title="Import transakcji CSV">
      <div className="importBox">
        <div>
          <strong>Dodaj eksport bankowy</strong>
          <p>Plik CSV jest analizowany po stronie serwera i nie jest przechowywany po imporcie.</p>
        </div>
        <label className={`uploadButton ${uploading ? "disabled" : ""}`}>
          <Upload size={18} />
          <span>{uploading ? "Importuję..." : "Wybierz CSV"}</span>
          <input type="file" accept=".csv,text/csv" disabled={uploading} onChange={(event) => onUpload(event.target.files?.[0])} />
        </label>
      </div>
      {importStatus && <div className={`importStatus ${importStatus.type}`}>{importStatus.message}</div>}
    </Panel>
  );
}
