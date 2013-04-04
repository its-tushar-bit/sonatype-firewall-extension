/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.license;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.model.license.License;
import com.sonatype.insight.brain.model.license.LicenseCategory;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.license.LicenseThreatGroupLicense;
import com.sonatype.insight.error.exception.NotFoundException;

public class LicenseThreatGroupDAO
    extends AbstractOperationalSqlDAO<LicenseThreatGroup>
{
    public static final int DEFAULT_LICENSE_THREAT_GROUP_COUNT = 4;

    private static final Logger log = LoggerFactory.getLogger( LicenseThreatGroupDAO.class );

    public List<LicenseThreatGroup> getByApplicationId( EntityManager em, String applicationId )
    {
        String sQuery = "SELECT entity FROM LicenseThreatGroup entity" + //
            " WHERE entity.applicationId=?1" + //
            " ORDER BY entity.name";
        return getList( em, sQuery, applicationId );
    }

    public List<LicenseThreatGroup> getByApplicationId( String applicationId )
    {
        EntityManager em = createEntityManager();
        try
        {
            return getByApplicationId( em, applicationId );
        }
        finally
        {
            close( em );
        }
    }

    public LicenseThreatGroup getByApplicationIdAndLicenseId( String applicationId, String licenseId )
    {
        String sQuery = "SELECT licenseThreatGroup" + //
            " FROM LicenseThreatGroup licenseThreatGroup, LicenseThreatGroupLicense licenseThreatGroupLicense" + //
            " WHERE licenseThreatGroup.id=licenseThreatGroupLicense.licenseThreatGroupId" + //
            " AND licenseThreatGroup.applicationId=?1 AND licenseThreatGroupLicense.licenseId=?2";
        return get( sQuery, applicationId, licenseId );
    }

    @Override
    protected LicenseThreatGroup getById( EntityManager em, String id )
    {
        String sQuery = "SELECT entity FROM LicenseThreatGroup entity" + //
            " WHERE entity.id=?1";
        return get( em, sQuery, id );
    }

    LicenseThreatGroup getByIdNotNull( EntityManager em, String id )
    {
        LicenseThreatGroup licenseThreatGroup = getById( id );
        if ( licenseThreatGroup == null )
        {
            throw new NotFoundException( "Cannot find a license threat group with id " + id );
        }
        return licenseThreatGroup;
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

    public void createDefaultGroups( EntityManager em, String applicationId )
    {
        long start = System.currentTimeMillis();

        LicenseThreatGroupLicenseDAO licenseThreatGroupLicenseDAO = new LicenseThreatGroupLicenseDAO();

        Map<String, LicenseThreatGroup> licenseThreatGroupsByName = new LinkedHashMap<String, LicenseThreatGroup>();
        List<License> allLicenses = new LicenseDAO().getAll();
        for ( License license : allLicenses )
        {
            String licenseCategoryId = license.getLicenseCategoryId();
            if ( licenseCategoryId == null )
            {
                continue;
            }
            String licenseThreatGroupName = null;
            int threatLevel = 0;
            if ( LicenseCategory.COPYLEFT_ID.equals( licenseCategoryId ) )
            {
                licenseThreatGroupName = "Copyleft";
                threatLevel = 9;
            }
            else if ( LicenseCategory.NON_STANDARD_ID.equals( licenseCategoryId ) )
            {
                licenseThreatGroupName = "Non Standard";
                threatLevel = 6;
            }
            else if ( LicenseCategory.WEAKCOPYLEFT_ID.equals( licenseCategoryId ) )
            {
                licenseThreatGroupName = "Weak Copyleft";
                threatLevel = 2;
            }
            else if ( LicenseCategory.LIBERAL_ID.equals( licenseCategoryId ) )
            {
                licenseThreatGroupName = "Liberal";
                threatLevel = 0;
            }
            else
            {
                throw new IllegalStateException( "Unknown license category id: " + licenseCategoryId );
            }
            LicenseThreatGroup licenseThreatGroup = licenseThreatGroupsByName.get( licenseThreatGroupName );
            if ( licenseThreatGroup == null )
            {
                licenseThreatGroup = new LicenseThreatGroup();
                licenseThreatGroup.setApplicationId( applicationId );
                licenseThreatGroup.setName( licenseThreatGroupName );
                licenseThreatGroup.setThreatLevel( threatLevel );
                insert( em, licenseThreatGroup );
                licenseThreatGroupsByName.put( licenseThreatGroupName, licenseThreatGroup );
            }
            LicenseThreatGroupLicense licenseThreatGroupLicense = new LicenseThreatGroupLicense();
            licenseThreatGroupLicense.setApplicationId( applicationId );
            licenseThreatGroupLicense.setLicenseThreatGroupId( licenseThreatGroup.getId() );
            licenseThreatGroupLicense.setLicenseId( license.getId() );
            licenseThreatGroupLicenseDAO.insert( em, licenseThreatGroupLicense );
        }

        log.debug( "Created default license threat groups for application id {} in {} ms.", applicationId,
                   System.currentTimeMillis() - start );
    }
}
