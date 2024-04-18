/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.api.experimental.development.prioritization;

import java.util.Objects;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;

public class PrioritizedComponent
{
  public static final String DEPENDENCY_TYPE_DIRECT = "Direct";

  public static final String DEPENDENCY_TYPE_TRANSITIVE = "Transitive";

  public static final String DEPENDENCY_TYPE_INNER_SOURCE = "Inner Source";

  public static final String DEPENDENCY_TYPE_UNKNOWN = "Unknown";

  private final String displayName;

  private final ComponentIdentifier componentIdentifier;

  private final String componentHash;

  private final String dependencyType;

  private final Boolean hasFailActionOnComponent;

  private String action;

  private final int highestThreat;

  public final String highestThreatPolicyName;

  public final String highestThreatPolicyConstraintName;

  private final int priority;

  public PrioritizedComponent(
      final String displayName,
      final ComponentIdentifier componentIdentifier,
      final String componentHash,
      final String dependencyType,
      final Boolean hasFailActionOnComponent,
      final String action,
      final int highestThreat,
      final String highestThreatPolicyName,
      final String highestThreatPolicyConstraintName,
      final int priority
  )
  {
    this.displayName = displayName;
    this.componentIdentifier = componentIdentifier;
    this.componentHash = componentHash;
    this.dependencyType = dependencyType;
    this.hasFailActionOnComponent = hasFailActionOnComponent;
    this.highestThreat = highestThreat;
    this.highestThreatPolicyName = highestThreatPolicyName;
    this.action = action;
    this.highestThreatPolicyConstraintName = highestThreatPolicyConstraintName;
    this.priority = priority;
  }

  public ComponentIdentifier getComponentIdentifier() {
    return componentIdentifier;
  }

  public String getComponentHash() {
    return componentHash;
  }

  public String getDependencyType() {
    return dependencyType;
  }

  public Boolean getHasFailActionOnComponent() {
    return hasFailActionOnComponent;
  }

  public int getHighestThreat() {
    return highestThreat;
  }

  public String getHighestThreatPolicyName() {
    return highestThreatPolicyName;
  }

  public String getHighestThreatPolicyConstraintName() {
    return highestThreatPolicyConstraintName;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getAction() {
    return action;
  }

  public int getPriority() {
    return priority;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PrioritizedComponent that = (PrioritizedComponent) o;
    return highestThreat == that.highestThreat &&
        priority == that.priority &&
        Objects.equals(displayName, that.displayName) &&
        Objects.equals(componentIdentifier, that.componentIdentifier) &&
        Objects.equals(componentHash,that.componentHash) &&
        Objects.equals(dependencyType, that.dependencyType) &&
        Objects.equals(hasFailActionOnComponent, that.hasFailActionOnComponent) &&
        Objects.equals(highestThreatPolicyName, that.highestThreatPolicyName) &&
        Objects.equals(highestThreatPolicyConstraintName, that.highestThreatPolicyConstraintName) &&
        Objects.equals(action, that.action);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        displayName,
        componentIdentifier,
        componentHash,
        dependencyType,
        hasFailActionOnComponent,
        action,
        highestThreat,
        highestThreatPolicyName,
        highestThreatPolicyConstraintName,
        priority);
  }

  @Override
  public String toString() {
    return "PrioritizedComponent{" +
        "displayName='" + displayName + '\'' +
        ", componentIdentifier=" + componentIdentifier +
        ", componentHash=" + componentHash +
        ", dependencyType='" + dependencyType + '\'' +
        ", hasFailActionOnComponent=" + hasFailActionOnComponent +
        ", action=" + action +
        ", highestThreat=" + highestThreat +
        ", highestThreatPolicyName=" + highestThreatPolicyName +
        ", highestThreatPolicyConstraintName=" + highestThreatPolicyConstraintName +
        ", priority=" + priority +
        '}';
  }
}
