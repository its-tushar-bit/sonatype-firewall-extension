/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;

import com.sonatype.insight.brain.db.DataSourceFactory;
import com.sonatype.insight.brain.model.Application;

public abstract class AbstractDbDAOTest
{
    protected static String applicationId;

    protected final static String applicationPublicId = "AbstractDbDAOTest_AppId";

    protected final static String applicationName = "AbstractDbDAOTest";

    @BeforeClass
    public static void setUp()
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

    @AfterClass
    public static void tearDown()
    {
        ApplicationDAO applicationDAO = new ApplicationDAO();
        Application application = applicationDAO.getById( applicationId );
        if ( application != null )
        {
            applicationDAO.delete( application );
        }

        DataSourceFactory.clear_ForTestsOnly();
    }
}
