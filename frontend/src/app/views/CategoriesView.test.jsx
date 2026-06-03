import { cleanup, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { CategoriesView } from "./CategoriesView.jsx";

function catalog() {
  return {
    groups: [
      { groupId: "obligatoryVariable", label: "Obowiązkowe zmienne", sortOrder: 2 },
      { groupId: "discretionary", label: "Nieobowiązkowe", sortOrder: 3 },
    ],
    categories: [
      {
        categoryId: "groceries", label: "Żywność i chemia", area: "Koszty codzienne", analyticsGroup: "Potrzeby podstawowe",
        groupId: "obligatoryVariable", budgetBucket: "Obowiązkowe zmienne", fixedness: "Zmienne konieczne", flowType: "livingExpense",
        discretionary: false, excluded: false, realIncome: false, dailyPaced: true, protectedFlag: false, sinkingFundEligible: false,
        archived: false, sortOrder: 18,
      },
      {
        categoryId: "diningOut", label: "Jedzenie poza domem", area: "Styl życia", analyticsGroup: "Styl życia",
        groupId: "discretionary", budgetBucket: "Nieobowiązkowe", fixedness: "Uznaniowe", flowType: "livingExpense",
        discretionary: true, excluded: false, realIncome: false, dailyPaced: true, protectedFlag: false, sinkingFundEligible: false,
        archived: false, sortOrder: 19,
      },
    ],
  };
}

afterEach(() => cleanup());

describe("CategoriesView", () => {
  it("renders groups with their categories", () => {
    render(<CategoriesView catalog={catalog()} onSaveCategory={vi.fn()} onSaveGroup={vi.fn()} />);
    expect(screen.getByLabelText("Nazwa grupy Obowiązkowe zmienne")).toBeInTheDocument();
    expect(screen.getByLabelText("Nazwa kategorii Żywność i chemia")).toBeInTheDocument();
  });

  it("saves a bucket edit by category id", async () => {
    const onSaveCategory = vi.fn();
    render(<CategoriesView catalog={catalog()} onSaveCategory={onSaveCategory} onSaveGroup={vi.fn()} />);

    await userEvent.selectOptions(screen.getByLabelText("Koszyk kategorii Żywność i chemia"), "Nieobowiązkowe");
    await userEvent.click(screen.getByLabelText("Zapisz kategorię Żywność i chemia"));

    expect(onSaveCategory).toHaveBeenCalledWith("groceries", expect.objectContaining({ budgetBucket: "Nieobowiązkowe" }));
  });

  it("hides a category by sending archived:true", async () => {
    const onSaveCategory = vi.fn();
    render(<CategoriesView catalog={catalog()} onSaveCategory={onSaveCategory} onSaveGroup={vi.fn()} />);

    await userEvent.click(screen.getByLabelText("Ukryj Żywność i chemia"));

    expect(onSaveCategory).toHaveBeenCalledWith("groceries", expect.objectContaining({ archived: true }));
  });

  it("renames a group via onSaveGroup", async () => {
    const onSaveGroup = vi.fn();
    render(<CategoriesView catalog={catalog()} onSaveCategory={vi.fn()} onSaveGroup={onSaveGroup} />);

    const input = screen.getByLabelText("Nazwa grupy Obowiązkowe zmienne");
    await userEvent.clear(input);
    await userEvent.type(input, "Konieczne zmienne");
    await userEvent.click(screen.getByLabelText("Zapisz grupę Obowiązkowe zmienne"));

    expect(onSaveGroup).toHaveBeenCalledWith("obligatoryVariable", expect.objectContaining({ label: "Konieczne zmienne" }));
  });

  it("adds a new category with a slug id", async () => {
    const onSaveCategory = vi.fn();
    render(<CategoriesView catalog={catalog()} onSaveCategory={onSaveCategory} onSaveGroup={vi.fn()} />);

    await userEvent.type(screen.getByLabelText("Nazwa nowej kategorii"), "Subskrypcje");
    await userEvent.click(screen.getByText("Dodaj kategorię"));

    expect(onSaveCategory).toHaveBeenCalledWith("subskrypcje", expect.objectContaining({ label: "Subskrypcje" }));
  });

  it("creates a classification rule", async () => {
    const onSaveRule = vi.fn();
    render(<CategoriesView catalog={catalog()} onSaveCategory={vi.fn()} onSaveGroup={vi.fn()} onSaveRule={onSaveRule} onDeleteRule={vi.fn()} />);

    await userEvent.type(screen.getByLabelText("Wzorzec nowej reguły"), "BIEDRONKA");
    await userEvent.selectOptions(screen.getByLabelText("Kategoria nowej reguły"), "groceries");
    await userEvent.click(screen.getByText("Dodaj regułę"));

    expect(onSaveRule).toHaveBeenCalledWith("new", expect.objectContaining({ pattern: "BIEDRONKA", categoryId: "groceries" }));
  });

  it("edits an existing rule by id", async () => {
    const onSaveRule = vi.fn();
    const withRule = {
      ...catalog(),
      rules: [{ ruleId: "5", matchType: "title", pattern: "OLDPATTERN", categoryId: "groceries", priority: 100, enabled: true, source: "user" }],
    };
    render(<CategoriesView catalog={withRule} onSaveCategory={vi.fn()} onSaveGroup={vi.fn()} onSaveRule={onSaveRule} onDeleteRule={vi.fn()} />);

    await userEvent.click(screen.getByLabelText("Zapisz regułę 5"));

    expect(onSaveRule).toHaveBeenCalledWith("5", expect.objectContaining({ pattern: "OLDPATTERN", categoryId: "groceries" }));
  });
});
