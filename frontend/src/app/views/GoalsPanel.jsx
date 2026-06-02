import { useState } from "react";
import { Panel } from "../components/ui/Panel.jsx";
import { money } from "../domain/formatters.js";
import { selectGoals } from "../domain/budgetSelectors.js";

function slug(value) {
  return (value || "")
    .toLowerCase()
    .normalize("NFD")
    .replace(/[̀-ͯ]/g, "")
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "")
    .slice(0, 60) || "cel";
}

function GoalRow({ goal, onSave, onDelete, saving }) {
  const [target, setTarget] = useState(String(goal.targetAmount ?? ""));
  const [current, setCurrent] = useState(String(goal.currentAmount ?? ""));
  const [date, setDate] = useState(goal.targetDate || "");
  const canSave = target !== "" && !Number.isNaN(Number(target)) && !saving;
  return (
    <tr>
      <td>{goal.name}</td>
      <td className="num">
        <input type="number" step="100" value={target} aria-label={`Kwota docelowa ${goal.name}`} onChange={(event) => setTarget(event.target.value)} />
      </td>
      <td className="num">
        <input type="number" step="100" value={current} aria-label={`Uzbierane ${goal.name}`} onChange={(event) => setCurrent(event.target.value)} />
      </td>
      <td>
        <input type="date" value={date} aria-label={`Termin ${goal.name}`} onChange={(event) => setDate(event.target.value)} />
      </td>
      <td className="num">
        {goal.achieved ? <span className="good">cel osiągnięty</span> : goal.monthlyNeed ? money(goal.monthlyNeed) : "—"}
      </td>
      <td>
        <div className="nwProgressTrack" role="img" aria-label={`Postęp ${goal.progressPercent}%`}>
          <div className="nwProgressFill" style={{ width: `${goal.progressPercent}%` }} />
        </div>
        <span className="nwMuted">
          {goal.progressPercent}%{goal.monthsLeft != null ? ` · ${goal.monthsLeft} mies.` : ""}{goal.overdue ? " · po terminie" : ""}
        </span>
      </td>
      <td>
        <button type="button" className="primaryButton" disabled={!canSave}
          onClick={() => onSave(goal.goalId, {
            name: goal.name,
            targetAmount: Number(target),
            currentAmount: current === "" ? 0 : Number(current),
            targetDate: date === "" ? null : date,
            note: goal.note || null,
          })}>
          {saving ? "..." : "Zapisz"}
        </button>
        <button type="button" className="nwGhost" disabled={saving} onClick={() => onDelete(goal.goalId)}>Usuń</button>
      </td>
    </tr>
  );
}

function NewGoalForm({ onSave, saving }) {
  const [name, setName] = useState("");
  const [target, setTarget] = useState("");
  const [date, setDate] = useState("");
  const canAdd = name.trim() !== "" && target !== "" && !Number.isNaN(Number(target)) && !saving;
  return (
    <div className="nwNewLiability">
      <input value={name} placeholder="Nazwa celu (np. Wkład na mieszkanie)" aria-label="Nazwa nowego celu" onChange={(event) => setName(event.target.value)} />
      <input type="number" step="100" value={target} placeholder="Kwota docelowa" aria-label="Kwota docelowa nowego celu" onChange={(event) => setTarget(event.target.value)} />
      <input type="date" value={date} aria-label="Termin nowego celu" onChange={(event) => setDate(event.target.value)} />
      <button type="button" className="primaryButton" disabled={!canAdd}
        onClick={() => {
          onSave(slug(name), { name: name.trim(), targetAmount: Number(target), currentAmount: 0, targetDate: date === "" ? null : date, note: null });
          setName("");
          setTarget("");
          setDate("");
        }}>
        {saving ? "Dodaję..." : "Dodaj cel"}
      </button>
    </div>
  );
}

export function GoalsPanel({ goals, asOf, onSaveGoal, onDeleteGoal, savingGoal = false, status }) {
  const model = selectGoals(goals, asOf);
  return (
    <Panel title="Cele finansowe">
      <p className="nwMuted">Ile odkładać miesięcznie, aby zdążyć na termin. Uzbieraną kwotę aktualizujesz ręcznie.</p>
      {status ? (
        <div className={`dataQualityBanner ${status.type === "error" ? "warn" : "neutral"}`}>
          <span>{status.message}</span>
        </div>
      ) : null}
      {model.goals.length ? (
        <>
          <div className="nwSummary">
            <div>
              <span className="nwMuted">Cel łącznie</span>
              <strong>{money(model.totalTarget)}</strong>
            </div>
            <div>
              <span className="nwMuted">Uzbierane</span>
              <strong>{money(model.totalCurrent)}</strong>
            </div>
            <div>
              <span className="nwMuted">Potrzebne / mies.</span>
              <strong>{money(model.totalMonthlyNeed)}</strong>
            </div>
          </div>
          <div className="nwTableScroll">
            <table className="nwTable">
              <thead>
                <tr>
                  <th>Cel</th>
                  <th className="num">Kwota docelowa</th>
                  <th className="num">Uzbierane</th>
                  <th>Termin</th>
                  <th className="num">Potrzebne / mies.</th>
                  <th>Postęp</th>
                  <th aria-label="Akcje" />
                </tr>
              </thead>
              <tbody>
                {model.goals.map((goal) => (
                  <GoalRow key={goal.goalId} goal={goal} onSave={onSaveGoal} onDelete={onDeleteGoal} saving={savingGoal} />
                ))}
              </tbody>
            </table>
          </div>
        </>
      ) : (
        <p className="nwMuted">Brak celów. Dodaj pierwszy poniżej.</p>
      )}
      <NewGoalForm onSave={onSaveGoal} saving={savingGoal} />
    </Panel>
  );
}
