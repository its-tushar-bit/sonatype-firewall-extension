/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.conditions;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.InvalidConditionException;
import com.sonatype.insight.brain.model.policy.conditions.valuetype.IntegerValueType;

public class LicenseThreatGroupLevelConditionType
    extends AbstractConditionType<Integer>
{
    public static final String ID = "License Threat Group Level";

    private static List<String> supportedOperators = new ArrayList<String>();

    static
    {
        supportedOperators.add( "<=" );
        supportedOperators.add( ">=" );
    }

    @Override
    public List<String> getSupportedOperators()
    {
        return supportedOperators;
    }

    @Override
    public String getValueTypeId()
    {
        return IntegerValueType.ID;
    }

    @Override
    public void validateCondition( Condition condition, String applicationId )
        throws InvalidConditionException
    {
        super.validateCondition( condition, applicationId );

        try
        {
            int value = Integer.parseInt( condition.getValue() );
            if ( value < 0 || value > 10 )
            {
                throw new InvalidConditionException( condition,
                                                     "The license threat group level must be between 0 and 10" );
            }
        }
        catch ( NumberFormatException e )
        {
            throw new InvalidConditionException( condition, "Invalid license threat group level: "
                + condition.getValue() );
        }
    }

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public String getName()
    {
        return "License Threat Group Level";
    }

    @Override
    public String generateDroolsConditionValue( String value )
    {
        return value;
    }

    @Override
    public String explainMatch( final Condition condition, final Component component )
    {
        final StringBuilder buf = new StringBuilder();
        final List<LicenseThreatGroup> groups =
            component.getLicenseThreatGroupsByLevel( Integer.parseInt( condition.getValue() ), condition.getOperator() );
        if ( groups.isEmpty() )
        {
            buf.append( "no" );
        }
        for ( int i = 0, size = groups.size(); i < size; i++ )
        {
            if ( buf.length() > 0 )
            {
                buf.append( " and " );
            }
            buf.append( '\'' ).append( groups.get( i ).getName() ).append( '\'' );
        }
        return "Found " + buf + " License Threat " + ( groups.size() != 1 ? "Groups" : "Group" ) + " with Level "
            + condition.getOperator() + " " + condition.getValue();
    }

    @Override
    protected boolean internalEvaluateCondition( Component component, String operator, Integer value )
    {
        // TODO Simplify
        return !component.getLicenseThreatGroupsByLevel( value, operator ).isEmpty();
    }
}
