/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.mock;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

public interface ResponseProvider
{
  static final String CONTENT_TYPE_JSON = "application/json; charset=UTF-8";

  static final String CONTENT_TYPE_OCTET_STREAM = "application/octet-stream";

  void render(HttpServletResponse response) throws IOException;
}
