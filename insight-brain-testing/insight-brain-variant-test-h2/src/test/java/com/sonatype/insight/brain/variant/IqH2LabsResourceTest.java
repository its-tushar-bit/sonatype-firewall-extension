/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.labs.LabsResource;

import org.junit.jupiter.api.Test;

@IqH2Test
class IqH2LabsResourceTest
{
  private IqTestContext ctx;

  @Test
  void testLabsGetMethod() throws Exception {
    assertGetMethod(LabsResource.RESOURCE_PATH);
  }

  @Test
  void testLabsGetMethod_Subpath() throws Exception {
    assertGetMethod(LabsResource.RESOURCE_PATH + "/test");
  }

  @Test
  void testLabsGetMethod_MultipleSubpath() throws Exception {
    assertGetMethod(LabsResource.RESOURCE_PATH + "/test/test2");
  }

  private void assertGetMethod(String path) throws Exception {
    ctx.hdsRespondWith("[]").atUri(path);
    HttpResponse response = ctx.restRequest().path(path).get();
    ctx.assertResponseStatus(200, response);
  }

  @Test
  void testLabsPostMethod() throws Exception {
    assertPostMethod(LabsResource.RESOURCE_PATH);
  }

  @Test
  void testLabsPostMethod_Subpath() throws Exception {
    assertPostMethod(LabsResource.RESOURCE_PATH + "/test");
  }

  @Test
  void testLabsPostMethod_MultipleSubpath() throws Exception {
    assertPostMethod(LabsResource.RESOURCE_PATH + "/test1/test2");
  }

  private void assertPostMethod(String path) throws Exception {
    ctx.hdsRespondWith("[]").atUri(path);
    HttpResponse response = ctx.restRequest().path(path).post();
    ctx.assertResponseStatus(200, response);
  }
}
