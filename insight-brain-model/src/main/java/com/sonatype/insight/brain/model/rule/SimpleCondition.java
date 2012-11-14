/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.rule;

public class SimpleCondition
{
    private ConditionType conditionType;

    private String operator;

    private String value;

    public ConditionType getConditionType()
    {
        return conditionType;
    }

    public void setConditionType( ConditionType conditionType )
    {
        this.conditionType = conditionType;
    }

    public String getOperator()
    {
        return operator;
    }

    public void setOperator( String operator )
    {
        this.operator = operator;
    }

    public String getValue()
    {
        return value;
    }

    public void setValue( String value )
    {
        this.value = value;
    }
}
