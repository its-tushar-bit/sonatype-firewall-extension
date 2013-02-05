/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.license;

import java.util.List;

import javax.persistence.EntityManager;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.license.LicenseThreatGroupLicense;

public class LicenseThreatGroupLicenseDAO
    extends AbstractOperationalSqlDAO<LicenseThreatGroupLicense>
{
    @Override
    protected LicenseThreatGroupLicense getById( EntityManager em, String id )
    {
        String sQuery = "SELECT entity FROM LicenseThreatGroupLicense entity" + //
            " WHERE entity.id=?1";
        return get( em, sQuery, id );
    }

    private LicenseThreatGroupLicense getByApplicationIdAndLicenseId( EntityManager em, String applicationId,
                                                                      String licenseId )
    {
        String sQuery = "SELECT entity FROM LicenseThreatGroupLicense entity" + //
            " WHERE entity.applicationId=?1 AND entity.licenseId=?2";
        return get( em, sQuery, applicationId, licenseId );
    }

    List<LicenseThreatGroupLicense> getByLicenseThreatGroupId( EntityManager em, String licenseThreatGroupId )
    {
        String sQuery = "SELECT entity FROM LicenseThreatGroupLicense entity" + //
            " WHERE entity.licenseThreatGroupId=?1" + //
            " ORDER BY entity.licenseId";
        return getList( em, sQuery, licenseThreatGroupId );
    }

    public List<LicenseThreatGroupLicense> getByLicenseThreatGroupId( String licenseThreatGroupId )
    {
        EntityManager em = createEntityManager();
        try
        {
            return getByLicenseThreatGroupId( em, licenseThreatGroupId );
        }
        finally
        {
            close( em );
        }
    }

    @Override
    public void update( EntityManager em, LicenseThreatGroupLicense licenseThreatGroupLicense )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void insert( EntityManager em, LicenseThreatGroupLicense entity )
    {
        new LicenseDAO().getByIdNotNull( entity.getLicenseId() );

        LicenseThreatGroupLicense other =
            getByApplicationIdAndLicenseId( em, entity.getApplicationId(), entity.getLicenseId() );
        if ( other != null )
        {
            LicenseThreatGroup licenseThreatGroup =
                new LicenseThreatGroupDAO().getById( other.getLicenseThreatGroupId() );
            throw new InvalidLicenseThreatGroupLicenseException( "The license is already in the '"
                + licenseThreatGroup.getName() + "' license threat group" );
        }
        super.insert( em, entity );
    }
}
