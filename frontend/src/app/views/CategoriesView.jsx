import { useState } from "react";
import { Panel } from "../components/ui/Panel.jsx";
import { selectCategoryCatalog } from "../domain/budgetSelectors.js";

const FLOW_LABELS = {
  income: "Przychód",
  livingExpense: "Wydatek bieżący",
  wealthTransfer: "Budowanie majątku",
  technicalTransfer: "Transfer techniczny",
  refundCorrection: "Zwrot / korekta",
  review: "Do rozbicia",
};

function slug(value) {
  return (
    (value || "")
      .toLowerCase()
      .normalize("NFD")
      .replace(/[̀-ͯ]/g, "")
      .replace(/[^a-z0-9]+/g, "-")
      .replace(/^-+|-+$/g, "")
      .slice(0, 60) || "kategoria"
  );
}

// Map a persisted category row to the upsert request body, optionally overriding fields.
function toRequest(category, overrides = {}) {
  return {
    label: category.label,
    area: category.area,
    analyticsGroup: category.analyticsGroup,
    groupId: category.groupId,
    budgetBucket: category.budgetBucket,
    fixedness: category.fixedness,
    flowType: category.flowType,
    discretionary: category.discretionary,
    excluded: category.excluded,
    realIncome: category.realIncome,
    dailyPaced: category.dailyPaced,
    protectedFlag: category.protectedFlag,
    sinkingFundEligible: category.sinkingFundEligible,
    archived: category.archived,
    sortOrder: category.sortOrder,
    ...overrides,
  };
}

function flowLabel(value) {
  return FLOW_LABELS[value] || value;
}

function CategoryEditorRow({ category, options, onSaveCategory, saving }) {
  const [label, setLabel] = useState(category.label || "");
  const [budgetBucket, setBudgetBucket] = useState(category.budgetBucket || "");
  const [fixedness, setFixedness] = useState(category.fixedness || "");
  const [flowType, setFlowType] = useState(category.flowType || "");
  const [dailyPaced, setDailyPaced] = useState(!!category.dailyPaced);
  const [protectedFlag, setProtectedFlag] = useState(!!category.protectedFlag);
  const [sinkingFundEligible, setSinkingFundEligible] = useState(!!category.sinkingFundEligible);
  const canSave = label.trim() !== "" && !saving;

  return (
    <tr>
      <td>
        <input value={label} aria-label={`Nazwa kategorii ${category.label}`} onChange={(event) => setLabel(event.target.value)} />
      </td>
      <td>
        <select value={budgetBucket} aria-label={`Koszyk kategorii ${category.label}`} onChange={(event) => setBudgetBucket(event.target.value)}>
          {options.bucketOptions.map((bucket) => (
            <option key={bucket} value={bucket}>{bucket}</option>
          ))}
        </select>
      </td>
      <td>
        <select value={fixedness} aria-label={`Charakter kategorii ${category.label}`} onChange={(event) => setFixedness(event.target.value)}>
          {options.fixednessOptions.map((value) => (
            <option key={value} value={value}>{value}</option>
          ))}
        </select>
      </td>
      <td>
        <select value={flowType} aria-label={`Typ przepływu ${category.label}`} onChange={(event) => setFlowType(event.target.value)}>
          {options.flowOptions.map((value) => (
            <option key={value} value={value}>{flowLabel(value)}</option>
          ))}
        </select>
      </td>
      <td>
        <label className="nwInline"><input type="checkbox" checked={dailyPaced} aria-label={`Tempo dzienne ${category.label}`} onChange={(event) => setDailyPaced(event.target.checked)} /> dzienne</label>
        <label className="nwInline"><input type="checkbox" checked={protectedFlag} aria-label={`Chroniona ${category.label}`} onChange={(event) => setProtectedFlag(event.target.checked)} /> chroń</label>
        <label className="nwInline"><input type="checkbox" checked={sinkingFundEligible} aria-label={`Fundusz celowy ${category.label}`} onChange={(event) => setSinkingFundEligible(event.target.checked)} /> fundusz</label>
      </td>
      <td>
        <button
          type="button"
          className="primaryButton"
          disabled={!canSave}
          aria-label={`Zapisz kategorię ${category.label}`}
          onClick={() => onSaveCategory(category.categoryId, toRequest(category, {
            label: label.trim(),
            budgetBucket,
            fixedness,
            flowType,
            dailyPaced,
            protectedFlag,
            sinkingFundEligible,
            archived: false,
          }))}
        >
          {saving ? "..." : "Zapisz"}
        </button>
        <button
          type="button"
          className="nwGhost"
          disabled={saving}
          aria-label={`Ukryj ${category.label}`}
          onClick={() => onSaveCategory(category.categoryId, toRequest(category, { archived: true }))}
        >
          Ukryj
        </button>
      </td>
    </tr>
  );
}

function GroupSection({ group, options, onSaveCategory, onSaveGroup, saving }) {
  const [label, setLabel] = useState(group.label || "");
  return (
    <div className="categoryGroup">
      <div className="nwNewLiability">
        <input value={label} aria-label={`Nazwa grupy ${group.label}`} onChange={(event) => setLabel(event.target.value)} />
        <button
          type="button"
          className="primaryButton"
          disabled={label.trim() === "" || saving}
          aria-label={`Zapisz grupę ${group.label}`}
          onClick={() => onSaveGroup(group.groupId, { label: label.trim(), sortOrder: group.sortOrder })}
        >
          {saving ? "..." : "Zapisz grupę"}
        </button>
      </div>
      {group.categories.length ? (
        <div className="nwTableScroll">
          <table className="nwTable">
            <thead>
              <tr>
                <th>Kategoria</th>
                <th>Koszyk</th>
                <th>Charakter</th>
                <th>Przepływ</th>
                <th>Flagi</th>
                <th aria-label="Akcje" />
              </tr>
            </thead>
            <tbody>
              {group.categories.map((category) => (
                <CategoryEditorRow key={category.categoryId} category={category} options={options} onSaveCategory={onSaveCategory} saving={saving} />
              ))}
            </tbody>
          </table>
        </div>
      ) : (
        <p className="nwMuted">Brak aktywnych kategorii w tej grupie.</p>
      )}
    </div>
  );
}

function NewCategoryForm({ options, onSaveCategory, saving }) {
  const [name, setName] = useState("");
  const [groupId, setGroupId] = useState(options.groupOptions[0]?.id || "");
  const [area, setArea] = useState(options.areaOptions[0] || "");
  const [budgetBucket, setBudgetBucket] = useState(options.bucketOptions[0] || "");
  const [fixedness, setFixedness] = useState(options.fixednessOptions[0] || "");
  const [flowType, setFlowType] = useState(options.flowOptions[0] || "");
  const canAdd = name.trim() !== "" && budgetBucket !== "" && area !== "" && !saving;
  return (
    <div className="nwNewLiability">
      <input value={name} placeholder="Nazwa nowej kategorii" aria-label="Nazwa nowej kategorii" onChange={(event) => setName(event.target.value)} />
      <select value={groupId} aria-label="Grupa nowej kategorii" onChange={(event) => setGroupId(event.target.value)}>
        {options.groupOptions.map((group) => (
          <option key={group.id} value={group.id}>{group.label}</option>
        ))}
      </select>
      <select value={area} aria-label="Obszar nowej kategorii" onChange={(event) => setArea(event.target.value)}>
        {options.areaOptions.map((value) => (
          <option key={value} value={value}>{value}</option>
        ))}
      </select>
      <select value={budgetBucket} aria-label="Koszyk nowej kategorii" onChange={(event) => setBudgetBucket(event.target.value)}>
        {options.bucketOptions.map((value) => (
          <option key={value} value={value}>{value}</option>
        ))}
      </select>
      <select value={fixedness} aria-label="Charakter nowej kategorii" onChange={(event) => setFixedness(event.target.value)}>
        {options.fixednessOptions.map((value) => (
          <option key={value} value={value}>{value}</option>
        ))}
      </select>
      <select value={flowType} aria-label="Typ przepływu nowej kategorii" onChange={(event) => setFlowType(event.target.value)}>
        {options.flowOptions.map((value) => (
          <option key={value} value={value}>{flowLabel(value)}</option>
        ))}
      </select>
      <button
        type="button"
        className="primaryButton"
        disabled={!canAdd}
        onClick={() => {
          onSaveCategory(slug(name), {
            label: name.trim(),
            area,
            analyticsGroup: name.trim(),
            groupId,
            budgetBucket,
            fixedness,
            flowType,
            discretionary: false,
            excluded: false,
            realIncome: false,
            dailyPaced: false,
            protectedFlag: false,
            sinkingFundEligible: false,
            archived: false,
            sortOrder: 999,
          });
          setName("");
        }}
      >
        {saving ? "Dodaję..." : "Dodaj kategorię"}
      </button>
    </div>
  );
}

export function CategoriesView({ catalog, onSaveCategory, onSaveGroup, saving = false, status }) {
  const model = selectCategoryCatalog(catalog);
  return (
    <Panel title="Kategorie i grupy">
      <p className="nwMuted">Dodawaj, zmieniaj nazwy i porządkuj kategorie. Koszyk, obszar i typ przepływu pochodzą ze stałego słownika planowania.</p>
      {status ? (
        <div className={`dataQualityBanner ${status.type === "error" ? "warn" : "neutral"}`}>
          <span>{status.message}</span>
        </div>
      ) : null}
      {model.groups.map((group) => (
        <GroupSection key={group.groupId} group={group} options={model} onSaveCategory={onSaveCategory} onSaveGroup={onSaveGroup} saving={saving} />
      ))}
      {model.archived.length ? (
        <div className="categoryGroup">
          <h3 className="nwMuted">Ukryte kategorie</h3>
          <div className="nwTableScroll">
            <table className="nwTable">
              <tbody>
                {model.archived.map((category) => (
                  <tr key={category.categoryId}>
                    <td>{category.label}</td>
                    <td>
                      <button
                        type="button"
                        className="nwGhost"
                        disabled={saving}
                        aria-label={`Przywróć ${category.label}`}
                        onClick={() => onSaveCategory(category.categoryId, toRequest(category, { archived: false }))}
                      >
                        Przywróć
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      ) : null}
      <h3 className="nwMuted">Nowa kategoria</h3>
      <NewCategoryForm options={model} onSaveCategory={onSaveCategory} saving={saving} />
    </Panel>
  );
}
