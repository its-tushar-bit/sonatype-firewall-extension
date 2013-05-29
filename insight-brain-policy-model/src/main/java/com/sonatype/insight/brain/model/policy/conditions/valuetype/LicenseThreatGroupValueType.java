/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.conditions.valuetype;

import java.util.List;

import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.policy.ConditionValueType;

public class LicenseThreatGroupValueType
    implements ConditionValueType<LicenseThreatGroup>
{
    public static final String ID = "LicenseThreatGroupValueType";

    private final String applicationId;

    public LicenseThreatGroupValueType( String applicationId )
    {
        this.applicationId = applicationId;
    }

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public String getDataType()
    {
        return "LicenseThreatGroup";
    }

    @Override
    public boolean isAllowMultiple()
    {
        return false;
    }

    @Override
    public List<LicenseThreatGroup> getAvailableValues()
    {
        return new LicenseThreatGroupDAO().getByOwnerId( applicationId );
    }
}
