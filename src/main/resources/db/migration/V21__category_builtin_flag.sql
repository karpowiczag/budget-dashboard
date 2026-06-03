-- Phase 3d: distinguish built-in (taxonomy-seeded) categories from user-created ones. Built-in
-- categories are archive-only — they are reproduced by hardcoded matchers/analysis on every rebuild,
-- so hard-deleting one would make classification throw. User categories (builtin = false) can be
-- hard-deleted/merged with reassignment. Backfill TRUE for the seeded taxonomy ids (public slugs only,
-- no private data); fresh boots set the flag in the seeder.
ALTER TABLE category ADD COLUMN builtin BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE category SET builtin = TRUE WHERE category_id IN (
    'salary', 'refundCorrection', 'taxRefund', 'interest', 'ownTransfers', 'cardRepayment',
    'cashToSettle', 'cashDeposit', 'loanInstallment', 'installmentDebt', 'loanOverpayment',
    'investments', 'savingsAccount', 'rent', 'electricity', 'telecom', 'insurance', 'taxes',
    'publicFees', 'bankFees', 'groceries', 'medicalPharmacy', 'pets', 'transportParking', 'fuelCar',
    'diningOut', 'beautyCosmetics', 'clothing', 'events', 'media', 'sportHobby', 'giftsSupport',
    'electronics', 'travel', 'travelShopping', 'homeGoods', 'renovationGarden', 'marketplaceOnline',
    'personalOther', 'unknownReview'
);
