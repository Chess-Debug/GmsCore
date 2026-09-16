# Foundragon Engineering Closure Pack — microG/GmsCore #2994

> **Status:** DRAFT / ACTIVE FORENSICS  
> **Owner:** Foundragon / Chess-Debug  
> **Issue:** microg/GmsCore#2994 — RCS support / provisioning  
> **Primary upstream PR:** microg/GmsCore#3784  
> **Working branch:** `hunter/gmscore-2994-frontier`  
> **Frozen baseline:** `276523b1d3e136b5b6e4ba0cf3f505b6702d60e2`  
> **Baseline meaning:** current `naormeit/GmsCore:rcs-bounty-2994` head at the start of this Foundragon pass; includes HumbleDrummer multipart MT-SMS merge.  
> **Physical RCS E2E:** **NOT YET PHYSICALLY VERIFIED**.

This file is the durable recovery and review record for the Foundragon pass. It is deliberately more explicit than a normal PR description. A reviewer should be able to answer, without asking the authors:

1. What was broken or incomplete?
2. Who originally implemented each retained part?
3. Which outside follow-up was adopted, partially adopted, rejected, or superseded?
4. What did Foundragon change and why?
5. Which regression proves the behavior?
6. Which claims are only source/protocol proofs and which were exercised on real hardware?

---

## 0. Operating rule

The work order is fixed:

1. **Written / already reported defects first.**
2. **Review other contributors' commits and PRs.** Keep working pieces with explicit provenance; repair only the broken/incomplete boundaries.
3. **Then hunt additional defects** in the same runtime paths.
4. Deliver one coherent package rather than parallel competing rewrites.

A contributor patch is not accepted merely because it compiles or has tests. Adoption requires a mechanical reason, source/protocol authority where applicable, regression coverage, and side-effect review.

No code is promoted from this ledger to `FIXED` until its acceptance condition is finite and reproducible.

---

## 1. Status vocabulary

| Status | Meaning |
|---|---|
| `CLOSED/TESTED` | Existing implementation has focused regression evidence and no current counter-evidence. Preserve it. |
| `PROVEN DISPOSITION` | Source/protocol review is sufficient to decide KEEP/TAKE/REJECT, but final integrated branch proof may still be pending. |
| `PRODUCT_DEFECT` | Current baseline behavior is mechanically inconsistent with a proven contract/invariant. |
| `EVIDENCE_GAP` | Proposed direction is plausible but the exact contract is not yet proven. Do not invent the missing rule. |
| `UNRESOLVED` | Observed behavior exists, but its product meaning is not proven enough to patch. |
| `NOT YET PHYSICALLY VERIFIED` | Unit/source/protocol evidence exists, but locked-bootloader / real-SIM RCS hardware validation has not been performed. |

Regression convention: **BEFORE_FAIL → AFTER_PASS**.  
Shell/Gradle gates must preserve failure status (`set -euo pipefail` or equivalent).

---

## 2. Baseline lineage that must retain credit

The current #3784 branch is a consolidation, not a single-author implementation. Preserve upstream authorship in final PR notes and commit trailers where code is reused.

### 2.1 `@opstic` — Constellation + Asterism foundation

**Origin:** microg/GmsCore#3359 and #3360, later integrated into #3784.

**Keep:** service/AIDL surface, phone-device-verification proto/gRPC path, verification flow architecture, Asterism consent service architecture, UPI/OTP, MO-SMS, MT-SMS, TS.43/EAP-AKA paths, and associated phenotype work unless a separately proven defect targets a specific boundary.

**Why:** #3784 is built on this implementation. Current Foundragon findings are correctness gaps around individual contracts, not evidence that the foundation should be discarded.

### 2.2 `@br413` — DroidGuard/Tachyon parity fixes

**Origin:** microg/GmsCore#3644, integrated by #3784.

**Keep:**
- `cache_dg` / `app_dg_cache` path parity;
- uppercase VM cache-key hex parity;
- `DroidGuardInitReply.createFromParcel()` null/PFD handling;
- phone-number hint fallback used when formatted E.164 is unavailable.

**Disposition:** `CLOSED/TESTED` unless a new reproducer contradicts it.

### 2.3 `@camilo-12ch` — empty IID fail-closed

**Keep:** IID retrieval must not silently return `""` and send an empty credential to GPNV.

**Disposition:** `CLOSED/TESTED`.

### 2.4 `@paulcakeface` — EC key / public-key ACK coherence

**Keep:** validate/regenerate the EC key before reading the public-key-ACK state, so `ClientAuth` cannot be emitted for a key the server never acknowledged.

**Disposition:** `CLOSED/TESTED` with existing regression.

### 2.5 `@nwinkelman2` — integration and lint repair

**Keep:** branch integration, Android lint corrections, instrumentation-manifest repair, removal of the unsubstantiated custom DroidGuard classloader experiment, and correction of the false JVM Parcel round-trip assertion.

**Disposition:** `CLOSED/TESTED` as integration provenance; do not reintroduce removed speculative behavior without new evidence.

### 2.6 `@naormeit` — account-aware DroidGuard request

**Keep:** dynamic `hasAccount` derivation from `AccountManager` for Google accounts, with `SecurityException` fallback.

**Disposition:** `PROVEN DISPOSITION / KEEP`.

### 2.7 `@HumbleDrummer` — multipart MT-SMS assembly

**Origin:** naormeit/GmsCore#5, merged as baseline commit `276523b1d3e136b5b6e4ba0cf3f505b6702d60e2`.

**Keep unchanged unless contradicted:** assemble ordered PDU body parts from one `SMS_RECEIVED` broadcast into one logical `ReceivedSms`; reject empty broadcasts and missing body parts; preserve single-part behavior; use first available originating address.

**Existing proof reported by contributor:**
- constellation-core Debug unit tests: 14 tests, 0 failures/errors;
- constellation-core Release unit tests: 14 tests, 0 failures/errors;
- changed-module Debug/Release assemble + lint passed;
- `git diff --check` passed.

**Physical limitation:** no real-SIM/locked-bootloader RCS E2E.  
**Disposition:** `CLOSED/TESTED / KEEP + CREDIT HumbleDrummer`.

---

## 3. Foundragon-proven current defects / gaps

### FD-RCS-001 — Asterism returns a bare IID where public RE describes a CompositeToken

**Status:** `PRODUCT_DEFECT` at the wire-contract boundary; `EVIDENCE_GAP` for `pia_token` presence semantics.

**Current behavior:** #3784 obtains an IID token and passes the raw string through the Asterism consent response as `gmscoreIidToken`.

**Public reverse-engineering evidence:** the RCS client contract has been documented as URL-safe/no-wrap Base64 of a serialized `CompositeToken` with fields:

- `iid_token = 1`
- `pia_token = 2`

The same research ties this returned value to the Asterism `getAsterismConsent` path.

**Important encoding note:** Android `Base64.NO_WRAP | Base64.URL_SAFE` does **not** imply `NO_PADDING`; padding must not be stripped unless separately proven.

**Do not do:**
- do not add arbitrary fields to the Constellation server proto just because the client value is protobuf-encoded;
- do not guess that `pia_token` means a particular integrity product;
- do not guess `empty` vs `absent` vs populated for field 2.

**Foundragon fix direction:** create the CompositeToken at the Asterism client-contract boundary after exact field-2 semantics are proven.

**Required regression:** deterministic encode/decode fixture proving exact field numbers and Base64 flags, plus a boundary test proving Asterism returns the composite value instead of the raw IID.

**Acceptance:** BEFORE returns raw IID; AFTER returns exact expected encoded protobuf fixture. Field-2 fixture remains blocked until presence semantics are proven.

---

### FD-RCS-002 — silent auto-consent crosses the user-consent boundary

**Status:** `PRODUCT_DEFECT`.

**Current behavior:** `runVerificationFlow()` checks RCS consent and, when not consented, constructs a `SetConsentRequest` with `CONSENTED`, sends it itself, logs failure if the call throws, then continues toward Sync.

**Why this is wrong:** earlier stock-behavior RE and observed server behavior show the opposite invariant: verification is gated by consent; the client/application is expected to drive the consent transition. A prior implementation in this lineage used a fail-closed gate, and the later auto-consent change was introduced as a workaround for a `5001/no-consent` retry loop rather than as a proven stock contract.

**Root-cause hypothesis promoted by subsequent code review:** the retry loop may be caused by Asterism misreading the server consent response (FD-RCS-003), not by a requirement for GmsCore to silently grant consent.

**Foundragon fix direction:**
1. fix Asterism consent resolution;
2. prove the legitimate `SetAsterismConsent` path;
3. restore a fail-closed verification gate;
4. remove the silent auto-consent workaround.

**Regression invariant:** when effective RCS consent is not granted, verification must not silently call `SetConsent` on the user's behalf and must not start Sync. Exact callback/status code will be frozen from the proven prior/stock contract before implementation.

---

### FD-RCS-003 — current Asterism consent handler ignores `rcs_consent` when Gaia entry is absent

**Status:** current omission is `PRODUCT_DEFECT`; exact Gaia-vs-RCS precedence remains `PROVEN DISPOSITION` from contributor tests but still needs independent stock-authority confirmation.

**Current behavior:** current Asterism response mapping searches `gaia_consents` for the requested Asterism client. If not found, it falls back directly to `NO_CONSENT`.

**Mechanical evidence:** `GetConsentResponse` also contains a distinct `rcs_consent` field. Current handler leaves it unused in this case.

**Outside follow-ups:**
- `@Anusha0501`, naormeit/GmsCore#4: matching Gaia first; for RCS, fall back to non-unknown `rcs_consent`; adds focused resolver tests.
- `@keeltrace`, naormeit/GmsCore#6: matching Gaia first; for RCS, fall back to `rcs_consent`; adds focused resolver tests and restores the Asterism unit-test source set.

**Keep/adopt:** the resolver extraction and focused matrix are useful and attributable.

**Still to prove:** whether a matching Gaia consent is always authoritative over `rcs_consent`, and whether `CONSENT_UNKNOWN` in `rcs_consent` must be treated as absent. Do not silently choose between #4 and #6 until that boundary is sourced.

**Required regression matrix:**
- matching Gaia present;
- no matching Gaia + RCS consent present;
- unrelated Gaia entry + RCS consent present;
- non-RCS client must not consume RCS-only consent;
- no consent data;
- `CONSENT_UNKNOWN` semantics after authority is resolved.

---

### FD-RCS-004 — Asterism tests exist in follow-ups but baseline source set does not execute them

**Status:** `PRODUCT/EVIDENCE DEFECT` in the candidate test packaging.

**Observation:** baseline Asterism Gradle config only wires `src/main/kotlin`; follow-ups #4/#6 restore `src/test/kotlin` and JUnit.

**Fix direction:** restore the test source set and JUnit dependency as part of the integrated hardening patch.

**Why:** a regression file that Gradle never executes is not evidence.

---

### FD-RCS-005 — Constellation signing failure is converted to an empty signature

**Status:** `PROVEN DISPOSITION / TAKE + ADD REGRESSION` from `@keeltrace` #6.

**Current behavior:** catch-all signing failure returns `ByteArray(0)`.

**Risk:** caller can serialize/send malformed client-auth material as though signing completed.

**Contributor fix:** throw explicit `IllegalStateException` preserving the root cause.

**Foundragon requirement:** add a direct regression that forces signing failure and proves no empty-signature request is emitted. Do not claim closure from code review alone.

---

### FD-RCS-006 — Gaia ID recovery skips `null`

**Status:** `PROVEN DISPOSITION / TAKE + ADD REGRESSION` from `@keeltrace` #6.

**Current behavior:** recovery runs only for `id == ""`.

**Gap:** `AccountManager.getUserData(..., "GoogleUserId")` may yield `null`; null bypasses the fallback path.

**Contributor fix:** `id.isNullOrEmpty()`.

**Required regression:** null and empty IDs both enter recovery; existing non-empty IDs do not.

---

### FD-RCS-007 — `X-Goog-Spatula` is explicitly missing from current #3784

**Status:** `KNOWN PARITY GAP / TAKE WITH CREDIT`, not yet integrated.

**#3784 explicitly marks:** Constellation gRPC `X-Goog-Spatula` out of scope because constellation-core cannot depend directly on play-services-core without circular dependency.

**Outside solution:** `@Anusha0501`, naormeit/GmsCore#4, bridges through existing `play-services-api` `IAppCertService`, initializes the provider in both Constellation and Asterism services, adds an interceptor header helper and caching, and repairs the Android-ID AppCert fallback that otherwise returned null.

**Foundragon disposition:** architecture is mechanically compatible with the existing synchronous Binder AppCert service contract. Rebase the narrow bridge onto current `276523b1…`; do not wholesale cherry-pick the older combined branch.

**Required tests:**
- blank/null provider result adds no header;
- nonblank result adds exact `X-Goog-Spatula` value;
- cache hit avoids repeat bind;
- cache expiry re-fetches;
- binder/service failure fails safely without manufacturing a header;
- AppCert Android-ID fallback returns its generated header rather than `null`.

**Physical validation:** still required against real RCS provisioning.

---

### FD-RCS-008 — TS.43 EAP identity used for key derivation can differ from transmitted `EAP_ID`

**Status:** `PROVEN DISPOSITION / TAKE + CREDIT keeltrace`.

**Origin:** naormeit/GmsCore#2, commit `d61bba261f44fbd1af35d52855567e33c45d9e54`.

**Problem:** alternate realm can be used to construct the transmitted `EAP_ID`, while the AKA helper rebuilds a default-realm identity before deriving keys.

**Authority:** RFC 4187 §7 derives the key material from the identity used in the exchange.

**Fix:** pass the exact transmitted identity into AKA key derivation.

**Contributor proof:** focused TS.43 tests, constellation-core lintDebug and assembleDebug reported PASS.

**Physical limitation:** no carrier/SIM TS.43 E2E.

---

### FD-RCS-009 — TS.43 explicit `terminal_id` loses to device IMEI

**Status:** `PROVEN DISPOSITION / TAKE + CREDIT keeltrace`.

**Origin:** naormeit/GmsCore#2, commit `5776fbdfb3bd830f190938f6465acf2f3f41299d`.

**Problem:** baseline chooses device IMEI before an explicitly supplied request `terminal_id`.

**Authority:** Android Service Entitlement uses explicit request terminal ID first, device identifier only as fallback.

**Fix:** non-empty request value wins; IMEI/device value is fallback.

**Existing contributor regression:** explicit terminal ID overrides device fallback; empty terminal ID uses device fallback.

---

### FD-RCS-010 — TS.43 terminal metadata may be emitted empty

**Status:** `PROVEN DISPOSITION / TAKE + CREDIT keeltrace`.

**Origin:** naormeit/GmsCore#2, commit `9b728c8eab66ce172339fb93adefadb94340db43`.

**Problem:** absent request vendor/model/software values can remain empty.

**Authority:** Android Service Entitlement runtime defaults from device values. Executable AOSP code uses `Build.VERSION.RELEASE` for software fallback.

**Fix:** explicit request values remain authoritative; otherwise fall back to `Build.MANUFACTURER`, `Build.MODEL`, `Build.VERSION.RELEASE`, preserving existing truncation lengths.

---

## 4. State-machine findings that are intentionally NOT patched yet

### FD-RCS-RADAR-001 — persisted `next_sync_time` currently has no effective consumer in the inspected flow

**Status:** `UNRESOLVED`, **not** promoted to product defect.

**Observed:** Sync and Proceed responses persist `next_sync_time`; full verification starts a new Sync per full-flow call; sequential pending challenges issue Proceed calls. No effective reader/predicate for the stored next-sync timestamp was identified in the inspected baseline.

**Why no patch:** field name alone does not prove that it is a client-side cooldown gate. Public searching did not establish the stock contract strongly enough. A guessed scheduler would be worse than an inert field.

**Next evidence needed:** stock/public RE or trace proving writer → storage → reader → predicate → transition.

### FD-RCS-RADAR-002 — Android < 26 Asterism methods return without callback

**Status:** `UNRESOLVED / reachability required`.

**Observed:** Asterism binder methods return early below O without invoking callback.

**Why no patch:** historical notes indicate old Messages flows may differ. Prove that supported callers can reach this path before changing callback semantics.

---

## 5. Existing correct behavior discovered during review — do not rewrite

### Retry/throttle mapping

`UnverifiedInfo.retry_after_time` is already converted to client `retryAfterSeconds`, and `THROTTLED` is mapped to the client verification status. This is a positive control: the proto contains an explicit retry contract and the baseline consumes it.

**Disposition:** `CLOSED/TESTED / KEEP` unless a reproducer says otherwise.

### E.164/null phone-number handling

The current flow resolves formatted E.164, then phone-number hint, then raw SIM number, then safe empty fallback. Existing tests cover the resolution matrix.

**Disposition:** `CLOSED/TESTED / KEEP`.

---

## 6. Contributor adoption policy for this package

Do **not** squash away authorship intellectually even if final integration is rebased/squashed mechanically.

For each adopted piece, final PR/commit notes must state:

- original author / PR / commit;
- exact part retained;
- whether retained unchanged, rebased, or modified;
- Foundragon-specific delta;
- why the delta was necessary;
- regression added by original contributor vs regression added by Foundragon;
- physical-validation status.

Current adoption decisions:

| Origin | Part | Decision | Foundragon delta |
|---|---|---|---|
| HumbleDrummer #5 | multipart MT-SMS assembly | KEEP | none unless new evidence appears |
| keeltrace #2 | TS.43 identity / terminal ID / metadata | TAKE | rebase to current head; preserve attribution |
| Anusha0501 #4 | Spatula/AppCert bridge | TAKE PART | rebase only the narrow bridge; do not import stale combined branch wholesale |
| Anusha0501 #4 | RCS consent fallback | TAKE AS EVIDENCE/CANDIDATE | reconcile `CONSENT_UNKNOWN` rule with external authority |
| keeltrace #6 | RCS consent resolver + Asterism test wiring | TAKE AS CANDIDATE | independently freeze precedence semantics |
| keeltrace #6 | signing fail-closed | TAKE | add direct failure regression |
| keeltrace #6 | Gaia null/empty recovery | TAKE | add null/empty/non-empty regression |

---

## 7. Regression and gate plan

### 7.1 Focused deterministic tests

1. CompositeToken exact protobuf + Base64URL fixture — blocked only on field-2 presence semantics.
2. Asterism response returns CompositeToken rather than bare IID.
3. Asterism consent resolution matrix.
4. Consent gate: NO_CONSENT cannot silently self-grant and cannot start Sync.
5. Legitimate SetAsterismConsent path changes effective consent and permits subsequent verification path.
6. Signing failure cannot degrade to empty signature.
7. Gaia ID null/empty recovery.
8. Spatula header injection/cache/failure behavior.
9. AppCert Android-ID fallback returns generated header.
10. Existing multipart MT-SMS tests stay green.
11. Existing TS.43 identity/terminal tests stay green.
12. Existing IID fail-closed and EC-key/ACK tests stay green.

### 7.2 Build/static gates

Run with failure-preserving shell behavior:

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

Repository-wide gates are run separately and their failures classified as introduced vs inherited. Do not relabel an inherited monorepo failure as a changed-module defect, and do not hide an introduced failure behind an inherited one.

### 7.3 Hardware gates

These remain mandatory before any claim of full #2994 completion:

- locked-bootloader microG ROM;
- active physical SIM;
- recent Google Messages;
- second RCS endpoint;
- send + receive;
- provisioning from clean/no-consent state;
- accountless path where practical;
- TS.43-capable carrier SIM where available;
- manually redacted logs only; never publish phone numbers, raw tokens, or credentials.

Until then the package may claim deterministic/protocol fixes, **not complete physical RCS success**.

---

## 8. Rehydration checklist

After interruption or model/tool crash:

1. Checkout `Chess-Debug/GmsCore:hunter/gmscore-2994-frontier`.
2. Read this file before touching code.
3. Confirm branch ancestry contains `276523b1d3e136b5b6e4ba0cf3f505b6702d60e2`.
4. Read `PROVENANCE-MATRIX.md` before cherry-picking/reimplementing contributor work.
5. Read `REGRESSION-PLAN.md` before claiming any fix complete.
6. Resume at the **Current Frontier** below; do not redo CLOSED/TESTED work without new counter-evidence.

---

## 9. Current frontier

**Primary:** prove exact `CompositeToken.pia_token` presence/value semantics in the Asterism/Google Messages contract.

Then, in order:

1. freeze CompositeToken regression and implement boundary-local fix;
2. freeze Asterism consent precedence / unknown semantics;
3. integrate consent resolver + restore test source set;
4. restore fail-closed user-consent gate and remove silent auto-consent workaround;
5. integrate Spatula/AppCert bridge;
6. integrate TS.43 corrections;
7. integrate signing + Gaia hardening with missing regressions;
8. run deterministic gates;
9. revisit `next_sync_time` / first-connect only with contract evidence;
10. perform new defect sweep;
11. physical RCS/TS.43 validation;
12. produce final bounty/PR evidence package.

---

## 10. Non-claims at this checkpoint

- No Foundragon code fix is claimed merged by this documentation checkpoint.
- No `pia_token` semantics are claimed known.
- No stock Gaia-vs-RCS precedence rule is claimed independently proven yet.
- No `next_sync_time` cooldown semantics are claimed.
- No locked-bootloader / real-SIM end-to-end RCS success is claimed.
- No TS.43 carrier/SIM success is claimed.

This checkpoint records **what is proven, what is adopted, what is still open, and exactly what must prove the next change**.
