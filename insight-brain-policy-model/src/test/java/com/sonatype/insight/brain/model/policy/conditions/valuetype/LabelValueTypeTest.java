/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.conditions.valuetype;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.label.Label;

public class LabelValueTypeTest
{
    private Organization org;

    private Application app;

    @Before
    public void setUp()
        throws Exception
    {
        org = new Organization( "orgName" );
        new OrganizationDAO().insert( org );
        app = new Application();
        app.setName( "appName" );
        app.setPublicId( "appId" );
        app.setOrganizationId( org.getId() );
        new ApplicationDAO().insert( app );
        Label label = new Label( app.getId(), "appLabel", null );
        new LabelDAO().insert( label );
    }

    @After
    public void tearDown()
        throws Exception
    {
        ApplicationDAO appDAO = new ApplicationDAO();
        for ( Application app : appDAO.getAll() )
        {
            appDAO.delete( app );
        }
        OrganizationDAO orgDAO = new OrganizationDAO();
        for ( Organization org : orgDAO.getAll() )
        {
            orgDAO.delete( org );
        }
    }

    @Test
    public void testGetAvailableValues_AppLevel()
    {
        LabelValueType type = new LabelValueType( app.getId() );
        List<Label> labels = type.getAvailableValues();
        assertNotNull( labels );
        assertEquals( 1, labels.size() );
    }

    @Test
    public void testGetAvailableValues_OrgLevel()
    {
        LabelValueType type = new LabelValueType( org.getId() );
        List<Label> labels = type.getAvailableValues();
        assertNotNull( labels );
        assertEquals( 0, labels.size() );
    }
}
