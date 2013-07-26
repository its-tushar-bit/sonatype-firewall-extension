/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.waiver;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.IdUtils;
import com.sonatype.insight.error.exception.NotFoundException;

@Named
@Path( WaiverResource.SERVICE_PATH )
public class WaiverResource
{
    public static final String SERVICE_BASEPATH = "rest/waiver/";

    public static final String SERVICE_PATH = SERVICE_BASEPATH + "{ownerType: application|organization}/{ownerId}";

    @Context
    private InsightWork work;
    
    /**
     * @since 1.6
     */
    @GET
    @Path( "applicable/context/{policyId}" )
    @Produces( MediaType.APPLICATION_JSON )
    public ApplicableContext getApplicableContexts( @PathParam( "ownerId" ) String applicationPublicId,
                                                    @PathParam( "policyId" ) String policyId )
    {
        // Currently it is impossible to retrieve a policy by its id without traversing all policy stores. That's why we
        // require a owner id and type here. Unfortunately, the UI doesn't know the exact owner id for a policy id. When
        // starting from a policy violation, the UI knows the policy id and the public id of the application for which
        // the
        // policy was evaluated. But the policy may belong to an organization. So we have to find the policy's owner id
        // from the policy id and the application public id.
        //
        // Hopefully that will be simplified in the new storage.

        Application application = new ApplicationDAO().getByPublicIdNotNull( applicationPublicId );
        String applicationId = application.getId();
        Policy policy = policyDAO().getByOwnerIdAndPolicyId( applicationId, policyId );
        if ( policy != null )
        {
            // The policy belongs to the application
            return new ApplicableContext( applicationId, application.getName(), IdUtils.TYPE_APPLICATION );
        }

        if ( application.getOrganizationId() != null )
        {
            policy = policyDAO().getByOwnerIdAndPolicyId( application.getOrganizationId(), policyId );
        }
        if ( policy == null )
        {
            throw new NotFoundException( "Cannot find a policy with id " + policyId + " for application public id "
                + applicationPublicId );
        }

        // The policy belongs to an organization
        Organization organization = new OrganizationDAO().getById( application.getOrganizationId() );
        ApplicableContext result =
            new ApplicableContext( organization.getId(), organization.getName(), IdUtils.TYPE_ORGANIZATION );
        result.children = new ArrayList<ApplicableContext>();
        // Currently we need only the application specified by the applicationPublicId. In the future we might need to
        // return all the applications for this organization.
        result.children.add( new ApplicableContext( applicationId, application.getName(), IdUtils.TYPE_APPLICATION ) );
        return result;
    }

    private PolicyDAO policyDAO()
    {
        return new PolicyDAO( work.getWorkDir() );
    }

    /**
     * Waivers can be applied in the context of an application or an organization. This class contains the hierarchy of
     * organizations and applications for which a waiver can be applied.
     */
    public static class ApplicableContext
    {
        public String id;

        public String name;

        /**
         * "application" or "organization"
         */
        public String type;

        public List<ApplicableContext> children;

        public ApplicableContext()
        {
        }

        public ApplicableContext( String id, String name, String type )
        {
            this.id = id;
            this.name = name;
            this.type = type;
        }
    }
}
