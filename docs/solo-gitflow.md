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
git push -u origin feature/category-rule
gh pr create --base develop --head feature/category-rule --fill
gh pr checks --watch
gh pr merge --squash --delete-branch
```

## Release

```powershell
gh pr create --base main --head develop --title "Release v2026.05.06-1" --body "Release from develop."
gh pr checks --watch
gh pr merge --merge
git switch main
git pull --ff-only
git tag v2026.05.06-1
git push origin v2026.05.06-1
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
git push -u origin hotfix/oauth-config
gh pr create --base main --head hotfix/oauth-config --fill
gh pr checks --watch
gh pr merge --squash --delete-branch
git switch -c fix/backport-oauth-config origin/develop
git cherry-pick <hotfix-merge-commit>
git push -u origin fix/backport-oauth-config
gh pr create --base develop --head fix/backport-oauth-config --fill
gh pr checks --watch
gh pr merge --squash --delete-branch
```

Run manual deploy from `main` if the hotfix must go live immediately.
