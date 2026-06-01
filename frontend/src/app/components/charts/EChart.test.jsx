import { cleanup, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { EChart } from "./EChart.jsx";

const chart = {
  dispose: vi.fn(),
  getDataURL: vi.fn(() => "data:image/png;base64,test"),
  off: vi.fn(),
  on: vi.fn(),
  resize: vi.fn(),
  setOption: vi.fn(),
};

vi.mock("echarts/core", () => ({
  init: vi.fn(() => chart),
  use: vi.fn(),
}));

afterEach(() => {
  cleanup();
  vi.clearAllMocks();
});

describe("EChart", () => {
  it("renders an empty state without initializing a chart", () => {
    render(<EChart empty option={{}} emptyMessage="Brak danych testowych." />);

    expect(screen.getByText("Brak danych testowych.")).toBeInTheDocument();
    expect(chart.setOption).not.toHaveBeenCalled();
  });

  it("sets options and exports PNG through the chart instance", async () => {
    vi.spyOn(HTMLAnchorElement.prototype, "click").mockImplementation(() => {});
    render(<EChart exportName="test-chart" option={{ series: [] }} />);

    await waitFor(() => expect(chart.setOption).toHaveBeenCalledWith({ animation: false, series: [], aria: { enabled: true } }, true));
    await userEvent.click(screen.getByRole("button", { name: /PNG/ }));

    expect(chart.getDataURL).toHaveBeenCalledWith({
      backgroundColor: "#ffffff",
      pixelRatio: 2,
      type: "png",
    });
  });
});
