# Source Provenance

Jee8pay is a flattened root monorepo. The former nested Git metadata is not part of root history. Its repository、branch and baseline values were recovered read-only earlier from `/tmp/jee8pay-nested-git.LJbTpL/`; the backup directory is unavailable at the 2026-08-12 final check, so this document preserves the recovered provenance without claiming the temporary metadata still exists.

## Backend source

- Repository: `https://github.com/jeequan/jeepay.git`
- Baseline SHA: `ba37111934c9c04183cc6cbdbdafc7f38941fa4b`
- Baseline branch: `master`

## Frontend source

- Repository: `https://github.com/jeequan/jeepay-ui.git`
- Baseline SHA: `17d057db22ae45babc15bd7cb43e5f73756dd080`
- Baseline branch: `main`

Do not recreate `jeepay/.git` or `jeepay-ui/.git`、convert them to submodules automatically、or commit any future `/tmp` metadata backup.
