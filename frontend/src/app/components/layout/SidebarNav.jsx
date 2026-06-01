import {
  BarChart3,
  CalendarCheck,
  ClipboardList,
  Database,
  Landmark,
  Moon,
  ReceiptText,
  ShieldCheck,
  Sun,
} from "lucide-react";

const ICONS = {
  control: CalendarCheck,
  plan: ClipboardList,
  reports: BarChart3,
  wealth: Landmark,
  obligations: ShieldCheck,
  transactions: ReceiptText,
  import: Database,
};

export function SidebarNav({ activeView, data, onViewChange, onYearChange, onToggleTheme, theme = "light", views = [], year, years = [] }) {
  return (
    <aside className="sidebarNav">
      <div className="sidebarBrand">
        <p className="eyebrow">Budżet domowy</p>
        <span className="brandYear">{data?.year || year}</span>
        <span>{data?.period || "Import CSV"}</span>
      </div>

      <nav aria-label="Moduły budżetu">
        {views.map((item) => {
          const Icon = ICONS[item.id] || BarChart3;
          return (
            <button
              type="button"
              key={item.id}
              className={activeView === item.id ? "active" : ""}
              onClick={() => onViewChange(item.id)}
            >
              <Icon size={18} />
              <span>{item.label}</span>
              {item.description && <em>{item.description}</em>}
            </button>
          );
        })}
      </nav>

      <div className="sidebarYears">
        <span>Rok</span>
        <div className="segmented compact">
          {years.map((option) => (
            <button
              type="button"
              key={option.year}
              className={String(year) === String(option.year) ? "active" : ""}
              onClick={() => onYearChange(String(option.year))}
            >
              {option.year}
            </button>
          ))}
        </div>
        {onToggleTheme && (
          <button
            type="button"
            className="themeToggle"
            onClick={onToggleTheme}
            aria-pressed={theme === "dark"}
          >
            {theme === "dark" ? <Sun size={16} /> : <Moon size={16} />}
            <span>{theme === "dark" ? "Jasny motyw" : "Ciemny motyw"}</span>
          </button>
        )}
      </div>
    </aside>
  );
}
