import { useEffect, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { fetchBudgetSettings, fetchDashboard, fetchImportRuns, fetchYears, rebuildTransactions, updateBudgetSettings, uploadTransactions } from "../api/budgetApi.js";
import { budgetQueryKeys } from "../api/queryKeys.js";

export function useBudgetData() {
  const [year, setYear] = useState("");
  const [importStatus, setImportStatus] = useState(null);
  const [settingsStatus, setSettingsStatus] = useState(null);
  const queryClient = useQueryClient();

  const yearsQuery = useQuery({
    queryKey: budgetQueryKeys.years,
    queryFn: fetchYears,
  });

  const settingsQuery = useQuery({
    queryKey: budgetQueryKeys.budgetSettings,
    queryFn: fetchBudgetSettings,
  });

  const importRunsQuery = useQuery({
    queryKey: budgetQueryKeys.importRuns,
    queryFn: fetchImportRuns,
  });

  const dashboardQuery = useQuery({
    queryKey: budgetQueryKeys.dashboard(year),
    queryFn: () => fetchDashboard(year),
    enabled: !!year,
  });

  const uploadMutation = useMutation({
    mutationFn: uploadTransactions,
  });

  const rebuildMutation = useMutation({
    mutationFn: rebuildTransactions,
  });

  const settingsMutation = useMutation({
    mutationFn: updateBudgetSettings,
  });

  const years = yearsQuery.data || [];
  const budgetSettings = settingsQuery.data || null;
  const importRuns = importRunsQuery.data || [];
  const data = dashboardQuery.data || null;
  const uploading = uploadMutation.isPending || rebuildMutation.isPending;
  const rebuilding = rebuildMutation.isPending;
  const loading = yearsQuery.isPending || settingsQuery.isPending || (!!year && dashboardQuery.isPending);
  const status = loading
    ? "loading"
    : yearsQuery.isError || settingsQuery.isError || importRunsQuery.isError || dashboardQuery.isError
      ? "error"
      : years.length === 0
        ? "empty"
        : data
          ? "ready"
          : "loading";

  useEffect(() => {
    const latest = years.at(-1)?.year;
    if (!year && latest) {
      setYear(String(latest));
    }
  }, [year, years]);

  async function handleUpload(file) {
    if (!file) return;
    setImportStatus({ type: "info", message: "Importuję i kategoryzuję transakcje..." });
    try {
      const payload = await uploadMutation.mutateAsync(file);
      await queryClient.invalidateQueries({ queryKey: ["budget"] });
      const nextYears = await queryClient.fetchQuery({
        queryKey: budgetQueryKeys.years,
        queryFn: fetchYears,
      });
      const importedYear = payload.years?.at(-1) || nextYears.at(-1)?.year;
      setImportStatus({
        type: "success",
        message: `Zaimportowano ${payload.transactions} nowych transakcji dla ${importedYear}. Pominięte istniejące/duplikaty: ${payload.duplicatesRemoved || 0}.`,
      });
      if (importedYear) {
        setYear(String(importedYear));
        await queryClient.fetchQuery({
          queryKey: budgetQueryKeys.dashboard(importedYear),
          queryFn: () => fetchDashboard(importedYear),
        });
      }
    } catch (error) {
      setImportStatus({ type: "error", message: error.message });
    }
  }

  async function handleRebuild(rebuildYear = year) {
    if (!rebuildYear) return;
    setImportStatus({ type: "info", message: `Przebudowuję ${rebuildYear} i odświeżam kategoryzację...` });
    try {
      const payload = await rebuildMutation.mutateAsync(rebuildYear);
      await queryClient.invalidateQueries({ queryKey: ["budget"] });
      setImportStatus({
        type: "success",
        message: `Przebudowano ${payload.transactions} transakcji dla ${rebuildYear}. Usunięte duplikaty: ${payload.duplicatesRemoved || 0}.`,
      });
      setYear(String(rebuildYear));
      await queryClient.fetchQuery({
        queryKey: budgetQueryKeys.dashboard(rebuildYear),
        queryFn: () => fetchDashboard(rebuildYear),
      });
    } catch (error) {
      setImportStatus({ type: "error", message: error.message });
    }
  }

  async function saveBudgetSettings(settings) {
    setSettingsStatus({ type: "info", message: "Zapisuję ustawienia budżetu..." });
    try {
      const saved = await settingsMutation.mutateAsync(settings);
      queryClient.setQueryData(budgetQueryKeys.budgetSettings, saved);
      await queryClient.invalidateQueries({ queryKey: ["budget", "dashboard"] });
      await queryClient.invalidateQueries({ queryKey: ["budget", "analytics"] });
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
    importRuns,
    status,
    uploading,
    rebuilding,
    importStatus,
    budgetSettings,
    settingsStatus,
    settingsSaving: settingsMutation.isPending,
    handleUpload,
    handleRebuild,
    saveBudgetSettings,
  };
}
