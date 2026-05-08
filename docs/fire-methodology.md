# FIRE Tracking Methodology

This module is a planning model for a Polish household targeting work-optional status at age 50. It is not investment, tax, or legal advice.

## Inputs

- The FIRE spending target is a separate FIRE setting, because future work-optional spending can differ from the current household budget limit.
- The household budget contributes only the current investment pace: investments plus net savings-account movement. Loan overpayments are shown separately as debt reduction.
- Investment portfolio values come from local MyFund `portfelSklad` CSV exports in `fire/investments_reports`.
- Private MyFund files stay local and ignored by Git.
- All model returns are real returns, so projected values are interpreted in today's PLN purchasing power.

## Polish Rules Used In The Model

- Capital gains tax assumption: 19% for taxable capital gains outside retirement wrappers, including PIT-38 style investment income.
- IKE 2026 contribution limit: 28,260 PLN per person. Qualified IKE access is generally after age 60, or 55 with pension entitlement conditions.
- IKZE 2026 contribution limit: 11,304 PLN per person, or 16,956 PLN for people conducting non-agricultural business activity. Qualified IKZE withdrawal is after age 65 and at least 5 calendar years of contributions, taxed at a flat 10%.
- ZUS statutory retirement age is 60 for women and 65 for men, so FIRE at 50 requires a separate bridge-capital model before statutory pension access.
- PPK is modeled as a separate retirement wrapper when present in source data; exact payroll contribution optimization is outside this v1 module.

## FIRE Number

The default FIRE number is:

```text
annual spending target / safe withdrawal rate
```

The annual spending target comes from the FIRE monthly spending setting, not from the current budget target. The default safe withdrawal rate is 3.5%, intentionally below the classic 4% heuristic because this household targets a long horizon starting at age 50 and has PLN/tax/regulatory constraints. The value is configurable through `APP_FIRE_SAFE_WITHDRAWAL_RATE`.

## Bridge Capital

The module separates:

- liquid FIRE capital: taxable brokerage, savings, and cash-like assets;
- retirement-locked capital: IKE/IKZE/PPK-like long-term retirement wrappers;
- bridge needs from age 50 to 60 and age 50 to 65.

This prevents the app from treating all net worth as equally spendable at age 50.

## Rebalancing Policy

The default policy allocation is:

- 80% equities,
- 10% bonds,
- 5% cash,
- 5% alternatives.

The default rebalance band is 5 percentage points. The model prefers using new contributions to correct drift before selling taxable assets.

## Sources Checked

- IKE 2026 limit, Ministry family portal: https://www.gov.pl/web/rodzina/ike-limit-wplat
- IKZE 2026 limits, KNF: https://www.knf.gov.pl/?articleId=81022&p_id=18
- Capital gains / PIT-38 investment income, podatki.gov.pl: https://www.podatki.gov.pl/podatki-osobiste/pit/informacje-podstawowe/co-jest-opodatkowane/zbycie-akcji/
- IKE withdrawal conditions, Gov.pl: https://www.gov.pl/web/rodzina/co-trzeba-wiedziec-o-ike
- IKZE withdrawal and 10% tax, Gov.pl: https://www.gov.pl/web/rodzina/roczny-limit-wplat-na-ikze
- ZUS statutory retirement age: https://www.zus.pl/swiadczenia/emerytury/emerytura-dla-osob-urodzonych-po-31-grudnia-1948/emerytura-w-wieku-powszechnym
- PPK contribution rules, official PPK portal: https://www.mojeppk.pl/faq/pracownik/wplaty-do-ppk_ile-wynosi-wplata-pracodawcy-a-ile-pracownika.html
- Rebalancing concepts, Vanguard: https://investor.vanguard.com/investor-resources-education/portfolio-management/rebalancing-your-portfolio
- Rebalancing bands, Bogleheads: https://www.bogleheads.org/wiki/Rebalancing
- Safe withdrawal research reference, Morningstar 2025/2026 retirement income research: https://www.morningstar.com/retirement/retirees-heres-what-your-withdrawal-rate-should-be-2025
