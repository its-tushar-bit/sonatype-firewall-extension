/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.conditions.valuetype;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.sonatype.insight.brain.model.component.LicenseCategory;
import com.sonatype.insight.brain.model.policy.ConditionValueType;

public class LicenseCategoryValueType
    implements ConditionValueType<LicenseCategory>
{
    public static final String ID = "LicenseCategoryValueType";

    private static List<LicenseCategory> licenseCategories;

    static
    {
        // TODO Return a list of all known license categories from the datamart db
        licenseCategories = new ArrayList<LicenseCategory>();
        licenseCategories.add( new LicenseCategory( "COPYLEFT", "Copyleft" ) );
        licenseCategories.add( new LicenseCategory( "NON-STANDARD", "Non-Standard" ) );
        licenseCategories.add( new LicenseCategory( "NOT-PROVIDED", "Not Provided" ) );
        licenseCategories.add( new LicenseCategory( "WEAKCOPYLEFT", "Weak Copyleft" ) );
        licenseCategories.add( new LicenseCategory( "LIBERAL", "Liberal" ) );
        licenseCategories = Collections.unmodifiableList( licenseCategories );
    }

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public String getDataType()
    {
        return "String";
    }

    @Override
    public boolean isAllowMultiple()
    {
        return false;
    }

    @Override
    public List<LicenseCategory> getAvailableValues()
    {
        return licenseCategories;
    }
}
