/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy;

public class Condition
{
    private String conditionTypeId;

    private String operator;

    private String value;

    public Condition()
    {
    }

    public Condition( final String conditionTypeId, final String operator )
    {
        this.conditionTypeId = conditionTypeId;
        this.operator = operator;
    }

    public Condition( final String conditionTypeId, final String operator, final String value )
    {
        this.conditionTypeId = conditionTypeId;
        this.operator = operator;
        this.value = value;
    }

    public String getConditionTypeId()
    {
        return conditionTypeId;
    }

    public void setConditionTypeId( final String conditionTypeId )
    {
        this.conditionTypeId = conditionTypeId;
    }

    public String getOperator()
    {
        return operator;
    }

    public void setOperator( final String operator )
    {
        this.operator = operator;
    }

    public String getValue()
    {
        return value;
    }

    public void setValue( final String value )
    {
        this.value = value;
    }
}
