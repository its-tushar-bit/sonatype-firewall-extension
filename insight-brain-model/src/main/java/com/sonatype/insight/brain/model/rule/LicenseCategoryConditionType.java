/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.rule;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class LicenseCategoryConditionType
    implements ConditionType
{
    public static final String ID = "LicenseCategory";

    private static List<String> supportedOperators = new ArrayList<String>();

    private static List<String> licenseCategoryNames = new ArrayList<String>();

    private static Map<String, String> licenseCategoryIdsByName = new LinkedHashMap<String, String>();

    static
    {
        supportedOperators.add( "is" );
        supportedOperators.add( "is not" );

        // TODO Return a list of all known license categories from the datamart db
        licenseCategoryIdsByName.put( "Copyleft", "COPYLEFT" );
        licenseCategoryIdsByName.put( "Non-Standard", "NON-STANDARD" );
        licenseCategoryIdsByName.put( "Not Provided", "NOT-PROVIDED" );
        licenseCategoryIdsByName.put( "Weak Copyleft", "WEAKCOPYLEFT" );
        licenseCategoryIdsByName.put( "Liberal", "LIBERAL" );

        licenseCategoryNames.addAll( licenseCategoryIdsByName.keySet() );
    }

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public String getOperandName()
    {
        return "License Category";
    }

    @Override
    public List<String> getSupportedOperators()
    {
        return supportedOperators;
    }

    @Override
    public List<String> getAvailableValues()
    {
        return licenseCategoryNames;
    }

    @Override
    public String generateDroolsCode( final SimpleCondition condition )
    {
        return "getLicenseThreat() " + ( "is".equals( condition.getOperator() ) ? "==" : "!=" ) + " \""
            + licenseCategoryIdsByName.get( condition.getValue() ) + "\"";
    }

    @Override
    public boolean isRequiresValue()
    {
        return true;
    }
}
