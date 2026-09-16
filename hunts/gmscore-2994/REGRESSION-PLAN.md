# GmsCore #2994 — Regression & Verification Plan

> This plan is intentionally stricter than “tests pass”. Every claimed fix must connect a reproducer to a root cause and a finite acceptance condition.

## 1. Evidence ladder

A claim may advance only through the levels it actually satisfies:

1. **SOURCE OBSERVED** — relevant writer/reader/branch exists in code.
2. **CONTRACT PROVEN** — stock RE, public protocol/standard, or authoritative implementation defines expected behavior.
3. **BEFORE FAIL** — current frozen baseline fails a deterministic reproducer.
4. **PATCHED** — minimal integration changes the proven boundary.
5. **AFTER PASS** — same reproducer passes.
6. **SIDE-EFFECT CHECK** — adjacent existing regressions remain green.
7. **MODULE GATES** — focused unit + lint + assemble pass.
8. **REPO GATES** — full-repo result recorded and classified as introduced vs inherited.
9. **PHYSICAL E2E** — only where behavior depends on real SIM/carrier/locked-bootloader/Google backend.

Never collapse levels 2–9 into “fixed”.

---

## 2. Frozen baseline

`276523b1d3e136b5b6e4ba0cf3f505b6702d60e2`

All BEFORE assertions are against this exact baseline unless a later checkpoint explicitly re-freezes the branch.

---

## 3. Required regression cases

### R-001 — CompositeToken encoding

**Targets:** FD-RCS-001.

**Precondition:** exact `pia_token` presence/value semantics must first be proven.

**Fixture must prove:**
- protobuf field 1 is IID token;
- protobuf field 2 behavior exactly matches stock contract;
- Android-equivalent `URL_SAFE | NO_WRAP` Base64 behavior;
- padding preserved unless stock evidence proves `NO_PADDING`;
- round-trip protobuf decode equals fixture input.

**BEFORE:** Asterism boundary returns bare IID / cannot match expected composite fixture.  
**AFTER:** exact expected composite fixture returned.

### R-002 — Asterism consent resolution

**Targets:** FD-RCS-003.

Matrix:
1. matching Gaia consent + RCS consent;
2. no matching Gaia + RCS consent;
3. unrelated Gaia + RCS consent;
4. non-RCS client + RCS consent;
5. no consent payload;
6. RCS `CONSENT_UNKNOWN` after exact authority is frozen.

**Do not freeze expected rows 1/6 from contributor code alone.** Use stock/public authority.

### R-003 — no silent auto-consent

**Targets:** FD-RCS-002.

Harness should replace/spy the PDV RPC client.

**BEFORE:** NO_CONSENT path invokes SetConsent with `CONSENTED` and can continue toward Sync.  
**AFTER invariant:** NO_CONSENT does not self-grant; no Sync begins before legitimate consent.  
**Status/callback expectation:** freeze from proven stock/prior implementation before assertion.

### R-004 — legitimate consent transition

Start at NO_CONSENT. Exercise the actual Asterism SetConsent request path as Google Messages would. Prove effective server/client consent state changes and a subsequent verification request can cross the gate.

This is distinct from R-003: fail-closed without a working user-consent transition would merely replace one loop with another.

### R-005 — signing failure is explicit

Force an ECDSA initialization/signing failure.

**BEFORE:** helper yields zero-length signature.  
**AFTER:** explicit exception/failure; no request is serialized as authenticated with empty signature.

### R-006 — Gaia GoogleUserId recovery

Cases:
- `null` user data → recovery attempted;
- `""` → recovery attempted;
- existing non-empty ID → no unnecessary recovery.

### R-007 — Spatula request decoration

Cases:
- provider null → no `X-Goog-Spatula` header;
- provider blank → no header;
- provider value → exact header once;
- cache hit → provider/binder not repeated within TTL;
- expired cache → fetch repeated;
- provider/binder throws → request proceeds/fails according to frozen contract without fabricated header.

### R-008 — AppCert Android-ID fallback

Force the primary AppCert route into Android-ID fallback.

**BEFORE:** generated fallback path terminates with null.  
**AFTER:** generated header is returned.

### R-009 — TS.43 EAP identity parity

Retain/rebase keeltrace tests:
- default realm identity;
- challenge-provided alternate realm preserved;
- exact transmitted identity reaches AKA key derivation.

### R-010 — TS.43 terminal-id precedence

Retain/rebase keeltrace tests:
- explicit request terminal ID wins;
- empty request terminal ID falls back to device identifier.

### R-011 — TS.43 metadata defaults

Retain/rebase keeltrace tests for vendor/model/software fallback and existing truncation lengths. Explicit request values must still win.

### R-012 — multipart MT-SMS non-regression

Preserve HumbleDrummer tests:
- multipart body spanning PDUs assembles and matches;
- single-part behavior preserved;
- empty broadcast ignored;
- missing body part rejects partial message.

### R-013 — existing #3784 correctness regressions

Must remain green:
- IID empty-token fail-closed;
- EC key/public-key ACK ordering;
- phone-number resolution fallback;
- DroidGuard parity-focused tests present in integrated branch.

---

## 4. Radar items — no regression until contract/reachability is proven

### `next_sync_time`

Observed persisted, but effective consumer not found. Do not create a speculative cooldown test. First obtain stock RE/trace identifying its reader, predicate and transition.

### Android < 26 Asterism callback behavior

Early return exists. Do not assert a callback/error behavior until caller reachability and supported-version contract are proven.

---

## 5. Focused Gradle gate sequence

Use a failure-preserving shell:

```bash
set -euo pipefail

./gradlew :play-services-asterism-core:testDebugUnitTest
./gradlew :play-services-constellation-core:testDebugUnitTest

./gradlew :play-services-asterism-core:lintDebug
./gradlew :play-services-constellation-core:lintDebug

./gradlew :play-services-asterism-core:assembleDebug
./gradlew :play-services-constellation-core:assembleDebug

./gradlew :play-services-asterism-core:assembleRelease
./gradlew :play-services-constellation-core:assembleRelease

git diff --check
```

Add Release unit tests where module configuration exposes them and keep the exact command/result in the checkpoint log.

---

## 6. Full-repository gate policy

Full-repo CI/build failures must be triaged into exactly one of:

- **INTRODUCED BY FOUNDRAGON** — blocker;
- **INHERITED BASELINE** — record exact baseline run/shape; not evidence that the new patch is wrong;
- **ENVIRONMENT/DEPENDENCY** — reproduce or identify the missing dependency/tool constraint;
- **UNRESOLVED** — do not wave through.

Prior contributor reports about inherited lint/dependency failures are context, not a substitute for rerunning the frozen Foundragon branch.

---

## 7. Physical validation protocol

Only declare full RCS success after a consenting tester produces redacted evidence for the actual integrated commit.

Required environment:
- locked-bootloader microG ROM;
- no root/Magisk dependency for the success claim;
- active SIM;
- current Google Messages;
- second RCS endpoint.

Minimum flow:
1. clean/no-consent or freshly reset provisioning state;
2. Messages reaches consent UI/path naturally;
3. Asterism consent fetch/set path is observed;
4. verification starts only after consent;
5. provisioning completes;
6. RCS outbound message succeeds;
7. RCS inbound message succeeds;
8. restart/reconnect does not regress provisioning;
9. accountless path tested where practical;
10. TS.43-capable carrier path tested when a suitable SIM exists.

Privacy:
- redact phone numbers, IMSI/ICCID, tokens, account IDs and raw authentication payloads;
- never request or publish credentials;
- retain only evidence needed to prove state transitions.

---

## 8. Checkpoint result template

Use this exact shape for every integrated fix:

```text
Finding: FD-RCS-XXX
Baseline: <sha>
Origin/provenance: <author/pr/commit or Foundragon>
Contract authority: <source/RE/standard>
Before reproducer: <command/test> -> FAIL <exact reason>
Patch: <files/functions>
Foundragon delta: <difference from source contribution>
After reproducer: <same command/test> -> PASS
Adjacent regressions: <tests> -> PASS/FAIL
Module lint: PASS/FAIL
Module assemble: PASS/FAIL
Full repo: PASS / inherited failure / introduced failure
Physical E2E: VERIFIED / NOT YET PHYSICALLY VERIFIED
Residual risk: <bounded statement>
```

A final bounty/PR report should be generated from these checkpoint records rather than reconstructed from memory.
