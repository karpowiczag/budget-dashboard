export function DashboardTabs({ views, activeView, onViewChange }) {
  return (
    <nav className="tabs" aria-label="Widoki dashboardu">
      {views.map((item) => (
        <button key={item.id} className={activeView === item.id ? "active" : ""} onClick={() => onViewChange(item.id)}>
          {item.label}
        </button>
      ))}
    </nav>
  );
}
