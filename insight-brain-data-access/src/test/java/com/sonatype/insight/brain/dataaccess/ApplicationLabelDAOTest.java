/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.util.Locale;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.sonatype.insight.brain.dataaccess.label.ApplicationLabelDAO;
import com.sonatype.insight.brain.db.DataSourceFactory;
import com.sonatype.insight.brain.db.OperationalDataStoreProvider;
import com.sonatype.insight.brain.model.label.ApplicationLabel;
import com.sonatype.insight.brain.model.label.Color;
import com.sonatype.insight.db.DatabaseConfig;

public class ApplicationLabelDAOTest
{
    @BeforeClass
    public static void setUp()
    {
        DatabaseConfig databaseConfig = new DatabaseConfig( null /* configDir */);
        OperationalDataStoreProvider.init( databaseConfig );
    }

    @AfterClass
    public static void tearDown()
    {
        DataSourceFactory.unloadAll();
    }

    @Test
    public void testCRUD()
        throws Exception
    {
        DatabaseConfig databaseConfig = new DatabaseConfig( null /* configDir */);
        OperationalDataStoreProvider.init( databaseConfig );
        ApplicationLabelDAO dao = new ApplicationLabelDAO();

        // Create
        ApplicationLabel applicationLabel = new ApplicationLabel();
        applicationLabel.setApplicationId( "ApplicationLabelDAOTest_AppId" );
        applicationLabel.setLabel( "My label" );
        applicationLabel.setColor( Color.blue );
        dao.insert( applicationLabel );
        Assert.assertNotNull( applicationLabel.getId() );

        applicationLabel = dao.getById( applicationLabel.getId() );
        Assert.assertNotNull( applicationLabel );
        assertApplicationLabel( "ApplicationLabelDAOTest_AppId", "My label", Color.blue, applicationLabel );

        // Update
        applicationLabel.setLabel( "My updated label" );
        dao.update( applicationLabel );

        applicationLabel = dao.getById( applicationLabel.getId() );
        Assert.assertNotNull( applicationLabel );
        assertApplicationLabel( "ApplicationLabelDAOTest_AppId", "My updated label", Color.blue, applicationLabel );

        // Delete
        dao.delete( applicationLabel );

        applicationLabel = dao.getById( applicationLabel.getId() );
        Assert.assertNull( applicationLabel );
    }

    private void assertApplicationLabel( String applicationId, String label, Color color, ApplicationLabel actual )
    {
        Assert.assertEquals( applicationId, actual.getApplicationId() );
        Assert.assertEquals( label, actual.getLabel() );
        Assert.assertEquals( label.toLowerCase( Locale.ENGLISH ), actual.getLabelLowercase() );
        Assert.assertEquals( color, actual.getColor() );
    }
}
