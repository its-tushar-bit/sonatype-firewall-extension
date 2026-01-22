/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.experimental;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.ComponentNearestFixedVersions;
import com.sonatype.insight.brain.api.experimental.ApiComponentNearestFixedVersionsRequestListDto.ApiComponentNearestFixedVersionsRequestDto;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.purl.InvalidPackageURLException;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import org.apache.commons.collections4.CollectionUtils;

@Named
public class ApiComponentNearestFixedVersionsService
{
  static final String HDS_COMPONENT_NEAREST_FIXED_VERSION_PATH = "rest/component/vulnerabilities/nearestFixedVersions";

  private final HdsClient client;

  @Inject
  public ApiComponentNearestFixedVersionsService(HdsClient client) {
    this.client = client;
  }

  public List<ComponentNearestFixedVersions> getNearestFixedVersions(
      ApiComponentNearestFixedVersionsRequestListDto listDto)
  {
    if (listDto == null || CollectionUtils.isEmpty(listDto.getComponents())) {
      throw new BadRequestException("No components provided in the request");
    }

    Set<ComponentIdentifier> components = new LinkedHashSet<>(listDto.getComponents().size());
    for (ApiComponentNearestFixedVersionsRequestDto dto : listDto.getComponents()) {
      try {
        components.add(new PackageUrlIdentifier(dto.getPackageUrl()).ensureCompleteIdentifier());
      }
      catch (InvalidPackageURLException e) {
        throw new BadRequestException("Invalid packageUrl: " + dto.getPackageUrl() + ". " + e.getMessage(), e);
      }
    }

    return Arrays.asList(
        client.post(ComponentNearestFixedVersions[].class, HDS_COMPONENT_NEAREST_FIXED_VERSION_PATH, components));
  }
}
