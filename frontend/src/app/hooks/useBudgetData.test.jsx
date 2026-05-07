import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { useBudgetData } from "./useBudgetData.js";
import {
  fetchBudgetSettings,
  fetchDashboard,
  fetchImportRuns,
  fetchYears,
  rebuildTransactions,
  updateBudgetSettings,
  uploadTransactions,
} from "../api/budgetApi.js";

vi.mock("../api/budgetApi.js", () => ({
  fetchBudgetSettings: vi.fn(),
  fetchDashboard: vi.fn(),
  fetchImportRuns: vi.fn(),
  fetchYears: vi.fn(),
  rebuildTransactions: vi.fn(),
  updateBudgetSettings: vi.fn(),
  uploadTransactions: vi.fn(),
}));

describe("useBudgetData", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    fetchYears.mockResolvedValue([{ year: 2026 }]);
    fetchBudgetSettings.mockResolvedValue({ categoryLimits: [] });
    fetchImportRuns.mockResolvedValue([]);
    fetchDashboard.mockResolvedValue({ year: 2026, monthly: [], kpis: {}, budgetMix: [] });
    rebuildTransactions.mockResolvedValue({ transactions: 4, duplicatesRemoved: 0, years: [2026] });
    updateBudgetSettings.mockResolvedValue({ categoryLimits: [{ category: "Dom", limit: 1000 }] });
    uploadTransactions.mockResolvedValue({ transactions: 3, duplicatesRemoved: 1, years: [2026] });
  });

  it("loads years, settings and selected dashboard through query cache", async () => {
    const { result } = renderHook(() => useBudgetData(), { wrapper: createWrapper() });

    await waitFor(() => expect(result.current.status).toBe("ready"));

    expect(result.current.year).toBe("2026");
    expect(result.current.years).toEqual([{ year: 2026 }]);
    expect(result.current.importRuns).toEqual([]);
    expect(fetchDashboard).toHaveBeenCalledWith("2026");
  });

  it("updates settings and uploads through mutations", async () => {
    const { result } = renderHook(() => useBudgetData(), { wrapper: createWrapper() });
    await waitFor(() => expect(result.current.status).toBe("ready"));

    await result.current.saveBudgetSettings({ categoryLimits: [{ category: "Dom", limit: 1000 }] });
    expect(updateBudgetSettings.mock.calls[0][0]).toEqual({ categoryLimits: [{ category: "Dom", limit: 1000 }] });
    await waitFor(() => expect(result.current.settingsStatus).toMatchObject({ type: "success" }));

    await result.current.handleUpload(new File(["csv"], "fake.csv", { type: "text/csv" }));
    expect(uploadTransactions).toHaveBeenCalledOnce();
    await waitFor(() => expect(result.current.importStatus).toMatchObject({ type: "success" }));

    await result.current.handleRebuild("2026");
    expect(rebuildTransactions.mock.calls[0][0]).toBe("2026");
    await waitFor(() => expect(result.current.importStatus).toMatchObject({ type: "success" }));
  });
});

function createWrapper() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
    },
  });
  return function Wrapper({ children }) {
    return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
  };
}
