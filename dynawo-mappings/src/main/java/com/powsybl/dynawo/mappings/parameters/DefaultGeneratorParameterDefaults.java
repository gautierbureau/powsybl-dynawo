/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings.parameters;

import com.google.auto.service.AutoService;

import java.util.Map;

/**
 * Generic values making a synchronous generator model run without asking the user for the hundreds
 * of parameters a detailed model declares.
 * <p>
 * They describe a plausible machine, not a specific one: a study needing accurate dynamics is
 * expected to override them.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
@AutoService(GeneratorParameterDefaults.class)
public class DefaultGeneratorParameterDefaults implements GeneratorParameterDefaults {

    @Override
    public Map<String, String> getReferences() {
        return Map.ofEntries(
                Map.entry("generator_P0Pu", "p_pu"),
                Map.entry("generator_Q0Pu", "q_pu"),
                Map.entry("generator_U0Pu", "v_pu"),
                Map.entry("generator_UPhase0", "angle_pu"),
                Map.entry("generator_PMax", "pMax"),
                Map.entry("generator_PMin", "pMin"),
                Map.entry("generator_PNom", "pMax"),
                Map.entry("governor_PMax", "pMax"),
                Map.entry("governor_PMin", "pMin"),
                Map.entry("governor_PNom", "pMax"),
                Map.entry("transformer_P10Pu", "p_pu"),
                Map.entry("transformer_Q10Pu", "q_pu"),
                Map.entry("transformer_U10Pu", "v_pu"),
                Map.entry("transformer_U1Phase0", "angle_pu"));
    }

    @Override
    public Map<String, String> getLowVoltageReferences() {
        return Map.of("generator_UNom", "vNom",
                "generator_UBaseLV", "vNom",
                "generator_UNomLV", "vNom",
                "generator_UBaseHV", "vNom",
                "generator_UNomHV", "vNom");
    }

    @Override
    public Map<String, String> getHighVoltageReferences() {
        return Map.of("generator_UBaseHV", "vNom",
                "generator_UNomHV", "vNom");
    }

    @Override
    public Map<String, String> getValues() {
        return Map.ofEntries(
                // machine
                Map.entry("generator_H", "3"),
                Map.entry("generator_DPu", "0"),
                Map.entry("generator_RaPu", "0.0035"),
                Map.entry("generator_XlPu", "0.2"),
                Map.entry("generator_XdPu", "1.66"),
                Map.entry("generator_XpdPu", "0.4"),
                Map.entry("generator_XppdPu", "0.25"),
                Map.entry("generator_XqPu", "1.34"),
                Map.entry("generator_XpqPu", "0.54"),
                Map.entry("generator_XppqPu", "0.25"),
                Map.entry("generator_Tpd0", "6"),
                Map.entry("generator_Tppd0", "0.064"),
                Map.entry("generator_Tpq0", "1.17"),
                Map.entry("generator_Tppq0", "0.1"),
                Map.entry("generator_md", "0.073"),
                Map.entry("generator_mq", "0.073"),
                Map.entry("generator_nd", "4.28"),
                Map.entry("generator_nq", "4.2"),
                Map.entry("generator_MdPuEfd", "0.7"),
                Map.entry("generator_ExcitationPu", "1"),
                Map.entry("generator_UseApproximation", "true"),
                Map.entry("generator_thetaCheck", "false"),
                Map.entry("generator_RTfPu", "0"),
                Map.entry("generator_XTfPu", "0"),
                Map.entry("generator_AlphaPuPNom", "20"),
                // simplified regulations
                Map.entry("governor_KGover", "20"),
                Map.entry("voltageRegulator_EfdMinPu", "0"),
                Map.entry("voltageRegulator_EfdMaxPu", "2.5"),
                Map.entry("voltageRegulator_Gain", "30"),
                Map.entry("voltageRegulator_LagEfdMax", "4"),
                Map.entry("voltageRegulator_LagEfdMin", "10"),
                Map.entry("voltageRegulator_UsRefMaxPu", "1.08"),
                Map.entry("voltageRegulator_UsRefMinPu", "0.9"),
                Map.entry("voltageRegulator_tIntegral", "5"),
                // generator transformer and auxiliaries
                Map.entry("transformer_BPu", "0"),
                Map.entry("transformer_GPu", "0"),
                Map.entry("transformer_RPu", "0"),
                Map.entry("transformer_XPu", "0"),
                Map.entry("transformer_rTfoPu", "1"),
                Map.entry("transformer_ZShortCircuit", "10"),
                Map.entry("transformer_copperLosses", "0.2"),
                Map.entry("transformer_iMagnetizing", "0.08"),
                Map.entry("auxHV_alpha", "1.5"),
                Map.entry("auxHV_beta", "2.5"),
                Map.entry("auxLV_alpha", "1.5"),
                Map.entry("auxLV_beta", "2.5"),
                Map.entry("underVoltageAutomaton_tLagAction", "0"),
                Map.entry("underVoltageAutomaton_UMinPu", "0"));
    }

    @Override
    public Map<String, Map<String, String>> getControlValues() {
        return Map.of("GovCt2", govCt2(),
                "GovSteam", govSteam(),
                "GovHydro4", govHydro4(),
                "HyGov", hyGov(),
                "TGov3", tGov3(),
                "St4b", st4b(),
                "IEEX2A", ieex2a(),
                "Scrx", scrx());
    }

    private static Map<String, String> govCt2() {
        return Map.ofEntries(
                Map.entry("governor_PGenBaseMw", "100"),
                Map.entry("governor_RSelectInt", "1"),
                Map.entry("governor_aSetPu", "10"),
                Map.entry("governor_DeltaOmegaDbPu", "0"),
                Map.entry("governor_DeltaOmegaMaxPu", "1"),
                Map.entry("governor_DeltaOmegaMinPu", "-1"),
                Map.entry("governor_DeltaT", "1"),
                Map.entry("governor_Dm", "0"),
                Map.entry("governor_KA", "10"),
                Map.entry("governor_KDGov", "0"),
                Map.entry("governor_KIGov", "0.45"),
                Map.entry("governor_KILoad", "1"),
                Map.entry("governor_KIMw", "0"),
                Map.entry("governor_KPGov", "4"),
                Map.entry("governor_KPLoad", "1"),
                Map.entry("governor_KTurb", "1.9168"),
                Map.entry("governor_PLdRefPu", "1"),
                Map.entry("governor_PRatePu", "0.017"),
                Map.entry("governor_RClosePu", "-99"),
                Map.entry("governor_RDownPu", "-99"),
                Map.entry("governor_ROpenPu", "99"),
                Map.entry("governor_RDroop", "0.05"),
                Map.entry("governor_RUpPu", "99"),
                Map.entry("governor_tActuator", "0.4"),
                Map.entry("governor_tA", "1"),
                Map.entry("governor_tB", "0.1"),
                Map.entry("governor_tC", "0"),
                Map.entry("governor_tDGov", "1"),
                Map.entry("governor_tDRatelim", "0.001"),
                Map.entry("governor_tEngine", "0"),
                Map.entry("governor_tFLoad", "3"),
                Map.entry("governor_tLastValue", "1e-5"),
                Map.entry("governor_tPElec", "2.5"),
                Map.entry("governor_tSA", "1e-6"),
                Map.entry("governor_tSB", "50"),
                Map.entry("governor_ValveMaxPu", "1"),
                Map.entry("governor_ValveMinPu", "0.175"),
                Map.entry("governor_WFnlPu", "0.187"),
                Map.entry("governor_WFSpdBool", "false"),
                Map.entry("governor_fLim1Hz", "49"),
                Map.entry("governor_fLim2Hz", "8"),
                Map.entry("governor_fLim3Hz", "7"),
                Map.entry("governor_fLim4Hz", "6"),
                Map.entry("governor_fLim5Hz", "5"),
                Map.entry("governor_fLim6Hz", "4"),
                Map.entry("governor_fLim7Hz", "3"),
                Map.entry("governor_fLim8Hz", "2"),
                Map.entry("governor_fLim9Hz", "1"),
                Map.entry("governor_fLim10Hz", "0"),
                Map.entry("governor_PLim1Pu", "0.8325"),
                Map.entry("governor_PLim2Pu", "0.8325"),
                Map.entry("governor_PLim3Pu", "0.8325"),
                Map.entry("governor_PLim4Pu", "0.8325"),
                Map.entry("governor_PLim5Pu", "0.8325"),
                Map.entry("governor_PLim6Pu", "0.8325"),
                Map.entry("governor_PLim7Pu", "0.8325"),
                Map.entry("governor_PLim8Pu", "0.8325"),
                Map.entry("governor_PLim9Pu", "0.8325"),
                Map.entry("governor_PLim10Pu", "0.8325"));
    }

    private static Map<String, String> govSteam() {
        return Map.ofEntries(
                Map.entry("governor_Db1", "0"),
                Map.entry("governor_Db2", "0"),
                Map.entry("governor_Eps", "0"),
                Map.entry("governor_H0", "false"),
                Map.entry("governor_K", "25"),
                Map.entry("governor_K1", "0.2"),
                Map.entry("governor_K2", "0"),
                Map.entry("governor_K3", "0.3"),
                Map.entry("governor_K4", "0.0"),
                Map.entry("governor_K5", "0.5"),
                Map.entry("governor_K6", "0"),
                Map.entry("governor_K7", "0"),
                Map.entry("governor_K8", "0"),
                Map.entry("governor_PMaxPu", "1"),
                Map.entry("governor_PMinPu", "0"),
                Map.entry("governor_Sdb1", "true"),
                Map.entry("governor_Sdb2", "true"),
                Map.entry("governor_t1", "0.00001"),
                Map.entry("governor_t2", "0.00001"),
                Map.entry("governor_t3", "0.1"),
                Map.entry("governor_t4", "0.3"),
                Map.entry("governor_t5", "5"),
                Map.entry("governor_t6", "0.5"),
                Map.entry("governor_t7", "0.00001"),
                Map.entry("governor_Uc", "-10"),
                Map.entry("governor_Uo", "1"),
                Map.entry("governor_ValveOn", "true"),
                Map.entry("governor_PgvTableName", "Pgv"),
                Map.entry("governor_PgvInvTableName", "PgvInv"));
    }

    private static Map<String, String> govHydro4() {
        return Map.ofEntries(
                Map.entry("governor_ATurb", "1.2"),
                Map.entry("governor_DeltaOmegaDbPu", "0"),
                Map.entry("governor_DeltaOmegaEpsPu", "0"),
                Map.entry("governor_DeltaPDbPu", "0"),
                Map.entry("governor_DTurb", "1.1"),
                Map.entry("governor_GMax", "1"),
                Map.entry("governor_GMin", "0"),
                Map.entry("governor_HDam", "1"),
                Map.entry("governor_QNl", "0"),
                Map.entry("governor_RPerm", "0.05"),
                Map.entry("governor_RTemp", "0.3"),
                Map.entry("governor_tG", "0.5"),
                Map.entry("governor_tP", "0.1"),
                Map.entry("governor_tR", "5"),
                Map.entry("governor_tW", "1"),
                Map.entry("governor_UC", "-0.2"),
                Map.entry("governor_UO", "0.2"));
    }

    private static Map<String, String> hyGov() {
        return Map.ofEntries(
                Map.entry("governor_At", "1.2"),
                Map.entry("governor_DTurb", "0"),
                Map.entry("governor_FlowNoLoad", "0.08"),
                Map.entry("governor_KDroopPerm", "0.05"),
                Map.entry("governor_KDroopTemp", "0.5"),
                Map.entry("governor_OpeningGateMax", "1"),
                Map.entry("governor_OpeningGateMin", "0"),
                Map.entry("governor_tF", "0.05"),
                Map.entry("governor_tG", "0.5"),
                Map.entry("governor_tR", "4"),
                Map.entry("governor_tW", "1.3"),
                Map.entry("governor_VelMaxPu", "0.2"));
    }

    private static Map<String, String> tGov3() {
        return Map.ofEntries(
                Map.entry("governor_K", "16.2"),
                Map.entry("governor_K1", "0.27"),
                Map.entry("governor_K2", "0.292"),
                Map.entry("governor_K3", "0.438"),
                Map.entry("governor_PMaxPu", "0.82"),
                Map.entry("governor_PMinPu", "0.3"),
                Map.entry("governor_PrMaxPu", "1.1"),
                Map.entry("governor_t1", "1"),
                Map.entry("governor_t2", "1"),
                Map.entry("governor_t3", "0.49"),
                Map.entry("governor_t4", "0.25"),
                Map.entry("governor_t5", "7"),
                Map.entry("governor_t6", "0.4"),
                Map.entry("governor_tA", "1"),
                Map.entry("governor_tB", "2"),
                Map.entry("governor_tC", "3"),
                Map.entry("governor_Uc", "-0.081"),
                Map.entry("governor_Uo", "0.081"),
                Map.entry("governor_FValveInvTableName", "FValveInv"),
                Map.entry("governor_FValveTableName", "FValve"));
    }

    private static Map<String, String> st4b() {
        return Map.ofEntries(
                Map.entry("voltageRegulator_Kc", "0.113"),
                Map.entry("voltageRegulator_Kg", "0"),
                Map.entry("voltageRegulator_Ki", "0"),
                Map.entry("voltageRegulator_Kim", "0"),
                Map.entry("voltageRegulator_Kir", "10.75"),
                Map.entry("voltageRegulator_Kp", "9.3"),
                Map.entry("voltageRegulator_Kpm", "1"),
                Map.entry("voltageRegulator_Kpr", "10.75"),
                Map.entry("voltageRegulator_tA", "0.02"),
                Map.entry("voltageRegulator_Thetap", "0"),
                Map.entry("voltageRegulator_tR", "0.02"),
                Map.entry("voltageRegulator_UOel0Pu", "10"),
                Map.entry("voltageRegulator_UUel0Pu", "0"),
                Map.entry("voltageRegulator_VaMaxPu", "1"),
                Map.entry("voltageRegulator_VaMinPu", "-0.87"),
                Map.entry("voltageRegulator_VbMaxPu", "11.63"),
                Map.entry("voltageRegulator_VmMaxPu", "99"),
                Map.entry("voltageRegulator_VmMinPu", "-99"),
                Map.entry("voltageRegulator_VrMaxPu", "1"),
                Map.entry("voltageRegulator_VrMinPu", "-0.87"),
                Map.entry("voltageRegulator_XlPu", "0.124"));
    }

    private static Map<String, String> ieex2a() {
        return Map.ofEntries(
                Map.entry("voltageRegulator_AEx", "0.00021"),
                Map.entry("voltageRegulator_BEx", "1.86416"),
                Map.entry("voltageRegulator_Ka", "400"),
                Map.entry("voltageRegulator_Ke", "1"),
                Map.entry("voltageRegulator_Kf", "0.25"),
                Map.entry("voltageRegulator_tA", "0.03"),
                Map.entry("voltageRegulator_tB", "1e-5"),
                Map.entry("voltageRegulator_tC", "1e-5"),
                Map.entry("voltageRegulator_tE", "0.8"),
                Map.entry("voltageRegulator_tF1", "1.25"),
                Map.entry("voltageRegulator_tF2", "1e-5"),
                Map.entry("voltageRegulator_tR", "1e-5"),
                Map.entry("voltageRegulator_VrMaxPu", "5.974"),
                Map.entry("voltageRegulator_VrMinPu", "-5.974"));
    }

    private static Map<String, String> scrx() {
        return Map.of("voltageRegulator_K", "100",
                "voltageRegulator_tA", "1",
                "voltageRegulator_tB", "10",
                "voltageRegulator_tE", "0.05",
                "voltageRegulator_VrMaxPu", "2.5",
                "voltageRegulator_VrMinPu", "0");
    }
}
