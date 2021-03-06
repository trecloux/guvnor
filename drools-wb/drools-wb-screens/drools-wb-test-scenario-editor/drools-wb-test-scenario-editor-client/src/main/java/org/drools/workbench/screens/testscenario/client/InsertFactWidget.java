/*
 * Copyright 2010 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.drools.workbench.screens.testscenario.client;

import org.drools.workbench.models.testscenarios.shared.ExecutionTrace;
import org.drools.workbench.models.testscenarios.shared.FactData;
import org.drools.workbench.models.testscenarios.shared.Fixture;
import org.drools.workbench.models.testscenarios.shared.FixtureList;
import org.drools.workbench.models.testscenarios.shared.Scenario;
import org.drools.workbench.screens.testscenario.client.resources.i18n.TestScenarioConstants;
import org.kie.workbench.common.widgets.client.popups.errors.ErrorPopup;
import org.kie.workbench.common.services.datamodel.oracle.PackageDataModelOracle;

public class InsertFactWidget extends FactWidget {

    public InsertFactWidget(String factType,
                            FixtureList definitionList,
                            Scenario scenario,
                            PackageDataModelOracle dmo,
                            ScenarioParentWidget parent,
                            ExecutionTrace executionTrace) {
        super(factType,
                definitionList,
                scenario,
                dmo,
                parent,
                executionTrace,
                TestScenarioConstants.INSTANCE.insertForScenario(factType));
    }

    public void onDelete() {
        boolean used = false;

        for (Fixture fixture : definitionList) {
            if (fixture instanceof FactData) {
                final FactData factData = (FactData) fixture;
                if (scenario.isFactDataReferenced(factData)) {
                    used = true;
                    break;
                }
            }
        }

        if (used) {
            ErrorPopup.showMessage(TestScenarioConstants.INSTANCE.CantRemoveThisBlockAsOneOfTheNamesIsBeingUsed());
        } else {
            super.onDelete();
        }
    }
}
