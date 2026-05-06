import {
  Area,
  AreaChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { moneyDec } from "../../domain/formatters.js";

export function CashflowChart({ data }) {
  return (
    <div className="chart">
      <ResponsiveContainer width="100%" height="100%">
        <AreaChart data={data} margin={{ top: 12, right: 18, left: 0, bottom: 0 }}>
          <defs>
            <linearGradient id="spend" x1="0" y1="0" x2="0" y2="1">
              <stop offset="5%" stopColor="#2563eb" stopOpacity={0.32} />
              <stop offset="95%" stopColor="#2563eb" stopOpacity={0.03} />
            </linearGradient>
            <linearGradient id="income" x1="0" y1="0" x2="0" y2="1">
              <stop offset="5%" stopColor="#0f766e" stopOpacity={0.28} />
              <stop offset="95%" stopColor="#0f766e" stopOpacity={0.02} />
            </linearGradient>
          </defs>
          <CartesianGrid stroke="#e5e7eb" vertical={false} />
          <XAxis dataKey="month" tick={{ fontSize: 12 }} />
          <YAxis tickFormatter={(value) => `${Math.round(value / 1000)}k`} tick={{ fontSize: 12 }} />
          <Tooltip formatter={(value) => moneyDec(value)} />
          <Area type="monotone" dataKey="income" name="Wpływy" stroke="#0f766e" fill="url(#income)" strokeWidth={2} isAnimationActive={false} />
          <Area type="monotone" dataKey="spend" name="Wydatki" stroke="#2563eb" fill="url(#spend)" strokeWidth={2} isAnimationActive={false} />
        </AreaChart>
      </ResponsiveContainer>
    </div>
  );
}
