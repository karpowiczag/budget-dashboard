# Solo GitFlow

This repository uses a small GitFlow variant to keep production stable without wasting CI minutes.

## Branches

- `main`: production-ready code only. Manual deploys run from this branch.
- `develop`: daily integration branch. Normal work lands here first.
- `feature/<name>`: optional branch for risky or multi-commit work.
- `release/<name>`: optional branch when preparing a larger release.
- `hotfix/<name>`: urgent fix started from `main`.

## Daily Work

```powershell
git switch develop
git pull --ff-only
git switch -c feature/category-rule
```

After the change is done:

```powershell
git switch develop
git merge --no-ff feature/category-rule
git branch -d feature/category-rule
git push origin develop
```

## Release

```powershell
git switch develop
git pull --ff-only
git switch main
git pull --ff-only
git merge --no-ff develop
git tag v2026.05.06-1
git push origin main develop --tags
```

Then run the `Deploy` workflow manually from `main`.

## Hotfix

```powershell
git switch main
git pull --ff-only
git switch -c hotfix/oauth-config
```

After the fix:

```powershell
git switch main
git merge --no-ff hotfix/oauth-config
git switch develop
git merge --no-ff hotfix/oauth-config
git push origin main develop
```

Run manual deploy from `main` if the hotfix must go live immediately.
