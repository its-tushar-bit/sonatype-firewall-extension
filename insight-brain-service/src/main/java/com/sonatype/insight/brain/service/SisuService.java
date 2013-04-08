/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.guice.bean.binders.SpaceModule;
import org.sonatype.guice.bean.binders.WireModule;
import org.sonatype.guice.bean.locators.BeanLocator;
import org.sonatype.guice.bean.reflect.ClassSpace;
import org.sonatype.guice.bean.reflect.URLClassSpace;
import org.sonatype.inject.BeanEntry;
import org.sonatype.inject.BeanScanning;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.sun.jersey.spi.inject.InjectableProvider;
import com.yammer.dropwizard.Service;
import com.yammer.dropwizard.config.Configuration;
import com.yammer.dropwizard.config.Environment;
import com.yammer.dropwizard.lifecycle.Managed;
import com.yammer.dropwizard.tasks.Task;
import com.yammer.metrics.core.HealthCheck;

/**
 * Local copy of SisuService from https://github.com/tesla/dropwizard-sisu with various tweaks for CLM.
 */
@SuppressWarnings( { "unchecked", "rawtypes" } )
public abstract class SisuService<T extends Configuration>
    extends Service<T>
{
    private static final Logger logger = LoggerFactory.getLogger( SisuService.class );

    @Override
    public void run( T configuration, Environment environment )
        throws Exception
    {
        Injector injector = createInjector( configuration );
        injector.injectMembers( this );
        runWithInjector( configuration, environment, injector );
    }

    private Injector createInjector( final T configuration )
    {
        List<Module> modules = new ArrayList<Module>();

        modules.add( new AbstractModule()
        {
            @Override
            protected void configure()
            {
                bind( (Class) configuration.getClass() ).toInstance( configuration );
            }
        } );

        modules.addAll( modules( configuration ) );

        ClassSpace space = new URLClassSpace( getClass().getClassLoader() );
        modules.add( new SpaceModule( space, scanning( configuration ) ) );

        return Guice.createInjector( new WireModule( modules ) );
    }

    //
    // Allow the application to customize the scanning
    //
    protected BeanScanning scanning( T configuration )
    {
        return BeanScanning.ON;
    }

    //
    // Allow the application to customize the modules
    //
    protected List<Module> modules( T configuration )
    {
        return Collections.emptyList();
    }

    //
    // Allow the application to customize the environment
    //
    protected void customize( T configuration, Environment environment )
    {
    }

    private void runWithInjector( T configuration, Environment environment, Injector injector )
    {
        customize( configuration, environment );
        BeanLocator locator = injector.getInstance( BeanLocator.class );
        addHealthChecks( environment, locator );
        addProviders( environment, locator );
        addInjectableProviders( environment, locator );
        addResources( environment, locator );
        addTasks( environment, locator );
        addManaged( environment, locator );
    }

    private static void addManaged( Environment environment, BeanLocator locator )
    {
        for ( BeanEntry<Annotation, Managed> managedBeanEntry : locator.locate( Key.get( Managed.class ) ) )
        {
            Managed managed = managedBeanEntry.getValue();
            environment.manage( managed );
            logger.debug( "Added managed: {}", managed );
        }
    }

    private static void addTasks( Environment environment, BeanLocator locator )
    {
        for ( BeanEntry<Annotation, Task> taskBeanEntry : locator.locate( Key.get( Task.class ) ) )
        {
            Task task = taskBeanEntry.getValue();
            environment.addTask( task );
            logger.debug( "Added task: {}", task );
        }
    }

    private static void addHealthChecks( Environment environment, BeanLocator locator )
    {
        for ( BeanEntry<Annotation, HealthCheck> healthCheckBeanEntry : locator.locate( Key.get( HealthCheck.class ) ) )
        {
            HealthCheck healthCheck = healthCheckBeanEntry.getValue();
            environment.addHealthCheck( healthCheck );
            logger.debug( "Added healthCheck: {}", healthCheck );
        }
    }

    private static void addInjectableProviders( Environment environment, BeanLocator locator )
    {
        for ( BeanEntry<Annotation, InjectableProvider> injectableProviderBeanEntry : locator.locate( Key.get( InjectableProvider.class ) ) )
        {
            InjectableProvider injectableProvider = injectableProviderBeanEntry.getValue();
            environment.addProvider( injectableProvider );
            logger.debug( "Added injectableProvider: {}", injectableProvider );
        }
    }

    private static void addProviders( Environment environment, BeanLocator locator )
    {
        for ( BeanEntry<Annotation, Provider> providerBeanEntry : locator.locate( Key.get( Provider.class ) ) )
        {
            Provider provider = providerBeanEntry.getValue();
            environment.addProvider( provider );
            logger.debug( "Added provider: {}", provider );
        }
    }

    private static void addResources( Environment environment, BeanLocator locator )
    {
        //
        // Unfortunately @Path is not a qualifier in JSR330, so we need to check all known bindings.
        // (In practice this isn't that slow because of various caches in Sisu to optimize lookups.)
        // We could always optimize this by introducing a marker interface for injectable resources.
        //
        for ( BeanEntry<Annotation, Object> resourceBeanEntry : locator.locate( Key.get( Object.class ) ) )
        {
            Class<?> impl = resourceBeanEntry.getImplementationClass();
            if ( impl != null && impl.isAnnotationPresent( Path.class ) )
            {
                try
                {
                    Object resource = resourceBeanEntry.getValue();
                    environment.addResource( resource );
                    logger.debug( "Added resource: {}", resource );
                }
                catch ( Exception e )
                {
                    logger.warn( "Unable to add resource: {}", impl, e );
                }
            }
        }
    }
}
