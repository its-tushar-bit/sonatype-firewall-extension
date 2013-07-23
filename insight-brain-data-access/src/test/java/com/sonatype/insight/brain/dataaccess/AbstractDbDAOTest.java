/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.After;

import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;

public abstract class AbstractDbDAOTest
{
    protected Application application;

    protected String applicationId;

    protected Organization organization;

    protected Set<Application> applicationsToDelete = new LinkedHashSet<Application>();

    protected Set<Organization> organizationsToDelete = new LinkedHashSet<Organization>();

    protected Organization createOrganization( String name )
    {
        Organization organization = new Organization( name );
        new OrganizationDAO().insert( organization );
        organizationsToDelete.add( organization );
        return organization;
    }

    protected void createDefaultApplication()
    {
        // Create an organization
        organization = createOrganization( "AbstractDbDAOTest" );

        application =
            new Application( "AbstractDbDAOTest_AppPublicId", "AbstractDbDAOTest-AppName", organization.getId() );
        new ApplicationDAO().insert( application );
        applicationsToDelete.add( application );
        applicationId = application.getId();
    }

    @After
    public void tearDown()
    {
        ApplicationDAO applicationDAO = new ApplicationDAO();
        for ( Application application : applicationsToDelete )
        {
            application = applicationDAO.getById( application.getId() );
            if ( application != null )
            {
                applicationDAO.delete( application );
            }
        }

        OrganizationDAO organizationDAO = new OrganizationDAO();
        for ( Organization organization : organizationsToDelete )
        {
            organization = organizationDAO.getById( organization.getId() );
            if ( organization != null )
            {
                organizationDAO.delete( organization );
            }
        }
    }
}
