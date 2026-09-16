# Foundragon — GmsCore #2994 Hunt Index

This directory is the durable engineering state for the Foundragon review of microg/GmsCore#2994 / PR #3784.

## Read in this order

1. **`GMSCORE-2994-CLOSURE-PACK.md`** — full engineering narrative, proven defects, kept fixes, open boundaries, current frontier.
2. **`PROVENANCE-MATRIX.md`** — who contributed what, what Foundragon will keep/take/modify, and why.
3. **`REGRESSION-PLAN.md`** — BEFORE_FAIL → AFTER_PASS tests, build gates, hardware validation, checkpoint result format.

## Frozen starting point

- Repository: `Chess-Debug/GmsCore`
- Branch: `hunter/gmscore-2994-frontier`
- Base: `276523b1d3e136b5b6e4ba0cf3f505b6702d60e2`
- Upstream candidate: `naormeit/GmsCore:rcs-bounty-2994`
- Upstream PR: `microg/GmsCore#3784`
- Issue: `microg/GmsCore#2994`

## Current frontier

1. Prove exact `CompositeToken.pia_token` presence/value semantics.
2. Freeze CompositeToken regression and implement the Asterism boundary fix.
3. Freeze Gaia-vs-RCS consent precedence / unknown-consent semantics.
4. Integrate consent resolver and restore Asterism test execution.
5. Remove silent auto-consent only after the legitimate consent transition is proven working.
6. Integrate the attributable Spatula, TS.43, signing and Gaia-ID hardening slices.
7. Run deterministic gates, then continue the new-bug sweep.
8. Physical RCS / TS.43 validation remains required for full completion claims.

## Recovery rule

Do not restart analysis from zero after an interruption. Resume from the closure pack's **Current frontier**. Do not rewrite `CLOSED/TESTED` code merely for ownership aesthetics. If another contributor's correct code is retained, preserve explicit attribution.

## Claim discipline

At this checkpoint:

- no new Foundragon code fix is claimed merged;
- deterministic/source/protocol findings are separated from physical-device claims;
- full RCS success is **NOT YET PHYSICALLY VERIFIED**;
- unresolved semantics are intentionally recorded instead of guessed.
