/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.IOException;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Assert;

import com.ning.http.client.Response;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.label.ComponentLabelDAO;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupLicenseDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.label.ComponentLabel;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.license.LicenseThreatGroupLicense;
import com.sonatype.insight.brain.model.policy.Policy;

public abstract class AbstractResourceTest
    extends AbstractBrainServiceTest
{
    protected Set<Application> applicationsToDelete = new LinkedHashSet<Application>();

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
        PolicyDAO policyDAO = new PolicyDAO( brain.getWorkDir() );
        List<Policy> policies = policyDAO.getByOwnerId( application.getId() );
        for ( Policy policy : policies )
        {
            policyDAO.delete( application.getId(), policy.getId() );
        }

        ComponentLabelDAO componentLabelDAO = new ComponentLabelDAO();
        List<ComponentLabel> componentLabels = componentLabelDAO.getByApplicationId( application.getId() );
        for ( ComponentLabel componentLabel : componentLabels )
        {
            componentLabelDAO.delete( componentLabel );
        }

        LabelDAO labelDAO = new LabelDAO();
        List<Label> labels = labelDAO.getByApplicationId( application.getId() );
        for ( Label label : labels )
        {
            labelDAO.delete( label );
        }

        LicenseThreatGroupDAO licenseThreatGroupDAO = new LicenseThreatGroupDAO();
        LicenseThreatGroupLicenseDAO licenseThreatGroupLicenseDAO = new LicenseThreatGroupLicenseDAO();
        List<LicenseThreatGroup> licenseThreatGroups = licenseThreatGroupDAO.getByApplicationId( application.getId() );
        for ( LicenseThreatGroup licenseThreatGroup : licenseThreatGroups )
        {
            List<LicenseThreatGroupLicense> licenses =
                licenseThreatGroupLicenseDAO.getByLicenseThreatGroupId( licenseThreatGroup.getId() );
            for ( LicenseThreatGroupLicense license : licenses )
            {
                licenseThreatGroupLicenseDAO.delete( license );
            }
            licenseThreatGroupDAO.delete( licenseThreatGroup );
        }
    }

    protected static void assertResponseStatus( final int expectedStatus, final Response response )
        throws IOException
    {
        final int actualStatus = response.getStatusCode();
        Assert.assertEquals( "URI:" + response.getUri() + ", StatusText:" + response.getStatusText() + ", ResponseBody:"
                                 + response.getResponseBody(), expectedStatus, actualStatus );
    }

    protected Application createApplication( String publicId )
    {
        // Application Name must be unique
        return createApplication( publicId, "DUMMYNAME" + new Date().getTime() );
    }

    protected Application createApplication( String publicId, String name )
    {
        ApplicationDAO applicationDAO = new ApplicationDAO();
        Application application = new Application();
        application.setPublicId( publicId );
        application.setName( name );
        applicationDAO.insert( application );
        applicationsToDelete.add( application );
        return application;
    }
}
