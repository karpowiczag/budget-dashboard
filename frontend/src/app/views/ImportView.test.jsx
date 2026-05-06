import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { ImportView } from "./ImportView.jsx";

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
});
