package br.com.andrewribeiro.ribrest.services.cdi;

import br.com.andrewribeiro.ribrest.services.cdi.annotations.RequestScope;
import br.com.andrewribeiro.ribrest.services.orm.EMFFactory;
import br.com.andrewribeiro.ribrest.services.orm.EMFactory;
import br.com.andrewribeiro.ribrest.services.dtos.FlowContainer;
import br.com.andrewribeiro.ribrest.services.orm.PersistenceUnitWrapper;
import javax.inject.Singleton;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.glassfish.hk2.api.Context;
import org.glassfish.hk2.api.DynamicConfiguration;
import org.glassfish.hk2.api.DynamicConfigurationService;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.BuilderHelper;

/**
 *
 * @author Andrew Ribeiro
 */
public class RibrestSLPopulator {

    public static void populate(ServiceLocator locator) {
        DynamicConfigurationService dcs = locator.getService(DynamicConfigurationService.class);
        DynamicConfiguration config = dcs.createDynamicConfiguration();

        config.bind(BuilderHelper.link(EMFFactory.class)
                .to(EntityManagerFactory.class)
                .in(Singleton.class)
                .buildFactory());

        config.bind(BuilderHelper.link(EMFactory.class)
                .to(EntityManager.class)
                .in(RequestScope.class.getName())
                .buildFactory());

        config.bind(BuilderHelper.link(PersistenceUnitWrapper.class)
                .in(Singleton.class)
                .build());

        config.bind(BuilderHelper.link(FlowContainer.class).
                in(RequestScope.class.getName()).
                build());

        config.bind(BuilderHelper.link(RequestContext.class).
                to(Context.class).
                in(Singleton.class.getName()).
                build());


        config.commit();
    }

}
