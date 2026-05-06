# Codex-Assisted GitFlow

This repo uses a solo GitFlow process with Codex-assisted implementation and review.

## One-Time Codex Cloud Setup

Repo files can prepare the workflow, but Codex Cloud/GitHub access still needs to be authorized by the GitHub account owner:

1. Open the Codex web experience from ChatGPT.
2. Connect GitHub and authorize the ChatGPT GitHub Connector for `karpowiczag/budget-dashboard`.
3. Enable Codex review for personal pull requests or for this repository.
4. Keep Codex access limited to this repository unless broader access is intentional.

Official OpenAI references:

- [Codex code review in GitHub](https://developers.openai.com/codex/integrations/github)
- [Using Codex with your ChatGPT plan](https://help.openai.com/en/articles/11369540/)

Official GitHub references:

- [About protected branches](https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-protected-branches/about-protected-branches)
- [GitHub conversation resolution changelog](https://github.blog/changelog/2021-06-16-new-tools-to-discover-and-resolve-pull-request-conversations-now-generally-available/)

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
npm --prefix frontend run build
npm --prefix frontend run sync:backend
.\mvnw.cmd test
```

4. Push and open a PR into `develop`.

```powershell
git push -u origin feature/short-name
gh pr create --base develop --head feature/short-name --fill
```

5. Review the PR.

- Let Codex Cloud auto-review when the GitHub integration is enabled.
- Do not rely on a sleep or timeout. Codex GitHub review readiness is signaled by a Codex review on the current head commit or, when there are no findings, by a `chatgpt-codex-connector[bot]` reaction to the current `@codex review` request.
- For local Codex review, inspect `gh pr diff`, post review comments with `gh pr review --comment`, and fix issues on the same PR branch.
- If UI changed, check the app in the browser before merge.

6. Wait for CI.

```powershell
gh pr checks --watch
```

7. Let GitHub branch protection enforce the merge gate.

The native gate must pass before merge. It checks:

- The branch is up to date with the base branch.
- The required `test` status check is passing.
- All GitHub review conversations are resolved.
- Stale reviews are dismissed after new pushes.
- Admins are included in enforcement.

8. Squash merge and delete the branch.

```powershell
gh pr merge --squash --delete-branch
```

## Release Flow

Release only when `develop` is stable:

```powershell
gh pr create --base main --head develop --title "Release vYYYY.MM.DD-N" --body "Release from develop."
gh pr checks --watch
gh pr merge --merge
git switch main
git pull --ff-only origin main
git tag vYYYY.MM.DD-N
git push origin vYYYY.MM.DD-N
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

Current native branch protection for this public repo:

- Protect `develop` and `main`.
- Require the `test` CI check before merge. The default CI intentionally stays lightweight and does not run native/GraalVM compilation.
- Require conversation resolution before merging.
- Require pull requests for changes.
- Dismiss stale reviews after new pushes.
- Require branches to be up to date before merge.
- Include admins in enforcement.
- Block force pushes and branch deletion.
- Do not require one approval until a second human or separate bot identity is available; the same GitHub account cannot provide meaningful independent approval.
