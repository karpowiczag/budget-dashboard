import { money } from "../../domain/formatters.js";

export function ModuleHeader({ action, header }) {
  if (!header) return null;
  return (
    <section className="moduleHeader">
      <div className="moduleIntro">
        <p className="eyebrow">{header.eyebrow}</p>
        <h1>{header.title}</h1>
        <span>{header.subtitle}</span>
      </div>
      {!!header.cards?.length && (
        <div className="moduleCards">
          {header.cards.map((card) => (
            <div className={`moduleCard ${card.tone || "neutral"}`} key={card.label}>
              <span>{card.label}</span>
              <strong>{formatValue(card)}</strong>
              <p>{formatDetail(card.detail)}</p>
            </div>
          ))}
        </div>
      )}
      {action && <div className="moduleAction">{action}</div>}
    </section>
  );
}

function formatValue(card) {
  if (card.textValue) return card.value || "";
  if (card.number) return String(card.value ?? 0);
  return money(Number(card.value || 0));
}

function formatDetail(detail) {
  if (detail == null) return "";
  if (typeof detail === "number") return money(detail);
  const text = String(detail);
  if (/wpływy|wydatki/i.test(text)) {
    return text.replace(/(-?\d+(?:\.\d+)?)/g, (_, amount) => money(Number(amount)));
  }
  return text
    .replace(/(-?\d+(?:\.\d+)?) dziennie/g, (_, amount) => `${money(Number(amount))} dziennie`)
    .replace(/(-?\d+(?:\.\d+)?) rocznie/g, (_, amount) => `${money(Number(amount))} rocznie`);
}
