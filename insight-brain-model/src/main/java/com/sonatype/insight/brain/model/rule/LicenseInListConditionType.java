/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.rule;

import java.util.ArrayList;
import java.util.List;

public class LicenseInListConditionType
    implements ConditionType
{
    public static final String ID = "LicenseInList";

    private static List<String> supportedOperators = new ArrayList<String>();

    static
    {
        supportedOperators.add( "in" );
        supportedOperators.add( "not in" );
    }

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public String getOperandName()
    {
        return "License";
    }

    @Override
    public List<String> getSupportedOperators()
    {
        return supportedOperators;
    }

    @Override
    public List<String> getAvailableValues()
    {
        // TODO Return a list of all known licenses
        List<String> licenses = new ArrayList<String>();
        licenses.add( "Apache-2.0" );
        licenses.add( "EPL-1.0" );
        licenses.add( "GPL-2.0" );
        licenses.add( "Not Provided" );
        licenses.add( "Non-Standard" );
        return licenses;
    }

    @Override
    public String generateDroolsCode( SimpleCondition condition )
    {
        // TODO Auto-generated method stub
        return null;
    }
}
