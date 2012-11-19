/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.List;

import com.sonatype.insight.brain.model.rule.Action;
import com.sonatype.insight.brain.model.rule.ActionType;
import com.sonatype.insight.brain.model.rule.AllActionTypes;
import com.sonatype.insight.brain.model.rule.AllConditionTypes;
import com.sonatype.insight.brain.model.rule.ConditionType;
import com.sonatype.insight.brain.model.rule.Rule;
import com.sonatype.insight.brain.model.rule.SimpleCondition;

public class DroolsGenerator
{
    public String generate( List<Rule> rules )
    {
        StringBuilder droolsCode = new StringBuilder();
        // TODO add imports for drools
        for (Rule rule : rules)
        {
            if ( !rule.isEnabled() )
            {
                continue;
            }
            droolsCode.append( '\n' );
            droolsCode.append( "rule \"" ).append( rule.getId() ).append( "\"\n" );
            droolsCode.append( "when\n" );
            // TODO deal with ALL/ANY - i.e. rule.getOperator()
            for (SimpleCondition condition : rule.getConditions())
            {
                ConditionType conditionType = AllConditionTypes.getById( condition.getConditionTypeId() );
                droolsCode.append( "\t" ).append( conditionType.generateDroolsCode( condition ) );
                droolsCode.append( ";\n" );
            }
            droolsCode.append( "then\n" );
            for ( Action action : rule.getActions() )
            {
                ActionType actionType = AllActionTypes.getById( action.getActionTypeId() );
                droolsCode.append( "\t" ).append( actionType.generateDroolsCode( action ) );
                droolsCode.append( ";\n" );
            }
            droolsCode.append( "end\n" );
        }
        return droolsCode.toString();
    }
}
