/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.InvalidComponentIdentifierException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ComponentPopularity
{
  private ComponentIdentifier componentIdentifier;

  private String artifactId;

  private long[] catalogDates;

  private int currentVersionIndex;

  private String groupId;

  private int[] popularity;

  private String version;

  public ComponentIdentifier getComponentIdentifier() {
    if (componentIdentifier == null) {
      // to support old reports that won't have the componentIdentifier
      try {
        componentIdentifier = ComponentIdentifier.createMavenCoordinates(groupId, artifactId, version);
      }
      catch (InvalidComponentIdentifierException e) {
        componentIdentifier = null;
      }
    }
    return componentIdentifier;
  }

  public long[] getCatalogDates() {
    return catalogDates;
  }

  public int getCurrentVersionIndex() {
    return currentVersionIndex;
  }

  public int[] getPopularity() {
    return popularity;
  }

  public void setComponentIdentifier(ComponentIdentifier componentIdentifier) {
    this.componentIdentifier = componentIdentifier;
  }

  @SuppressWarnings("unused")
  @Deprecated
  /**
   * @deprecated since 1.13.0
   *             simply used for deserialization from older reports where componentIdentifier wasn't available
   */
  private void setArtifactId(String artifactId) {
    this.artifactId = artifactId;
  }

  public void setCatalogDates(long[] catalogDates) {
    this.catalogDates = catalogDates;
  }

  public void setCurrentVersionIndex(int currentVersionIndex) {
    this.currentVersionIndex = currentVersionIndex;
  }

  @SuppressWarnings("unused")
  @Deprecated
  /**
   * @deprecated since 1.13.0
   *             simply used for deserialization from older reports where componentIdentifier wasn't available
   */
  private void setGroupId(String groupId) {
    this.groupId = groupId;
  }

  public void setPopularity(int[] popularity) {
    this.popularity = popularity;
  }

  @SuppressWarnings("unused")
  @Deprecated
  /**
   * @deprecated since 1.13.0
   *             simply used for deserialization from older reports where componentIdentifier wasn't available
   */
  private void setVersion(String version) {
    this.version = version;
  }
}
