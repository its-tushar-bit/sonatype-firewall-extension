/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Set;
import java.util.TreeSet;

import javax.inject.Named;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.error.exception.BadRequestException;

import com.google.common.collect.Sets;

import static com.sonatype.clm.dto.model.component.ComponentIdentifier.FORMAT_MAVEN;
import static com.sonatype.clm.dto.model.component.ComponentIdentifier.FORMAT_NUGET;
import static com.sonatype.clm.dto.model.component.ComponentIdentifier.MAVEN_ARTIFACT_ID;
import static com.sonatype.clm.dto.model.component.ComponentIdentifier.MAVEN_EXTENSION;
import static com.sonatype.clm.dto.model.component.ComponentIdentifier.MAVEN_GROUP_ID;
import static com.sonatype.clm.dto.model.component.ComponentIdentifier.NUGET_PACKAGE_ID;
import static com.sonatype.clm.dto.model.component.ComponentIdentifier.VERSION;

/**
 * @since 1.13.0
 */
@Named
public class ApiComponentIdentifierValidator
{
  public static final String MISSING_COORDINATES = "Coordinates missing the following required entries for the given format: ";

  private static final Set<String> MAVEN_REQUIRED_COORDINATE_NAMES = Sets.newHashSet(
      MAVEN_GROUP_ID, MAVEN_ARTIFACT_ID, VERSION, MAVEN_EXTENSION);

  private static final Set<String> NUGET_REQUIRED_COORDINATE_NAMES = Sets.newHashSet(NUGET_PACKAGE_ID, VERSION);

  public void validate(final ComponentIdentifier componentIdentifier) {
    if (componentIdentifier == null) {
      throw new BadRequestException("The component identifier cannot be null.");
    }

    String format = componentIdentifier.getFormat();
    Set<String> keys = componentIdentifier.getCoordinates().keySet();
    if (format.equals(FORMAT_MAVEN)) {
      validateRequiredCoordinates(keys, MAVEN_REQUIRED_COORDINATE_NAMES);
    }
    else if (format.equals(FORMAT_NUGET)) {
      validateRequiredCoordinates(keys, NUGET_REQUIRED_COORDINATE_NAMES);
    }
  }

  private void validateRequiredCoordinates(final Set<String> keys, final Set<String> requiredKeys) {
    if (!keys.containsAll(requiredKeys)) {
      Set<String> missingKeys = new TreeSet<>(requiredKeys);
      missingKeys.removeAll(keys);
      throw new BadRequestException(MISSING_COORDINATES + missingKeys);
    }
  }
}
