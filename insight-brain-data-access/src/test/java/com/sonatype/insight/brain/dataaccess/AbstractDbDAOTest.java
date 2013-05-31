/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;

public abstract class AbstractDbDAOTest
{
    protected String applicationId;

    protected static String applicationPublicId = "AbstractDbDAOTest_AppId";

    protected static String applicationName = "AbstractDbDAOTest";

    protected Organization organization;

    @Before
    public void setUp()
    {
        // Create an application
        ApplicationDAO applicationDAO = new ApplicationDAO();
        Application application = new Application();
        application.setName( applicationName );
        application.setPublicId( applicationPublicId );
        applicationDAO.insert( application );
        applicationId = application.getId();
        Assert.assertNotNull( applicationId );
    }

    @After
    public void tearDown()
    {
        ApplicationDAO applicationDAO = new ApplicationDAO();
        Application application = applicationDAO.getById( applicationId );
        if ( application != null )
        {
            applicationDAO.delete( application );
        }

        if ( organization != null && organization.getId() != null )
        {
            new OrganizationDAO().delete( organization );
        }
    }
}
