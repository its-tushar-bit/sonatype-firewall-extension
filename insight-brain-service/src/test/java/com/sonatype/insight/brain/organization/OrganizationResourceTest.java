/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.ning.http.client.Response;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.insight.test.RestAccess;
import com.yammer.dropwizard.testing.JsonHelpers;

public class OrganizationResourceTest
    extends AbstractResourceTest
{
    @Test
    public void testCRUD()
        throws Exception
    {
        // Create
        Organization organization = new Organization();
        organization.setName( "OrganizationResourceTest" );

        Response response = RestAccess.post( getServiceURL(), JsonHelpers.asJson( organization ) );
        assertResponseStatus( 200, response );
        organization = JsonHelpers.fromJson( response.getResponseBody(), Organization.class );
        assertNotNull( organization );
        assertNotNull( organization.getId() );
        assertEquals( "OrganizationResourceTest", organization.getName() );
        String organizationId = organization.getId();

        // Update
        organization.setName( "OrganizationResourceTest updated" );
        response = RestAccess.put( getServiceURL(), JsonHelpers.asJson( organization ) );
        assertResponseStatus( 200, response );
        organization = JsonHelpers.fromJson( response.getResponseBody(), Organization.class );
        assertNotNull( organization );
        assertEquals( organizationId, organization.getId() );
        assertEquals( "OrganizationResourceTest updated", organization.getName() );

        // Delete
        response = RestAccess.delete( getServiceURL() + "/" + organizationId );
        assertResponseStatus( 404, response );

        new OrganizationDAO().delete( organization );
    }

    private String getServiceURL()
    {
        return getRestBaseUrl() + OrganizationResource.SERVICE_PATH;
    }
}
