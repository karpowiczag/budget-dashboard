import { useEffect, useState } from "react";
import { fetchBudgetSettings, fetchDashboard, fetchYears, updateBudgetSettings, uploadTransactions } from "../api/budgetApi.js";

export function useBudgetData() {
  const [years, setYears] = useState([]);
  const [year, setYear] = useState("");
  const [data, setData] = useState(null);
  const [status, setStatus] = useState("loading");
  const [uploading, setUploading] = useState(false);
  const [importStatus, setImportStatus] = useState(null);
  const [budgetSettings, setBudgetSettings] = useState(null);
  const [settingsStatus, setSettingsStatus] = useState(null);

  useEffect(() => {
    let cancelled = false;
    Promise.all([fetchYears(), fetchBudgetSettings()])
      .then(([payload, settings]) => {
        if (cancelled) return;
        setYears(payload);
        setBudgetSettings(settings);
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
    fetchDashboard(year)
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
        setData(await fetchDashboard(importedYear));
        setStatus("ready");
      }
    } catch (error) {
      setImportStatus({ type: "error", message: error.message });
      if (!data) setStatus("empty");
    } finally {
      setUploading(false);
    }
  }

  async function saveBudgetSettings(settings) {
    setSettingsStatus({ type: "info", message: "Zapisuję ustawienia budżetu..." });
    try {
      const saved = await updateBudgetSettings(settings);
      setBudgetSettings(saved);
      setSettingsStatus({ type: "success", message: "Ustawienia zapisane dla kolejnych analiz." });
      return saved;
    } catch (error) {
      setSettingsStatus({ type: "error", message: error.message });
      throw error;
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
    budgetSettings,
    settingsStatus,
    handleUpload,
    saveBudgetSettings,
  };
}
