import { CalendarDays } from "lucide-react";
import { money } from "../../domain/formatters.js";
import { FilterChips } from "./FilterChips.jsx";

export function TimeScopeControl({
  calendarStats,
  chips = [],
  months = [],
  time,
  variant = "compact",
  onChange,
  onClearDrill,
}) {
  const scope = time?.scope || "month";
  const selectedMonth = time?.month || "";
  const showCalendar = variant === "full" && calendarStats && scope !== "year" && scope !== "all";

  function patch(next) {
    onChange?.({ ...time, ...next });
  }

  return (
    <section className={`timeScopeControl ${variant === "full" ? "full" : "compact"}`}>
      <div className="timeScopeRow">
        <div className="segmented compact" aria-label="Zakres czasu">
          <button type="button" className={scope === "year" || scope === "all" ? "active" : ""} onClick={() => patch({ scope: "year", day: "" })}>Rok</button>
          <button type="button" className={scope === "month" ? "active" : ""} onClick={() => patch({ scope: "month", day: "" })}>Miesiąc</button>
          <button type="button" className={scope === "day" ? "active" : ""} onClick={() => patch({ scope: "day", day: time?.day || calendarStats?.selected || "" })}>Dzień</button>
        </div>
        <label className="select">
          <CalendarDays size={16} />
          <select value={selectedMonth} onChange={(event) => patch({ month: event.target.value, day: "" })}>
            {months.map((row) => (
              <option key={row.month} value={row.month}>{row.month}</option>
            ))}
          </select>
        </label>
      </div>

      <FilterChips chips={chips} onClear={time?.drillFilter ? onClearDrill : null} />

      {showCalendar && (
        <div className="globalCalendar localCalendar">
          <div className="calendarWeekdays">
            {["Pon", "Wt", "Śr", "Czw", "Pt", "Sob", "Nd"].map((day) => (
              <span key={day}>{day}</span>
            ))}
          </div>
          <div className="calendarGrid compactCalendar">
            {calendarStats.cells.map((cell) =>
              cell.empty ? (
                <div className="calendarCell empty" key={cell.key} />
              ) : (
                <button
                  key={cell.key}
                  className={`calendarCell ${calendarStats.selected === cell.day ? "active" : ""} ${cell.spend >= 1000 ? "hot" : cell.spend >= 300 ? "warm" : ""}`}
                  onClick={() => patch({ scope: "day", day: String(cell.day) })}
                >
                  <span>{cell.day}</span>
                  <strong>{cell.spend ? money(cell.spend) : ""}</strong>
                  <em>{cell.transactions ? `${cell.transactions} tx` : ""}</em>
                </button>
              ),
            )}
          </div>
        </div>
      )}
    </section>
  );
}
