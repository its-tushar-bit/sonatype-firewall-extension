/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.conditions;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.InvalidConditionException;
import com.sonatype.insight.brain.model.policy.conditions.valuetype.LicenseThreatGroupValueType;

public class LicenseThreatGroupConditionType
    extends AbstractConditionType<String>
{
    public static final String ID = "License Threat Group";

    private static List<String> supportedOperators = new ArrayList<String>();

    static
    {
        supportedOperators.add( "is" );
        supportedOperators.add( "is not" );
    }

    @Override
    public List<String> getSupportedOperators()
    {
        return supportedOperators;
    }

    @Override
    public String getValueTypeId()
    {
        return LicenseThreatGroupValueType.ID;
    }

    @Override
    public void validateCondition( Condition condition, String ownerId )
        throws InvalidConditionException
    {
        super.validateCondition( condition, ownerId );

        String licenseThreatGroupId = condition.getValue();
        LicenseThreatGroupValueType licenseThreatGroupValueType = new LicenseThreatGroupValueType( ownerId );
        for ( LicenseThreatGroup licenseThreatGroup : licenseThreatGroupValueType.getAvailableValues() )
        {
            if ( licenseThreatGroup.getId().equals( licenseThreatGroupId ) )
            {
                return;
            }
        }
        throw new InvalidConditionException( condition, "Invalid license threat group id: " + licenseThreatGroupId );
    }

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public String getName()
    {
        return "License Threat Group";
    }

    @Override
    public String generateDroolsConditionValue( String value )
    {
        LicenseThreatGroup licenseThreatGroup = new LicenseThreatGroupDAO().getById( value );
        return "\"" + value + "\"" + asDroolsComment( "License threat group name: " + licenseThreatGroup.getName() );
    }

    @Override
    public String explainCondition( final Condition condition )
    {
        return getName() + ' ' + condition.getOperator() + " '"
            + new LicenseThreatGroupDAO().getById( condition.getValue() ).getName() + '\'';
    }

    @Override
    public String explainMatch( final Condition condition, final Component component )
    {
        final StringBuilder buf = new StringBuilder();
        final Set<LicenseThreatGroup> licenseThreatGroups = component.getLicenseThreatGroups();
        if ( licenseThreatGroups.isEmpty() )
        {
            buf.append( "no" );
        }
        for ( LicenseThreatGroup licenseThreatGroup : licenseThreatGroups )
        {
            if ( buf.length() > 0 )
            {
                buf.append( " and " );
            }
            buf.append( '\'' ).append( licenseThreatGroup.getName() ).append( '\'' );
        }
        return "Found " + buf + " License Threat " + ( licenseThreatGroups.size() != 1 ? "Groups" : "Group" );
    }

    @Override
    protected boolean internalEvaluateCondition( Component component, String operator, String value )
    {
        boolean result = component.hasLicenseInLicenseThreatGroup( value );
        return "is".equals( operator ) ? result : !result;
    }
}
