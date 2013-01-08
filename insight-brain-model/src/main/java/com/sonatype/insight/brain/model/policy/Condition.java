/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy;

import com.sonatype.insight.brain.model.policy.conditions.ConditionTypes;

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

    public ValidationResult validate()
    {
        ConditionType conditionType = ConditionTypes.getById( conditionTypeId );
        if ( conditionType == null )
        {
            return new ValidationResult( "Invalid condition type id: '" + conditionTypeId + "'" );
        }

        try
        {
            conditionType.validateCondition( this );
        }
        catch ( InvalidConditionException e )
        {
            return new ValidationResult( e );
        }

        return null;
    }

    public String toMessageString()
    {
        return conditionTypeId + ' ' + operator + ' ' + value;
    }

    @Override
    public String toString()
    {
        return "Condition [conditionTypeId=" + conditionTypeId + ", operator=" + operator + ", value=" + value + "]";
    }
}
