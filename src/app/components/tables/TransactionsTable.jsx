import { moneyDec } from "../../domain/formatters.js";

export function TransactionsTable({ transactions }) {
  return (
    <div className="tableWrap transactions">
      <table>
        <thead>
          <tr>
            <th>Data</th>
            <th>Sprzedawca</th>
            <th>Kategoria</th>
            <th>Podkategoria</th>
            <th>Koszyk</th>
            <th>Kwota</th>
            <th>Pewność</th>
          </tr>
        </thead>
        <tbody>
          {transactions.map((tx) => (
            <tr key={tx.id || tx.lp}>
              <td>{tx.postedDate || tx.date}</td>
              <td>{tx.merchant}</td>
              <td>{tx.correctedCategory || tx.category}</td>
              <td>{tx.subcategory}</td>
              <td>{tx.bucket}</td>
              <td className={`num ${Number(tx.amount) < 0 ? "neg" : "pos"}`}>{moneyDec(tx.amount)}</td>
              <td>
                <span className={`badge ${tx.confidence}`}>{tx.confidence}</span>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
