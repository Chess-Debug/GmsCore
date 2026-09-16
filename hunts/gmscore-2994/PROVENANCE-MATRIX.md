# GmsCore #2994 — Provenance Matrix

> Companion to `GMSCORE-2994-CLOSURE-PACK.md`.  
> Purpose: preserve contributor credit and make integration decisions auditable.

## Rules

- `KEEP` means preserve current implementation unless new evidence contradicts it.
- `TAKE` means integrate the named outside fix into the Foundragon branch with attribution.
- `TAKE PART` means only the named mechanical slice is adopted; do not wholesale cherry-pick unrelated/stale changes.
- `REVIEW` means useful evidence exists but one or more semantics remain unfrozen.
- `FOUNDRAGON` means defect/solution was independently identified in this pass; outside work may still overlap and must be credited where it does.
- No row implies physical locked-bootloader RCS verification unless explicitly stated.

| Origin | Reference | Contribution / observation | Decision | Why retained / changed | Foundragon responsibility |
|---|---|---|---|---|---|
| `@opstic` | microg/GmsCore#3359, #3360 | Constellation + Asterism implementation, AIDL/proto/gRPC, verification paths, phenotype work | KEEP | Architectural foundation of #3784 | Patch only separately proven boundaries; preserve foundation credit |
| `@br413` | microg/GmsCore#3644 | DroidGuard cache-path parity, uppercase VM key, Parcel null handling, phone-number hint fallback | KEEP | Integrated parity fixes with no current counter-evidence | Keep regression behavior green |
| `@camilo-12ch` | integrated in #3784 | IID retrieval fails instead of returning empty credential | KEEP | Prevents empty token from reaching GPNV | Do not regress when CompositeToken is added |
| `@paulcakeface` | integrated in #3784 | Validate/regenerate EC key before public-key ACK state is consumed | KEEP | Prevents ClientAuth for unacknowledged key | Preserve existing regression |
| `@nwinkelman2` | #3784 integration lineage | Branch integration, lint/manifest repair, removal of speculative DG classloader, JVM Parcel test correction | KEEP | Makes candidate coherent and removes unproven behavior | Do not reintroduce speculation |
| `@naormeit` | microg/GmsCore#3784 | `hasAccount` derived from Google accounts instead of hardcoded false | KEEP | Corrects account-dependent DG request semantics | Preserve during rebases |
| `@HumbleDrummer` | naormeit/GmsCore#5; merge `276523b1…` | Multipart MT-SMS PDU assembly + tests | KEEP | Solves challenge split across SMS segments; baseline already contains it | Preserve unchanged unless new reproducer appears |
| `@keeltrace` | naormeit/GmsCore#2 `d61bba26…` | Use exact transmitted EAP identity for AKA key derivation | TAKE | Matches RFC 4187 identity/key derivation contract | Rebase to current head; retain tests/credit |
| `@keeltrace` | naormeit/GmsCore#2 `5776fbdf…` | Explicit TS.43 `terminal_id` wins over device fallback | TAKE | Matches Android Service Entitlement behavior | Rebase + retain tests/credit |
| `@keeltrace` | naormeit/GmsCore#2 `9b728c8e…` | Device fallback for terminal vendor/model/software | TAKE | Matches AOSP runtime defaults; keeps truncation | Rebase + retain tests/credit |
| `@Anusha0501` | naormeit/GmsCore#4 | AppCert Binder bridge for `X-Goog-Spatula`, caching, service initialization, Android-ID fallback repair | TAKE PART | Solves #3784’s stated circular-dependency blocker without direct play-services-core dependency | Rebase narrow bridge to `276523b1…`; add/retain focused tests; inspect timeout/failure behavior |
| `@Anusha0501` | naormeit/GmsCore#4 | Asterism RCS consent fallback when Gaia match absent; treats `CONSENT_UNKNOWN` as missing | REVIEW | Mechanically fixes ignored `rcs_consent`; unknown/precedence semantics need independent authority | Freeze exact rule before final integration |
| `@keeltrace` | naormeit/GmsCore#6 `3aacca36…` | Asterism Gaia→RCS consent resolver + focused tests | REVIEW/TAKE CANDIDATE | Current baseline ignores `rcs_consent`; resolver extraction is valuable | Independently freeze precedence and unknown semantics |
| `@keeltrace` | naormeit/GmsCore#6 | Restore `src/test/kotlin` + JUnit in Asterism core | TAKE | Baseline otherwise does not execute focused Asterism JVM tests | Integrate as evidence infrastructure |
| `@keeltrace` | naormeit/GmsCore#6 | Signing failure throws instead of returning empty signature | TAKE | Fail-closed behavior avoids malformed auth payload | Add direct forced-failure regression before closure |
| `@keeltrace` | naormeit/GmsCore#6 | Gaia `GoogleUserId` recovery uses `isNullOrEmpty()` | TAKE | Baseline only recovers exact empty string | Add null/empty/non-empty regression |
| Public RCS RE referenced in #2994 | CompositeToken contract | `gmscore_instance_id_token` / Asterism token documented as Base64URL serialized `CompositeToken(iid_token=1, pia_token=2)` | FOUNDRAGON DEFECT EVIDENCE | Current #3784 returns bare IID | Prove field-2 presence/value semantics; implement boundary-local fix and fixture |
| Earlier stock-flow RE / empirical server observations | consent gate | Verification should be gated by consent; real SetConsent transitioned server out of no-consent failure | FOUNDRAGON DEFECT EVIDENCE | Current baseline silently self-grants consent | Fix resolver, prove legit consent path, restore fail-closed gate |
| Foundragon source trace | `next_sync_time` | Persisted from Sync/Proceed, no effective consumer found in inspected baseline | REVIEW ONLY | Inert state observed, but semantic contract not proven | Do not patch until stock reader/predicate/transition is proven |
| Foundragon source trace | Android < 26 Asterism early return | Binder methods return without callback | REVIEW ONLY | Could hang caller, but reachability/support contract unclear | Prove caller path before changing behavior |

## Contributor-specific notes

### HumbleDrummer / multipart MT-SMS

Retain as-is. The merged change is already the baseline parent and includes direct regression coverage. Do not rewrite merely to make the final package appear more “Foundragon-owned”. Final package should explicitly credit HumbleDrummer.

### keeltrace / TS.43

Three TS.43 corrections are independently separable and should remain attributable as such. If rebased/squashed, preserve original commit IDs in the commit body / PR notes. Hardware limitation remains: protocol/build/unit proof is not a carrier-SIM E2E claim.

### Anusha0501 / Spatula

The combined #4 branch contains several concerns. Foundragon should not merge it wholesale because its base predates `276523b1…` and overlaps later hardening work. Extract the AppCert/Spatula architecture and its tests, then integrate it on the current baseline. The approach is valuable specifically because it resolves the dependency-cycle objection documented in #3784.

### keeltrace / release hardening #6

#6 is especially useful because it is stacked directly on the current `276523b1…` baseline. Still, Foundragon should distinguish:

- **well-targeted implementation change** from
- **fully proven contract**.

The consent fallback implementation is mechanically needed because the baseline ignores an explicit response field, but exact Gaia/RCS precedence must still be independently frozen. Signing and Gaia-ID fixes need focused regressions before final closure.

## Attribution requirement for final integration commits

Each commit that materially incorporates another contributor’s work should include a body section similar to:

```text
Provenance:
- Based on: <author> <PR/commit>
- Retained: <exact behavior/code slice>
- Foundragon delta: <what changed>
- Reason for delta: <contract/regression/root-cause>
- Verification: <focused tests/gates>
- Hardware status: <verified / NOT YET PHYSICALLY VERIFIED>
```

Where practical and accurate, preserve `Co-authored-by` trailers from the actual source commit. Do not fabricate co-author trailers for people whose code was only used as research evidence.

## Final-package credit list currently expected

At minimum, if their corresponding code remains in the delivered package, final notes should preserve credit to:

- `@opstic`
- `@br413`
- `@camilo-12ch`
- `@paulcakeface`
- `@nwinkelman2`
- `@naormeit`
- `@HumbleDrummer`
- `@keeltrace`
- `@Anusha0501`

This list is additive; new contributors discovered during the remaining sweep must be added rather than silently absorbed.
