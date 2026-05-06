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
            <tr key={tx.Lp}>
              <td>{tx.Data}</td>
              <td>{tx.Sprzedawca}</td>
              <td>{tx["Kategoria skorygowana"]}</td>
              <td>{tx.Podkategoria}</td>
              <td>{tx["Koszyk budżetu"]}</td>
              <td className={`num ${Number(tx.Kwota) < 0 ? "neg" : "pos"}`}>{moneyDec(tx.Kwota)}</td>
              <td>
                <span className={`badge ${tx["Pewność kategorii"]}`}>{tx["Pewność kategorii"]}</span>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
