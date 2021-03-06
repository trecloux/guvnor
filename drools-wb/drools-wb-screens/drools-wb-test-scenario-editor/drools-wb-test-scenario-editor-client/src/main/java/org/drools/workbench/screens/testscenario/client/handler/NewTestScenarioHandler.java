package org.drools.workbench.screens.testscenario.client.handler;

import com.google.gwt.core.client.Callback;
import com.google.gwt.user.client.ui.IsWidget;
import org.drools.workbench.models.testscenarios.shared.Scenario;
import org.jboss.errai.bus.client.api.RemoteCallback;
import org.jboss.errai.ioc.client.api.Caller;
import org.kie.workbench.common.widgets.client.callbacks.HasBusyIndicatorDefaultErrorCallback;
import org.kie.workbench.common.widgets.client.handlers.DefaultNewResourceHandler;
import org.kie.workbench.common.widgets.client.handlers.NewResourcePresenter;
import org.kie.workbench.common.widgets.client.resources.i18n.CommonConstants;
import org.kie.workbench.common.widgets.client.widget.BusyIndicatorView;
import org.drools.workbench.screens.testscenario.client.TestScenarioResourceType;
import org.drools.workbench.screens.testscenario.client.resources.i18n.TestScenarioConstants;
import org.drools.workbench.screens.testscenario.client.resources.images.TestScenarioAltedImages;
import org.drools.workbench.screens.testscenario.service.ScenarioTestEditorService;
import org.uberfire.backend.vfs.Path;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class NewTestScenarioHandler
        extends DefaultNewResourceHandler {

    @Inject
    private BusyIndicatorView busyIndicatorView;

    @Inject
    private Caller<ScenarioTestEditorService> service;

    @Inject
    private TestScenarioResourceType resourceType;

    @Override
    public String getDescription() {
        return TestScenarioConstants.INSTANCE.NewTestScenarioDescription();
    }

    @Override
    public IsWidget getIcon() {
        return TestScenarioAltedImages.INSTANCE.TestScenario();
    }

    @Override
    public void create( Path context,
                        String baseFileName,
                        NewResourcePresenter presenter ) {
        busyIndicatorView.showBusyIndicator( CommonConstants.INSTANCE.Saving() );

        service.call(
                getSuccessCallback( presenter ),
                new HasBusyIndicatorDefaultErrorCallback( busyIndicatorView )
                    ).create( context,
                              buildFileName( resourceType, baseFileName ),
                              new Scenario(),
                              "" );
    }

    @Override
    public void acceptPath( final Path path,
                            final Callback<Boolean, Void> callback ) {
        if ( path == null ) {
            callback.onSuccess( false );
        } else {
            projectService.call( new RemoteCallback<Path>() {
                @Override
                public void callback( final Path path ) {
                    callback.onSuccess( path != null );
                }
            } ).resolveTestPackage( path );
        }
    }
}
