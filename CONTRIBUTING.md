# Branching Strategy

## Branch Naming
- `main` — production-ready code only
- `develop` — integration branch for features
- `feature/<week>-<description>` — new features (e.g., `feature/w1-order-service`)
- `fix/<description>` — bug fixes
- `chore/<description>` — setup, config, tooling

## Workflow
1. Branch off `develop` for new features
2. Commit often with conventional commits (see below)
3. Open PR → `develop` when done
4. `develop` → `main` at end of each week milestone

## Commit Message Format (Conventional Commits)