/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.license;

import java.util.List;

import javax.persistence.EntityManager;

import com.sonatype.insight.brain.dataaccess.AbstractSqlDAO;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.license.LicenseThreatGroupLicense;

public class LicenseThreatGroupDAO
    extends AbstractSqlDAO<LicenseThreatGroup>
{
    public List<LicenseThreatGroup> getByApplicationId( String applicationId )
    {
        String sQuery = "SELECT entity FROM LicenseThreatGroup entity" + //
            " WHERE entity.applicationId=?1" + //
            " ORDER BY entity.name";
        return getList( sQuery, applicationId );
    }

    @Override
    protected LicenseThreatGroup getById( EntityManager em, String id )
    {
        String sQuery = "SELECT entity FROM LicenseThreatGroup entity" + //
            " WHERE entity.id=?1";
        return get( em, sQuery, id );
    }

    private LicenseThreatGroup getByApplicationIdAndName( EntityManager em, String applicationId, String name )
    {
        String sQuery = "SELECT entity FROM LicenseThreatGroup entity" + //
            " WHERE entity.applicationId=?1 AND entity.name=?2";
        return get( em, sQuery, applicationId, name );
    }

    @Override
    public void insert( EntityManager em, LicenseThreatGroup licenseThreatGroup )
    {
        validateThreatLevel( licenseThreatGroup.getThreatLevel() );
        if ( getByApplicationIdAndName( em, licenseThreatGroup.getApplicationId(), licenseThreatGroup.getName() ) != null )
        {
            throw new InvalidLicenseThreatGroupException( "A license threat group with the same name already exists" );
        }
        super.insert( em, licenseThreatGroup );
    }

    @Override
    public void update( EntityManager em, LicenseThreatGroup licenseThreatGroup )
    {
        validateThreatLevel( licenseThreatGroup.getThreatLevel() );
        LicenseThreatGroup otherLicenseThreatGroup =
            getByApplicationIdAndName( em, licenseThreatGroup.getApplicationId(), licenseThreatGroup.getName() );
        if ( otherLicenseThreatGroup != null && !otherLicenseThreatGroup.getId().equals( licenseThreatGroup.getId() ) )
        {
            throw new InvalidLicenseThreatGroupException( "A license threat group with the same name already exists" );
        }
        super.update( em, licenseThreatGroup );
    }

    private void validateThreatLevel( int threatLevel )
    {
        if ( threatLevel < 0 || threatLevel > 10 )
        {
            throw new InvalidLicenseThreatGroupException( "The threat level must be a number between 0 and 10" );
        }
    }

    @Override
    public void delete( EntityManager em, LicenseThreatGroup licenseThreatGroup )
    {
        LicenseThreatGroupLicenseDAO licenseThreatGroupLicenseDAO = new LicenseThreatGroupLicenseDAO();
        List<LicenseThreatGroupLicense> licenseThreatGroupLicenses =
            licenseThreatGroupLicenseDAO.getByLicenseThreatGroupId( em, licenseThreatGroup.getId() );
        for ( LicenseThreatGroupLicense licenseThreatGroupLicense : licenseThreatGroupLicenses )
        {
            licenseThreatGroupLicenseDAO.delete( em, licenseThreatGroupLicense );
        }
        super.delete( em, licenseThreatGroup );
    }
}
