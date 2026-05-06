import {
  Bar,
  BarChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { moneyDec } from "../../domain/formatters.js";

export function SpendBarChart({
  data,
  dataKey,
  yAxisWidth = 126,
  color = "#0f766e",
  left = 86,
  onSelect,
}) {
  return (
    <div className="chart compact">
      <ResponsiveContainer width="100%" height="100%">
        <BarChart data={data} layout="vertical" margin={{ top: 6, right: 18, left, bottom: 4 }}>
          <CartesianGrid stroke="#e5e7eb" horizontal={false} />
          <XAxis type="number" tickFormatter={(value) => `${Math.round(value / 1000)}k`} />
          <YAxis dataKey={dataKey} type="category" width={yAxisWidth} tick={{ fontSize: 12 }} />
          <Tooltip formatter={(value) => moneyDec(value)} />
          <Bar
            dataKey="spend"
            name="Wydatki"
            fill={color}
            radius={[0, 4, 4, 0]}
            isAnimationActive={false}
            onClick={onSelect}
            cursor={onSelect ? "pointer" : "default"}
          />
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}
