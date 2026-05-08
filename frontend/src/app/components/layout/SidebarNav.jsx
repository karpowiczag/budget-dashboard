import {
  BarChart3,
  CalendarCheck,
  ClipboardList,
  Database,
  Landmark,
  ReceiptText,
  ShieldCheck,
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

export function SidebarNav({ activeView, data, onViewChange, onYearChange, views = [], year, years = [] }) {
  return (
    <aside className="sidebarNav">
      <div className="sidebarBrand">
        <p className="eyebrow">Budżet domowy</p>
        <h1>{data?.year || year}</h1>
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
      </div>
    </aside>
  );
}
