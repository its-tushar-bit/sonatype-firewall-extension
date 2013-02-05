package com.sonatype.insight.brain.dataaccess.license;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.insight.brain.dataaccess.AbstractDatamartSqlDAO;
import com.sonatype.insight.brain.model.license.LicenseCategory;
import com.sonatype.insight.error.exception.NotFoundException;

// Copied from com.sonatype.insight.datamart.dao.LicenseCategoryDAO
public class LicenseCategoryDAO
    extends AbstractDatamartSqlDAO<LicenseCategory>
    implements LicenseMXBean
{
    private static final Logger log = LoggerFactory.getLogger( LicenseCategoryDAO.class );

    private static volatile Map<String, LicenseCategory> licenseCategoriesById = null;

    private static volatile Map<String, LicenseCategory> licenseCategoriesByName = null;

    @Override
    public LicenseCategory getById( String id )
    {
        if ( licenseCategoriesById == null )
        {
            load();
        }
        return licenseCategoriesById.get( id );
    }

    public LicenseCategory getByIdNotNull( String id )
    {
        LicenseCategory licenseCategory = getById( id );
        if ( licenseCategory == null )
        {
            throw new NotFoundException( "A license category with id '" + id + "' does not exist." );
        }
        return licenseCategory;
    }

    public LicenseCategory getByName( String name )
    {
        if ( licenseCategoriesByName == null )
        {
            load();
        }
        return licenseCategoriesByName.get( name );
    }

    public LicenseCategory getByNameNotNull( String name )
    {
        LicenseCategory licenseCategory = getByName( name );
        if ( licenseCategory == null )
        {
            throw new NotFoundException( "A license category with name '" + name + "' does not exist." );
        }
        return licenseCategory;
    }

    private synchronized void load()
    {
        long start = System.currentTimeMillis();

        String sQuery = "SELECT entity FROM LicenseCategory entity" + //
            " ORDER BY entity.severity DESC";
        List<LicenseCategory> licenseCategories = getList( sQuery );

        Map<String, LicenseCategory> _licenseCategoriesById = new LinkedHashMap<String, LicenseCategory>();
        for ( LicenseCategory licenseCategory : licenseCategories )
        {
            _licenseCategoriesById.put( licenseCategory.getId(), licenseCategory );
        }
        licenseCategoriesById = _licenseCategoriesById;

        Map<String, LicenseCategory> _licenseCategoriesByName =
            new TreeMap<String, LicenseCategory>( String.CASE_INSENSITIVE_ORDER );
        for ( LicenseCategory licenseCategory : licenseCategories )
        {
            _licenseCategoriesByName.put( licenseCategory.getName(), licenseCategory );
        }
        licenseCategoriesByName = _licenseCategoriesByName;

        log.debug( "Loaded all license categories in {} ms.", System.currentTimeMillis() - start );
    }

    @Override
    public void reloadCache()
    {
        load();
    }

    public Collection<LicenseCategory> getAll()
    {
        if ( licenseCategoriesById == null )
        {
            load();
        }
        return licenseCategoriesById.values();
    }
}
