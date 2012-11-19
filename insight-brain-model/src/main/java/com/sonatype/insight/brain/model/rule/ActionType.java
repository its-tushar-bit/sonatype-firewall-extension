/**
 * Copyright (c) 2011-2012 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/insight/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.rule;

import java.util.List;

public interface ActionType
{
    String getId();

    String getName();

    /**
     * Returns a list of available values or null if the condition does not support available values.
     */
    List<String> getAvailableValues();

    String generateDroolsCode( Action action );
}
