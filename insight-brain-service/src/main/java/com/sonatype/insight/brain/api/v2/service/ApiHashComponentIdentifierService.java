/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.HashSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.InvalidComponentIdentifierException;
import com.sonatype.insight.brain.api.v2.dto.ApiHashComponentIdentifierDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiHashComponentIdentifiersDTO;
import com.sonatype.insight.brain.component.HashComponentIdentifierService;
import com.sonatype.insight.brain.dataaccess.component.HashComponentIdentifierDAO;
import com.sonatype.insight.brain.model.component.HashComponentIdentifier;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.purl.PackageUrlIdentifier;

/**
 * @since 1.85
 */
@Named
public class ApiHashComponentIdentifierService
{
  private final HashComponentIdentifierService hashComponentIdentifierService;

  private final HashComponentIdentifierDAO hashComponentIdentifierDAO;

  @Inject
  public ApiHashComponentIdentifierService(
      HashComponentIdentifierService hashComponentIdentifierService,
      HashComponentIdentifierDAO hashComponentIdentifierDAO)
  {
    this.hashComponentIdentifierService = hashComponentIdentifierService;
    this.hashComponentIdentifierDAO = hashComponentIdentifierDAO;
  }

  public ApiHashComponentIdentifierDTO get(String hash) {
    return new ApiHashComponentIdentifierDTO(hashComponentIdentifierService.get(hash));
  }

  @Authorize(permission = Permission.CLAIM_COMPONENT)
  public ApiHashComponentIdentifiersDTO getAll() {
    return new ApiHashComponentIdentifiersDTO(hashComponentIdentifierDAO.getAll()
        .stream()
        .map(ApiHashComponentIdentifierDTO::new)
        .collect(Collectors.toList()));
  }

  @Authorize(permission = Permission.CLAIM_COMPONENT)
  public ApiHashComponentIdentifierDTO set(ApiHashComponentIdentifierDTO apiHashComponentIdentifierDTO) {
    HashComponentIdentifier hashComponentIdentifier = validateAndComplete(apiHashComponentIdentifierDTO);
    return new ApiHashComponentIdentifierDTO(
        hashComponentIdentifierDAO.getByHash(hashComponentIdentifier.getHash()) == null
            ? hashComponentIdentifierService
                .set(hashComponentIdentifier)
            : hashComponentIdentifierService.update(hashComponentIdentifier));
  }

  private HashComponentIdentifier validateAndComplete(ApiHashComponentIdentifierDTO apiHashComponentIdentifierDTO) {
    if (apiHashComponentIdentifierDTO == null || apiHashComponentIdentifierDTO.hash == null ||
        (apiHashComponentIdentifierDTO.componentIdentifier == null &&
            apiHashComponentIdentifierDTO.packageUrl == null))
    {
      throw new BadRequestException("A component hash and identifier/package url are required.");
    }

    try {
      HashComponentIdentifier hashComponentIdentifier = apiHashComponentIdentifierDTO.toHashComponentIdentifier();
      ComponentIdentifier componentIdentifier = hashComponentIdentifier.getComponentIdentifier();
      ComponentIdentifier componentIdentifierFromPackageUrl = null;
      if (apiHashComponentIdentifierDTO.packageUrl != null) {
        componentIdentifierFromPackageUrl =
            new PackageUrlIdentifier(apiHashComponentIdentifierDTO.packageUrl).toComponentIdentifier();
        componentIdentifierFromPackageUrl.ensureComplete();
      }
      if (componentIdentifier == null) {
        componentIdentifier = componentIdentifierFromPackageUrl;
      }
      else {
        componentIdentifier.ensureComplete();
        validateNonBlankCoordinates(componentIdentifier);
      }
      if (componentIdentifierFromPackageUrl != null && !componentIdentifier.equals(componentIdentifierFromPackageUrl)) {
        throw new BadRequestException("Mismatched component identifier and package url.");
      }
      hashComponentIdentifier.setComponentIdentifier(componentIdentifier);
      return hashComponentIdentifier;
    }
    catch (InvalidComponentIdentifierException e) {
      throw new BadRequestException(e.getMessage(), e);
    }
  }

  private void validateNonBlankCoordinates(ComponentIdentifier componentIdentifier) {
    Set<String> requiredCoords = ComponentIdentifier.getAllRequiredCoordinateNames(componentIdentifier.getFormat());
    SortedMap<String, String> componentIdentifierCoordinates = componentIdentifier.getCoordinates();
    Set<String> foundEmptyCoords = new HashSet<>();

    componentIdentifierCoordinates.forEach((coordinate, value) -> {
      if (requiredCoords.contains(coordinate) && value.isBlank()) {
        foundEmptyCoords.add(coordinate);
      }
    });

    if (!foundEmptyCoords.isEmpty()) {
      throw new BadRequestException(
          String.format("The following coordinates cannot be empty for given format: %s", foundEmptyCoords));
    }
  }

  public void delete(String hash) {
    hashComponentIdentifierService.delete(hash);
  }
}
