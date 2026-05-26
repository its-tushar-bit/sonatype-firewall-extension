/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentOrPurlIdentifierDTOV2;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadRequestException;
import jakarta.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.mockito.Mock;

public class ApiComponentVersionsServiceV2Test
    extends AbstractComponentTest
{
  @Inject
  private ApiComponentVersionsServiceV2 apiComponentVersionsServiceV2;

  @Mock
  private HdsClient client;

  @Test
  public void testGetComponentVersions() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1");

    when(client.get(List.class, ApiComponentVersionsServiceV2.HDS_COMPONENT_VERSIONS_LIST_PATH,
        Collections.singletonMap("componentIdentifier", ComponentIdentifierAdapter.toJson(componentIdentifier))))
            .thenReturn(Arrays.asList("v1", "v2", "v3", "v4"));

    ApiComponentOrPurlIdentifierDTOV2 apiComponentOrPurlIdentifierDTOV2 =
        ApiComponentOrPurlIdentifierDTOV2.fromComponentIdentifier(componentIdentifier);

    List<String> versions = apiComponentVersionsServiceV2.getComponentVersions(apiComponentOrPurlIdentifierDTOV2);

    assertThat(versions).containsExactly("v1", "v2", "v3", "v4");
  }

  @Test
  public void testGetComponentVersions_PurlIdentifier() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1");

    when(client.get(List.class, ApiComponentVersionsServiceV2.HDS_COMPONENT_VERSIONS_LIST_PATH,
        Collections.singletonMap("componentIdentifier", ComponentIdentifierAdapter.toJson(componentIdentifier))))
            .thenReturn(Arrays.asList("v1", "v2", "v3", "v4"));

    ApiComponentOrPurlIdentifierDTOV2 apiComponentOrPurlIdentifierDTOV2 = new ApiComponentOrPurlIdentifierDTOV2();
    apiComponentOrPurlIdentifierDTOV2.setPackageUrl("pkg:maven/g1/a1@v1?classifier=c1&type=e1");

    List<String> versions = apiComponentVersionsServiceV2.getComponentVersions(apiComponentOrPurlIdentifierDTOV2);

    assertThat(versions).containsExactly("v1", "v2", "v3", "v4");
  }

  @Test
  public void testGetComponentVersions_EmptyVersion() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "");

    when(client.get(List.class, ApiComponentVersionsServiceV2.HDS_COMPONENT_VERSIONS_LIST_PATH,
        Collections.singletonMap("componentIdentifier", ComponentIdentifierAdapter.toJson(componentIdentifier))))
            .thenReturn(Arrays.asList("v1", "v2", "v3", "v4"));

    ApiComponentOrPurlIdentifierDTOV2 apiComponentOrPurlIdentifierDTOV2 =
        ApiComponentOrPurlIdentifierDTOV2.fromComponentIdentifier(componentIdentifier);

    List<String> versions = apiComponentVersionsServiceV2.getComponentVersions(apiComponentOrPurlIdentifierDTOV2);

    assertThat(versions).containsExactly("v1", "v2", "v3", "v4");
  }

  @Test
  public void testGetComponentVersions_PurlIdentifier_EmptyVersion() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "");

    when(client.get(List.class, ApiComponentVersionsServiceV2.HDS_COMPONENT_VERSIONS_LIST_PATH,
        Collections.singletonMap("componentIdentifier", ComponentIdentifierAdapter.toJson(componentIdentifier))))
            .thenReturn(Arrays.asList("v1", "v2", "v3", "v4"));

    ApiComponentOrPurlIdentifierDTOV2 apiComponentOrPurlIdentifierDTOV2 = new ApiComponentOrPurlIdentifierDTOV2();
    apiComponentOrPurlIdentifierDTOV2.setPackageUrl("pkg:maven/g1/a1@");

    List<String> versions = apiComponentVersionsServiceV2.getComponentVersions(apiComponentOrPurlIdentifierDTOV2);

    assertThat(versions).containsExactly("v1", "v2", "v3", "v4");
  }

  @Test
  public void testGetComponentVersions_NullVersion() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", null);

    when(client.get(List.class, ApiComponentVersionsServiceV2.HDS_COMPONENT_VERSIONS_LIST_PATH,
        Collections.singletonMap("componentIdentifier", ComponentIdentifierAdapter.toJson(componentIdentifier))))
            .thenReturn(Arrays.asList("v1", "v2", "v3", "v4"));

    ApiComponentOrPurlIdentifierDTOV2 apiComponentOrPurlIdentifierDTOV2 =
        ApiComponentOrPurlIdentifierDTOV2.fromComponentIdentifier(componentIdentifier);

    List<String> versions = apiComponentVersionsServiceV2.getComponentVersions(apiComponentOrPurlIdentifierDTOV2);

    assertThat(versions).containsExactly("v1", "v2", "v3", "v4");
  }

  @Test
  public void testGetComponentVersions_PurlIdentifier_NullVersion() {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", null);

    when(client.get(List.class, ApiComponentVersionsServiceV2.HDS_COMPONENT_VERSIONS_LIST_PATH,
        Collections.singletonMap("componentIdentifier", ComponentIdentifierAdapter.toJson(componentIdentifier))))
            .thenReturn(Arrays.asList("v1", "v2", "v3", "v4"));

    ApiComponentOrPurlIdentifierDTOV2 apiComponentOrPurlIdentifierDTOV2 = new ApiComponentOrPurlIdentifierDTOV2();
    apiComponentOrPurlIdentifierDTOV2.setPackageUrl("pkg:maven/g1/a1");

    List<String> versions = apiComponentVersionsServiceV2.getComponentVersions(apiComponentOrPurlIdentifierDTOV2);
    assertThat(versions).containsExactly("v1", "v2", "v3", "v4");
  }

  @Test
  public void testGetComponentVersions_InvalidComponentIdentifier() {
    ApiComponentOrPurlIdentifierDTOV2 apiComponentOrPurlIdentifierDTOV2 = new ApiComponentOrPurlIdentifierDTOV2();
    apiComponentOrPurlIdentifierDTOV2.setFormat(ComponentIdentifier.FORMAT_MAVEN);
    apiComponentOrPurlIdentifierDTOV2.setCoordinates(Collections.singletonMap("no-such-coordinate", "x"));

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> apiComponentVersionsServiceV2.getComponentVersions(apiComponentOrPurlIdentifierDTOV2))
        .withMessage("Coordinates contain the following incorrect entries for the given format: [no-such-coordinate]");
  }

  @Test
  public void testGetComponentVersions_NullValue() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> apiComponentVersionsServiceV2.getComponentVersions(null))
        .withMessage("Missing component identifier");
  }
}
