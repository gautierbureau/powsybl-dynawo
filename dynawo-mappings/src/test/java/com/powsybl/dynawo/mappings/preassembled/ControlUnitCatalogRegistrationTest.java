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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Registers controls at run time, the way a study describing a control nothing ships would.
 * <p>
 * The catalog is a JVM-wide singleton, so every test puts it back as it found it.
 *
 * @author Gautier Bureau {@literal <gautier.bureau at rte-france.com>}
 */
class ControlUnitCatalogRegistrationTest {

    private static final String GOVERNORS = "Dynawo.Electrical.Controls.Machines.Governors.";
    private static final String STABILISERS = "Dynawo.Electrical.Controls.Machines.PowerSystemStabilizers.";
    private static final String MY_LIBRARY = "MyLibrary.Stabilisers.";

    private final ControlUnitCatalog catalog = ControlUnitCatalog.getInstance();

    @AfterEach
    void resetCatalog() {
        catalog.resetToBase();
    }

    /**
     * GovCt2 as the open catalog wires it, under a name of its own so it is a control nobody knows.
     */
    private static MachineControlUnit myGovernor() {
        return new MachineControlUnit("governor", GOVERNORS + "Standard.Generic.GovCt2", GOVERNORS + "GovernorPmPGen_INIT")
                .named("MyGov")
                .reading("PGenPu", MachineQuantity.ACTIVE_POWER)
                .reading("PmPu", MachineQuantity.MECHANICAL_POWER)
                .reading("omegaPu", MachineQuantity.SPEED)
                .startingFrom("PGen0Pu", MachineQuantity.INIT_ACTIVE_POWER)
                .startingFrom("Pm0Pu", MachineQuantity.INIT_MECHANICAL_POWER);
    }

    /**
     * A stabiliser of a library of one's own, wired as Pss2a is.
     */
    private static RegulatorControlUnit myStabiliser() {
        return new RegulatorControlUnit("powerSystemStabilizer", MY_LIBRARY + "MyPss", STABILISERS + "Pss_INIT")
                .reading("PGenPu", MachineQuantity.ACTIVE_POWER)
                .reading("omegaPu", MachineQuantity.SPEED)
                .startingFrom("PGen0Pu", MachineQuantity.INIT_ACTIVE_POWER)
                .driving("VPssPu", RegulatorInput.STABILISER);
    }

    private static ControlUnitProvider governors(MachineControlUnit... units) {
        return new ControlUnitProvider() {
            @Override
            public List<MachineControlUnit> getGovernors() {
                return List.of(units);
            }
        };
    }

    @Test
    void shouldDesignAModelWithARegisteredGovernor() {
        GeneratorModelDesigner designer = new GeneratorModelDesigner(catalog, ModelNaming.DYNAWO_1_7_0);
        GeneratorControls controls = new GeneratorControls("MyGov", "St4b");
        assertThat(designer.design(controls, Windings.FOUR_WINDINGS, false, false))
                .as("a governor nobody described")
                .isEmpty();

        catalog.register(governors(myGovernor()));

        Optional<PreassembledModel> model = designer.design(controls, Windings.FOUR_WINDINGS, false, false);
        assertThat(model).isPresent();
        assertThat(model.get().getId()).isEqualTo("GeneratorSynchronousFourWindingsMyGovSt4b");
        // wired as GovCt2 is, the machine's speed read through its connector in 1.7.0
        assertThat(model.get().getConnections())
                .contains(new UnitConnection("governor", "omegaPu", "generator", "omegaPu.value", false));
    }

    @Test
    void shouldDesignAModelWithARegisteredStabiliser() {
        catalog.register(new ControlUnitProvider() {
            @Override
            public List<RegulatorControlUnit> getRegulatorControls() {
                return List.of(myStabiliser());
            }
        });

        // registered under the last part of its model, the name an extension gives it
        assertThat(catalog.getRegulatorControl("MyPss")).isPresent();
        Optional<PreassembledModel> model = new GeneratorModelDesigner(catalog, ModelNaming.DYNAWO_1_7_0)
                .design(new GeneratorControls("GovCt2", "St4b", "MyPss"), Windings.FOUR_WINDINGS, false, false);
        assertThat(model).isPresent();
        assertThat(model.get().getId()).isEqualTo("GeneratorSynchronousFourWindingsGovCt2St4bMyPss");
        // its output meets the input St4b names for a stabiliser
        assertThat(model.get().getConnections())
                .contains(new UnitConnection("powerSystemStabilizer", "VPssPu", "voltageRegulator", "UPssPu", false));
    }

    @Test
    void shouldKeepAKnownControlOnRegister() {
        MachineControlUnit shipped = catalog.getGovernor("GovCt2").orElseThrow();
        MachineControlUnit redefined = myGovernor().named("GovCt2");

        catalog.register(governors(redefined));

        assertThat(catalog.getGovernor("GovCt2")).containsSame(shipped);
    }

    @Test
    void shouldReplaceAKnownControlOnOverride() {
        MachineControlUnit shipped = catalog.getGovernor("GovCt2").orElseThrow();
        MachineControlUnit redefined = myGovernor().named("GovCt2");

        catalog.override(governors(redefined));
        assertThat(catalog.getGovernor("GovCt2")).containsSame(redefined);

        catalog.resetToBase();
        assertThat(catalog.getGovernor("GovCt2")).containsSame(shipped);
    }

    @Test
    void shouldForgetRegisteredControlsOnReset() {
        catalog.register(governors(myGovernor()));
        assertThat(catalog.getGovernor("MyGov")).isPresent();

        catalog.resetToBase();

        assertThat(catalog.getGovernor("MyGov")).isEmpty();
        assertThat(catalog.getGovernor("GovCt2")).as("what ships stays").isPresent();
    }
}
