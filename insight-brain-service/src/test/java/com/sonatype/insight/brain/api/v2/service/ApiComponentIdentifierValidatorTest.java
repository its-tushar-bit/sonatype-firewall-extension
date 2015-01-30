/*
 * Copyright (c) 2011-2015 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.HashMap;
import java.util.Map;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.error.exception.BadRequestException;

import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @since 1.13.0
 */
public class ApiComponentIdentifierValidatorTest
{
  private ApiComponentIdentifierValidator validator = new ApiComponentIdentifierValidator();

  @Test
  public void testValidMavenComponentIdentifier() {
    Map<String, String> coordinates = new HashMap<>();
    coordinates.put(ComponentIdentifier.MAVEN_GROUP_ID, "g1");
    coordinates.put(ComponentIdentifier.MAVEN_ARTIFACT_ID, "a1");
    coordinates.put(ComponentIdentifier.VERSION, "v1");
    coordinates.put(ComponentIdentifier.MAVEN_EXTENSION, "e1");
    ComponentIdentifier componentIdentifier = new ComponentIdentifier(ComponentIdentifier.FORMAT_MAVEN, coordinates);

    // Throws exception if test fails
    validator.validate(componentIdentifier);
  }

  @Test
  public void testMavenComponentIdentifierMissingExtension() {
    Map<String, String> coordinates = new HashMap<>();
    coordinates.put(ComponentIdentifier.MAVEN_GROUP_ID, "g1");
    coordinates.put(ComponentIdentifier.MAVEN_ARTIFACT_ID, "a1");
    coordinates.put(ComponentIdentifier.VERSION, "v1");
    coordinates.put(ComponentIdentifier.MAVEN_CLASSIFIER, "c1");
    ComponentIdentifier componentIdentifier = new ComponentIdentifier(ComponentIdentifier.FORMAT_MAVEN, coordinates);

    try {
      validator.validate(componentIdentifier);
      Assert.fail("Expected BadRequestException");
    }
    catch (BadRequestException e) {
      assertThat(e.getMessage(), is(ApiComponentIdentifierValidator.MISSING_COORDINATES + "[extension]"));
    }
  }

  @Test
  public void testValidNugetComponentIdentifier() {
    Map<String, String> coordinates = new HashMap<>();
    coordinates.put(ComponentIdentifier.NUGET_PACKAGE_ID, "p1");
    coordinates.put(ComponentIdentifier.VERSION, "v1");
    ComponentIdentifier componentIdentifier = new ComponentIdentifier(ComponentIdentifier.FORMAT_NUGET, coordinates);

    // Throws exception if test fails
    validator.validate(componentIdentifier);
  }

  @Test
  public void testNugetComponentIdentifierMissingVersion() {
    Map<String, String> coordinates = new HashMap<>();
    coordinates.put(ComponentIdentifier.NUGET_PACKAGE_ID, "p1");
    ComponentIdentifier componentIdentifier = new ComponentIdentifier(ComponentIdentifier.FORMAT_NUGET, coordinates);

    try {
      validator.validate(componentIdentifier);
      Assert.fail("Expected BadRequestException");
    }
    catch (BadRequestException e) {
      assertThat(e.getMessage(), is(ApiComponentIdentifierValidator.MISSING_COORDINATES + "[version]"));
    }
  }
}
