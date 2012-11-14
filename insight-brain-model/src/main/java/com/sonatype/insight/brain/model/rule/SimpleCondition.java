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

    protected ConditionType getConditionType()
    {
        return conditionType;
    }

    protected void setConditionType( ConditionType conditionType )
    {
        this.conditionType = conditionType;
    }

    protected String getOperator()
    {
        return operator;
    }

    protected void setOperator( String operator )
    {
        this.operator = operator;
    }

    protected String getValue()
    {
        return value;
    }

    protected void setValue( String value )
    {
        this.value = value;
    }
}
