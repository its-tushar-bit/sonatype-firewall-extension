package com.sonatype.insight.brain.dataaccess.license;

import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.license.MultiLicense;

public class MultiLicenseDAOTest
    extends AbstractDbDAOTest
{
    @Test
    public void testCRUD()
        throws Exception
    {
        MultiLicenseDAO dao = new MultiLicenseDAO();

        MultiLicense multiLicense = new MultiLicense();
        multiLicense.setDescription( "Description" );
        multiLicense.setLicenseUrl( "License Url" );
        multiLicense.setShortDisplayName( "SDN" );
        multiLicense.setLongDisplayName( "Long Display Name" );
        dao.insert( multiLicense );
        Assert.assertNotNull( multiLicense.getId() );

        multiLicense = dao.getById( multiLicense.getId() );
        Assert.assertNotNull( multiLicense );
        Assert.assertEquals( "Description", multiLicense.getDescription() );
        Assert.assertEquals( "License Url", multiLicense.getLicenseUrl() );
        Assert.assertEquals( "SDN", multiLicense.getShortDisplayName() );
        Assert.assertEquals( "Long Display Name", multiLicense.getLongDisplayName() );

        multiLicense.setLongDisplayName( "New Long Display Name" );
        dao.update( multiLicense );

        dao.getById( multiLicense.getId() );
        Assert.assertNotNull( multiLicense );
        Assert.assertEquals( "New Long Display Name", multiLicense.getLongDisplayName() );

        dao.delete( multiLicense );

        multiLicense = dao.getById( multiLicense.getId() );
        Assert.assertNull( multiLicense );
    }

    @Test
    public void testGetAll()
    {
        MultiLicenseDAO dao = new MultiLicenseDAO();
        Collection<MultiLicense> multiLicenses = dao.getAll();

        Assert.assertNotNull( multiLicenses );
        Assert.assertFalse( multiLicenses.isEmpty() );
    }

    @Test
    public void testGetLicenseThreatLevelByApplicationIdAndMultiLicenseId()
    {
        MultiLicenseDAO dao = new MultiLicenseDAO();
        Collection<MultiLicense> multiLicenses = dao.getAll();

        for ( MultiLicense multiLicense : multiLicenses )
        {
            Integer threat =
                dao.getLicenseThreatLevelByApplicationIdAndMultiLicenseId( applicationId, multiLicense.getId() );
            Assert.assertTrue( "Multilicense Threat Level between null and 10", threat == null
                || ( threat >= 0 && threat <= 10 ) );
        }
    }
}
