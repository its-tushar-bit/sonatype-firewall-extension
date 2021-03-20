/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;

import org.apache.commons.lang3.StringUtils;

public class ApplicationComponentLicensesDTO
{
  private static final char LICENSES_DELIMITER_CHAR = '\n';

  /** The licenses delimiter character escaped for regular expressions. */
  private static final String LICENSES_DELIMITER_REGEX = "\\" + LICENSES_DELIMITER_CHAR;

  private String hash;

  private String componentIdFormat;

  private String componentIdCoordinatesJson;

  private String licensesString;

  private ComponentIdentifier componentIdentifier;

  public ApplicationComponentLicensesDTO(
      String hash,
      String componentIdFormat,
      String componentIdCoordinatesJson,
      String licensesString)
  {
    this.hash = hash;
    this.componentIdFormat = componentIdFormat;
    this.componentIdCoordinatesJson = componentIdCoordinatesJson;
    this.licensesString = licensesString;
  }

  public String getHash() {
    return hash;
  }

  public ComponentIdentifier getComponentIdentifier() {
    if (StringUtils.isAnyBlank(componentIdFormat, componentIdCoordinatesJson)) {
      return null;
    }
    if (componentIdentifier == null) {
      componentIdentifier =
          ComponentIdentifierAdapter.formatAndJsonToComponentIdentifier(componentIdFormat, componentIdCoordinatesJson);
    }
    return componentIdentifier;
  }

  public Set<String> getLicenses() {
    if (StringUtils.isEmpty(licensesString)) {
      return Collections.emptySet();
    }

    return new HashSet<>(Arrays.asList(licensesString.split(LICENSES_DELIMITER_REGEX)));
  }
}
