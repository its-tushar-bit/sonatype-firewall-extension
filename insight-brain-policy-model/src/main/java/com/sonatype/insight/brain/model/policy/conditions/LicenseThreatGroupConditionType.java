/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.conditions;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.InvalidConditionException;
import com.sonatype.insight.brain.model.policy.conditions.valuetype.LicenseThreatGroupValueType;

public class LicenseThreatGroupConditionType
    extends AbstractConditionType
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
    public void validateCondition( Condition condition, String applicationId )
        throws InvalidConditionException
    {
        super.validateCondition( condition, applicationId );

        String licenseThreatGroupId = condition.getValue();
        LicenseThreatGroupValueType licenseThreatGroupValueType = new LicenseThreatGroupValueType( applicationId );
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
    public String generateDroolsCode( Condition condition )
    {
        LicenseThreatGroup licenseThreatGroup = new LicenseThreatGroupDAO().getById( condition.getValue() );
        return ( "is".equals( condition.getOperator() ) ? "" : "! " ) + "hasLicenseInLicenseThreatGroup( \""
            + condition.getValue() + "\" )" + //
            " /* License threat group name: " + licenseThreatGroup.getName().replace( "*/", "" ) + " */";
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
        final List<LicenseThreatGroup> groups = component.getLicenseThreatGroupsByLevel( 0, ">=" );
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
        return "Found " + buf + " License Threat " + ( groups.size() != 1 ? "Groups" : "Group" );
    }
}
