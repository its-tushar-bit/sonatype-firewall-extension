/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.util.HashMap;
import java.util.Map;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;

/**
 * Contains component details used by the IQ for SCM feature for the auto PRs and PR commenting flows,
 * in particular whether a component is a direct or transitive dependency and its display name.
 */
public class SourceControlComponentDetails
{
  private final Map<String, ComponentInfo> hashToComponentInfoMap;

  private final Map<ComponentIdentifier, ComponentInfo> identifierToComponentInfoMap;

  public SourceControlComponentDetails() {
    this.hashToComponentInfoMap = new HashMap<>();
    this.identifierToComponentInfoMap = new HashMap<>();
  }

  public ComponentInfo getComponentInfo(final String hash) {
    return hashToComponentInfoMap.get(hash);
  }

  public ComponentInfo getComponentInfo(final ComponentIdentifier componentIdentifier) {
    return identifierToComponentInfoMap.get(componentIdentifier);
  }

  public Map<String, ComponentInfo> getHashToComponentInfoMap() {
    return hashToComponentInfoMap;
  }

  public Map<ComponentIdentifier, ComponentInfo> getIdentifierToComponentInfoMap() {
    return identifierToComponentInfoMap;
  }

  public static class ComponentInfo
  {
    private final String displayName;

    private final Boolean directDependency;

    public ComponentInfo(final String displayName, final Boolean directDependency) {
      this.displayName = displayName;
      this.directDependency = directDependency;
    }

    public String getDisplayName() {
      return displayName;
    }

    public Boolean getDirectDependency() {
      return directDependency;
    }
  }
}
