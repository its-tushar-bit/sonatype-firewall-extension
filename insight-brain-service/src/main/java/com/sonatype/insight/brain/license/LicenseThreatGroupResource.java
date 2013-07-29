/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.license;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.conditions.LicenseThreatGroupConditionType;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

@Named
@Path( LicenseThreatGroupResource.SERVICE_PATH )
public class LicenseThreatGroupResource
{
    public static final String SERVICE_PATH = "rest/licenseThreatGroup/{ownerType: application|organization}/{ownerId}";

    private LicenseThreatGroupDAO licenseThreatGroupDAO = new LicenseThreatGroupDAO();

    private final InsightWork work;

    @Inject
    public LicenseThreatGroupResource( InsightWork work )
    {
        this.work = work;
    }

    @GET
    @Produces( { MediaType.APPLICATION_JSON } )
    public List<LicenseThreatGroup> getLicenseThreatGroups( @PathParam( "ownerType" ) String ownerType,
                                                            @PathParam( "ownerId" ) String ownerId )
    {
        ownerId = IdUtils.getInternalOwnerId( ownerType, ownerId );

        return licenseThreatGroupDAO.getByOwnerId( ownerId );
    }

    /**
     * @since 1.6
     */
    @GET
    @Path( "applicable" )
    @Produces( { MediaType.APPLICATION_JSON } )
    public ApplicableLicenseThreatGroups getApplicableLicenseThreatGroups( @PathParam( "ownerType" ) String ownerType,
                                                                           @PathParam( "ownerId" ) String ownerId )
    {
        ownerId = IdUtils.getInternalOwnerId( ownerType, ownerId );

        ApplicableLicenseThreatGroups result = new ApplicableLicenseThreatGroups();

        String organizationId;
        if ( IdUtils.TYPE_APPLICATION.equals( ownerType ) )
        {
            Application app = new ApplicationDAO().getByIdNotNull( ownerId );
            result.add( app.getId(), app.getName(), IdUtils.TYPE_APPLICATION,
                        licenseThreatGroupDAO.getByOwnerId( app.getId() ) );
            organizationId = app.getOrganizationId();
        }
        else
        {
            organizationId = ownerId;
        }
        if ( organizationId != null )
        {
            Organization org = new OrganizationDAO().getByIdNotNull( organizationId );
            result.add( org.getId(), org.getName(), IdUtils.TYPE_ORGANIZATION,
                        licenseThreatGroupDAO.getByOwnerId( org.getId() ) );
        }

        return result;
    }

    @POST
    @Consumes( MediaType.APPLICATION_JSON )
    @Produces( MediaType.APPLICATION_JSON )
    public LicenseThreatGroup addLicenseThreatGroup( @PathParam( "ownerType" ) String ownerType,
                                                     @PathParam( "ownerId" ) String ownerId,
                                                     LicenseThreatGroup licenseThreatGroup )
    {
        ownerId = IdUtils.getInternalOwnerId( ownerType, ownerId );

        licenseThreatGroup.setId( null );
        licenseThreatGroup.setOwnerId( ownerId );
        licenseThreatGroupDAO.insert( licenseThreatGroup );

        return licenseThreatGroup;
    }

    @PUT
    @Consumes( MediaType.APPLICATION_JSON )
    @Produces( MediaType.APPLICATION_JSON )
    public LicenseThreatGroup updateLicenseThreatGroup( @PathParam( "ownerType" ) String ownerType,
                                                        @PathParam( "ownerId" ) String ownerId,
                                                        LicenseThreatGroup licenseThreatGroup )
    {
        ownerId = IdUtils.getInternalOwnerId( ownerType, ownerId );

        licenseThreatGroup.setOwnerId( ownerId );
        licenseThreatGroupDAO.update( licenseThreatGroup );

        return licenseThreatGroup;
    }

    @DELETE
    @Path( "{licenseThreatGroupId}" )
    public void deleteLicenseThreatGroup( @PathParam( "ownerType" ) String ownerType,
                                          @PathParam( "ownerId" ) String ownerId,
                                          @PathParam( "licenseThreatGroupId" ) String licenseThreatGroupId )
    {
        String internalOwnerId = IdUtils.getInternalOwnerId( ownerType, ownerId );

        LicenseThreatGroup licenseThreatGroup = licenseThreatGroupDAO.getByIdNotNull( licenseThreatGroupId );
        if ( !internalOwnerId.equals( licenseThreatGroup.getOwnerId() ) )
        {
            throw new NotFoundException( "Cannot find a license threat group with id " + licenseThreatGroupId + " for "
                + ownerType + " id " + ownerId );
        }

        // Verify that the license threat group is not used in a policy condition
        PolicyDAO policyDAO = new PolicyDAO( work.getWorkDir() );

        String inUseError =
            "Cannot delete the license threat group because it is used in a condition for the '%s' policy";

        for ( Policy policy : policyDAO.getByOwnerId( internalOwnerId ) )
        {
            if ( isLicenseThreatGroupUsedInPolicy( licenseThreatGroupId, policy ) )
            {
                throw new BadRequestException( String.format( inUseError, policy.getName() ) );
            }
        }

        if ( IdUtils.TYPE_ORGANIZATION.equals( ownerType ) )
        {
            inUseError = inUseError + " in application '%s'";

            for ( Application app : new ApplicationDAO().getByOrganizationId( internalOwnerId ) )
            {
                for ( Policy policy : policyDAO.getByOwnerId( app.getId() ) )
                {
                    if ( isLicenseThreatGroupUsedInPolicy( licenseThreatGroupId, policy ) )
                    {
                        throw new BadRequestException( String.format( inUseError, policy.getName(), app.getName() ) );
                    }
                }
            }
        }

        licenseThreatGroupDAO.delete( licenseThreatGroup );
    }

    public static class ApplicableLicenseThreatGroups
    {
        public List<LicenseThreatGroupsByOwner> licenseThreatGroupsByOwner =
            new ArrayList<LicenseThreatGroupsByOwner>();

        public void add( String ownerId, String ownerName, String ownerType,
                         List<LicenseThreatGroup> licenseThreatGroups )
        {
            LicenseThreatGroupsByOwner ltgbo = new LicenseThreatGroupsByOwner();
            ltgbo.ownerId = ownerId;
            ltgbo.ownerName = ownerName;
            ltgbo.ownerType = ownerType;
            ltgbo.licenseThreatGroups = licenseThreatGroups;
            licenseThreatGroupsByOwner.add( ltgbo );
        }
    }

    public static class LicenseThreatGroupsByOwner
    {
        public String ownerId;

        public String ownerName;

        public String ownerType;

        public List<LicenseThreatGroup> licenseThreatGroups;
    }

    /**
     * Returns {@code true} if the given licenseThreatGroupId is used in the given policy; otherwise {@code false}.
     * 
     * @since 1.6
     */
    private static boolean isLicenseThreatGroupUsedInPolicy( String licenseThreatGroupId, Policy policy )
    {
        for ( Constraint constraint : policy.getConstraints() )
        {
            for ( Condition condition : constraint.getConditions() )
            {
                if ( LicenseThreatGroupConditionType.ID.equals( condition.getConditionTypeId() )
                    && licenseThreatGroupId.equals( condition.getValue() ) )
                {
                    return true;
                }
            }
        }
        return false;
    }
}
