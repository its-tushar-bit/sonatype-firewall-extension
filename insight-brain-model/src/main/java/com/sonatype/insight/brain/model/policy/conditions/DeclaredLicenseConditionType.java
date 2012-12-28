/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.conditions;

import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.ConditionType;

public class DeclaredLicenseConditionType
    extends AbstractLicenseInListConditionType
    implements ConditionType
{
    public static final String ID = "DeclaredLicense";

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public String getName()
    {
        return "Declared License";
    }

    @Override
    public String generateDroolsCode( Condition condition )
    {
        return ( "is".equals( condition.getOperator() ) ? "" : "! " ) + "hasDeclaredLicenseId( \""
            + condition.getValue() + "\" )";
    }
}
