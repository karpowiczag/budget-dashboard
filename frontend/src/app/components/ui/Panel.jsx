export function Panel({ title, action, children, className = "" }) {
  return (
    <section className={`panel ${className}`}>
      <div className="panelHead">
        <h2>{title}</h2>
        {action}
      </div>
      {children}
    </section>
  );
}
