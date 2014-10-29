/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.dto;

import com.sonatype.clm.dto.model.ide.ComponentIdentifier;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class ApiMavenComponentDTOTest
{
  @Test
  public void testCreateMaven() {
    ApiMavenComponentDTO dto = ApiMavenComponentDTO.create("h1",
        ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1"));
    assertThat(dto, is(notNullValue()));
    assertThat(dto.hash, is("h1"));
    assertThat(dto.groupId, is("g1"));
    assertThat(dto.artifactId, is("a1"));
    assertThat(dto.version, is("v1"));
  }

  @Test
  public void testCreateNuget() {
    ApiMavenComponentDTO dto = ApiMavenComponentDTO
        .create("h1", ComponentIdentifier.createNugetCoordinates("p1", "v1"));
    assertThat(dto, is(notNullValue()));
    assertThat(dto.hash, is("h1"));
    assertThat(dto.groupId, is(nullValue()));
    assertThat(dto.artifactId, is("p1"));
    assertThat(dto.version, is("v1"));
  }

  @Test
  public void testCreateUnknownFormat() {
    ApiMavenComponentDTO dto = ApiMavenComponentDTO.create("h1",
        new ComponentIdentifier("unknown", null /* coordinates */));
    assertThat(dto, is(notNullValue()));
    assertThat(dto.hash, is("h1"));
    assertThat(dto.groupId, is(nullValue()));
    assertThat(dto.artifactId, is(nullValue()));
    assertThat(dto.version, is(nullValue()));
  }
}
