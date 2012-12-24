/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.conditions.valuetype;

import java.util.List;

import com.sonatype.insight.brain.model.policy.ConditionValueType;

public class PercentageValueType
    implements ConditionValueType<Float>
{
    public static final String ID = "PercentageValueType";

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public String getDataType()
    {
        return "Integer";
    }

    @Override
    public boolean isAllowMultiple()
    {
        return false;
    }

    @Override
    public List<Float> getAvailableValues()
    {
        return null;
    }
}
