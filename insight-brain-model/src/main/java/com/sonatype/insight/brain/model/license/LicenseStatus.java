/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.license;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class LicenseStatus
{
    private final static Map<String, LicenseStatus> statusByName = new LinkedHashMap<String, LicenseStatus>();

    private final static Map<String, LicenseStatus> statusById = new LinkedHashMap<String, LicenseStatus>();

    private final static List<LicenseStatus> all = new ArrayList<LicenseStatus>();

    // Note: The order the statuses are defined here determines the order they are displayed in the UI
    public static final LicenseStatus OPEN = new LicenseStatus( "OPEN", "Open" );

    public static final LicenseStatus ACKNOWLEDGED = new LicenseStatus( "ACKNOWLEDGED", "Acknowledged" );

    public static final LicenseStatus OVERRIDDEN = new LicenseStatus( "OVERRIDDEN", "Overridden" );

    public static final LicenseStatus SELECTED = new LicenseStatus( "SELECTED", "Selected" );

    public static final LicenseStatus CONFIRMED = new LicenseStatus( "CONFIRMED", "Confirmed" );

    private final String id;

    private final String name;

    private LicenseStatus( String id, String name )
    {
        this.id = id;
        this.name = name;
        statusById.put( id, this );
        statusByName.put( name, this );
        all.add( this );
    }

    public static LicenseStatus getByName( String name )
    {
        return statusByName.get( name );
    }

    public static LicenseStatus getById( String id )
    {
        return statusById.get( id );
    }

    public static List<LicenseStatus> getAll()
    {
        return Collections.unmodifiableList( all );
    }

    public String getId()
    {
        return id;
    }

    public String getName()
    {
        return name;
    }

    @Override
    public String toString()
    {
        return id;
    }
}
