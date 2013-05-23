/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.license;

import java.util.List;
import java.util.Locale;

import org.junit.Assert;
import org.junit.Test;

import com.sonatype.insight.brain.model.license.License;

public class LicenseDAOTest
    extends AbstractLicenseDAOTest
{
    @Test
    public void testGetAll()
    {
        LicenseDAO dao = new LicenseDAO();
        List<License> licenses = dao.getAll();
        Assert.assertNotNull( licenses );
        Assert.assertTrue( licenses.size() > 0 );
        for ( int i = 0; i < licenses.size() - 1; i++ )
        {
            License license1 = licenses.get( i );
            License license2 = licenses.get( i + 1 );
            Assert.assertTrue( license1.getShortDisplayName() + " >= " + license2.getShortDisplayName(),
                               license1.getShortDisplayName().toLowerCase( Locale.ENGLISH ).compareTo( license2.getShortDisplayName().toLowerCase( Locale.ENGLISH ) ) < 0 );
        }
    }

    @Test
    public void testLicenseDataRefresh()
    {
        String newId = "new license id";
        LicenseDAO dao = new LicenseDAO();
        Assert.assertNull( dao.getById( newId ) );
        int count = dao.getAll().size();

        License newLicense = new License();
        newLicense.setId( newId );
        newLicense.setShortDisplayName( "New short name" );
        newLicense.setLongDisplayName( "New long name" );
        newLicense.setDescription( "New description" );
        newLicense.setLicenseCategoryId( "COPYLEFT" );
        dao.insert( newLicense );
        Assert.assertNull( dao.getById( newId ) );

        LicenseDataUpdater.setUpdater( new DummyLicenseDataUpdater() );

        Assert.assertNotNull( dao.getById( newId ) );
        Assert.assertEquals( count + 1, dao.getAll().size() );

        dao.delete( newLicense );
        dao.load();
    }
}
