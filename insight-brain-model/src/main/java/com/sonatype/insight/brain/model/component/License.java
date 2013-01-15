/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.plexus.util.IOUtil;

import com.sonatype.insight.json.store.JsonUtils;

public class License
{
    public static final String UNSPECIFIED_ID = "UNSPECIFIED";

    public static final String UNKNOWN_ID = "UNKNOWN";

    private String id;

    private String shortDisplayName;

    private String longDisplayName;

    private String description;

    private String licenseUrl;

    private String licenseCategoryId;

    private static List<License> licenses;

    private static Map<String, License> licensesById;

    static
    {
        // TODO Return a list of all known licenses from the datamart db
        License[] licenseArray = loadJson();
        licenses = Arrays.asList( licenseArray );
        licenses = Collections.unmodifiableList( licenses );

        licensesById = new LinkedHashMap<String, License>();
        for ( License license : licenses )
        {
            licensesById.put( license.getId(), license );
        }
    }

    public String getId()
    {
        return id;
    }

    public void setId( String id )
    {
        this.id = id;
    }

    public String getShortDisplayName()
    {
        return shortDisplayName;
    }

    public void setShortDisplayName( String shortDisplayName )
    {
        this.shortDisplayName = shortDisplayName;
    }

    public String getLongDisplayName()
    {
        return longDisplayName;
    }

    public void setLongDisplayName( String longDisplayName )
    {
        this.longDisplayName = longDisplayName;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription( String description )
    {
        this.description = description;
    }

    public String getLicenseUrl()
    {
        return licenseUrl;
    }

    public void setLicenseUrl( String licenseUrl )
    {
        this.licenseUrl = licenseUrl;
    }

    public String getLicenseCategoryId()
    {
        return licenseCategoryId;
    }

    public void setLicenseCategoryId( String licenseCategoryId )
    {
        this.licenseCategoryId = licenseCategoryId;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( id == null ) ? 0 : id.hashCode() );
        return result;
    }

    @Override
    public boolean equals( Object obj )
    {
        if ( this == obj )
            return true;
        if ( obj == null )
            return false;
        if ( getClass() != obj.getClass() )
            return false;
        License other = (License) obj;
        if ( id == null )
        {
            if ( other.id != null )
                return false;
        }
        else if ( !id.equals( other.id ) )
            return false;
        return true;
    }

    @Override
    public String toString()
    {
        return id;
    }

    private static License[] loadJson()
    {
        InputStream is = License.class.getClassLoader().getResourceAsStream( "licenses.json" );
        if ( is == null )
        {
            throw new RuntimeException( "Cannot find resource file: licenses.json" );
        }

        try
        {
            byte[] licenseData = IOUtil.toByteArray( is );
            return JsonUtils.parse( licenseData, License[].class );
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
        finally
        {
            IOUtil.close( is );
        }
    }

    public static List<License> getAll()
    {
        return licenses;
    }

    public static License getById( String licenseId )
    {
        return licensesById.get( licenseId );
    }
}
