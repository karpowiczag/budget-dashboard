import { useEffect, useState } from "react";
import { fetchBudget, fetchYears, uploadTransactions } from "../api/budgetApi.js";

export function useBudgetData() {
  const [years, setYears] = useState([]);
  const [year, setYear] = useState("");
  const [data, setData] = useState(null);
  const [status, setStatus] = useState("loading");
  const [uploading, setUploading] = useState(false);
  const [importStatus, setImportStatus] = useState(null);

  useEffect(() => {
    let cancelled = false;
    fetchYears()
      .then((payload) => {
        if (cancelled) return;
        setYears(payload);
        const latest = payload.at(-1)?.year;
        if (latest) {
          setYear(String(latest));
        } else {
          setStatus("empty");
        }
      })
      .catch(() => {
        if (!cancelled) setStatus("error");
      });
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    if (!year) return undefined;
    let cancelled = false;
    setStatus("loading");
    fetchBudget(year)
      .then((payload) => {
        if (!cancelled) {
          setData(payload);
          setStatus("ready");
        }
      })
      .catch(() => {
        if (!cancelled) setStatus("error");
      });
    return () => {
      cancelled = true;
    };
  }, [year]);

  async function handleUpload(file) {
    if (!file) return;
    setUploading(true);
    setImportStatus({ type: "info", message: "Importuję i kategoryzuję transakcje..." });
    try {
      const payload = await uploadTransactions(file);
      const nextYears = await fetchYears();
      setYears(nextYears);
      const importedYear = payload.years?.at(-1) || nextYears.at(-1)?.year;
      setImportStatus({ type: "success", message: `Zaimportowano ${payload.transactions} transakcji dla ${importedYear}.` });
      if (importedYear) {
        setYear(String(importedYear));
        setData(await fetchBudget(importedYear));
        setStatus("ready");
      }
    } catch (error) {
      setImportStatus({ type: "error", message: error.message });
      if (!data) setStatus("empty");
    } finally {
      setUploading(false);
    }
  }

  return {
    years,
    year,
    setYear,
    data,
    status,
    uploading,
    importStatus,
    handleUpload,
  };
}
