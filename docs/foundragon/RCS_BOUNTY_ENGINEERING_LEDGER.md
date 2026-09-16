# Foundragon V5 — RCS / Constellation Bounty Engineering Ledger

Status: ACTIVE ENGINEERING CHECKPOINT  
Branch: `foundragon/rcs-bounty-pack-v5`  
Baseline: `276523b1d3e136b5b6e4ba0cf3f505b6702d60e2`  
Baseline provenance: naormeit/GmsCore `rcs-bounty-2994`, including HumbleDrummer multipart MT-SMS merge.

## 1. Mission

This branch is a provenance-preserving hardening and completion branch for the RCS / Constellation / Asterism work around microg/GmsCore#2994 and #3784.

The engineering order is fixed:

1. Close defects already documented in issue / PR history.
2. Review other contributors' commits and PRs mechanically.
3. Keep working parts with explicit attribution.
4. Repair only the broken / incomplete boundary; do not erase contributor provenance.
5. Add BEFORE/AFTER regression coverage for every behavior change that can be isolated.
6. After documented defects are closed, search for additional defects in the same state machine and protocol surface.
7. Only then assemble a single bounty-ready package with a complete evidence story.

This branch must not claim physical locked-bootloader RCS success unless such validation is actually performed and recorded.

## 2. Engineering rules

- **Attribution is part of correctness.** If a contributor supplied a working algorithm, fix, test, or discovery, retain their authorship in commit metadata / documentation and identify what was reused.
- **No blind cherry-picks.** A patch may be stale while one of its ideas is correct. Rebase behavior, not accidental branch history.
- **No speculative protocol fields.** Unknown wire semantics remain OPEN until supported by RE, protocol behavior, or reproducible runtime evidence.
- **No workaround promotion.** A workaround that bypasses a user-consent, authentication, or state-machine boundary must not become the final implementation merely because it makes the flow proceed.
- **Regression before closure.** A code change is not CLOSED until its mechanical behavior is covered or the absence of a practical test is explicitly documented.
- **Inherited failures stay inherited.** Existing full-repo CI failures are not silently reclassified as regressions introduced by this branch.

## 3. Current disposition ledger

| ID | Area | Current finding | Disposition | Evidence / provenance | Required closure |
|---|---|---|---|---|---|
| FD-RCS-001 | Asterism token contract | Current Asterism path exposes a bare IID token while public RE for Google Messages / Samsung RCS describes `gmscore_instance_id_token` as URL-safe Base64 of serialized `CompositeToken`. | **PRODUCT DEFECT — PROVEN CONTRACT MISMATCH** | microg/GmsCore#2994 public RE; current `GetIidToken` / Asterism path | Implement exact CompositeToken boundary only after field-presence semantics are closed; add deterministic encoding regression. |
| FD-RCS-002 | CompositeToken | Known fields: `iid_token = 1`, `pia_token = 2`. Exact `pia_token` absent/empty/populated semantics are not yet proven. | **OPEN — DO NOT GUESS** | Public RE proves field numbers / outer encoding, not current PIA presence rule | Find stock/runtime authority; keep implementation blocked on this sub-boundary if necessary. |
| FD-RCS-003 | Consent state machine | Full verification currently auto-writes `CONSENTED` through `SetConsent` when consent is absent, then continues even if SetConsent fails. | **PRODUCT DEFECT — PROVEN** | Current `VerifyPhoneNumber.kt`; historical stock-parity gate existed before auto-consent workaround | Restore fail-closed verification gate after consent retrieval is repaired; regression must prove verification does not self-authorize. |
| FD-RCS-004 | Asterism consent mapping | Current Asterism handler ignores top-level `rcs_consent` when no matching Gaia consent exists. | **PRODUCT DEFECT — PROVEN BY CODE/PROTO** | `GetConsentResponse` has `rcs_consent`; current handler uses Gaia-only mapping | Adopt tested resolver behavior; independently preserve uncertainty about exact precedence if stock authority is incomplete. |
| FD-RCS-005 | Consent resolver implementation | KeelTrace PR #6 adds `resolveAsterismConsent`: matching Gaia entry first, RCS top-level fallback for RCS client, NO_CONSENT otherwise. | **TAKE + CREDIT KEELTRACE, WITH PRECEDENCE REVIEW** | naormeit/GmsCore#6, head `3aacca36...` | Port behavior to current branch; keep focused JUnit tests; separately document precedence authority level. |
| FD-RCS-006 | Asterism test source set | Current baseline lost focused Asterism unit-test source-set wiring. | **EVIDENCE PACKAGING DEFECT** | KeelTrace PR #6 restores `src/test/kotlin` + JUnit dependency | Restore test source set before relying on Asterism regressions. |
| FD-RCS-007 | Constellation signing | Signing helper catches all exceptions and returns an empty signature, collapsing failure into malformed-but-apparently-successful client auth. | **PRODUCT DEFECT — PROVEN BY CODE** | KeelTrace PR #6 identified and changes to fail-closed exception | Port fail-closed behavior and add focused regression / injectable failure seam if practical. |
| FD-RCS-008 | Gaia ID recovery | Gaia signal builder only recovers when ID is exactly `""`; `null` skips recovery. | **PRODUCT DEFECT — PROVEN BY CODE** | KeelTrace PR #6 changes condition to `isNullOrEmpty()` | Port fix and add null + empty regression coverage. |
| FD-RCS-009 | Multipart MT-SMS | Multipart PDU parts must be assembled into one logical message before challenge matching. | **CLOSED WITH TEST — KEEP + CREDIT HUMBLEDRUMMER** | Baseline merge `276523b1...`, HumbleDrummer | Preserve unchanged unless later evidence proves a defect. |
| FD-RCS-010 | E.164 request number fallback | Request builder uses formatted E.164, then phone-number hint, then raw SIM number, then empty string. | **CLOSED WITH TEST — KEEP + CREDIT EXISTING CONTRIBUTOR** | Current baseline focused tests | Preserve. |
| FD-RCS-011 | Spatula header | Current Constellation RPC client does not provide `X-Goog-Spatula`; Julius Hill follow-up adds AppCert-backed provider for Constellation and Asterism. | **TAKE + CREDIT JULIUS HILL; REBASE REQUIRED** | Contributor follow-up is older than current baseline | Reapply behavior onto current head, not stale branch history; test initialization / header injection boundaries. |
| FD-RCS-012 | AppCert fallback | Existing AppCert path constructs Android-ID fallback state but terminates with `return null`. | **PRODUCT DEFECT — PROVEN BY CODE** | Identified while reviewing Julius Hill Spatula work | Repair with focused unit coverage if service boundary can be isolated. |
| FD-RCS-013 | TS.43 EAP identity | Key derivation must use the identity actually used in the EAP-AKA exchange. | **TAKE + CREDIT KEELTRACE** | Contributor patch + RFC 4187 behavior | Port and regression-test identity/K_aut coupling. Physical carrier validation remains separate. |
| FD-RCS-014 | TS.43 terminal ID | Explicit terminal ID must take precedence over device IMEI fallback. | **TAKE + CREDIT KEELTRACE** | Contributor patch + Android Service Entitlement reference behavior | Port + focused request-builder test. |
| FD-RCS-015 | TS.43 terminal metadata | Missing vendor/model/software request values require device fallbacks; executable AOSP implementation uses `Build.VERSION.RELEASE` for software version. | **TAKE + CREDIT KEELTRACE** | Contributor patch + Android Service Entitlement implementation | Port + focused builder tests. |
| FD-RCS-016 | `next_sync_time` | Sync/Proceed responses persist `next_sync_time`, but no effective reader/predicate has yet been established on the current path. | **UNRESOLVED — SEMANTICS NOT PROVEN** | Current state store + call-chain review | Do not invent a cooldown consumer. Find stock/runtime authority first. |
| FD-RCS-017 | `retry_after_time` | Unverified/throttled server response is mapped to client `retryAfterSeconds`. | **CLOSED / KEEP** | Current `VerificationMappings.kt` | Preserve. |

## 4. Contributor provenance map

### HumbleDrummer
Retained contribution:
- Multipart MT-SMS PDU assembly before pending-challenge matching.
- Regression tests for multipart, single-part, empty broadcast, and missing segment.

Why retained:
- Behavior is mechanically correct for the represented broadcast model.
- It is present in the selected baseline and already has focused coverage.

### KeelTrace
Candidate contributions to retain, with credit:
- RCS consent fallback resolver and focused precedence/fallback tests.
- Asterism test source-set restoration.
- Fail-closed Constellation signing.
- `null` + empty Gaia ID recovery.
- TS.43 identity / terminal metadata hardening from earlier follow-up work.

What Foundragon changes around this work:
- Adds explicit regression requirements for signing and Gaia recovery instead of treating code review alone as closure.
- Separates "resolver direction is correct" from "exact Gaia-vs-RCS precedence is externally proven".
- Integrates onto the current baseline rather than treating an older branch as the release branch.

### Julius Hill
Candidate contribution to retain, with credit:
- AppCert-backed `X-Goog-Spatula` provider usable by both Asterism and Constellation.
- Architectural use of existing `IAppCertService` boundary instead of introducing an unnecessary direct module dependency.

What Foundragon changes around this work:
- Rebase behavior onto current baseline.
- Independently repair / test the AppCert Android-ID fallback path discovered during review.

### Existing #3784 / #2994 contributors
Retain all mechanically verified working behavior already present in the chosen baseline, including E.164 fallback, current request/response mappings, DroidGuard/Tachyon hardening, and public-key/ACK state coherence, unless a later regression proves otherwise.

## 5. Required regression matrix

| Regression ID | BEFORE | AFTER |
|---|---|---|
| REG-CONSENT-001 | RCS top-level consent with no matching Gaia entry resolves to NO_CONSENT | Resolves to RCS consent for RCS client |
| REG-CONSENT-002 | No consent causes verification path to self-call SetConsent and continue | Verification does not self-authorize; flow fails/stops according to stock-parity gate |
| REG-CONSENT-003 | Failed SetConsent during workaround can still be followed by Sync | No Sync is reachable merely because self-consent failed |
| REG-SIGN-001 | Forced signing failure returns zero-length signature | Forced signing failure is explicit and cannot be serialized as successful auth |
| REG-GAIA-001 | `GoogleUserId == null` skips recovery | `null` enters same recovery path as empty ID |
| REG-GAIA-002 | `GoogleUserId == ""` recovery remains supported | Behavior preserved |
| REG-TOKEN-001 | Bare IID returned where CompositeToken wire contract is expected | Exact serialized CompositeToken encoded with Android-compatible URL-safe Base64 flags |
| REG-TS43-001 | Alternate identity can diverge from identity used for K_aut | Same effective EAP identity feeds exchange and key derivation |
| REG-SPATULA-001 | Constellation request has no Spatula header provider | Header is injected when AppCert service provides it; failure semantics are explicit |

## 6. Open science / protocol frontiers

1. Exact `pia_token` presence semantics in CompositeToken.
2. Stock authority for Gaia-vs-top-level RCS consent precedence when both exist and disagree.
3. Exact operational meaning / consumer of `next_sync_time` in stock state scheduling.
4. First-connect predicate and transition semantics beyond the immediate Sync/Proceed call graph.
5. Physical carrier / SIM / locked-bootloader validation after software gates are green.

These are not license to add speculative behavior. Each remains OPEN until evidence closes it.

## 7. Commit protocol for this branch

Every Foundragon engineering commit should answer, in its message or linked ledger update:

- Which `FD-RCS-*` item is affected?
- Is the change original Foundragon work or derived from another contributor?
- If derived, who supplied the retained behavior and what exactly was kept?
- What was changed relative to that contributor and why?
- Which BEFORE behavior fails?
- Which AFTER test proves the intended boundary?
- What remains unverified?

Suggested sequence:

1. `docs(foundragon): checkpoint RCS bounty engineering ledger`
2. `test(asterism): restore focused consent regression lane`
3. `fix(asterism): preserve RCS consent fallback` — credit KeelTrace
4. `test(constellation): cover signing and Gaia recovery failures`
5. `fix(constellation): fail closed on signing and recover missing Gaia IDs` — credit KeelTrace
6. `fix(consent): restore fail-closed verification gate` — preserve historical contributor provenance, remove only the workaround behavior
7. `fix(asterism): implement proven CompositeToken boundary` — only after `pia_token` semantics closure
8. `fix(ts43): integrate proven request/auth hardening` — credit KeelTrace
9. `fix(rpc): restore X-Goog-Spatula AppCert bridge` — credit Julius Hill
10. `test(foundragon): run focused + changed-module regression matrix`
11. Final evidence / bounty packaging commit.

## 8. Current validation statement

At this checkpoint:

- The branch base is fixed and reproducible.
- Several source-level defects are proven.
- Contributor provenance is mapped.
- No claim is made that all listed fixes are implemented yet.
- No claim is made of physical RCS end-to-end success.
- `pia_token`, exact mixed-consent precedence, and `next_sync_time` semantics remain explicitly open.

This ledger is the recovery point if interactive analysis is interrupted. Future commits must update it when an item's disposition changes.
