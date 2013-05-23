/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.saas;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.After;
import org.junit.Test;

import com.sonatype.insight.brain.dataaccess.license.LicenseCategoryDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseDAO;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.model.license.License;
import com.sonatype.insight.brain.model.license.LicenseCategory;
import com.sonatype.insight.brain.model.license.MultiLicense;
import com.sonatype.insight.brain.saas.DefaultLicenseDataUpdater.LicenseData;
import com.sonatype.insight.brain.service.AbstractBrainServiceTest;
import com.yammer.dropwizard.testing.JsonHelpers;

public class DefaultLicenseDataUpdaterTest
    extends AbstractBrainServiceTest
{
    @After
    public void after()
    {
        setSaasResponseForURI( DefaultLicenseDataUpdater.SAAS_LICENSE_DATA_PATH, null, 404 );
    }

    @Test
    public void testLicenseCategory()
        throws Exception
    {
        LicenseData licenseData = createLicenseData();
        setSaasResponseForURI( DefaultLicenseDataUpdater.SAAS_LICENSE_DATA_PATH, JsonHelpers.asJson( licenseData ), 200 );
        String newId = "New license category id";
        LicenseCategoryDAO licenseCategoryDAO = new LicenseCategoryDAO();
        assertNull( licenseCategoryDAO.getById( newId ) );

        LicenseCategory newLicenseCategory = new LicenseCategory();
        newLicenseCategory.setId( newId );
        newLicenseCategory.setName( "New name" );
        newLicenseCategory.setSeverity( 4 );
        licenseData.categories.add( newLicenseCategory );
        setSaasResponseForURI( DefaultLicenseDataUpdater.SAAS_LICENSE_DATA_PATH, JsonHelpers.asJson( licenseData ), 200 );
        assertNotNull( licenseCategoryDAO.getById( newId ) );
    }

    @Test
    public void testLicense()
        throws Exception
    {
        LicenseData licenseData = createLicenseData();
        setSaasResponseForURI( DefaultLicenseDataUpdater.SAAS_LICENSE_DATA_PATH, JsonHelpers.asJson( licenseData ), 200 );
        String newId = "New license id";
        LicenseDAO licenseDAO = new LicenseDAO();
        assertNull( licenseDAO.getById( newId ) );

        License newLicense = new License();
        newLicense.setId( newId );
        newLicense.setShortDisplayName( "New short name" );
        newLicense.setLongDisplayName( "New long name" );
        newLicense.setDescription( "New description" );
        newLicense.setLicenseCategoryId( "COPYLEFT" );
        licenseData.licenses.add( newLicense );
        setSaasResponseForURI( DefaultLicenseDataUpdater.SAAS_LICENSE_DATA_PATH, JsonHelpers.asJson( licenseData ), 200 );
        assertNotNull( licenseDAO.getById( newId ) );
    }

    @Test
    public void testMultiLicense()
        throws Exception
    {
        LicenseData licenseData = createLicenseData();
        setSaasResponseForURI( DefaultLicenseDataUpdater.SAAS_LICENSE_DATA_PATH, JsonHelpers.asJson( licenseData ), 200 );
        String newId = "New license id";
        MultiLicenseDAO multiLicenseDAO = new MultiLicenseDAO();
        assertNull( multiLicenseDAO.getById( newId ) );

        MultiLicense newMultiLicense = new MultiLicense();
        newMultiLicense.setId( newId );
        newMultiLicense.setShortDisplayName( "New short name" );
        newMultiLicense.setLongDisplayName( "New long name" );
        newMultiLicense.setDescription( "New description" );
        licenseData.multiLicenses.add( newMultiLicense );
        Set<String> multiLicenseMappings = new LinkedHashSet<String>();
        multiLicenseMappings.add( "GPL-2.0" );
        licenseData.multiLicenseMappings.put( newId, multiLicenseMappings );
        setSaasResponseForURI( DefaultLicenseDataUpdater.SAAS_LICENSE_DATA_PATH, JsonHelpers.asJson( licenseData ), 200 );
        assertNotNull( multiLicenseDAO.getById( newId ) );
        assertEquals( "GPL-2.0", multiLicenseDAO.getLicensesByMultiLicenseId( newId ).iterator().next().getId() );
    }

    @Test
    public void testNoSaaSServer()
    {
        saas.stop();

        String newId = "New license id";
        MultiLicenseDAO multiLicenseDAO = new MultiLicenseDAO();
        try
        {
            multiLicenseDAO.getById( newId );
            fail( "Expected RuntimeException" );
        }
        catch ( RuntimeException e )
        {
            assertTrue( e.getMessage().startsWith( "Could not retrieve license data from SaaS:" ) );
        }
    }

    private LicenseData createLicenseData()
    {
        LicenseData licenseData = new LicenseData();
        licenseData.categories = new ArrayList<LicenseCategory>();
        licenseData.licenses = new ArrayList<License>();
        licenseData.multiLicenses = new ArrayList<MultiLicense>();
        licenseData.multiLicenseMappings = new LinkedHashMap<String, Set<String>>();
        return licenseData;
    }
}
