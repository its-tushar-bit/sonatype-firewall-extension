/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.dto.remediation.actions;

import java.util.Collections;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;

import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class ApiComponentChangeActionDTOTest
{
  @Test
  public void testRetrieveVersion_WithValidComponentAndIdentifier_ReturnsVersion() {
    String expectedVersion = "1.0.0";
    ComponentIdentifier componentIdentifier =
        new ComponentIdentifier("maven", Collections.singletonMap("version", expectedVersion));
    ApiComponentDTOV2 component = new ApiComponentDTOV2();
    component.componentIdentifier = ApiComponentIdentifierDTOV2.fromComponentIdentifier(componentIdentifier);
    ApiComponentChangeActionDTO dto = new ApiComponentChangeActionDTO(component);

    String version = dto.retrieveVersion();

    assertThat(version).isEqualTo(expectedVersion);
  }

  @Test
  public void testRetrieveVersion_WithNullComponent_ReturnsNull() {
    ApiComponentChangeActionDTO dto = new ApiComponentChangeActionDTO(null);

    String version = dto.retrieveVersion();

    assertThat(version).isNull();
  }

  @Test
  public void testRetrieveVersion_WithNullComponentIdentifier_ReturnsNull() {
    ApiComponentDTOV2 component = new ApiComponentDTOV2();
    component.componentIdentifier = ApiComponentIdentifierDTOV2.fromComponentIdentifier(null);
    ApiComponentChangeActionDTO dto = new ApiComponentChangeActionDTO(component);

    String version = dto.retrieveVersion();

    assertThat(version).isNull();
  }
}
