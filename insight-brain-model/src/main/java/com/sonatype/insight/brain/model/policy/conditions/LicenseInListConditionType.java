/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.conditions;

import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.ConditionType;

public class LicenseInListConditionType
    extends AbstractLicenseInListConditionType
    implements ConditionType
{
    public static final String ID = "LicenseInList";

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public String getName()
    {
        return "License";
    }

    @Override
    public String generateDroolsCode( Condition condition )
    {
        String methodName;
        if ("in list".equals( condition.getOperator() ))
        {
            methodName = "hasLicenseInList";
        }
        else
        {
            methodName = "hasLicenseNotInList";
        }
        
        return methodName + "( \"" + condition.getValue() + "\".split(\",\") )";
    }
}
