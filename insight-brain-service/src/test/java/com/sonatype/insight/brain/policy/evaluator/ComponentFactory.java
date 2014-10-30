/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import com.sonatype.clm.dto.model.ide.ComponentIdentifier;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.MatchState;

/**
 * @since 1.13
 */
public class ComponentFactory
{
  /**
   * Helper method to construct Maven Components from GAV properties.
   */
  public static Component forGav(String groupId, String artifactId, String version, MatchState matchState){
    Component component = new Component(ComponentIdentifier.createMavenCoordinates(groupId, artifactId, version));
    component.setMatchState(matchState);
    return component;
  }
}
