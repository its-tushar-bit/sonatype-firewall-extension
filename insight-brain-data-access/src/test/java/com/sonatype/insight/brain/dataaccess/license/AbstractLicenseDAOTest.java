/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.license;

import org.junit.After;
import org.junit.Before;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;

public abstract class AbstractLicenseDAOTest
    extends AbstractDbDAOTest
{
    private LicenseDataUpdater savedLicenseDataUpdater;

    @Before
    public void before()
    {
        savedLicenseDataUpdater = LicenseDataUpdater.getUpdater();
    }

    @After
    public void after()
    {
        LicenseDataUpdater.setUpdater( savedLicenseDataUpdater );
    }
}
