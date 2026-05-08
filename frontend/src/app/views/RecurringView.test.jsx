import { cleanup, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { RecurringView } from "./RecurringView.jsx";

vi.mock("../components/charts/RecurringTimelineChart.jsx", () => ({
  RecurringTimelineChart: () => <div data-testid="recurring-timeline-chart" />,
}));

afterEach(() => cleanup());

describe("RecurringView", () => {
  it("shows recurring monthly obligations and opens merchant drilldown", async () => {
    const onInspect = vi.fn();

    render(
      <RecurringView
        onInspect={onInspect}
        recurringSummary={{
          cards: [
            { label: "Cykliczne koszty", value: 600, detail: "3 rozpoznanych sprzedawców" },
            { label: "Fundusze celowe", value: 200, detail: "2 rezerwy miesięczne" },
            { label: "Najbliższy miesiąc", value: "05.2026", detail: "bazuje na cyklicznych wzorcach i limitach", textValue: true },
          ],
          recurringCalendar: [
            { merchant: "NETFLIX", category: "Multimedia", avgDay: 2, months: 5, monthlyAverage: 60 },
          ],
          recurringTimeline: [
            { merchant: "NETFLIX", category: "Multimedia", day: 2, amount: 60 },
          ],
          sinkingFunds: [
            { name: "Ubezpieczenie", category: "Auto", monthlySetAside: 100, yearlyNeed: 1200 },
          ],
        }}
      />
    );

    expect(screen.getByText("Zobowiązania miesiąca")).toBeInTheDocument();
    expect(screen.getByText("Co przyjdzie w tym miesiącu")).toBeInTheDocument();
    expect(screen.getByTestId("recurring-timeline-chart")).toBeInTheDocument();
    expect(screen.getByText("Ubezpieczenie")).toBeInTheDocument();

    await userEvent.click(screen.getByRole("button", { name: /NETFLIX/ }));
    expect(onInspect).toHaveBeenCalledWith({
      title: "Transakcje: NETFLIX",
      filters: { query: "NETFLIX" },
      useTimeScope: false,
    });
  });
});
