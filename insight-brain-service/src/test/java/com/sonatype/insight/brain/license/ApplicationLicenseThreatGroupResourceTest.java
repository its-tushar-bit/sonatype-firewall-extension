/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.license;

import org.junit.Test;

import com.sonatype.insight.brain.model.Application;

public class ApplicationLicenseThreatGroupResourceTest
    extends AbstractLicenseThreatGroupResourceTest
{
    @Test
    public void testCRUD()
        throws Exception
    {
        String appPublicId = "LicenseThreatGroupResourceTest_AppId";
        Application application = createApplication( appPublicId );
        testCRUD( appPublicId, application.getId() );
    }

    @Test
    public void testDelete_OwnerIdMismatch()
        throws Exception
    {
        String appPublicId1 = "LicenseThreatGroupResourceTest_AppId1";
        Application application1 = createApplication( appPublicId1 );
        String appPublicId2 = "LicenseThreatGroupResourceTest_AppId2";
        Application application2 = createApplication( appPublicId2 );
        testDelete_OwnerIdMismatch( appPublicId1, application1.getId(), appPublicId2, application2.getId() );
    }

    @Override
    protected String getOwnerType()
    {
        return "application";
    }
}
