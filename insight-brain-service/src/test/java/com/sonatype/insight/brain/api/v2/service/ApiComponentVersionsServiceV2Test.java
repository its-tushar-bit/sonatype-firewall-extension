/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.dataaccess.component.ComponentIdentifierAdapter;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadRequestException;

import com.google.inject.Binder;
import org.junit.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

public class ApiComponentVersionsServiceV2Test
    extends AbstractComponentTest
{
  @Inject
  private ApiComponentVersionsServiceV2 apiComponentVersionsServiceV2;

  @Mock
  private HdsClient client;

  @Override
  public void configure(Binder binder) {
    binder.bind(HdsClient.class).toInstance(client);
    super.configure(binder);
  }

  @Test
  public void testGetComponentVersions() throws Exception {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1");

    when(client.get(List.class, ApiComponentVersionsServiceV2.HDS_COMPONENT_VERSIONS_LIST_PATH,
        Collections.singletonMap("componentIdentifier", ComponentIdentifierAdapter.toJson(componentIdentifier))))
            .thenReturn(Arrays.asList("v1", "v2", "v3", "v4"));
    
    List<String> versions = apiComponentVersionsServiceV2
        .getComponentVersions(ApiComponentIdentifierDTOV2.fromComponentIdentifier(componentIdentifier));

    assertThat(versions).containsExactly("v1", "v2", "v3", "v4");
  }

  @Test
  public void testGetComponentVersions_EmptyVersion() throws Exception {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", "");

    when(client.get(List.class, ApiComponentVersionsServiceV2.HDS_COMPONENT_VERSIONS_LIST_PATH,
        Collections.singletonMap("componentIdentifier", ComponentIdentifierAdapter.toJson(componentIdentifier))))
            .thenReturn(Arrays.asList("v1", "v2", "v3", "v4"));

    List<String> versions = apiComponentVersionsServiceV2
        .getComponentVersions(ApiComponentIdentifierDTOV2.fromComponentIdentifier(componentIdentifier));

    assertThat(versions).containsExactly("v1", "v2", "v3", "v4");
  }

  @Test
  public void testGetComponentVersions_NullVersion() throws Exception {
    ComponentIdentifier componentIdentifier = ComponentIdentifier.createMavenCoordinates("g1", "a1", null);

    when(client.get(List.class, ApiComponentVersionsServiceV2.HDS_COMPONENT_VERSIONS_LIST_PATH,
        Collections.singletonMap("componentIdentifier", ComponentIdentifierAdapter.toJson(componentIdentifier))))
            .thenReturn(Arrays.asList("v1", "v2", "v3", "v4"));

    List<String> versions = apiComponentVersionsServiceV2
        .getComponentVersions(ApiComponentIdentifierDTOV2.fromComponentIdentifier(componentIdentifier));

    assertThat(versions).containsExactly("v1", "v2", "v3", "v4");
  }

  @Test
  public void testGetComponentVersions_InvalidComponentIdentifier() throws Exception {
    ApiComponentIdentifierDTOV2 apiComponentIdentifierDTOV2 = new ApiComponentIdentifierDTOV2();
    apiComponentIdentifierDTOV2.setFormat(ComponentIdentifier.FORMAT_MAVEN);
    apiComponentIdentifierDTOV2.setCoordinates(Collections.singletonMap("no-such-coordinate", "x"));

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> {
      apiComponentVersionsServiceV2.getComponentVersions(apiComponentIdentifierDTOV2);
    }).withMessage("Coordinates contain the following incorrect entries for the given format: [no-such-coordinate]");
  }
}
