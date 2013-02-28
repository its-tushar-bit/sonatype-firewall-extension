package com.sonatype.insight.brain.dataaccess.license;

import java.lang.management.ManagementFactory;
import java.util.Collection;
import java.util.Collections;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.management.ObjectName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.insight.brain.dataaccess.AbstractDatamartSqlDAO;
import com.sonatype.insight.brain.model.license.License;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.license.MultiLicense;
import com.sonatype.insight.brain.model.license.MultiLicenseLicenseInternal;
import com.sonatype.insight.error.exception.NotFoundException;

// Copied from com.sonatype.insight.datamart.dao.MultiLicenseDAO
public class MultiLicenseDAO
    extends AbstractDatamartSqlDAO<MultiLicense>
    implements LicenseMXBean
{
    private static final Logger log = LoggerFactory.getLogger( MultiLicenseDAO.class );

    private static volatile Map<String, MultiLicense> multiLicensesById = null;

    private static volatile Map<String, MultiLicense> multiLicensesByName = null;

    private static volatile Map<String, Set<License>> licenseSetsById = null;

    public MultiLicenseDAO()
    {
        try
        {
            String hash = String.format( "0x%08X", new Integer( System.identityHashCode( this ) ) );
            Hashtable<String, String> props = new Hashtable<String, String>();
            props.put( "type", getClass().getSimpleName() );
            props.put( "hash", hash );
            ObjectName jmxName = ObjectName.getInstance( "com.sonatype.insight", props );
            ManagementFactory.getPlatformMBeanServer().registerMBean( this, jmxName );
        }
        catch ( Exception e )
        {
            log.error( "Could not register LicenseMXBean", e );
        }
    }

    public Collection<MultiLicense> getAll()
    {
        return multiLicensesByName.values();
    }

    @Override
    public MultiLicense getById( String id )
    {
        if ( multiLicensesById == null )
        {
            load();
        }
        return multiLicensesById.get( id );
    }

    public MultiLicense getByIdNotNull( String id )
    {
        MultiLicense license = getById( id );
        if ( license == null )
        {
            // most probably, a new license was added to the DB so reload the caches and try again
            log.debug( "Reloading license caches after miss for {}", id );
            reloadCache();

            license = getById( id );
            if ( license == null )
            {
                throw new NotFoundException( "A license with id '" + id + "' does not exist." );
            }
        }
        return license;
    }

    public MultiLicense getByName( String name )
    {
        if ( multiLicensesByName == null )
        {
            load();
        }
        return multiLicensesByName.get( name );
    }

    public MultiLicense getByNameNotNull( String name )
    {
        MultiLicense license = getByName( name );
        if ( license == null )
        {
            throw new NotFoundException( "A license with name '" + name + "' does not exist." );
        }
        return license;
    }

    public Set<License> getLicensesByMultiLicenseId( String id )
    {
        if ( licenseSetsById == null )
        {
            load();
        }
        return licenseSetsById.get( id );
    }

    public Integer getLicenseThreatLevelByApplicationIdAndMultiLicenseId( String appId, String multiLicenseId )
    {
        final LicenseThreatGroupDAO licenseThreatGroupDAO = new LicenseThreatGroupDAO();
        final Set<License> licenses = getLicensesByMultiLicenseId( multiLicenseId );
        Integer threatLevel = null;
        for ( License license : licenses )
        {
            LicenseThreatGroup licenseThreatGroup =
                licenseThreatGroupDAO.getByApplicationIdAndLicenseId( appId, license.getId() );
            if ( licenseThreatGroup != null )
            {
                if ( threatLevel == null )
                {
                    threatLevel = licenseThreatGroup.getThreatLevel();
                }
                else
                {
                    threatLevel = Math.max( threatLevel, licenseThreatGroup.getThreatLevel() );
                }
            }
        }
        return threatLevel;
    }

    private synchronized void load()
    {
        long start = System.currentTimeMillis();

        String sQuery = "SELECT license FROM MultiLicense license" + //
            " ORDER BY license.shortDisplayName";
        List<MultiLicense> multiLicenses = getList( sQuery );

        sQuery = "SELECT license FROM MultiLicenseLicenseInternal license";
        @SuppressWarnings( { "unchecked", "rawtypes" } )
        List<MultiLicenseLicenseInternal> mappings = (List) getList( sQuery );

        Map<String, Set<License>> _licenseSetsById = new LinkedHashMap<String, Set<License>>();

        Map<String, MultiLicense> _licensesById = new LinkedHashMap<String, MultiLicense>();
        for ( MultiLicense license : multiLicenses )
        {
            _licensesById.put( license.getId(), license );
            _licenseSetsById.put( license.getId(), new LinkedHashSet<License>() );
        }
        multiLicensesById = _licensesById;

        Map<String, MultiLicense> _licensesByName = new TreeMap<String, MultiLicense>( String.CASE_INSENSITIVE_ORDER );
        for ( MultiLicense license : multiLicenses )
        {
            _licensesByName.put( license.getShortDisplayName(), license );
        }
        multiLicensesByName = _licensesByName;

        LicenseDAO licenseDAO = new LicenseDAO();
        for ( MultiLicenseLicenseInternal mapping : mappings )
        {
            License license = licenseDAO.getByIdNotNull( mapping.getLicenseId() );
            _licenseSetsById.get( mapping.getMultiLicenseId() ).add( license );
        }

        for ( Map.Entry<String, Set<License>> entry : _licenseSetsById.entrySet() )
        {
            entry.setValue( Collections.unmodifiableSet( entry.getValue() ) );
        }
        licenseSetsById = _licenseSetsById;

        log.debug( "Loaded all multi-licenses in {} ms.", System.currentTimeMillis() - start );
    }

    @Override
    public void insert( MultiLicense license )
    {
        super.insert( license );
        load();
    }

    @Override
    public void delete( MultiLicense license )
    {
        super.delete( license );
        load();
    }

    @Override
    public void reloadCache()
    {
        new LicenseCategoryDAO().reloadCache();
        new LicenseDAO().reloadCache();
        load();
    }
}
