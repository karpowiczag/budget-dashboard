export function Kpi({ icon: Icon, label, value, detail, tone = "neutral" }) {
  return (
    <section className={`kpi ${tone}`}>
      <div className="kpiIcon" aria-hidden="true">
        <Icon size={18} />
      </div>
      <div>
        <p>{label}</p>
        <strong>{value}</strong>
        <span>{detail}</span>
      </div>
    </section>
  );
}
