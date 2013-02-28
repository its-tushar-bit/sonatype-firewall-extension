package com.sonatype.insight.brain.dataaccess.license;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sonatype.insight.brain.dataaccess.AbstractDatamartSqlDAO;
import com.sonatype.insight.brain.model.license.License;
import com.sonatype.insight.error.exception.NotFoundException;

// Copied from com.sonatype.insight.datamart.dao.LicenseDAO
public class LicenseDAO
    extends AbstractDatamartSqlDAO<License>
    implements LicenseMXBean
{
    private static final Logger log = LoggerFactory.getLogger( LicenseDAO.class );

    private static volatile List<License> licenses;

    private static volatile Map<String, License> licensesById = null;

    public LicenseDAO()
    {
        if ( licenses == null )
        {
            load();
        }
    }

    @Override
    public License getById( String id )
    {
        return licensesById.get( id );
    }

    public License getByIdNotNull( String id )
    {
        License license = getById( id );
        if ( license == null )
        {
            throw new NotFoundException( "A license with id '" + id + "' does not exist." );
        }
        return license;
    }

    private void load()
    {
        synchronized ( this.getClass() )
        {
            long start = System.currentTimeMillis();

            String sQuery = "SELECT license FROM License license";
            licenses = new ArrayList<License>();
            licenses.addAll( getList( sQuery ) );
            Collections.sort( licenses, new Comparator<License>()
            {
                @Override
                public int compare( License license1, License license2 )
                {
                    return license1.getShortDisplayName().toLowerCase( Locale.ENGLISH ).compareTo( license2.getShortDisplayName().toLowerCase( Locale.ENGLISH ) );
                }
            } );
            licenses = Collections.unmodifiableList( licenses );

            Map<String, License> _licensesById = new LinkedHashMap<String, License>();
            for ( License license : licenses )
            {
                _licensesById.put( license.getId(), license );
            }
            licensesById = _licensesById;

            log.debug( "Loaded all licenses in {} ms.", System.currentTimeMillis() - start );
        }
    }

    @Override
    public void insert( License license )
    {
        super.insert( license );
        load();
    }

    @Override
    public void delete( License license )
    {
        super.delete( license );
        load();
    }

    @Override
    public void reloadCache()
    {
        load();
    }

    public List<License> getAll()
    {
        return licenses;
    }
}
