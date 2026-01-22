/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Collections;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.InvalidComponentIdentifierException;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentOrPurlIdentifierDTOV2;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.47
 */
@Named
@Singleton
public class ApiComponentVersionsServiceV2
{
  public static final String HDS_COMPONENT_VERSIONS_LIST_PATH = "rest/component/versions";

  private static final Logger log = LoggerFactory.getLogger(ApiComponentVersionsServiceV2.class);

  private final HdsClient hdsClient;

  @Inject
  public ApiComponentVersionsServiceV2(HdsClient hdsClient) {
    this.hdsClient = hdsClient;
  }

  public List<String> getComponentVersions(final ApiComponentOrPurlIdentifierDTOV2 componentOrPurlIdentifierDTOV2) {
    if (componentOrPurlIdentifierDTOV2 != null) {
      if (componentOrPurlIdentifierDTOV2.getPackageUrl() != null) {
        return getComponentVersions(componentOrPurlIdentifierDTOV2.getPackageUrl());
      }
      else {
        return getComponentVersions((ApiComponentIdentifierDTOV2) componentOrPurlIdentifierDTOV2);
      }
    }
    else {
      throw new BadRequestException("Missing component identifier");
    }
  }

  private List<String> getComponentVersions(final ApiComponentIdentifierDTOV2 componentIdentifierDTO) {
    try {
      ComponentIdentifier componentIdentifier = componentIdentifierDTO.toComponentIdentifier();
      return getComponentVersions(componentIdentifier);
    }
    catch (InvalidComponentIdentifierException e) {
      throw new BadRequestException(e.getMessage(), e);
    }
  }

  private List<String> getComponentVersions(final String packageUrl) {
    try {
      PackageUrlIdentifier packageURLIdentifier = new PackageUrlIdentifier(packageUrl);
      return getComponentVersions(packageURLIdentifier.toComponentIdentifier());
    }
    catch (IllegalArgumentException e) {
      throw new BadRequestException(e.getMessage(), e);
    }
  }

  private List<String> getComponentVersions(final ComponentIdentifier componentIdentifier) {
    long start = System.currentTimeMillis();

    List<String> versions = hdsClient.get(List.class, HDS_COMPONENT_VERSIONS_LIST_PATH,
        Collections.singletonMap("componentIdentifier", ComponentIdentifierAdapter.toJson(componentIdentifier)));

    log.debug("Got {} versions of component {} from HDS in {} ms.", versions.size(), componentIdentifier,
        System.currentTimeMillis() - start);

    return versions;
  }
}
