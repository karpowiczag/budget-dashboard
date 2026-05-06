# Codex-Assisted GitFlow

This repo uses a solo GitFlow process with Codex-assisted implementation and review.

## One-Time Codex Cloud Setup

Repo files can prepare the workflow, but Codex Cloud/GitHub access still needs to be authorized by the GitHub account owner:

1. Open the Codex web experience from ChatGPT.
2. Connect GitHub and authorize the ChatGPT GitHub Connector for `karpowiczag/budget-dashboard`.
3. Enable Codex review for personal pull requests or for this repository.
4. Keep repository access limited to this private repo unless broader access is intentional.

Official OpenAI references:

- [Codex cloud](https://platform.openai.com/docs/codex)
- [Using Codex with your ChatGPT plan](https://help.openai.com/en/articles/11369540/)

## Daily Development Flow

Codex GitHub smoke tests should use a small docs-only PR first. A healthy setup should show the `chatgpt-codex-connector[bot]` reacting to `@codex` mentions and, when PR review is enabled in Codex settings, a Codex review on the pull request. To validate automatic review, open a fresh non-draft PR and wait before adding any manual `@codex review` comment.

1. Create or select an issue.

```powershell
gh issue create --title "..." --body "..." --label "type:feature,area:backend,priority:p2,codex"
```

2. Start from `develop`.

```powershell
git switch develop
git pull --ff-only origin develop
git switch -c feature/short-name
```

3. Implement the change and run the normal checks.

```powershell
npm run build
npm run sync:frontend
.\mvnw.cmd test
```

4. Push and open a PR into `develop`.

```powershell
git push -u origin feature/short-name
gh pr create --base develop --head feature/short-name --fill
```

5. Review the PR.

- Let Codex Cloud auto-review when the GitHub integration is enabled.
- For local Codex review, inspect `gh pr diff`, post review comments with `gh pr review --comment`, and fix issues on the same PR branch.
- If UI changed, check the app in the browser before merge.

6. Wait for CI.

```powershell
gh pr checks --watch
```

7. Squash merge and delete the branch.

```powershell
gh pr merge --squash --delete-branch
```

## Release Flow

Release only when `develop` is stable:

```powershell
git switch main
git pull --ff-only origin main
git merge --no-ff develop
git tag vYYYY.MM.DD-N
git push origin main develop --tags
```

Then run the manual Deploy workflow from `main`.

## Review Checklist

Every PR should be checked for:

- Correct totals for income, spend, excluded transfers, credit-card repayments, and refunds.
- No committed bank exports, generated reports, local DB files, or secrets.
- No private transaction details in code, docs, tests, issues, or PRs.
- Package boundary rules still passing.
- Spring Data JDBC remains the persistence approach.
- CI does not add unnecessary native/GraalVM work to default branches.
- Frontend changes preserve desktop dashboard usability.

## Branch Protection Recommendation

For solo development, keep branch rules pragmatic:

- Protect `develop` and `main`.
- Require the `test` CI check before merge. The default CI intentionally stays lightweight and does not run native/GraalVM compilation.
- Prefer pull requests for changes.
- Do not require one approval until a second human or separate bot identity is available; the same GitHub account cannot provide meaningful independent approval.
