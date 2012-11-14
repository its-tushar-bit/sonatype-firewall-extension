package com.sonatype.insight.brain.model.rule;

import java.util.List;

public interface ConditionType
{
    String getOperandName();

    List<String> getSupportedOperators();

    /**
     * Returns a list of available values or null if the condition does not support available values.
     */
    List<String> getAvailableValues();
}
