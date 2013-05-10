/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

import org.codehaus.plexus.util.IOUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupLicenseDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.license.LicenseThreatGroupLicense;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.conditions.LabelConditionType;
import com.sonatype.insight.brain.model.policy.conditions.LicenseThreatGroupConditionType;
import com.sonatype.insight.brain.product.license.CLMLicenseManager;
import com.sonatype.insight.brain.service.BaseUrl;
import com.sonatype.insight.brain.service.InsightBrainService;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.client.utils.AuditUtils;
import com.sonatype.insight.error.exception.PaymentRequiredException;
import com.sonatype.insight.json.store.JsonUtils;

@Named
@Path( PolicyResource.SERVICE_PATH )
public class PolicyResource
{
    public static final String SERVICE_PATH = "rest/policy/{policyOwnerId}";

    private static final Logger log = LoggerFactory.getLogger( PolicyResource.class );

    @Context
    private InsightWork work;

    @Context
    private BaseUrl baseUrl;

    @Inject
    private CLMLicenseManager licenseManager;

    static String getInternalPolicyOwnerId( String policyOwnerId )
    {
        Application application = new ApplicationDAO().getByPublicIdNotNull( policyOwnerId );
        return application.getId();
    }

    @GET
    @Produces( MediaType.APPLICATION_JSON )
    public List<Policy> getPolicies( @PathParam( "policyOwnerId" ) final String policyOwnerId )
    {
        log.debug( "Received request to get all policies for policyOwnerId {}", policyOwnerId );

        String internalPolicyOwnerId = getInternalPolicyOwnerId( policyOwnerId );

        return policyDAO().getByOwnerId( internalPolicyOwnerId );
    }

    @POST
    @Consumes( MediaType.APPLICATION_JSON )
    @Produces( MediaType.APPLICATION_JSON )
    public Policy addPolicy( @PathParam( "policyOwnerId" ) final String policyOwnerId, final Policy policy,
                             @QueryParam( "user" ) final String user, @QueryParam( "where" ) final String where,
                             @Context final HttpServletRequest request )
    {
        log.debug( "Received request to add policy for policyOwnerId {}", policyOwnerId );

        String internalPolicyOwnerId = getInternalPolicyOwnerId( policyOwnerId );

        return policyDAO().session( user, AuditUtils.findIP( request ), where ).insert( internalPolicyOwnerId, policy );
    }

    @PUT
    @Consumes( MediaType.APPLICATION_JSON )
    @Produces( MediaType.APPLICATION_JSON )
    public Policy updatePolicy( @PathParam( "policyOwnerId" ) final String policyOwnerId, final Policy policy,
                                @QueryParam( "user" ) final String user, @QueryParam( "where" ) final String where,
                                @Context final HttpServletRequest request )
    {
        log.debug( "Received request to update policy for policyOwnerId {}, policyId {}", policyOwnerId, policy.getId() );

        String internalPolicyOwnerId = getInternalPolicyOwnerId( policyOwnerId );

        return policyDAO().session( user, AuditUtils.findIP( request ), where ).update( internalPolicyOwnerId, policy );
    }

    @DELETE
    @Path( "{policyId}" )
    public void deletePolicy( @PathParam( "policyOwnerId" ) final String policyOwnerId,
                              @PathParam( "policyId" ) final String policyId, @QueryParam( "user" ) final String user,
                              @QueryParam( "where" ) final String where, @Context final HttpServletRequest request )
    {
        log.debug( "Received request to delete policy for policyOwnerId {}, policyId {}", policyOwnerId, policyId );

        String internalPolicyOwnerId = getInternalPolicyOwnerId( policyOwnerId );

        policyDAO().session( user, AuditUtils.findIP( request ), where ).delete( internalPolicyOwnerId, policyId );
    }

    @GET
    @Path( "export" )
    @Produces( MediaType.APPLICATION_JSON )
    public PolicyExportResult exportPolicies( @PathParam( "policyOwnerId" ) String policyOwnerId )
        throws IOException
    {
        String internalPolicyOwnerId = getInternalPolicyOwnerId( policyOwnerId );

        ExportDTO exportDTO = new ExportDTO();
        exportDTO.policies = policyDAO().getByOwnerId( internalPolicyOwnerId );
        exportDTO.labels = new LabelDAO().getByApplicationId( internalPolicyOwnerId );
        exportDTO.licenseThreatGroups = new LicenseThreatGroupDAO().getByApplicationId( internalPolicyOwnerId );
        exportDTO.licenseThreatGroupLicenses =
            new LicenseThreatGroupLicenseDAO().getByApplicationId( internalPolicyOwnerId );
        File exportDir = new File( new File( work.getWorkDir(), "export" ), internalPolicyOwnerId );
        exportDir.mkdirs();
        File exportFile = File.createTempFile( "policy-export-", ".json", exportDir );
        JsonUtils.write( exportFile, exportDTO );

        PolicyExportResult result = new PolicyExportResult();
        result.filename = exportFile.getAbsolutePath();

        return result;
    }

    @PUT
    @Path( "import" )
    @Produces( MediaType.APPLICATION_JSON )
    public PolicyImportResult importPolicies( @PathParam( "policyOwnerId" ) String policyOwnerId,
                                              @Context HttpServletRequest servletRequest )
        throws IOException
    {
        byte[] importBytes;
        InputStream importInputStream = servletRequest.getInputStream();
        try
        {
            importBytes = IOUtil.toByteArray( importInputStream );
        }
        finally
        {
            IOUtil.close( importInputStream );
        }
        ExportDTO exportDTO = JsonUtils.parse( importBytes, ExportDTO.class );

        // Create an application
        ApplicationDAO applicationDAO = new ApplicationDAO();
        Application application = new Application();
        application.setPublicId( policyOwnerId );
        application.setName( policyOwnerId );
        if ( applicationDAO.getByName( application.getName() ) != null )
        {
            application.setName( application.getName() + " " + System.currentTimeMillis() );
        }

        EntityManager em = applicationDAO.createEntityManager();
        try
        {
            em.getTransaction().begin();

            int appLimit = licenseManager.getApplicationCountLimit();
            if ( applicationDAO.getAll( em ).size() >= appLimit )
            {
                throw new PaymentRequiredException( "You have exceeded the licensed limit of " + appLimit
                    + " applications." );
            }
            applicationDAO.insert( em, application, false /* createLicenseThreatGroups */);
            String applicationId = application.getId();

            LabelDAO labelDAO = new LabelDAO();
            if ( exportDTO.labels.size() > 0 )
            {
                Map<String, String> idMap = new HashMap<String, String>();
                for ( Label label : exportDTO.labels )
                {
                    String oldId = label.getId();
                    label.setId( null );
                    label.setApplicationId( applicationId );
                    labelDAO.insert( em, label );
                    idMap.put( oldId, label.getId() );
                }
                for ( Policy policy : exportDTO.policies )
                {
                    for ( Constraint constraint : policy.getConstraints() )
                    {
                        for ( Condition condition : constraint.getConditions() )
                        {
                            if ( LabelConditionType.ID.equals( condition.getConditionTypeId() ) )
                            {
                                condition.setValue( idMap.get( condition.getValue() ) );
                            }
                        }
                    }
                }
            }
            if ( exportDTO.licenseThreatGroups.size() > 0 )
            {
                Map<String, String> idMap = new HashMap<String, String>();
                LicenseThreatGroupDAO licenseThreatGroupDAO = new LicenseThreatGroupDAO();
                for ( LicenseThreatGroup licenseThreatGroup : exportDTO.licenseThreatGroups )
                {
                    String oldId = licenseThreatGroup.getId();
                    licenseThreatGroup.setId( null );
                    licenseThreatGroup.setApplicationId( applicationId );
                    licenseThreatGroupDAO.insert( em, licenseThreatGroup );
                    idMap.put( oldId, licenseThreatGroup.getId() );
                }
                LicenseThreatGroupLicenseDAO licenseThreatGroupLicenseDAO = new LicenseThreatGroupLicenseDAO();
                for ( LicenseThreatGroupLicense licenseThreatGroupLicense : exportDTO.licenseThreatGroupLicenses )
                {
                    licenseThreatGroupLicense.setId( null );
                    licenseThreatGroupLicense.setApplicationId( applicationId );
                    licenseThreatGroupLicense.setLicenseThreatGroupId( idMap.get( licenseThreatGroupLicense.getLicenseThreatGroupId() ) );
                    licenseThreatGroupLicenseDAO.insert( em, licenseThreatGroupLicense );
                }
                for ( Policy policy : exportDTO.policies )
                {
                    for ( Constraint constraint : policy.getConstraints() )
                    {
                        for ( Condition condition : constraint.getConditions() )
                        {
                            if ( LicenseThreatGroupConditionType.ID.equals( condition.getConditionTypeId() ) )
                            {
                                condition.setValue( idMap.get( condition.getValue() ) );
                            }
                        }
                    }
                }
            }
            em.getTransaction().commit();

            PolicyDAO policyDAO = policyDAO();
            for ( Policy policy : exportDTO.policies )
            {
                policyDAO.insert( application.getId(), policy );
            }
        }
        finally
        {
            ApplicationDAO.close( em );
        }

        PolicyImportResult result = new PolicyImportResult();
        result.applicationName = application.getName();
        UriBuilder uriBuilder =
            baseUrl.redirect().path( InsightBrainService.POLICY_ASSET_PATH ).path( "index.html" ).queryParam( "appId",
                                                                                                              policyOwnerId );
        result.applicationURL = uriBuilder.build().toString();

        return result;
    }

    private PolicyDAO policyDAO()
    {
        return new PolicyDAO( work.getWorkDir() );
    }
    
    private static class ExportDTO
    {
        public List<Policy> policies;

        public List<Label> labels;

        public List<LicenseThreatGroup> licenseThreatGroups;

        public List<LicenseThreatGroupLicense> licenseThreatGroupLicenses;
    }
}
