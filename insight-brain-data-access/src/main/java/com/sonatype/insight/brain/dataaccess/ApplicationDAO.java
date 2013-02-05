/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;

import com.sonatype.insight.brain.dataaccess.license.LicenseDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupLicenseDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.license.License;
import com.sonatype.insight.brain.model.license.LicenseCategory;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.license.LicenseThreatGroupLicense;
import com.sonatype.insight.error.exception.NotFoundException;

public class ApplicationDAO
    extends AbstractOperationalSqlDAO<Application>
{
    public static final int DEFAULT_LICENSE_THREAT_GROUP_COUNT = 4;

    @Override
    protected Application getById( EntityManager em, String id )
    {
        String sQuery = "SELECT entity FROM Application entity" + //
            " WHERE entity.id=?1";
        return get( em, sQuery, id );
    }

    public Application getByIdNotNull( String id )
    {
        Application application = getById( id );
        if ( application == null )
        {
            throw new NotFoundException( "Cannot find application with id " + id );
        }
        return application;
    }

    public Application getOrInsertByPublicId( String publicId )
    {
        Application application = getByPublicId( publicId );
        if ( application == null )
        {
            application = new Application();
            application.setPublicId( publicId );
            insert( application );
        }
        return application;
    }

    public Application getByPublicId( String publicId )
    {
        if ( publicId == null || publicId.trim().isEmpty() )
        {
            throw new DataAccessException( "The application public ID cannot be null or empty." );
        }

        publicId = publicId.trim();
        String sQuery = "SELECT entity FROM Application entity" + //
            " WHERE entity.publicId=?1";
        return get( sQuery, publicId );
    }

    public Application getByPublicIdNotNull( String publicId )
    {
        Application application = getByPublicId( publicId );
        if ( application == null )
        {
            throw new NotFoundException( "Cannot find application with public id " + publicId );
        }
        return application;
    }

    @Override
    public void insert( EntityManager em, Application application )
    {
        super.insert( em, application );

        // Create default license threat groups for the new application
        LicenseThreatGroupDAO licenseThreatGroupDAO = new LicenseThreatGroupDAO();
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
                throw new NotFoundException( "Unknown license category id: " + licenseCategoryId );
            }
            LicenseThreatGroup licenseThreatGroup = licenseThreatGroupsByName.get( licenseThreatGroupName );
            if (licenseThreatGroup == null)
            {
                licenseThreatGroup = new LicenseThreatGroup();
                licenseThreatGroup.setApplicationId( application.getId() );
                licenseThreatGroup.setName( licenseThreatGroupName );
                licenseThreatGroup.setThreatLevel( threatLevel );
                licenseThreatGroupDAO.insert( em, licenseThreatGroup );
                licenseThreatGroupsByName.put( licenseThreatGroupName, licenseThreatGroup );
            }
            LicenseThreatGroupLicense licenseThreatGroupLicense = new LicenseThreatGroupLicense();
            licenseThreatGroupLicense.setApplicationId( application.getId() );
            licenseThreatGroupLicense.setLicenseThreatGroupId( licenseThreatGroup.getId() );
            licenseThreatGroupLicense.setMultiLicenseId( license.getId() );
            licenseThreatGroupLicenseDAO.insert( em, licenseThreatGroupLicense );
        }
    }

    @Override
    public void delete( EntityManager em, Application application )
    {
        LicenseThreatGroupDAO licenseThreatGroupDAO = new LicenseThreatGroupDAO();
        List<LicenseThreatGroup> licenseThreatGroups = licenseThreatGroupDAO.getByApplicationId( application.getId() );
        for ( LicenseThreatGroup licenseThreatGroup : licenseThreatGroups )
        {
            licenseThreatGroupDAO.delete( em, licenseThreatGroup );
        }
        super.delete( em, application );
    }
}
