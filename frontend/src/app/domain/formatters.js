const PLN = new Intl.NumberFormat("pl-PL", {
  style: "currency",
  currency: "PLN",
  maximumFractionDigits: 0,
});

const PLN_DEC = new Intl.NumberFormat("pl-PL", {
  style: "currency",
  currency: "PLN",
  maximumFractionDigits: 2,
});

const PCT = new Intl.NumberFormat("pl-PL", {
  style: "percent",
  maximumFractionDigits: 1,
});

export function money(value) {
  return PLN.format(Number(value || 0));
}

export function moneyDec(value) {
  return PLN_DEC.format(Number(value || 0));
}

export function percent(value) {
  return PCT.format(Number(value || 0));
}
