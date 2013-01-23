/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.label;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.junit.Assert;
import org.junit.Test;

import com.ning.http.client.Response;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.label.Color;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.policy.Action;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.Constraint;
import com.sonatype.insight.brain.model.policy.LogicalOperator;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.conditions.LabelConditionType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.policy.PolicyResource;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.test.RestAccess;
import com.yammer.dropwizard.testing.JsonHelpers;

public class LabelResourceTest
    extends AbstractResourceTest
{
    private LabelDAO labelDAO = new LabelDAO();

    @Test
    public void testDeleteLabel_UsedInPolicyCondition()
        throws Exception
    {
        // Create an application with one label
        String appPublicId = "LabelResourceTest_AppId";
        Application application = createApplication( appPublicId );
        Label label = new Label();
        label.setColor( Color.blue );
        label.setLabel( "MyLabel" );
        Response response = RestAccess.post( getServiceURL( appPublicId ), JsonHelpers.asJson( label ) );
        assertResponseStatus( 200, response );
        label = JsonHelpers.fromJson( response.getResponseBody(), Label.class );

        // Create a policy that uses the label
        Condition condition = new Condition( LabelConditionType.ID, "is", label.getId() );
        Constraint constraint = new Constraint( "ConstraintId1", "Constraint name 1", LogicalOperator.AND );
        constraint.addCondition( condition );
        List<Constraint> constraints = new ArrayList<Constraint>();
        constraints.add( constraint );
        Policy policy = new Policy( "PolicyId1", "Policy Name 1" );
        policy.setConstraints( constraints );
        policy.addAction( BuildStageType.ID, new Action( FailActionType.ID ) );
        response =
            RestAccess.post( getRestBaseUrl()
                                 + PolicyResource.SERVICE_PATH.replace( "{applicationPublicId}", appPublicId ),
                             JsonHelpers.asJson( policy ) );
        assertResponseStatus( 200, response );

        // Try to delete the label
        response = RestAccess.delete( getServiceURL( appPublicId ) + "/" + label.getId() );
        assertResponseStatus( 400, response );
        Assert.assertEquals( "Cannot delete the label because it is used in a condition for the 'Policy Name 1' policy",
                             response.getResponseBody() );
        // Verify that the label was not deleted
        response = RestAccess.get( getServiceURL( appPublicId ) );
        assertResponseStatus( 200, response );
        Label[] labels = JsonHelpers.fromJson( response.getResponseBody(), Label[].class );
        Assert.assertNotNull( labels );
        Assert.assertEquals( 1, labels.length );
        assertLabel( application.getId(), "MyLabel", Color.blue, labels[0] );
    }

    @Test
    public void testDeleteLabel_Nonexistant()
        throws Exception
    {
        String appPublicId = "LabelResourceTest_AppId";
        createApplication( appPublicId );

        Response response = RestAccess.delete( getServiceURL( appPublicId ) + "/YettiId" );
        assertResponseStatus( 404, response );
        Assert.assertEquals( "Cannot find a label with id YettiId", response.getResponseBody() );
    }

    @Test
    public void testDeleteLabel_ApplicationIdMismatch()
        throws Exception
    {
        String appPublicId1 = "LabelResourceTest_AppId1";
        Application application1 = createApplication( appPublicId1 );
        String appPublicId2 = "LabelResourceTest_AppId2";
        createApplication( appPublicId2 );

        Label label = new Label();
        label.setColor( Color.blue );
        label.setLabel( "MyLabel" );
        Response response = RestAccess.post( getServiceURL( appPublicId1 ), JsonHelpers.asJson( label ) );
        assertResponseStatus( 200, response );
        label = JsonHelpers.fromJson( response.getResponseBody(), Label.class );

        response = RestAccess.delete( getServiceURL( appPublicId2 ) + "/" + label.getId() );
        assertResponseStatus( 404, response );
        Assert.assertEquals( "Cannot find a label with id " + label.getId() + " for application id " + appPublicId2,
                             response.getResponseBody() );
        // Verify that the label was not deleted
        response = RestAccess.get( getServiceURL( appPublicId1 ) );
        assertResponseStatus( 200, response );
        Label[] labels = JsonHelpers.fromJson( response.getResponseBody(), Label[].class );
        Assert.assertNotNull( labels );
        Assert.assertEquals( 1, labels.length );
        assertLabel( application1.getId(), "MyLabel", Color.blue, labels[0] );
    }

    @Test
    public void testAddDuplicateLabel()
        throws Exception
    {
        String appPublicId = "LabelResourceTest_AppId";
        Application application = createApplication( appPublicId );

        // Add a label
        Label label = new Label();
        label.setColor( Color.blue );
        label.setLabel( "MyLabel" );
        Response response = RestAccess.post( getServiceURL( appPublicId ), JsonHelpers.asJson( label ) );
        assertResponseStatus( 200, response );
        label = JsonHelpers.fromJson( response.getResponseBody(), Label.class );
        assertLabel( application.getId(), "MyLabel", Color.blue, label );

        // Add another label with the same name
        label = new Label();
        label.setColor( Color.blue );
        label.setLabel( "MyLabel" );
        response = RestAccess.post( getServiceURL( appPublicId ), JsonHelpers.asJson( label ) );
        assertResponseStatus( 409, response );
        String message = response.getResponseBody();
        Assert.assertEquals( "A label with the same name already exists", message );
    }

    @Test
    public void testUpdateLabel_Duplicate()
        throws Exception
    {
        String appPublicId = "LabelResourceTest_AppId";
        Application application = createApplication( appPublicId );

        Label label1 = new Label();
        label1.setColor( Color.blue );
        label1.setLabel( "MyLabel1" );
        Response response = RestAccess.post( getServiceURL( appPublicId ), JsonHelpers.asJson( label1 ) );
        assertResponseStatus( 200, response );
        label1 = JsonHelpers.fromJson( response.getResponseBody(), Label.class );
        assertLabel( application.getId(), "MyLabel1", Color.blue, label1 );
        Label label2 = new Label();
        label2.setColor( Color.blue );
        label2.setLabel( "MyLabel2" );
        response = RestAccess.post( getServiceURL( appPublicId ), JsonHelpers.asJson( label2 ) );
        assertResponseStatus( 200, response );
        label2 = JsonHelpers.fromJson( response.getResponseBody(), Label.class );
        assertLabel( application.getId(), "MyLabel2", Color.blue, label2 );

        // Update without changing the name
        label2.setColor( Color.red );
        response = RestAccess.put( getServiceURL( appPublicId ), JsonHelpers.asJson( label2 ) );
        assertResponseStatus( 200, response );
        label2 = JsonHelpers.fromJson( response.getResponseBody(), Label.class );
        assertLabel( application.getId(), "MyLabel2", Color.red, label2 );

        // Update with a conflicting name
        label2.setLabel( label1.getLabel() );
        response = RestAccess.put( getServiceURL( appPublicId ), JsonHelpers.asJson( label2 ) );
        assertResponseStatus( 409, response );
        String message = response.getResponseBody();
        Assert.assertEquals( "A label with the same name already exists", message );
    }

    @Test
    public void testCRUD()
        throws Exception
    {
        // Create an application
        String appPublicId = "LabelResourceTest_AppId";
        Application application = createApplication( appPublicId );

        // Get all labels
        Response response = RestAccess.get( getServiceURL( appPublicId ) );
        assertResponseStatus( 200, response );
        Label[] labels = JsonHelpers.fromJson( response.getResponseBody(), Label[].class );
        Assert.assertNotNull( labels );
        Assert.assertEquals( 0, labels.length );

        // Add a label
        Label label = new Label();
        label.setLabel( "MyLabel" );
        response = RestAccess.post( getServiceURL( appPublicId ), JsonHelpers.asJson( label ) );
        assertResponseStatus( 200, response );
        label = JsonHelpers.fromJson( response.getResponseBody(), Label.class );
        assertLabel( application.getId(), "MyLabel", null /* color */, label );

        // Get all labels
        response = RestAccess.get( getServiceURL( appPublicId ) );
        assertResponseStatus( 200, response );
        labels = JsonHelpers.fromJson( response.getResponseBody(), Label[].class );
        Assert.assertNotNull( labels );
        Assert.assertEquals( 1, labels.length );
        assertLabel( application.getId(), "MyLabel", null /* color */, labels[0] );

        // Update a label
        label.setLabel( "MyUpdatedLabel" );
        response = RestAccess.put( getServiceURL( appPublicId ), JsonHelpers.asJson( label ) );
        assertResponseStatus( 200, response );
        label = JsonHelpers.fromJson( response.getResponseBody(), Label.class );
        assertLabel( application.getId(), "MyUpdatedLabel", null /* color */, label );

        // Get all labels
        response = RestAccess.get( getServiceURL( appPublicId ) );
        assertResponseStatus( 200, response );
        labels = JsonHelpers.fromJson( response.getResponseBody(), Label[].class );
        Assert.assertNotNull( labels );
        Assert.assertEquals( 1, labels.length );
        assertLabel( application.getId(), "MyUpdatedLabel", null /* color */, labels[0] );

        // Delete a label
        response = RestAccess.delete( getServiceURL( appPublicId ) + "/" + label.getId() );
        assertResponseStatus( 204, response );

        // Get all labels
        response = RestAccess.get( getServiceURL( appPublicId ) );
        assertResponseStatus( 200, response );
        labels = JsonHelpers.fromJson( response.getResponseBody(), Label[].class );
        Assert.assertNotNull( labels );
        Assert.assertEquals( 0, labels.length );
    }

    private void assertLabel( String applicationId, String label, Color color, Label actual )
    {
        Assert.assertEquals( applicationId, actual.getApplicationId() );
        Assert.assertEquals( label, actual.getLabel() );
        Assert.assertEquals( label.toLowerCase( Locale.ENGLISH ), actual.getLabelLowercase() );
        Assert.assertEquals( color, actual.getColor() );
    }

    private String getServiceURL( final String appId )
    {
        return getRestBaseUrl() + LabelResource.SERVICE_PATH.replace( "{applicationPublicId}", appId );
    }

    @Override
    protected void cleanupApplication( Application application )
    {
        for ( Label label : labelDAO.getByApplicationId( application.getId() ) )
        {
            labelDAO.delete( label );
        }
        super.cleanupApplication( application );
    }
}
