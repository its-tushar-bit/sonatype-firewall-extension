/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.conditions.valuetype;

import java.util.List;

import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.model.component.MultiLicense;
import com.sonatype.insight.brain.model.policy.ConditionValueType;

public class LicenseValueType
    implements ConditionValueType<MultiLicense>
{
    public static final String ID = "LicenseValueType";

    public static MultiLicense getLicenseById( String licenseId )
    {
        return new MultiLicenseDAO().getById( licenseId );
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
    public List<MultiLicense> getAvailableValues()
    {
        return new MultiLicenseDAO().getAll();
    }
}
