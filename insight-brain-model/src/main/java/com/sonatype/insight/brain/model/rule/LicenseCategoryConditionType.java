/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.rule;

import java.util.ArrayList;
import java.util.List;

public class LicenseCategoryConditionType
    implements ConditionType
{
    public static final String ID = "LicenseCategory";

    private static List<String> supportedOperators = new ArrayList<String>();

    static
    {
        supportedOperators.add( "is" );
        supportedOperators.add( "is not" );
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
        // TODO Return a list of all known license categories
        List<String> licenseCategories = new ArrayList<String>();
        licenseCategories.add( "Copyleft" );
        licenseCategories.add( "Non-Standard" );
        licenseCategories.add( "Not Provided" );
        licenseCategories.add( "Weak Copyleft" );
        licenseCategories.add( "Liberal" );
        return licenseCategories;
    }

    @Override
    public String generateDroolsCode( SimpleCondition condition )
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean requiresValue()
    {
        return true;
    }
}
