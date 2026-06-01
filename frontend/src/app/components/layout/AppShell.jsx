export function AppShell({ sidebar, children, contentKey }) {
  return (
    <div className="appShell">
      <a className="skipLink" href="#mainContent">Przejdź do treści</a>
      {sidebar}
      <main className="appContent" id="mainContent" tabIndex={-1} key={contentKey}>
        {children}
      </main>
    </div>
  );
}
