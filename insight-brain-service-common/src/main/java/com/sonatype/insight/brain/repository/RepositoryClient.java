/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.core.Response.Status;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;

import com.google.common.collect.ImmutableSet;

/**
 * @since 1.127
 */
public interface RepositoryClient
{
  Set<String> REPOSITORY_SUPPORTED_FORMATS =
      ImmutableSet.of(ComponentIdentifier.FORMAT_MAVEN, ComponentIdentifier.FORMAT_NPM);

  RepositoryAllVersionsResponse getAllVersions(Map<String, String> queryParams) throws IOException;

  Status getServerStatus() throws IOException;
}
