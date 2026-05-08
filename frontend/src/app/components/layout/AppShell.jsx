export function AppShell({ sidebar, children }) {
  return (
    <main className="appShell">
      {sidebar}
      <div className="appContent">
        {children}
      </div>
    </main>
  );
}
