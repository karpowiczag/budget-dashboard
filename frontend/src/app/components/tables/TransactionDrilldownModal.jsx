import { CalendarDays, Loader2, X } from "lucide-react";
import { useEffect, useRef } from "react";
import { money } from "../../domain/formatters.js";
import { TransactionsTable } from "./TransactionsTable.jsx";

export function TransactionDrilldownModal({
  title,
  subtitle,
  page,
  loading,
  error,
  onClose,
  onPageChange,
}) {
  const dialogRef = useRef(null);

  useEffect(() => {
    const previouslyFocused = document.activeElement;
    const dialog = dialogRef.current;
    const focusable = () => (dialog
      ? Array.from(dialog.querySelectorAll('button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])'))
          .filter((element) => !element.disabled && element.getAttribute("aria-hidden") !== "true")
      : []);

    (focusable()[0] || dialog)?.focus();

    function handleKeyDown(event) {
      if (event.key === "Escape") {
        onClose();
        return;
      }
      if (event.key !== "Tab") return;
      const items = focusable();
      if (!items.length) {
        event.preventDefault();
        return;
      }
      const first = items[0];
      const last = items[items.length - 1];
      if (event.shiftKey && document.activeElement === first) {
        event.preventDefault();
        last.focus();
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault();
        first.focus();
      }
    }

    document.addEventListener("keydown", handleKeyDown);
    return () => {
      document.removeEventListener("keydown", handleKeyDown);
      if (previouslyFocused instanceof HTMLElement) {
        previouslyFocused.focus();
      }
    };
  }, [onClose]);

  const items = page?.items || [];
  const currentPage = page?.page || 0;
  const totalPages = page?.totalPages || 0;
  const totalItems = page?.totalItems || 0;
  const visibleSpend = items.reduce((sum, tx) => sum + Number(tx.spend || 0), 0);
  const visibleIncome = items.reduce((sum, tx) => sum + Number(tx.income || 0), 0);
  const visibleExcludedOutgoing = items.reduce((sum, tx) => sum + Number(tx.excludedOutgoing || 0), 0);

  return (
    <div className="modalBackdrop" role="presentation" onMouseDown={onClose}>
      <section
        ref={dialogRef}
        aria-label={title}
        aria-modal="true"
        className="transactionModal"
        role="dialog"
        tabIndex={-1}
        onMouseDown={(event) => event.stopPropagation()}
      >
        <header className="modalHead">
          <div>
            <p className="eyebrow">Transakcje</p>
            <h2>{title}</h2>
            {subtitle && <span>{subtitle}</span>}
          </div>
          <button type="button" className="closeButton" onClick={onClose} aria-label="Zamknij">
            <X size={20} />
          </button>
        </header>

        <div className="modalMeta">
          <span><CalendarDays size={15} /> Pokazuje {items.length} z {totalItems}</span>
          <span>Wydatki na stronie: {money(visibleSpend)}</span>
          <span>Dochód na stronie: {money(visibleIncome)}</span>
          <span>Oszcz./nadpłaty na stronie: {money(visibleExcludedOutgoing)}</span>
        </div>

        <div className="modalBody">
          {loading && (
            <div className="modalState">
              <Loader2 size={18} className="spin" />
              Ładowanie transakcji...
            </div>
          )}
          {!loading && error && <div className="modalState error">{error}</div>}
          {!loading && !error && items.length === 0 && <div className="modalState">Brak transakcji dla tego filtra.</div>}
          {!loading && !error && items.length > 0 && <TransactionsTable transactions={items} />}
        </div>

        <footer className="modalFoot">
          <div>
            Strona {totalPages ? currentPage + 1 : 0} / {totalPages}
          </div>
          <div className="pager modalPager">
            <button type="button" disabled={currentPage <= 0 || loading} onClick={() => onPageChange(currentPage - 1)}>Poprzednia</button>
            <button type="button" disabled={!totalPages || currentPage >= totalPages - 1 || loading} onClick={() => onPageChange(currentPage + 1)}>Następna</button>
          </div>
        </footer>
      </section>
    </div>
  );
}
