/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.dto;

import com.sonatype.clm.dto.model.ide.ComponentIdentifier;

/**
 * @since 1.12.0
 */
public class ApiMavenComponentDTO
{

  public String hash;

  public String groupId;

  public String artifactId;

  public String version;

  public static ApiMavenComponentDTO create(String hash, ComponentIdentifier componentIdentifier) {
    ApiMavenComponentDTO result = new ApiMavenComponentDTO();
    result.hash = hash;
    if (componentIdentifier != null) {
      switch (componentIdentifier.getFormat()) {
        case ComponentIdentifier.FORMAT_MAVEN:
          result.groupId = componentIdentifier.get(ComponentIdentifier.MAVEN_GROUP_ID);
          result.artifactId = componentIdentifier.get(ComponentIdentifier.MAVEN_ARTIFACT_ID);
          result.version = componentIdentifier.get(ComponentIdentifier.VERSION);
          break;
        case ComponentIdentifier.FORMAT_NUGET:
          result.artifactId = componentIdentifier.get(ComponentIdentifier.NUGET_PACKAGE_ID);
          result.version = componentIdentifier.get(ComponentIdentifier.VERSION);
          break;
        default:
          // We don't want to throw an exception if the format is unknown.
      }
    }
    return result;
  }
}
