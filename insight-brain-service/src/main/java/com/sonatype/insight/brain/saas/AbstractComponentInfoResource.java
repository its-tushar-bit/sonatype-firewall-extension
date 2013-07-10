/*
 * Copyright (c) 2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.saas;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sonatype.clm.dto.model.License;
import com.sonatype.clm.dto.model.SecurityVulnerability;
import com.sonatype.clm.dto.model.ide.ComponentDetails;
import com.sonatype.clm.dto.model.ide.ComponentDetailsList;
import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.component.ComponentDAO;
import com.sonatype.insight.brain.dataaccess.component.HashGAVDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseDAO;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.HashGAV;
import com.sonatype.insight.brain.model.component.IdentificationSource;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.component.SecurityVulnerabilityStatus;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.license.MultiLicense;
import com.sonatype.insight.brain.model.policy.stages.DevelopStageType;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluator;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.error.exception.NotFoundException;

public abstract class AbstractComponentInfoResource
{
    private static final Logger log = LoggerFactory.getLogger( AbstractComponentInfoResource.class );

    private ApplicationDAO applicationDAO = new ApplicationDAO();

    private LicenseDAO licenseDAO = new LicenseDAO();

    private PolicyEvaluator evaluator = new PolicyEvaluator();

    @Context
    private SaasClient client;

    @Context
    private InsightWork work;

    @Context
    private HttpServletRequest request;

    @GET
    @Path( "versions/{applicationPublicId}" )
    @Produces( MediaType.APPLICATION_JSON )
    /**
     * @deprecated Used by eclipse plugin < 2.1.1, ci plugin < 2.8
     */
    public Response getComponentVersionDetails( @PathParam( "applicationPublicId" ) String applicationPublicId,
                                                @QueryParam( "instanceId" ) String instanceId,
                                                @QueryParam( "groupId" ) String groupId,
                                                @QueryParam( "artifactId" ) String artifactId,
                                                @QueryParam( "version" ) String version )
        throws IOException
    {
        log.debug( "Getting {} component version details for application id {}, GAV {}:{}:{}.", getToolName(),
                   applicationPublicId, groupId, artifactId, version );
        return client.doProxy( request, "rest/ide/component/details/versions" );
    }

    @GET
    @Path( "list/{applicationPublicId}" )
    @Produces( MediaType.APPLICATION_JSON )
    public ComponentDetailsList getComponentDetailsList( @PathParam( "applicationPublicId" ) String applicationPublicId,
                                                         @QueryParam( "groupId" ) String groupId,
                                                         @QueryParam( "artifactId" ) String artifactId,
                                                         @QueryParam( "version" ) String version )
        throws IOException
    {
        long start = System.currentTimeMillis();

        log.debug( "Getting {} component details list for application id {}, GAV {}:{}:{}.", getToolName(),
                   applicationPublicId, groupId, artifactId, version );
        Application app = applicationDAO.getByPublicIdNotNull( applicationPublicId );
        String applicationId = app.getId();

        ComponentDetailsList componentDetailsList =
            client.get( request, ComponentDetailsList.class, "rest/ide/component/details/list" );

        for ( ComponentDetails componentDetails : componentDetailsList.getList() )
        {
            loadComponent( applicationId, componentDetails );
        }

        log.debug( "Loaded component details list for {}:{}:{} in {} ms.", groupId, artifactId, version,
                   System.currentTimeMillis() - start );

        return componentDetailsList;
    }

    @GET
    @Path( "{applicationPublicId}" )
    @Produces( MediaType.APPLICATION_JSON )
    public ComponentDetails getComponentDetails( @PathParam( "applicationPublicId" ) String applicationPublicId,
                                                 @QueryParam( "instanceId" ) String instanceId,
                                                 @QueryParam( "groupId" ) String groupId,
                                                 @QueryParam( "artifactId" ) String artifactId,
                                                 @QueryParam( "version" ) String version,
                                                 @QueryParam( "hash" ) String hash,
                                                 @QueryParam( "matchState" ) String matchState,
                                                 @QueryParam( "proprietary" ) boolean proprietary )
        throws IOException
    {
        long start = System.currentTimeMillis();

        log.debug( "Getting {} component details for application id {}, GAV {}:{}:{}, hash {}.", getToolName(),
                   applicationPublicId, groupId, artifactId, version, hash );
        Application app = applicationDAO.getByPublicIdNotNull( applicationPublicId );
        String applicationId = app.getId();

        // Get component details from the SAAS server
        ComponentDetails componentDetails;
        try
        {
            componentDetails =
                client.get( request, ComponentDetails.class, "rest/ide/component/details" );
        }
        catch ( NotFoundException e )
        {
            // GAV is unknown to SaaS, still want to provide minimal data for details view
            componentDetails = new ComponentDetails( groupId, artifactId, version );
        }

        if ( hash != null && !hash.isEmpty() )
        {
            componentDetails.setHash( hash );
        }
        if ( matchState != null && !matchState.isEmpty() )
        {
            componentDetails.setMatchState( matchState );
        }
        else
        {
            componentDetails.setMatchState( MatchState.EXACT.getId() );
        }

        // Is this a manually claimed component?
        HashGAV hashGAV = null;
        if ( componentDetails.getHash() != null )
        {
            hashGAV = new HashGAVDAO().getByHash( componentDetails.getHash() );
        }
        if ( hashGAV != null )
        {
            componentDetails.setGroupId( hashGAV.getGroupId() );
            componentDetails.setArtifactId( hashGAV.getArtifactId() );
            componentDetails.setVersion( hashGAV.getVersion() );
            componentDetails.setMatchState( MatchState.EXACT.getId() );
            componentDetails.setCatalogDate( hashGAV.getCreateTimeLong() );
            componentDetails.setIdentificationSource( IdentificationSource.MANUAL.getId() );
            componentDetails.setIdentificationSourceComment( hashGAV.getComment() );
        }
        else
        {
            componentDetails.setIdentificationSource( IdentificationSource.SONATYPE.getId() );
        }

        Component component = loadComponent( applicationId, componentDetails );
        component.setProprietary( proprietary );

        // Evaluate the policies
        List<PolicyAlert> policyAlerts =
            evaluator.evaluate( applicationId, new Stage( DevelopStageType.ID ), policyDAO(),
                                Collections.singletonList( component ) );
        componentDetails.setPolicyAlerts( policyAlerts );

        log.debug( "Loaded component details for {}:{}:{}, hash {}, in {} ms.", groupId, artifactId, version, hash,
                   System.currentTimeMillis() - start );

        return componentDetails;
    }

    private Component loadComponent( String applicationId, ComponentDetails componentDetails )
        throws IOException
    {
        // Load the augmented data for licenses and security vulnerabilities
        ObjectNode licenseData =
            AugmentUtil.getLicenseData( work, applicationId, componentDetails.getGroupId(),
                                        componentDetails.getArtifactId(), componentDetails.getVersion() );
        ArrayNode svData =
            AugmentUtil.getSVData( work, applicationId, componentDetails.getGroupId(),
                                   componentDetails.getArtifactId(), componentDetails.getVersion(),
                                   componentDetails.getSecurityVulnerabilities() );
        ComponentDAO componentDAO = new ComponentDAO();
        Component component = componentDAO.getComponent( applicationId, componentDetails, licenseData, svData );

        // Use CLM data to populate the component details
        for ( String overriddenLicenseId : component.getOverriddenLicenseIds() )
        {
            com.sonatype.insight.brain.model.license.License overriddenLicense =
                licenseDAO.getByIdNotNull( overriddenLicenseId );
            componentDetails.getOverriddenLicenses().add( new License( overriddenLicense.getId(),
                                                                       overriddenLicense.getShortDisplayName() ) );
        }
        if ( !component.getLicenseThreatGroups().isEmpty() )
        {
            int licenseThreatLevel = 0;
            for ( LicenseThreatGroup licenseThreatGroup : component.getLicenseThreatGroups() )
            {
                licenseThreatLevel = Math.max( licenseThreatLevel, licenseThreatGroup.getThreatLevel() );
            }
            componentDetails.setLicenseThreatLevel( licenseThreatLevel );
        }
        if ( componentDetails.getSecurityVulnerabilities() != null )
        {
            for ( SecurityVulnerability issue : componentDetails.getSecurityVulnerabilities() )
            {
                issue.setStatus( SecurityVulnerabilityStatus.OPEN.getName() );
                for ( com.sonatype.insight.brain.model.component.SecurityVulnerability sv : component.getSecurityVulnerabilities() )
                {
                    if ( issue.getRefId().equals( sv.getRefId() ) && issue.getSource().equals( sv.getSource() ) )
                    {
                        issue.setStatus( sv.getStatus().getName() );
                        break;
                    }
                }
            }
        }
        return component;
    }

    private PolicyDAO policyDAO()
    {
        return new PolicyDAO( work.getWorkDir() );
    }

    @GET
    @Path( "selectableLicenses/{applicationPublicId}" )
    @Produces( { MediaType.APPLICATION_JSON } )
    public Set<License> getSelectableLicenses( @PathParam( "applicationPublicId" ) String applicationPublicId,
                                               @QueryParam( "instanceId" ) String instanceId,
                                               @QueryParam( "groupId" ) String groupId,
                                               @QueryParam( "artifactId" ) String artifactId,
                                               @QueryParam( "version" ) String version )
        throws IOException
    {
        applicationDAO.getByPublicIdNotNull( applicationPublicId );

        // Get component details from the SAAS server
        ComponentDetails componentDetails =
            client.get( request, ComponentDetails.class, "rest/ide/component/details" );

        MultiLicenseDAO multiLicenseDAO = new MultiLicenseDAO();
        Set<License> result = new LinkedHashSet<License>();
        Set<License> licenses = new LinkedHashSet<License>();
        licenses.addAll( componentDetails.getDeclaredLicenses() );
        licenses.addAll( componentDetails.getObservedLicenses() );
        Iterator<License> licenseIter = licenses.iterator();
        while ( licenseIter.hasNext() )
        {
            License license = licenseIter.next();
            MultiLicense multiLicense = multiLicenseDAO.getById( license.getLicenseId() );
            if ( multiLicense.isUnspecified() )
            {
                continue;
            }
            Set<com.sonatype.insight.brain.model.license.License> _licenses =
                multiLicenseDAO.getLicensesByMultiLicenseId( multiLicense.getId() );
            for ( com.sonatype.insight.brain.model.license.License _license : _licenses )
            {
                if ( _license.getId().endsWith( "-UNSPECIFIED" ) )
                {
                    String licenseIdPrefix =
                        _license.getId().substring( 0, _license.getId().length() - "UNSPECIFIED".length() );
                    for ( com.sonatype.insight.brain.model.license.License otherLicense : licenseDAO.getAll() )
                    {
                        if ( otherLicense.getId().startsWith( licenseIdPrefix )
                            && !_license.getId().equals( otherLicense.getId() ) )
                        {
                            result.add( new License( otherLicense.getId(), otherLicense.getShortDisplayName() ) );
                        }
                    }
                }
                result.add( new License( _license.getId(), _license.getShortDisplayName() ) );
            }
        }
        return result;
    }

    protected abstract String getToolName();
}
