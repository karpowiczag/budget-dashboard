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

function CategoryEditorRow({ category, options, onSaveCategory, onDeleteCategory, saving }) {
  const [label, setLabel] = useState(category.label || "");
  const [budgetBucket, setBudgetBucket] = useState(category.budgetBucket || "");
  const [fixedness, setFixedness] = useState(category.fixedness || "");
  const [flowType, setFlowType] = useState(category.flowType || "");
  const [dailyPaced, setDailyPaced] = useState(!!category.dailyPaced);
  const [protectedFlag, setProtectedFlag] = useState(!!category.protectedFlag);
  const [sinkingFundEligible, setSinkingFundEligible] = useState(!!category.sinkingFundEligible);
  const [mergeTarget, setMergeTarget] = useState("");
  const canSave = label.trim() !== "" && !saving;
  const mergeTargets = options.categoryOptions.filter((option) => option.id !== category.categoryId);

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
        {category.builtin ? (
          <span className="nwMuted">systemowa</span>
        ) : (
          <>
            <select value={mergeTarget} aria-label={`Cel przeniesienia ${category.label}`} onChange={(event) => setMergeTarget(event.target.value)}>
              <option value="">Przenieś do…</option>
              {mergeTargets.map((option) => (
                <option key={option.id} value={option.id}>{option.label}</option>
              ))}
            </select>
            <button
              type="button"
              className="nwGhost"
              disabled={!mergeTarget || saving}
              aria-label={`Usuń kategorię ${category.label}`}
              onClick={() => {
                if (window.confirm(`Usunąć „${category.label}” i przenieść jej transakcje oraz reguły do wybranej kategorii?`)) {
                  onDeleteCategory(category.categoryId, mergeTarget);
                }
              }}
            >
              Usuń / Scal
            </button>
          </>
        )}
      </td>
    </tr>
  );
}

function GroupSection({ group, options, onSaveCategory, onSaveGroup, onDeleteCategory, saving }) {
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
                <CategoryEditorRow key={category.categoryId} category={category} options={options} onSaveCategory={onSaveCategory} onDeleteCategory={onDeleteCategory} saving={saving} />
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

function RuleEditorRow({ rule, categoryOptions, onSaveRule, onDeleteRule, saving }) {
  const [pattern, setPattern] = useState(rule.pattern || "");
  const [categoryId, setCategoryId] = useState(rule.categoryId || "");
  const [priority, setPriority] = useState(String(rule.priority ?? 100));
  const [enabled, setEnabled] = useState(rule.enabled !== false);
  const canSave = pattern.trim() !== "" && categoryId !== "" && !saving;
  return (
    <tr>
      <td>
        <input value={pattern} aria-label={`Wzorzec reguły ${rule.ruleId}`} onChange={(event) => setPattern(event.target.value)} />
      </td>
      <td>
        <select value={categoryId} aria-label={`Kategoria reguły ${rule.ruleId}`} onChange={(event) => setCategoryId(event.target.value)}>
          {categoryOptions.map((option) => (
            <option key={option.id} value={option.id}>{option.label}</option>
          ))}
        </select>
      </td>
      <td className="num">
        <input type="number" value={priority} aria-label={`Priorytet reguły ${rule.ruleId}`} onChange={(event) => setPriority(event.target.value)} />
      </td>
      <td>
        <label className="nwInline"><input type="checkbox" checked={enabled} aria-label={`Aktywna reguła ${rule.ruleId}`} onChange={(event) => setEnabled(event.target.checked)} /> aktywna</label>
      </td>
      <td className="nwMuted">{rule.source}</td>
      <td>
        <button
          type="button"
          className="primaryButton"
          disabled={!canSave}
          aria-label={`Zapisz regułę ${rule.ruleId}`}
          onClick={() => onSaveRule(rule.ruleId, { pattern: pattern.trim(), categoryId, priority: Number(priority) || 0, enabled })}
        >
          {saving ? "..." : "Zapisz"}
        </button>
        <button type="button" className="nwGhost" disabled={saving} aria-label={`Usuń regułę ${rule.ruleId}`} onClick={() => onDeleteRule(rule.ruleId)}>
          Usuń
        </button>
      </td>
    </tr>
  );
}

function NewRuleForm({ categoryOptions, onSaveRule, saving }) {
  const [pattern, setPattern] = useState("");
  const [categoryId, setCategoryId] = useState(categoryOptions[0]?.id || "");
  const canAdd = pattern.trim() !== "" && categoryId !== "" && !saving;
  return (
    <div className="nwNewLiability">
      <input value={pattern} placeholder="Wzorzec (regex po opisie transakcji)" aria-label="Wzorzec nowej reguły" onChange={(event) => setPattern(event.target.value)} />
      <select value={categoryId} aria-label="Kategoria nowej reguły" onChange={(event) => setCategoryId(event.target.value)}>
        {categoryOptions.map((option) => (
          <option key={option.id} value={option.id}>{option.label}</option>
        ))}
      </select>
      <button
        type="button"
        className="primaryButton"
        disabled={!canAdd}
        onClick={() => {
          onSaveRule("new", { pattern: pattern.trim(), categoryId, priority: 100, enabled: true });
          setPattern("");
        }}
      >
        {saving ? "Dodaję..." : "Dodaj regułę"}
      </button>
    </div>
  );
}

export function CategoriesView({ catalog, onSaveCategory, onSaveGroup, onDeleteCategory, onSaveRule, onDeleteRule, saving = false, status }) {
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
        <GroupSection key={group.groupId} group={group} options={model} onSaveCategory={onSaveCategory} onSaveGroup={onSaveGroup} onDeleteCategory={onDeleteCategory} saving={saving} />
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

      <h3 className="nwMuted">Reguły klasyfikacji</h3>
      <p className="nwMuted">Reguła dopasowuje opis transakcji (regex) do kategorii; niższy priorytet wygrywa. Zmiany działają po ponownej analizie / imporcie.</p>
      {model.rules.length ? (
        <div className="nwTableScroll">
          <table className="nwTable">
            <thead>
              <tr>
                <th>Wzorzec</th>
                <th>Kategoria</th>
                <th className="num">Priorytet</th>
                <th>Aktywna</th>
                <th>Źródło</th>
                <th aria-label="Akcje" />
              </tr>
            </thead>
            <tbody>
              {model.rules.map((rule) => (
                <RuleEditorRow key={rule.ruleId} rule={rule} categoryOptions={model.categoryOptions} onSaveRule={onSaveRule} onDeleteRule={onDeleteRule} saving={saving} />
              ))}
            </tbody>
          </table>
        </div>
      ) : null}
      <NewRuleForm categoryOptions={model.categoryOptions} onSaveRule={onSaveRule} saving={saving} />
    </Panel>
  );
}
