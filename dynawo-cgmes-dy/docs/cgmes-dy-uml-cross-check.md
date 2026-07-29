# dynawo-cgmes-dy — DY parameter names cross-checked against the CIM UML

**References used**
- **cimpy 1.1.0** — Python CGMES **v2.4.15** (CIM16) generated classes.
- **libcimpp** (CIM++, C++) — generated classes for **4** model versions:
  CGMES 2.4.13, 2.4.15 (16FEB2016), 2.4.15 (27JAN2020) and **3.0.0 (CIM17)**.

Method: parse all 151 SPARQL queries, group predicates by the output `?var`
they bind (the parser deliberately queries several spelling variants of one
attribute into one variable), and validate each group against the UML.

## Headline

- **123 / 151 models are 100% UML-clean** against CIM16 alone, including the
  patch's `ExcST3 → ExcST3A` rename (both cimpy and libcimpp confirm `ExcST3A`).
- Of 2854 output-variable groups, **2740 match CIM16**; 76 rely on the parser's
  multi-spelling tolerance (e.g. `efdmax`+`edfmax`, `qn1`+`qnl`, `pmin`+`pmm`).
- libcimpp's CIM17 (3.0.0) classes then explain most of the remainder.

---

## Finding 1 — Association role names use a non-standard `xxxId` convention  ⚠️ interop risk

For **data attributes**, the parser matches the standard. For **associations**
(links to other objects, written `rdf:resource`), it is inconsistent:

- ✅ Correct (UML PascalCase) for the core dynamics→equipment links, e.g.
  `ExcitationSystemDynamics.SynchronousMachineDynamics`,
  `RotatingMachineDynamics.RotatingMachine`,
  `AsynchronousMachineDynamics.AsynchronousMachine`.
- ❌ Invented lowercase `xxxId` / `xxxIECId` for **Load** composite parts and
  the **Wind IEC** interconnections. These predicate names **do not exist** in
  any CIM version — real CGMES files (and cimpy/libcimpp) use PascalCase roles.
  Against a standard DY file these links are silently dropped.

| Parser predicate (SPARQL) | Real CGMES role name (libcimpp) |
|---|---|
| `LoadAggregate.energyConsumerId` | `LoadAggregate.EnergyConsumer` |
| `LoadAggregate.loadMotorId` | `LoadAggregate.LoadMotor` |
| `LoadAggregate.loadStaticId` | `LoadAggregate.LoadStatic` |
| `LoadComposite.energyConsumerId` | `LoadComposite.EnergyConsumer` |
| `LoadGenericNonLinear.energyConsumerId` | `LoadGenericNonLinear.EnergyConsumer` |
| `WindGenTurbineType*.windProtectionIECId` | `…WindProtectionIEC` |
| `WindGenTurbineType*.windMechIECId` | `…WindMechIEC` |
| `WindGenTurbineType2IEC.windContRotorRIECId` | `…WindContRotorRIEC` |
| `WindGenTurbineType3*.windContPType3IECId` | `…WindContPType3IEC` |
| `WindGenTurbineType3*.windContCurrLimIECId` | `…WindContCurrLimIEC` |
| `WindGenTurbineType3*.windContQIECId` | `…WIndContQIEC` *(note the CGMES "WInd" typo)* |
| `WindGenTurbineType1aIEC.windAeroConstIECId` | `…WindAeroConstIEC` |
| `WindPlantIEC.windPlantFreqPcontrolIECId` | `…WindPlantFreqPcontrolIEC` |
| `WindPlantIEC.windPlantReactiveControlIECId` | `…WindPlantReactiveControlIEC` |

The repo's own test fixtures use the same `xxxId` spelling, so the unit tests
pass — but `mini_DY.xml` already uses the standard
`WindTurbineType3or4IEC.WindProtectionIEC`, showing the inconsistency.

**Status: FIXED.** The 25 confirmed standard roles (declaring-class-qualified,
per libcimpp) have been added as tolerant `OPTIONAL` siblings alongside the
existing `xxxId` predicates in the 9 Load/Wind queries — the same idiom the
parser already uses for attribute spellings. Both conventions now parse; all
470 module tests still pass. Note the Load `EnergyConsumer` link is declared on
the parent `LoadDynamics`, and the standard's own `WIndContQIEC` typo is
preserved deliberately. A handful of `xxxId` links have no standard equivalent
in any CIM version (`asynchronousMachineId`, `windAeroLinearIECId`,
`windContPitchAngleIECId`, and everything on the non-standard 4A/4B classes) and
were left untouched.

A few `xxxId` links have **no** libcimpp equivalent even as PascalCase
(`WindGenTurbineType*.asynchronousMachineId`, `…windContPitchAngleIECId`,
`…windAeroLinearIECId`) — the parser models associations the standard places on
a different class / via a `*Dynamics` intermediary; worth a modelling review.

---

## Finding 2 — Wind + "vendor" classes cimpy couldn't see, resolved via libcimpp CIM17

cimpy's 2.4.15 package lacks part of the IEC wind package and the newer models.
libcimpp's **CGMES 3.0.0 (CIM17)** classes cover most of them:

| Class | Status | Notes |
|---|---|---|
| `ExcNI` | ✅ standard CIM17 | in libcimpp 3.0.0; params match |
| `ExcRQB` | ✅ standard CIM17 | 3.0.0 members match exactly (ki0,ki1,klir,klus,lsat,lus,mesu,t4m,tc,te,tf,ucmax,ucmin) |
| `PssRQB` | ✅ standard CIM17 | in libcimpp 3.0.0; params match |
| `WindGenTurbineType1aIEC` / `1bIEC` | ✅ standard CIM17 | classes exist in 3.0.0; only the `xxxId` associations diverge (Finding 1) |
| `WindGenTurbineType4aIEC` / `4bIEC` | ⚠️ non-standard split | CGMES has a single `WindGenType4IEC`; parser splits 4A/4B (IEC 61400-27 style). `dipmax,diqmax,tg` not in CGMES |
| `OverexcLimX` | ⚠️ renamed | CGMES has `OverexcLimX1` / `OverexcLimX2`, no plain `OverexcLimX` |
| `VoltageAdjusterIEEE` | ⚠️ non-standard | CGMES has `VoltageAdjusterDynamics` / `…UserDefined`; no IEEE variant |
| `VoltageCompensatorIEEE` | ⚠️ non-standard | CGMES uses `VCompIEEEType1` (which the parser also models correctly) |
| `ExcSYMPTR` | ⚠️ vendor | not in any CIM version — genuine proprietary/RTE model |

---

## Finding 3 — Attributes absent from every CIM version

- `RotatingMachineDynamics.mBase` (queried in all 6 sync/async machine models) —
  not present in CIM16 or CIM17. It's a machine-base extension read; against a
  pure-standard file `?mBase` is always empty. Confirm whether inputs carry it,
  or map from the static `SynchronousMachine.ratedS` instead.
- `GovGAST4.ty` — CGMES `GovGAST4` has `tv`, not `ty`; only `ty` is queried
  (likely a `ty`→`tv` typo).
- `GovHydro4.qnl` — CGMES `GovHydro4` uses `qn1`; harmless because `qn1` is
  queried separately, but the `qnl` line is dead against standard data.
- `WindContCurrLimIEC.kpqu`, `.upqumax` — not on that class in CIM16/17.

---

## Bottom line

The bulk of the parser (all IEEE exciters, governors, PSS, machines) models the
**correct** CGMES parameter names, and the multi-spelling tolerance is sound.
The one systematic risk is **Finding 1**: association role names use a private
`xxxId` convention that won't match standard CGMES exports. The vendor/wind
classes flagged by cimpy are mostly **standard CIM17** (validated via libcimpp),
with a handful of genuinely non-standard models (`ExcSYMPTR`, the `…IEEE`
voltage models, the 4A/4B wind split).
