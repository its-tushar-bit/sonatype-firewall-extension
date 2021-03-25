/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.labs;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Test;

public class LabsResourceTest
    extends AbstractResourceTest
{
  @Test
  public void testLabsGetMethod() throws Exception {
    assertGetMethod(LabsResource.RESOURCE_PATH);
  }

  @Test
  public void testLabsGetMethod_Subpath() throws Exception {
    assertGetMethod(LabsResource.RESOURCE_PATH + "/test");
  }

  @Test
  public void testLabsGetMethod_MultipleSubpath() throws Exception {
    assertGetMethod(LabsResource.RESOURCE_PATH + "/test/test2");
  }

  private void assertGetMethod(String path) throws Exception {
    hdsRespondWith("[]").atUri(path);
    HttpResponse response = restRequest().path(path).get();
    assertResponseStatus(200, response);
  }

  @Test
  public void testLabsPostMethod() throws Exception {
    assertPostMethod(LabsResource.RESOURCE_PATH);
  }

  @Test
  public void testLabsPostMethod_Subpath() throws Exception {
    assertPostMethod(LabsResource.RESOURCE_PATH + "/test");
  }

  @Test
  public void testLabsPostMethod_MultipleSubpath() throws Exception {
    assertPostMethod(LabsResource.RESOURCE_PATH + "/test1/test2");
  }

  private void assertPostMethod(String path) throws Exception {
    hdsRespondWith("[]").atUri(path);
    HttpResponse response = restRequest().path(path).post();
    assertResponseStatus(200, response);
  }
}
