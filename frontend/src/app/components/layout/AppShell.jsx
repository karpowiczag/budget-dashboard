export function AppShell({ sidebar, children }) {
  return (
    <div className="appShell">
      <a className="skipLink" href="#mainContent">Przejdź do treści</a>
      {sidebar}
      <main className="appContent" id="mainContent" tabIndex={-1}>
        {children}
      </main>
    </div>
  );
}
