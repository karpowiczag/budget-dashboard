import { SpendBarChart } from "../components/charts/SpendBarChart.jsx";
import { Panel } from "../components/ui/Panel.jsx";
import { CHART_COLORS } from "../domain/charts.js";
import { money, percent } from "../domain/formatters.js";

export function CategoriesView({ categoryTop, hierarchyTop, scopedStats, onDrill }) {
  return (
    <section className="viewStack">
      <section className="categorySummary">
        {scopedStats.areaTop.slice(0, 6).map((row, index) => (
          <button className="categoryTile clickable" key={row.area} onClick={() => onDrill({ type: "area", value: row.area, label: `Obszar: ${row.area}` })}>
            <i style={{ background: CHART_COLORS[index % CHART_COLORS.length] }} />
            <span>{row.area}</span>
            <strong>{money(row.spend)}</strong>
            <em>{percent(row.spend / (scopedStats.spend || 1))}</em>
          </button>
        ))}
      </section>

      <section className="gridTwo">
        <Panel title="Obszary budżetu">
          <SpendBarChart
            data={scopedStats.areaTop.slice(0, 8)}
            dataKey="area"
            yAxisWidth={140}
            left={100}
            onSelect={(row) => onDrill({ type: "area", value: row.area, label: `Obszar: ${row.area}` })}
          />
        </Panel>

        <Panel title="Grupy kosztów">
          <SpendBarChart
            data={scopedStats.groupTop.slice(0, 10)}
            dataKey="group"
            color="#2563eb"
            onSelect={(row) => onDrill({ type: "group", value: row.group, label: `Grupa: ${row.group}` })}
          />
        </Panel>
      </section>

      <section className="gridTwo">
        <Panel title="Top kategorie">
          <SpendBarChart
            data={categoryTop}
            dataKey="category"
            yAxisWidth={120}
            color="#b45309"
            onSelect={(row) => onDrill({ type: "category", value: row.category, label: `Kategoria: ${row.category}` })}
          />
        </Panel>

        <Panel title="Top podkategorie">
          <div className="tableWrap smallRows">
            <table>
              <thead>
                <tr>
                  <th>Podkategoria</th>
                  <th>Kwota</th>
                  <th>Udział</th>
                </tr>
              </thead>
              <tbody>
                {scopedStats.subcategoryTop.slice(0, 16).map((row) => (
                  <tr
                    className="clickableRow"
                    key={row.subcategory}
                    onClick={() => onDrill({ type: "subcategory", value: row.subcategory, label: `Podkategoria: ${row.subcategory}` })}
                  >
                    <td>{row.subcategory}</td>
                    <td className="num">{money(row.spend)}</td>
                    <td className="num">{percent(row.spend / (scopedStats.spend || 1))}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Panel>
      </section>

      <Panel title="Hierarchia: obszar -> grupa -> kategoria -> podkategoria">
        <div className="tableWrap smallRows">
          <table>
            <thead>
              <tr>
                <th>Obszar</th>
                <th>Grupa</th>
                <th>Kategoria</th>
                <th>Podkategoria</th>
                <th>Kwota</th>
                <th>Liczba</th>
                <th>Udział</th>
              </tr>
            </thead>
            <tbody>
              {hierarchyTop.map((row) => (
                <tr
                  className="clickableRow"
                  key={`${row.area}-${row.group}-${row.category}-${row.subcategory}`}
                  onClick={() => onDrill({ type: "subcategory", value: `${row.category} · ${row.subcategory}`, label: `Podkategoria: ${row.category} · ${row.subcategory}` })}
                >
                  <td>{row.area}</td>
                  <td>{row.group}</td>
                  <td>{row.category}</td>
                  <td>{row.subcategory}</td>
                  <td className="num">{money(row.spend)}</td>
                  <td className="num">{row.count}</td>
                  <td className="num">{percent(row.spend / (scopedStats.spend || 1))}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Panel>
    </section>
  );
}
