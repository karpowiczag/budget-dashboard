import { Download } from "lucide-react";

export function DashboardHeader({ data, year, years, onYearChange }) {
  return (
    <header className="topbar">
      <div>
        <p className="eyebrow">Budżet domowy</p>
        <h1>Analiza {data.year}</h1>
        <span>{data.period}</span>
      </div>
      <div className="controls">
        <div className="segmented" aria-label="Wybór roku">
          {years.map((option) => (
            <button key={option.year} className={year === String(option.year) ? "active" : ""} onClick={() => onYearChange(String(option.year))}>
              {option.year}
            </button>
          ))}
        </div>
        <a className="iconButton" href={`/api/budget/${year}`} title="Otwórz JSON API">
          <Download size={18} />
        </a>
      </div>
    </header>
  );
}
