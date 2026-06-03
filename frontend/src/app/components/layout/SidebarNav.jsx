import {
  BarChart3,
  ClipboardList,
  Database,
  Landmark,
  LayoutDashboard,
  Moon,
  ReceiptText,
  Sun,
} from "lucide-react";

const ICONS = {
  overview: LayoutDashboard,
  budget: ClipboardList,
  transactions: ReceiptText,
  analysis: BarChart3,
  wealth: Landmark,
  import: Database,
};

export function SidebarNav({ activeView, data, onViewChange, onYearChange, onToggleTheme, theme = "light", views = [], year, years = [] }) {
  const sections = views;
  const mainSections = sections.filter((section) => !section.utility);
  const utilitySections = sections.filter((section) => section.utility);
  const isActive = (section) => (section.views || []).some((entry) => entry.id === activeView);

  function renderSectionButton(section) {
    const Icon = ICONS[section.id] || BarChart3;
    return (
      <button
        type="button"
        key={section.id}
        className={isActive(section) ? "active" : ""}
        onClick={() => onViewChange((section.views[0] || {}).id)}
      >
        <Icon size={18} />
        <span>{section.label}</span>
        {section.description && <em>{section.description}</em>}
      </button>
    );
  }

  return (
    <aside className="sidebarNav">
      <div className="sidebarBrand">
        <p className="eyebrow">Budżet domowy</p>
        <span className="brandYear">{data?.year || year}</span>
        <span>{data?.period || "Import CSV"}</span>
      </div>

      <nav aria-label="Sekcje budżetu">
        {mainSections.map(renderSectionButton)}
      </nav>

      {utilitySections.length > 0 && (
        <div className="sidebarUtility" aria-label="Narzędzia">
          {utilitySections.map(renderSectionButton)}
        </div>
      )}

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
