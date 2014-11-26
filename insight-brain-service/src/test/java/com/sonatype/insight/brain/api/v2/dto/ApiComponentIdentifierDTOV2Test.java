/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;

import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.Matchers.is;

/**
 * Since we are exposing the component identifier coordinates map to the public api, we need to make sure the names
 * of the coordinates don't change. Or if they do we need to transform them.
 */
public class ApiComponentIdentifierDTOV2Test
{
  @Test
  public void testMavenComponentIdentifierNamesAReAsExpected() {
    ComponentIdentifier identifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1");
    ApiComponentIdentifierDTOV2 apiComponentIdentifier = new ApiComponentIdentifierDTOV2(identifier);

    // Don't use the static finals from ComponentIdentifier so we can verify they have not changed
    Assert.assertThat(apiComponentIdentifier.getFormat(), is("maven"));
    Assert.assertThat(apiComponentIdentifier.getCoordinates().get("groupId"), is("g1"));
    Assert.assertThat(apiComponentIdentifier.getCoordinates().get("artifactId"), is("a1"));
    Assert.assertThat(apiComponentIdentifier.getCoordinates().get("version"), is("v1"));
    Assert.assertThat(apiComponentIdentifier.getCoordinates().get("classifier"), is("c1"));
    Assert.assertThat(apiComponentIdentifier.getCoordinates().get("extension"), is("e1"));
  }

  @Test
  public void testNugetComponentIdentifierNamesAReAsExpected() {
    ComponentIdentifier identifier = ComponentIdentifier.createNugetCoordinates("p1", "v1");
    ApiComponentIdentifierDTOV2 apiComponentIdentifier = new ApiComponentIdentifierDTOV2(identifier);

    // Don't use the static finals from ComponentIdentifier so we can verify they have not changed
    Assert.assertThat(apiComponentIdentifier.getFormat(), is("nuget"));
    Assert.assertThat(apiComponentIdentifier.getCoordinates().get("packageId"), is("p1"));
    Assert.assertThat(apiComponentIdentifier.getCoordinates().get("version"), is("v1"));
  }
}
