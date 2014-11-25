/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.saas;

import com.sonatype.clm.dto.model.component.ComponentDisplayName;
import com.sonatype.clm.dto.model.component.NamedComponentDetails;

public class TestNamedComponentDetails
    extends NamedComponentDetails
{
  private ComponentDisplayName displayName;

  private String groupId;

  private String artifactId;

  private String version;

  @Override
  public ComponentDisplayName getDisplayName() {
    return displayName;
  }

  public void setDisplayName(ComponentDisplayName displayName) {
    this.displayName = displayName;
  }

  @Override
  public String getGroupId() {
    return groupId;
  }

  public void setGroupId(String groupId) {
    this.groupId = groupId;
  }

  @Override
  public String getArtifactId() {
    return artifactId;
  }

  public void setArtifactId(String artifactId) {
    this.artifactId = artifactId;
  }

  @Override
  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }
}
