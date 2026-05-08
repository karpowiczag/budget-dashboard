export function FilterChips({ chips = [], onClear }) {
  const visible = chips.filter((chip) => chip?.value);
  if (!visible.length) return null;
  return (
    <div className="filterChips">
      {visible.map((chip) => (
        <span key={`${chip.label}-${chip.value}`}>
          <strong>{chip.label}</strong>
          {chip.value}
        </span>
      ))}
      {onClear && (
        <button type="button" onClick={onClear}>
          Wyczyść drilldown
        </button>
      )}
    </div>
  );
}
