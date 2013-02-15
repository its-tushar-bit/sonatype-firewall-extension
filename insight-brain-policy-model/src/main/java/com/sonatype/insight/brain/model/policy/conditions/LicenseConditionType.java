/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.conditions;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.sonatype.insight.brain.dataaccess.license.LicenseDAO;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.license.License;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.InvalidConditionException;
import com.sonatype.insight.brain.model.policy.conditions.valuetype.LicenseValueType;

public class LicenseConditionType
    extends AbstractConditionType
{
    public static final String ID = "License";

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
        return LicenseValueType.ID;
    }

    @Override
    public void validateCondition( Condition condition, String applicationId )
        throws InvalidConditionException
    {
        super.validateCondition( condition, applicationId );

        String licenseId = condition.getValue();
        if ( LicenseValueType.getLicenseById( licenseId ) == null )
        {
            throw new InvalidConditionException( condition, "Invalid license id: " + licenseId );
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
        return "License";
    }

    @Override
    public String generateDroolsCode( Condition condition )
    {
        return ( "is".equals( condition.getOperator() ) ? "" : "! " ) + "hasLicenseId( \"" + condition.getValue()
            + "\" )";
    }

    @Override
    public String explainRule( final Condition condition )
    {
        return getName() + ' ' + condition.getOperator() + " '"
            + new LicenseDAO().getById( condition.getValue() ).getShortDisplayName() + '\'';
    }

    @Override
    public String explainMatch( final Condition condition, final Component component )
    {
        final LicenseDAO licenseDAO = new LicenseDAO();
        final StringBuilder buf = new StringBuilder();
        final Set<String> licenseIds = component.getLicenseIds();
        if ( licenseIds.isEmpty() )
        {
            buf.append( "no" );
        }
        for ( String licenseId : licenseIds )
        {
            if ( buf.length() > 0 )
            {
                buf.append( " and " );
            }
            final License license = licenseDAO.getById( licenseId );
            if ( license != null )
            {
                buf.append( '\'' ).append( license.getShortDisplayName() ).append( '\'' );
            }
        }
        return "Found " + buf + ( licenseIds.size() != 1 ? " Licenses" : " License" );
    }
}
