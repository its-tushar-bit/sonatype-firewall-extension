/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.After;
import org.junit.Assert;

import com.ning.http.client.Response;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.Application;

public abstract class AbstractResourceTest
    extends AbstractBrainServiceTest
{
    private Set<Application> applicationsToDelete = new LinkedHashSet<Application>();

    @After
    public void cleanup()
    {
        ApplicationDAO applicationDAO = new ApplicationDAO();
        for ( Application application : applicationsToDelete )
        {
            cleanupApplication( application );
            applicationDAO.delete( application );
        }
        applicationsToDelete.clear();
    }

    protected void cleanupApplication( Application application )
    {
    }

    protected static void assertResponseStatus( final int expectedStatus, final Response response )
        throws IOException
    {
        final int actualStatus = response.getStatusCode();
        Assert.assertEquals( "URI:" + response.getUri() + ", StatusText:" + response.getStatusText()
            + ", ResponseBody:" + response.getResponseBody(), expectedStatus, actualStatus );
    }

    protected Application createApplication( String publicId )
    {
        ApplicationDAO applicationDAO = new ApplicationDAO();
        Application application = new Application();
        application.setPublicId( publicId );
        applicationDAO.insert( application );
        applicationsToDelete.add( application );
        return application;
    }
}
