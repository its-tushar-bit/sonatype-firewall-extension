/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy;

import java.util.List;

import com.sonatype.insight.brain.model.component.Component;

public interface ConditionType<T>
{
    String getId();

    String getName();

    List<String> getSupportedOperators();

    /**
     * @return The ID of a ConditionValueType that defines the value type for this condition type or null if the
     *         condition type does not require or support values.
     */
    String getValueTypeId();

    String getValueHint();

    String generateDroolsConditionValue( String value );

    String generateDroolsCode( Condition condition );

    String explainCondition( Condition condition );

    String explainMatch( Condition condition, Component component );

    void validateCondition( Condition condition, String applicationId )
        throws InvalidConditionException;

    boolean evaluateCondition( Component component, String operator, T value );
}
