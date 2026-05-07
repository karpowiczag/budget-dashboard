import { describe, expect, it } from "vitest";
import { apiFilters } from "./App.jsx";

describe("apiFilters", () => {
  it("falls back from day to month scope when no day is selected", () => {
    expect(apiFilters({
      timeScope: "day",
      selectedMonth: "05.2026",
      selectedDay: "",
      drillFilter: null,
    })).toEqual({
      scope: "month",
      month: "2026-05",
    });
  });

  it("keeps day scope when both month and day are selected", () => {
    expect(apiFilters({
      timeScope: "day",
      selectedMonth: "05.2026",
      selectedDay: "7",
      drillFilter: { type: "category", value: "Transport" },
    })).toEqual({
      scope: "day",
      month: "2026-05",
      date: "2026-05-07",
      category: "Transport",
    });
  });

  it("falls back to year scope when month scope has no month yet", () => {
    expect(apiFilters({
      timeScope: "month",
      selectedMonth: "",
      selectedDay: "",
      drillFilter: null,
    })).toEqual({
      scope: "year",
    });
  });
});
