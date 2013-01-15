/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy.conditions;

import java.util.ArrayList;
import java.util.List;

import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.policy.Condition;
import com.sonatype.insight.brain.model.policy.ConditionType;
import com.sonatype.insight.brain.model.policy.InvalidConditionException;
import com.sonatype.insight.brain.model.policy.conditions.valuetype.LabelValueType;

public class LabelConditionType
    extends AbstractConditionType
    implements ConditionType
{
    public static final String ID = "Label";

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
        return LabelValueType.ID;
    }

    @Override
    public void validateCondition( Condition condition, String applicationId )
        throws InvalidConditionException
    {
        super.validateCondition( condition, applicationId );

        String labelId = condition.getValue();
        LabelValueType labelValueType = new LabelValueType( applicationId );
        for ( Label label : labelValueType.getAvailableValues() )
        {
            if ( label.getId().equals( labelId ) )
            {
                return;
            }
        }
        throw new InvalidConditionException( condition, "Invalid label id: " + labelId );
    }

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public String getName()
    {
        return "Label";
    }

    @Override
    public String generateDroolsCode( Condition condition )
    {
        String labelId = condition.getValue();
        Label label = new LabelDAO().getById( labelId );
        return ( "is".equals( condition.getOperator() ) ? "" : "! " ) + "hasLabelId( \"" + labelId + "\" ) /* label: "
            + label.getLabel() + " */";
    }
}
