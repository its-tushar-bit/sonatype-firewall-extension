/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model;

import java.util.HashMap;
import java.util.Map;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.nexus.scm.api.common.JsonUtils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class OwnerComponentLicensesDTOTest
{
  @Test
  public void testGetComponentIdentifier_Null() {
    OwnerComponentLicensesDTO dto = new OwnerComponentLicensesDTO(
        null,
        null,
        null,
        null,
        null);

    assertThat(dto.getComponentIdentifier()).isNull();
  }

  @Test
  public void testGetComponentIdentifier_Conan_Null() {
    Map<String, String> coordinates = new HashMap<>();
    coordinates.put(ComponentIdentifier.CONAN_CHANNEL, null);
    coordinates.put(ComponentIdentifier.CONAN_OWNER, null);
    coordinates.put(ComponentIdentifier.CONAN_NAME, "bzip2");
    coordinates.put(ComponentIdentifier.VERSION, "1.0.8");

    OwnerComponentLicensesDTO dto = new OwnerComponentLicensesDTO(
        null,
        null,
        ComponentIdentifier.FORMAT_CONAN,
        JsonUtils.toJson(coordinates),
        null);

    ComponentIdentifier componentIdentifier = dto.getComponentIdentifier();
    assertThat(componentIdentifier.getFormat()).isEqualTo(ComponentIdentifier.FORMAT_CONAN);
    assertThat(componentIdentifier.getCoordinates().size()).isEqualTo(2);
    assertThat(componentIdentifier.getCoordinates())
        .hasFieldOrPropertyWithValue(ComponentIdentifier.CONAN_NAME, "bzip2")
        .hasFieldOrPropertyWithValue(ComponentIdentifier.VERSION, "1.0.8");
  }

  @Test
  public void testGetComponentIdentifier_Conan_Empty() {
    Map<String, String> coordinates = Map.of(
        ComponentIdentifier.CONAN_CHANNEL, "",
        ComponentIdentifier.CONAN_OWNER, "",
        ComponentIdentifier.CONAN_NAME, "bzip2",
        ComponentIdentifier.VERSION, "1.0.8");

    OwnerComponentLicensesDTO dto = new OwnerComponentLicensesDTO(
        null,
        null,
        ComponentIdentifier.FORMAT_CONAN,
        JsonUtils.toJson(coordinates),
        null);

    ComponentIdentifier componentIdentifier = dto.getComponentIdentifier();
    assertThat(componentIdentifier.getFormat()).isEqualTo(ComponentIdentifier.FORMAT_CONAN);
    assertThat(componentIdentifier.getCoordinates().size()).isEqualTo(2);
    assertThat(componentIdentifier.getCoordinates())
        .hasFieldOrPropertyWithValue(ComponentIdentifier.CONAN_NAME, "bzip2")
        .hasFieldOrPropertyWithValue(ComponentIdentifier.VERSION, "1.0.8");
  }

  @Test
  public void testGetComponentIdentifier_Conan_Full() {
    Map<String, String> coordinates = new HashMap<>();
    coordinates.put(ComponentIdentifier.CONAN_CHANNEL, "channel");
    coordinates.put(ComponentIdentifier.CONAN_OWNER, "owner");
    coordinates.put(ComponentIdentifier.CONAN_NAME, "bzip2");
    coordinates.put(ComponentIdentifier.VERSION, "1.0.8");

    OwnerComponentLicensesDTO dto = new OwnerComponentLicensesDTO(
        null,
        null,
        ComponentIdentifier.FORMAT_CONAN,
        JsonUtils.toJson(coordinates),
        null);

    ComponentIdentifier componentIdentifier = dto.getComponentIdentifier();
    assertThat(componentIdentifier.getFormat()).isEqualTo(ComponentIdentifier.FORMAT_CONAN);
    assertThat(componentIdentifier.getCoordinates().size()).isEqualTo(4);
    assertThat(componentIdentifier.getCoordinates())
        .hasFieldOrPropertyWithValue(ComponentIdentifier.CONAN_CHANNEL, "channel")
        .hasFieldOrPropertyWithValue(ComponentIdentifier.CONAN_OWNER, "owner")
        .hasFieldOrPropertyWithValue(ComponentIdentifier.CONAN_NAME, "bzip2")
        .hasFieldOrPropertyWithValue(ComponentIdentifier.VERSION, "1.0.8");
  }

  @Test
  public void testGetComponentIdentifier_NotConan() {
    Map<String, String> coordinates = Map.of(
        ComponentIdentifier.ANAME_NAME, "n",
        ComponentIdentifier.ANAME_QUALIFIER, "",
        ComponentIdentifier.VERSION, "v");

    OwnerComponentLicensesDTO dto = new OwnerComponentLicensesDTO(
        null,
        null,
        ComponentIdentifier.FORMAT_ANAME,
        JsonUtils.toJson(coordinates),
        null);

    ComponentIdentifier componentIdentifier = dto.getComponentIdentifier();
    assertThat(componentIdentifier.getFormat()).isEqualTo(ComponentIdentifier.FORMAT_ANAME);
    assertThat(componentIdentifier.getCoordinates().size()).isEqualTo(3);
    assertThat(componentIdentifier.getCoordinates())
        .hasFieldOrPropertyWithValue(ComponentIdentifier.ANAME_NAME, "n")
        .hasFieldOrPropertyWithValue(ComponentIdentifier.ANAME_QUALIFIER, "")
        .hasFieldOrPropertyWithValue(ComponentIdentifier.VERSION, "v");
  }
}
