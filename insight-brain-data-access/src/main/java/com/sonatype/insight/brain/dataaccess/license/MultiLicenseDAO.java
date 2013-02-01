/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.license;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.plexus.util.IOUtil;

import com.sonatype.insight.brain.model.license.MultiLicense;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.json.store.JsonUtils;

public class MultiLicenseDAO
{
    private static List<MultiLicense> multiLicenses;

    private static Map<String, MultiLicense> multiLicensesById;

    private static Map<String, MultiLicense> multiLicensesByName;

    static
    {
        // TODO Return a list of all known licenses from the datamart db
        MultiLicense[] licenseArray = loadJson();
        multiLicenses = Arrays.asList( licenseArray );

        multiLicensesById = new LinkedHashMap<String, MultiLicense>();
        multiLicensesByName = new LinkedHashMap<String, MultiLicense>();
        for ( MultiLicense license : multiLicenses )
        {
            multiLicensesById.put( license.getId(), license );
            multiLicensesByName.put( license.getShortDisplayName(), license );
        }
    }

    private static MultiLicense[] loadJson()
    {
        InputStream is = MultiLicense.class.getClassLoader().getResourceAsStream( "multi-licenses.json" );
        if ( is == null )
        {
            throw new RuntimeException( "Cannot find resource: multi-licenses.json" );
        }

        try
        {
            byte[] licenseData = IOUtil.toByteArray( is );
            return JsonUtils.parse( licenseData, MultiLicense[].class );
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

    public List<MultiLicense> getAll()
    {
        return multiLicenses;
    }

    public MultiLicense getById( String licenseId )
    {
        return multiLicensesById.get( licenseId );
    }

    public MultiLicense getByIdNotNull( String licenseName )
    {
        MultiLicense license = multiLicensesById.get( licenseName );
        if ( license == null )
        {
            throw new NotFoundException( "A license with id '" + licenseName + "' does not exist." );
        }
        return license;
    }

    public MultiLicense getByNameNotNull( String licenseName )
    {
        MultiLicense license = multiLicensesByName.get( licenseName );
        if ( license == null )
        {
            throw new NotFoundException( "A license with name '" + licenseName + "' does not exist." );
        }
        return license;
    }
}
