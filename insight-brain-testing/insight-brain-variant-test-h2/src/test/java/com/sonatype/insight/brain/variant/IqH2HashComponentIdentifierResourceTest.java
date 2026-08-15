/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URLEncoder;
import java.util.Date;

import jakarta.ws.rs.core.UriBuilder;

import com.sonatype.clm.dto.model.ComponentSummary;
import com.sonatype.clm.dto.model.component.ComponentDisplayName;
import com.sonatype.clm.dto.model.component.ComponentDisplayNamePart;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.component.ComponentDisplayNameUtil;
import com.sonatype.insight.brain.component.HashComponentIdentifierDTO;
import com.sonatype.insight.brain.component.HashComponentIdentifierResource;
import com.sonatype.insight.brain.model.component.HashComponentIdentifier;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@IqH2Test
class IqH2HashComponentIdentifierResourceTest
{
  private IqTestContext ctx;

  private static final String hash = "ab1234ab1234ab";

  private static final ComponentIdentifier COMPONENT_IDENTIFIER = ComponentIdentifier.createMavenCoordinates("g1",
      "a1", "v1", "c1", "e1");

  private static final String comment = "my comment";

  private final ObjectMapper objectMapper = new ObjectMapper();

  private HashComponentIdentifier hashComponentIdentifier;

  private HttpRequest restRequest() {
    return ctx.restRequest().path(HashComponentIdentifierResource.RESOURCE_PATH);
  }

  @BeforeEach
  void setup() {
    hashComponentIdentifier = new HashComponentIdentifier(hash, COMPONENT_IDENTIFIER);
  }

  private void mockComponentSummary(
      ComponentIdentifier componentIdentifier,
      ComponentSummary componentSummary) throws Exception
  {
    ctx.hdsRespondWith(componentSummary)
        .atUri(UriBuilder.fromPath("rest/component/summary")
            .queryParam("componentIdentifier", URLEncoder.encode(toJson(componentIdentifier), "UTF-8"))
            .build());
  }

  private String toJson(Object object) {
    try {
      return objectMapper.writeValueAsString(object);
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Test
  void testCRUD() throws Exception {
    Date createTime = new Date();
    hashComponentIdentifier.setComment(comment);
    hashComponentIdentifier.setCreateTime(createTime);

    // component must be unknown or we cannot claim it
    mockComponentSummary(hashComponentIdentifier.getComponentIdentifier(), ComponentSummary.create(false));

    // create
    HttpResponse response = restRequest().body(hashComponentIdentifier).post();
    ctx.assertResponseStatus(200, response);
    HashComponentIdentifierDTO serverResponse = response.getBody(HashComponentIdentifierDTO.class);
    assertHashComponentIdentifierDTO(hash, COMPONENT_IDENTIFIER, comment, createTime, serverResponse);

    // read
    response = restRequest().path(hashComponentIdentifier.getHash()).get();
    assertHashComponentIdentifierDTO(hash, COMPONENT_IDENTIFIER, comment, createTime, serverResponse);

    // update
    ComponentIdentifier updatedComponentIdentifier = COMPONENT_IDENTIFIER.createAlternativeVersion("updated-version");
    hashComponentIdentifier.setComponentIdentifier(updatedComponentIdentifier);
    mockComponentSummary(hashComponentIdentifier.getComponentIdentifier(), ComponentSummary.create(false));
    response = restRequest().body(hashComponentIdentifier).put();
    ctx.assertResponseStatus(200, response);
    serverResponse = response.getBody(HashComponentIdentifierDTO.class);

    assertHashComponentIdentifierDTO(hash, updatedComponentIdentifier, comment, createTime, serverResponse);

    // verify using GET
    response = restRequest().path(hashComponentIdentifier.getHash()).get();
    assertHashComponentIdentifierDTO(hash, updatedComponentIdentifier, comment, createTime, serverResponse);

    // delete
    response = restRequest().path(hashComponentIdentifier.getHash()).delete();
    ctx.assertResponseStatus(204, response);

    // verify using GET
    response = restRequest().path(hashComponentIdentifier.getHash()).get();
    ctx.assertResponseStatus(404, response);
  }

  private void assertHashComponentIdentifierDTO(
      String hash,
      ComponentIdentifier componentIdentifier,
      String comment,
      Date createTime,
      HashComponentIdentifierDTO hashComponentIdentifier)
  {
    assertThat(hashComponentIdentifier.hash).isEqualTo(hash);
    assertThat(hashComponentIdentifier.componentIdentifier).isEqualTo(componentIdentifier);
    assertThat(hashComponentIdentifier.comment).isEqualTo(comment);
    assertThat(hashComponentIdentifier.createTime).isEqualTo(createTime);

    ComponentDisplayName componentDisplayName = ComponentDisplayNameUtil.fromIdentifier(componentIdentifier);
    assertThat(hashComponentIdentifier.displayName.parts).hasSameSizeAs(componentDisplayName.parts);
    for (int i = 0; i < componentDisplayName.parts.size(); i++) {
      ComponentDisplayNamePart expected = componentDisplayName.parts.get(i);
      ComponentDisplayNamePart actual = hashComponentIdentifier.displayName.parts.get(i);
      assertThat(actual.field).isEqualTo(expected.field);
      assertThat(actual.value).isEqualTo(expected.value);
    }
    assertThat(hashComponentIdentifier.coordinates).isEqualTo(componentDisplayName.toString());
  }
}
