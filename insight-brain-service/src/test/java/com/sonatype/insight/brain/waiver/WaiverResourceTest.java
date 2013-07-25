/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.waiver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.ning.http.client.Response;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.conditions.SecurityVulnerabilityConditionType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.policy.PolicyResource;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.brain.waiver.WaiverResource.ApplicableContext;
import com.sonatype.insight.test.RestAccess;
import com.yammer.dropwizard.testing.JsonHelpers;

public class WaiverResourceTest
    extends AbstractResourceTest
{
    @Test
    public void testGetApplicableContexts_Application()
        throws Exception
    {
        String appPublicId = "testGetApplicableContexts_Application";
        Application application = createApplication( appPublicId );

        // Create a policy for the application
        Condition condition = new Condition( SecurityVulnerabilityConditionType.ID, "present" );
        Constraint constraint = new Constraint( null, "Constraint name 1", LogicalOperator.AND );
        constraint.addCondition( condition );
        List<Constraint> constraints = new ArrayList<Constraint>();
        constraints.add( constraint );
        Policy policy = new Policy( null, "Policy Name 1" );
        policy.setConstraints( constraints );
        policy.addAction( BuildStageType.ID, new Action( FailActionType.ID ) );
        Response response =
            RestAccess.post( getRestBaseUrl() + expandRestUrl( PolicyResource.SERVICE_PATH, "application", appPublicId ),
                             JsonHelpers.asJson( policy ) );
        assertResponseStatus( 200, response );
        policy = JsonHelpers.fromJson( response.getResponseBody(), Policy.class );

        response =
            RestAccess.get( getServiceURL( "application", appPublicId ) + "/applicable/context/" + policy.getId() );
        assertResponseStatus( 200, response );
        ApplicableContext result = JsonHelpers.fromJson( response.getResponseBody(), ApplicableContext.class );
        assertApplicableContext( application.getId(), application.getName(), "application", result );
    }

    @Test
    public void testGetApplicableContexts_Organization()
        throws Exception
    {
        String appPublicId = "testGetApplicableContexts_Organization";
        Application application = createApplication( appPublicId );
        Organization organization = new OrganizationDAO().getByIdNotNull( application.getOrganizationId() );

        // Create a policy for the organization
        Condition condition = new Condition( SecurityVulnerabilityConditionType.ID, "present" );
        Constraint constraint = new Constraint( null, "Constraint name 1", LogicalOperator.AND );
        constraint.addCondition( condition );
        List<Constraint> constraints = new ArrayList<Constraint>();
        constraints.add( constraint );
        Policy policy = new Policy( null, "Policy Name 1" );
        policy.setConstraints( constraints );
        policy.addAction( BuildStageType.ID, new Action( FailActionType.ID ) );
        Response response =
            RestAccess.post( getRestBaseUrl()
                                 + expandRestUrl( PolicyResource.SERVICE_PATH, "organization",
                                                  application.getOrganizationId() ), JsonHelpers.asJson( policy ) );
        assertResponseStatus( 200, response );
        policy = JsonHelpers.fromJson( response.getResponseBody(), Policy.class );

        response =
            RestAccess.get( getServiceURL( "application", appPublicId ) + "/applicable/context/" + policy.getId() );
        assertResponseStatus( 200, response );
        ApplicableContext result = JsonHelpers.fromJson( response.getResponseBody(), ApplicableContext.class );
        assertApplicableContext( organization.getId(), organization.getName(), "organization", result );
        assertNotNull( result.children );
        assertEquals( 1, result.children.size() );
        ApplicableContext childContext = result.children.get( 0 );
        assertApplicableContext( application.getId(), application.getName(), "application", childContext );
        assertNull( childContext.children );
    }

    private void assertApplicableContext( String id, String name, String type, ApplicableContext actual )
    {
        assertNotNull( actual );
        assertEquals( id, actual.id );
        assertEquals( name, actual.name );
        assertEquals( type, actual.type );
    }

    private String getServiceURL( String ownerType, String ownerId )
    {
        return getRestBaseUrl() + WaiverResource.SERVICE_BASEPATH + ownerType + "/" + ownerId;
    }
}
