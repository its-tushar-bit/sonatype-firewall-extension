package com.sonatype.insight.brain.product.license;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.service.InsightBrainService;
import com.sun.jersey.api.model.AbstractMethod;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import com.sun.jersey.spi.container.ResourceFilter;
import com.sun.jersey.spi.container.ResourceFilterFactory;

@Named
public class LicenseAwareContainerResourceFilterFactory
    implements ResourceFilterFactory
{
    @Inject
    private CLMLicenseManager licenseManager;

    private class Filter
        implements ResourceFilter, ContainerRequestFilter
    {
        private final CLMLicenseManager licenseManager;

        private final Set<CLMEnforcementPoint> enforcementPoints;

        private final Logger log = LoggerFactory.getLogger( Filter.class );

        @Context
        private BaseUrl baseUrl;

        public Filter( CLMLicenseManager licenseManager, Set<CLMEnforcementPoint> enforcementPoints )
        {
            this.licenseManager = licenseManager;
            this.enforcementPoints = enforcementPoints;
        }

        @Override
        public ContainerRequest filter( ContainerRequest request )
        {
            String path = request.getPath();

            try
            {
                boolean passed = enforcementPoints.isEmpty();
                licenseManager.validate();
                for ( CLMEnforcementPoint enforcementPoint : enforcementPoints )
                {
                    try
                    {
                        licenseManager.validateEnforcementPoint( enforcementPoint );
                        passed = true;
                        break;
                    }
                    catch ( InvalidLicenseException e )
                    {
                        log.debug( "EnforcementPoint " + enforcementPoint.name() + " NOT licensed." );
                    }
                }

                if ( !passed )
                {
                    throw new InvalidLicenseException( "Unable to validate the license." );
                }
            }
            catch ( InvalidLicenseException e )
            {
                log.error( e.getMessage(), e );

                if ( path.equals( InsightBrainService.APPLICATION_ASSET_PATH + "index.html" )
                    || path.equals( InsightBrainService.POLICY_ASSET_PATH + "index.html" ) )
                {
                    throw new WebApplicationException(
                                                       Response.seeOther( baseUrl.redirect().path( InsightBrainService.UNLICENSED_ASSET_PATH ).path( "index.html" ).build() ).build() );
                }
                else
                {
                    throw e;
                }
            }

            return request;
        }

        @Override
        public ContainerRequestFilter getRequestFilter()
        {
            return this;
        }

        @Override
        public ContainerResponseFilter getResponseFilter()
        {
            return null;
        }
    }

    @Override
    public List<ResourceFilter> create( AbstractMethod am )
    {
        if ( am.isAnnotationPresent( UnlicensedPath.class )
            || am.getMethod().getDeclaringClass().isAnnotationPresent( UnlicensedPath.class ) )
        {
            // unlicensed, so no filter necessary
            return null;
        }

        Set<CLMEnforcementPoint> enforcementPoints = new HashSet<CLMEnforcementPoint>();

        ProductLicenseEnforcementPoint ep = am.getAnnotation( ProductLicenseEnforcementPoint.class );

        if ( ep != null )
        {
            enforcementPoints.addAll( Arrays.asList( ep.value() ) );
        }

        ep = am.getMethod().getDeclaringClass().getAnnotation( ProductLicenseEnforcementPoint.class );

        if ( ep != null )
        {
            enforcementPoints.addAll( Arrays.asList( ep.value() ) );
        }

        return Collections.<ResourceFilter> singletonList( new Filter( licenseManager, enforcementPoints ) );
    }
}
