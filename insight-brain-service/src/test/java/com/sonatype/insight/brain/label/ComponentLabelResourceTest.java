/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.label;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.ning.http.client.Response;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.test.RestAccess;
import com.yammer.dropwizard.testing.JsonHelpers;

public class ComponentLabelResourceTest
    extends AbstractResourceTest
{

    @Test
    public void testSetGetApplicationComponentLabels()
        throws Exception
    {
        // Create an application
        String appPublicId = "ComponentLabelResourceTest_AppId";
        createApplication( appPublicId );

        String hash = "bababababa";
        Set<String> labels = toLabelSet( "LabelY", "LabelX" );
        ComponentLabelState state = new ComponentLabelState();
        state.setLabels( labels );
        Response response =
            RestAccess.put( getServiceURLForApplication( appPublicId, hash ), JsonHelpers.asJson( state ) );
        assertResponseStatus( 200, response );
        Label[] componentLabels = JsonHelpers.fromJson( response.getResponseBody(), Label[].class );
        Assert.assertEquals( 2, componentLabels.length );
        Assert.assertEquals( "LabelX", componentLabels[0].getLabel() );
        Assert.assertEquals( "LabelY", componentLabels[1].getLabel() );

        response = RestAccess.get( getServiceURLForApplication( appPublicId, hash ) );
        assertResponseStatus( 200, response );
        componentLabels = JsonHelpers.fromJson( response.getResponseBody(), Label[].class );
        Assert.assertEquals( 2, componentLabels.length );
        Assert.assertEquals( "LabelX", componentLabels[0].getLabel() );
        Assert.assertEquals( "LabelY", componentLabels[1].getLabel() );
    }

    @Test
    public void testSetGetOrganizationComponentLabels()
        throws Exception
    {
        // Create an organization
        String orgName = "ComponentLabelResourceTestOrgName";
        Organization organization = createOrganization( orgName );

        String hash = "bababababa";
        Set<String> labels = toLabelSet( "LabelY", "LabelX" );
        ComponentLabelState state = new ComponentLabelState();
        state.setLabels( labels );
        Response response =
            RestAccess.put( getServiceURL( "organization", organization.getId(), hash ), JsonHelpers.asJson( state ) );
        assertResponseStatus( 200, response );
        Label[] componentLabels = JsonHelpers.fromJson( response.getResponseBody(), Label[].class );
        Assert.assertEquals( 2, componentLabels.length );
        Assert.assertEquals( "LabelX", componentLabels[0].getLabel() );
        Assert.assertEquals( "LabelY", componentLabels[1].getLabel() );

        response = RestAccess.get( getServiceURL( "organization", organization.getId(), hash ) );
        assertResponseStatus( 200, response );
        componentLabels = JsonHelpers.fromJson( response.getResponseBody(), Label[].class );
        Assert.assertEquals( 2, componentLabels.length );
        Assert.assertEquals( "LabelX", componentLabels[0].getLabel() );
        Assert.assertEquals( "LabelY", componentLabels[1].getLabel() );
    }

    private Set<String> toLabelSet( String... labels )
    {
        Set<String> labelSet = new LinkedHashSet<String>();
        Collections.addAll( labelSet, labels );
        return labelSet;
    }

    private String getServiceURLForApplication( String applicationPublicId, String hash )
    {
        return getServiceURL( "application", applicationPublicId, hash );
    }

    private String getServiceURL( String ownerType, String ownerId, String hash )
    {
        return getRestBaseUrl() + ComponentLabelResource.SERVICE_BASEPATH + ownerType + "/" + ownerId + "/" + hash;
    }

}
