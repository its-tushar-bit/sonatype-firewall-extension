/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.conditions;

import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.ConditionType;
import com.sonatype.insight.brain.model.policy.InvalidConditionException;

public abstract class AbstractConditionType<T>
    implements ConditionType<T>
{
    @Override
    public void validateCondition( Condition condition, String ownerId )
        throws InvalidConditionException
    {
        if ( condition.getOperator() == null )
        {
            throw new InvalidConditionException( condition, "Operator is null" );
        }
        if ( !getSupportedOperators().contains( condition.getOperator() ) )
        {
            throw new InvalidConditionException( condition, "Operator is not supported" );
        }
        if ( getValueTypeId() != null && condition.getValue() == null )
        {
            throw new InvalidConditionException( condition, "Value is null" );
        }
    }

    @Override
    public String getValueHint()
    {
        return null;
    }

    @Override
    public String explainCondition( Condition condition )
    {
        return getName() + ' ' + condition.getOperator()
            + ( condition.getValue() != null ? ' ' + condition.getValue() : "" );
    }

    protected abstract boolean internalEvaluateCondition( Component component, String operator, T value );

    @Override
    public final boolean evaluateCondition( Component component, String operator, T value )
    {
        /*
         * Only interested in facts about known components, or facts about match state and proprietary state of unknown
         * components.
         */
        if ( MatchState.UNKNOWN == component.getMatchState() && !( this instanceof MatchStateConditionType )
            && !( this instanceof ProprietaryConditionType ) )
        {
            return false;
        }
        return internalEvaluateCondition( component, operator, value );
    }

    @Override
    public final String generateDroolsCode( Condition condition )
    {
        return "ConditionTypes." + getClass().getSimpleName() + ".evaluateCondition(this, \"" + condition.getOperator()
            + "\", " + generateDroolsConditionValue( condition.getValue() ) + ")";
    }

    protected static String asDroolsComment( String text )
    {
        return " /* " + text.replace( "*/", "" ).replaceAll( "[\r\n]+", " " ) + " */";
    }

    protected static String asDroolsString( String value )
    {
        if ( value == null )
        {
            value = "null";
        }
        else
        {
            value = value.replace( "\\", "\\\\" );
            value = value.replace( "\n", "\\n" );
            value = value.replace( "\r", "\\r" );
            value = value.replace( "\"", "\\\"" );
            value = '"' + value + '"';
        }
        return value;
    }

    protected static String asDroolsInteger( String value )
    {
        // We've seen issues similar to https://issues.jboss.org/browse/JBRULES-3628 so we use explicit boxing
        return "Integer.valueOf( " + value + " )";
    }
}
