export function ReportWorkspace({ activeMode, activeReport, children, onModeChange, onReportChange, reports = [] }) {
  const current = reports.find((item) => item.id === activeReport) || reports[0];
  const modes = current?.modes || ["breakdown", "trends"];
  return (
    <section className="reportWorkspace">
      <div className="workspaceHead">
        <div className="segmented compact" aria-label="Raport">
          {reports.map((report) => (
            <button type="button" key={report.id} className={activeReport === report.id ? "active" : ""} onClick={() => onReportChange(report.id)}>
              {report.label}
            </button>
          ))}
        </div>
        <div className="segmented compact" aria-label="Tryb raportu">
          {modes.map((mode) => (
            <button type="button" key={mode} className={activeMode === mode ? "active" : ""} onClick={() => onModeChange(mode)}>
              {mode === "breakdown" ? "Breakdown" : "Trends"}
            </button>
          ))}
        </div>
      </div>
      {current?.question && <p className="workspaceQuestion">{current.question}</p>}
      {children}
    </section>
  );
}
