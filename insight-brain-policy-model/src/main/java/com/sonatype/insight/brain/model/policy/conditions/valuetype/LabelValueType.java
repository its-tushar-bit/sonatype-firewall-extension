/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.conditions.valuetype;

import java.util.List;

import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.policy.ConditionValueType;

public class LabelValueType
    implements ConditionValueType<Label>
{
    public static final String ID = "LabelValueType";

    private final String applicationId;

    public LabelValueType( String applicationId )
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
        return "Label";
    }

    @Override
    public boolean isAllowMultiple()
    {
        return false;
    }

    @Override
    public List<Label> getAvailableValues()
    {
        return new LabelDAO().getByApplicationId( applicationId );
    }
}
