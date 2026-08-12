---
name: git-delivery
description: Safely deliver completed repository work through review, verification, intentional staging, commit, push, and remote SHA verification. Use for commit, push, 提交, 發布 Git, 任務完成推上 Git, or integration delivery requests.
---

# Git Delivery

Follow this order. Treat Git CLI state as the local source of truth; use GitHub MCP only as an optional remote helper, never as a delivery dependency.

1. Confirm the task is complete and identify the designated Git Integrator. In multi-session work, enforce one writer per architectural area and defer shared-worktree commit/push until every parallel worker has finished.
2. Run `git status --short`; inspect `git diff --stat` and `git diff`. Classify every path as intended, pre-existing, generated, sensitive, or unintended.
3. Run task-appropriate tests/builds. Reconcile canonical docs, ADRs, and debt with the actual implementation state.
4. Scan intended changes for credentials. Classify findings as `REAL_SECRET`, `PUBLIC_UPSTREAM_DEV_DEFAULT`, `PLACEHOLDER`, `EXAMPLE`, or `UNKNOWN_CREDENTIAL_LIKE_VALUE`. Require `REAL_SECRETS_STAGED = 0`; review unknown values before proceeding.
5. Scan for oversized or generated files such as database dumps, logs, binaries, bundles, `node_modules/`, `target/`, `dist/`, `build/`, runtime data, IDE files, caches, local credentials, and temporary backups. Do not reject a binary solely by type; allow required SDK/JAR files and source assets when their purpose is verified.
6. Stage only explicit intended paths. Never use blind `git add .`.
7. Inspect `git diff --cached --stat` and `git diff --cached`; verify staged content matches the tested work and contains no secrets or generated artifacts.
8. Commit with a message that states the true scope and completion level. Use an appropriate simple prefix such as `feat:`, `fix:`, `docs:`, `chore:`, or `test:`; never claim unfinished capabilities and never use `--no-verify`.
9. Before push, verify branch and remote. Compare repository identity independently from transport: SSH and HTTPS URLs for the same owner/repository are the same identity. Stop only for an unauthorized repository identity conflict. Never replace a remote without explicit authorization, perform a hidden reset/rebase, rewrite history, or force push.
10. Push normally, then compare local `HEAD` with the intended remote branch SHA. Report the commit, push result, SHA match, and final worktree state.

If tests fail, a real secret is staged, an unknown credential-like value remains unresolved, the remote identity conflicts with the authorized target, parallel writers remain active, or unrelated changes cannot be isolated, do not commit/push. Preserve completed local work and report the precise delivery blocker.

Never use force push, `--no-verify`, blind staging, hidden reset/rebase, or remote replacement without user authority. Never commit secrets or generated/runtime files that are not intentional source artifacts.
