export function Kpi({ icon: Icon, label, value, detail, tone = "neutral", onInspect }) {
  const className = `kpi ${tone}${onInspect ? " inspectable" : ""}`;
  const content = (
    <>
      <div className="kpiIcon" aria-hidden="true">
        <Icon size={18} />
      </div>
      <div>
        <p>{label}</p>
        <strong>{value}</strong>
        <span>{detail}</span>
      </div>
    </>
  );

  if (onInspect) {
    return (
      <button type="button" className={className} onClick={onInspect} title="Pokaż transakcje">
        {content}
      </button>
    );
  }

  return (
    <section className={className}>
      {content}
    </section>
  );
}
