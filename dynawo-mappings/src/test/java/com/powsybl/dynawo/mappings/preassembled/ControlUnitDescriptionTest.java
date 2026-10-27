/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.dynawo.mappings.preassembled;

import com.powsybl.dynawo.extensions.api.generator.SynchronousGeneratorProperties.Windings;
import com.powsybl.dynawo.mappings.preassembled.MachineControlUnit.MachineQuantity;
import com.powsybl.dynawo.mappings.preassembled.MachineControlUnit.RegulatorInput;
import com.powsybl.dynawo.mappings.preassembled.MachineControlUnit.Wire;
import com.powsybl.dynawo.mappings.preassembled.RegulatorControlUnit.Drive;
import com.powsybl.dynawo.mappings.preassembled.RegulatorControlUnit.Kind;
import com.powsybl.dynawo.mappings.preassembled.RegulatorControlUnit.Watch;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * What a machine offers and what each control reads and drives, read without assembling anything.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
class ControlUnitDescriptionTest {

    private final ControlUnitCatalog catalog = ControlUnitCatalog.getInstance();

    @Test
    void shouldTellWhatAMachineOffersWhileRunningAndAtInitialisation() {
        Map<MachineQuantity, String> offered = MachineQuantity.offeredBy(
                new GeneratorSynchronousUnit(Windings.FOUR_WINDINGS, false, ModelNaming.DYNAWO_1_7_0));

        assertThat(offered)
                .containsEntry(MachineQuantity.SPEED, "omegaPu.value")
                .containsEntry(MachineQuantity.INIT_MECHANICAL_POWER, "Pm0Pu")
                .containsEntry(MachineQuantity.INIT_STATOR_VOLTAGE, "UStator0Pu")
                // a 1.7.0 machine has no running quantity to offer
                .doesNotContainKey(MachineQuantity.RUNNING);
        assertThat(MachineQuantity.SPEED.isInitialisation()).isFalse();
        assertThat(MachineQuantity.INIT_MECHANICAL_POWER.isInitialisation()).isTrue();
        assertThat(offered.keySet().stream().filter(MachineQuantity::isInitialisation)).hasSize(14);
        assertThat(offered.keySet().stream().filter(q -> !q.isInitialisation())).hasSize(15);
    }

    @Test
    void shouldTellWhatAGovernorAndARegulatorRead() {
        MachineControlUnit govCt2 = catalog.getGovernors().get("GovCt2");
        assertThat(govCt2.getWires()).containsExactly(
                new Wire("PGenPu", MachineQuantity.ACTIVE_POWER, false),
                new Wire("PmPu", MachineQuantity.MECHANICAL_POWER, false),
                new Wire("omegaPu", MachineQuantity.SPEED, false),
                new Wire("PGen0Pu", MachineQuantity.INIT_ACTIVE_POWER, true),
                new Wire("Pm0Pu", MachineQuantity.INIT_MECHANICAL_POWER, true));

        MachineControlUnit st4b = catalog.getVoltageRegulators().get("St4b");
        assertThat(st4b.getWires()).contains(new Wire("ut0Pu", MachineQuantity.INIT_TERMINAL_VOLTAGE, true));
        assertThat(st4b.getInputs()).containsEntry(RegulatorInput.STABILISER, "UPssPu");
    }

    @Test
    void shouldTellWhatPartARegulatorControlPlays() {
        RegulatorControlUnit pss2a = catalog.getRegulatorControls().get("Pss2a");
        assertThat(pss2a.getKind()).hasValue(Kind.PSS);
        assertThat(pss2a.getWatches()).contains(new Watch("omegaPu", MachineQuantity.SPEED, false));
        assertThat(pss2a.getDrives()).containsExactly(new Drive("VPssPu", RegulatorInput.STABILISER, false));

        // a limiter driving more than one input of the regulator is still one limiter
        RegulatorControlUnit uel2c = catalog.getRegulatorControls().get("Uel2c");
        assertThat(uel2c.getKind()).hasValue(Kind.UNDER_EXCITATION_LIMITER);
        assertThat(uel2c.getDrives()).extracting(Drive::input).contains(RegulatorInput.UNDER_EXCITATION, RegulatorInput.FEEDBACK);

        // every control the open catalog ships says what it is
        assertThat(catalog.getRegulatorControls().values()).allSatisfy(c -> assertThat(c.getKind()).isPresent());
    }

    @Test
    void shouldListWhatShipsApartFromWhatAStudyRegistered() {
        MachineControlUnit mine = new MachineControlUnit("governor", "MyLibrary.MyGov", null).named("MyGov");
        try {
            catalog.register(new ControlUnitProvider() {
                @Override
                public List<MachineControlUnit> getGovernors() {
                    return List.of(mine);
                }
            });
            assertThat(catalog.getGovernors()).containsKey("MyGov");
            assertThat(ControlUnitCatalog.shipped().getGovernors()).doesNotContainKey("MyGov").containsKey("GovCt2");
        } finally {
            catalog.resetToBase();
        }
    }

    @Test
    @SuppressWarnings("deprecation")
    void shouldReadTheKindOfAControlDeclaredWithoutOneOffItsId() {
        assertThat(new RegulatorControlUnit("overExcitationLimiter", "M", null).getKind())
                .hasValue(Kind.OVER_EXCITATION_LIMITER);
        assertThat(new RegulatorControlUnit("somethingElse", "M", null).getKind()).isEmpty();
    }
}
