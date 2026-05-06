import { CalendarDays } from "lucide-react";
import { money } from "../../domain/formatters.js";

export function GlobalTimeFilter({
  activeTimeLabel,
  calendarStats,
  drillFilter,
  monthly,
  selectedMonth,
  timeScope,
  onClearDrill,
  onMonthChange,
  onSelectDay,
  onTimeScopeChange,
}) {
  return (
    <section className="globalTime">
      <div className="timeHead">
        <div>
          <strong>Globalny filtr czasu</strong>
          <span>{activeTimeLabel}</span>
        </div>
        <div className="timeControls">
          <div className="segmented compact" aria-label="Zakres czasu">
            <button className={timeScope === "all" ? "active" : ""} onClick={() => onTimeScopeChange("all")}>Cały rok</button>
            <button className={timeScope === "month" ? "active" : ""} onClick={() => onTimeScopeChange("month")}>Wybrany miesiąc</button>
            <button className={timeScope === "day" ? "active" : ""} onClick={() => onTimeScopeChange("day")}>Wybrany dzień</button>
          </div>
          <label className="select">
            <CalendarDays size={16} />
            <select value={selectedMonth} onChange={(event) => onMonthChange(event.target.value)}>
              {monthly.map((row) => (
                <option key={row.month} value={row.month}>{row.month}</option>
              ))}
            </select>
          </label>
        </div>
      </div>

      {calendarStats && timeScope !== "all" && (
        <div className="globalCalendar">
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
                  onClick={() => onSelectDay(String(cell.day))}
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

      {drillFilter && (
        <div className="activeDrill">
          <span>Filtr: {drillFilter.label}</span>
          <button onClick={onClearDrill}>Wyczyść</button>
        </div>
      )}
    </section>
  );
}
