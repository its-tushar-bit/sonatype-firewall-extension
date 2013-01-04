/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.conditions.valuetype;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.plexus.util.IOUtil;

import com.sonatype.insight.brain.model.component.License;
import com.sonatype.insight.brain.model.policy.ConditionValueType;
import com.sonatype.insight.json.store.JsonUtils;

public class LicenseValueType
    implements ConditionValueType<License>
{
    public static final String ID = "LicenseValueType";

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

    public static License getLicenseById( String licenseId )
    {
        return licensesById.get( licenseId );
    }

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public String getDataType()
    {
        return "License";
    }

    @Override
    public boolean isAllowMultiple()
    {
        return false;
    }

    @Override
    public List<License> getAvailableValues()
    {
        return licenses;
    }

    private static License[] loadJson()
    {
        InputStream is = LicenseValueType.class.getClassLoader().getResourceAsStream( "licenses.json" );
        if ( is == null )
        {
            throw new RuntimeException( "Cannot find resource file licenses.json" );
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
}
