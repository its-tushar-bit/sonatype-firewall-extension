/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.InvalidComponentIdentifierException;
import com.sonatype.insight.brain.hds.ComponentDetailsDTO;
import com.sonatype.insight.brain.hds.ComponentDetailsLoader;
import com.sonatype.insight.brain.hds.ComponentDetailsLoaderFactory;
import com.sonatype.insight.brain.hds.ComponentInfoService;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.lqa.LqaFormat;

@Named
@Singleton
public class PathForwardInspector
{
  private final ComponentInfoService componentInfoService;

  private final ComponentDetailsLoaderFactory componentDetailsLoaderFactory;

  private final Map<ComponentIdentifier, Boolean> violatedComponentMap = new ConcurrentHashMap<>();

  @Inject
  public PathForwardInspector(
      final ComponentInfoService componentInfoService,
      final ComponentDetailsLoaderFactory componentDetailsLoaderFactory)
  {
    this.componentInfoService = componentInfoService;
    this.componentDetailsLoaderFactory = componentDetailsLoaderFactory;
    componentInfoService.setToolName("ci");
  }

  public Map<ComponentIdentifier, Boolean> getViolatedComponentMap() {
    return violatedComponentMap;
  }

  public boolean containsUpgradeableVersion(
      final ComponentIdentifier componentIdentifier,
      final Owner owner,
      final String stageId,
      final String scanId)
  {
    if (!isKnownFormat(componentIdentifier)) {
      return false;
    }

    Boolean hasPathForward = violatedComponentMap.get(componentIdentifier);
    if (hasPathForward != null) {
      return hasPathForward;
    }

    ComponentDetailsLoader componentDetailsLoader = componentDetailsLoaderFactory.newInstance(owner);
    List<ComponentDetailsDTO> componentDetailsDTOList =
        componentInfoService.getComponentDetailsForAllVersionsNoAuth(owner,
            componentIdentifier, stageId, null, scanId, null, componentDetailsLoader, true).getLeft();

    int currentComponentIndex = findCurrentComponentIndex(componentDetailsDTOList, componentIdentifier);
    if (currentComponentIndex == -1) {
      return false;
    }
    hasPathForward = componentDetailsDTOList.stream()
        .parallel()
        .skip(currentComponentIndex)
        .anyMatch(dto -> dto.violatedPolicyCount == 0);

    violatedComponentMap.putIfAbsent(componentIdentifier, hasPathForward);

    return hasPathForward;
  }

  private int findCurrentComponentIndex(
      List<ComponentDetailsDTO> allVersions,
      ComponentIdentifier componentIdentifier)
  {
    ComponentIdentifier completeIdentifier = ensureCompleteIfNeeded(componentIdentifier);

    return IntStream.range(0, allVersions.size())
        .filter(i -> ensureCompleteIfNeeded(allVersions.get(i).componentIdentifier)
            .equals(completeIdentifier))
        .findFirst()
        .orElse(-1);
  }

  private ComponentIdentifier ensureCompleteIfNeeded(ComponentIdentifier componentIdentifier) {
    if (ComponentIdentifier.FORMAT_CONAN.equals(componentIdentifier.getFormat())) {
      try {
        componentIdentifier.ensureComplete();
      }
      catch (InvalidComponentIdentifierException e) {
        throw new BadRequestException(e.getMessage(), e);
      }
    }
    return componentIdentifier;
  }

  private boolean isKnownFormat(ComponentIdentifier identifier) {
    return ComponentIdentifier.getFormatsSupportedByHds().contains(identifier.getFormat())
        || LqaFormat.isLqaFormat(identifier.getFormat());
  }

  public void cleanUp() {
    violatedComponentMap.clear();
  }
}
