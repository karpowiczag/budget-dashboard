import { cleanup, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { ImportView } from "./ImportView.jsx";

afterEach(() => cleanup());

describe("ImportView", () => {
  it("passes selected csv file to upload handler", async () => {
    const onUpload = vi.fn();
    const file = new File(["csv"], "transactions.csv", { type: "text/csv" });

    render(<ImportView onUpload={onUpload} uploading={false} importStatus={null} />);

    await userEvent.upload(screen.getByLabelText(/wybierz csv/i), file);

    expect(onUpload).toHaveBeenCalledWith(file);
  });

  it("shows upload status and disables picker while uploading", () => {
    render(
      <ImportView
        onUpload={vi.fn()}
        uploading
        importStatus={{ type: "info", message: "Importuję i kategoryzuję transakcje..." }}
      />
    );

    expect(screen.getByText("Importuję...")).toBeInTheDocument();
    expect(screen.getByText("Importuję i kategoryzuję transakcje...")).toBeInTheDocument();
    expect(screen.getByLabelText(/importuję/i)).toBeDisabled();
  });

  it("shows duplicate audit history without repeating header health cards", () => {
    render(
      <ImportView
        onUpload={vi.fn()}
        uploading={false}
        importStatus={null}
        importHealth={{
          cards: [
            { label: "Lata w bazie", value: 2, detail: "2025, 2026" },
            { label: "Usunięte duplikaty", value: 4, detail: "ostatnie 50 importów" },
          ],
        }}
        importRuns={[
          { id: 1, year: 2026, inputCsv: "2026 (3 CSV files)", status: "ok", message: "local rebuild", duplicatesRemoved: 4, createdAt: "2026-05-07T10:00:00Z" },
        ]}
      />
    );

    expect(screen.queryByRole("heading", { name: "Stan danych" })).not.toBeInTheDocument();
    expect(screen.getByText("Lokalny rebuild")).toBeInTheDocument();
    expect(screen.getByText(/duplikaty 4/)).toBeInTheDocument();
  });

  it("can rebuild active local year to apply new categorization rules", async () => {
    const onRebuild = vi.fn();

    render(
      <ImportView
        activeYear="2026"
        onRebuild={onRebuild}
        onUpload={vi.fn()}
        rebuilding={false}
        uploading={false}
        importStatus={null}
      />
    );

    await userEvent.click(screen.getByRole("button", { name: /przebuduj rok/i }));

    expect(onRebuild).toHaveBeenCalledWith("2026");
  });
});
