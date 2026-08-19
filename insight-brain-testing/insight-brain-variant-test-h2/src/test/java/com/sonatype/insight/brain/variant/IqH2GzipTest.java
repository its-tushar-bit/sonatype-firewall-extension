/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.zip.GZIPOutputStream;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.service.ApiComponentVersionsServiceV2;
import com.sonatype.insight.json.store.JsonUtils;

import com.google.common.net.HttpHeaders;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@IqH2Test
class IqH2GzipTest
{
  private IqTestContext ctx;

  private byte[] gzip(Object pojo) {
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      try (GZIPOutputStream gzip = new GZIPOutputStream(baos)) {
        gzip.write(JsonUtils.generate(pojo));
      }
      return baos.toByteArray();
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private HttpRequest restRequest() {
    return ctx.restRequest().header(HttpHeaders.CONTENT_ENCODING, "gzip");
  }

  @Test
  public void testCompressedRequestBody() throws Exception {
    ctx.hdsRespondWith(Collections.singletonList("1.0"))
        .atUri(ApiComponentVersionsServiceV2.HDS_COMPONENT_VERSIONS_LIST_PATH);

    ApiComponentIdentifierDTOV2 request = ApiComponentIdentifierDTOV2
        .fromComponentIdentifier(ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1", "c1", "e1"));

    HttpResponse response = restRequest().path(PublicApiPaths.COMPONENT_VERSIONS_PATH_V2).body(gzip(request)).post();
    ctx.assertResponseStatus(200, response);

    assertThat(response.getBodyList()).containsExactly("1.0");
  }
}
