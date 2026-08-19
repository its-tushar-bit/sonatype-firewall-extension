/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

// Vendored/copied from hosted-data-services/insight-test
package com.sonatype.insight.test;

import java.io.File;
import java.net.URL;

public class TestResources
{
  public static File getFile(String resourcePath) {
    URL resourceUrl = TestResources.class.getClassLoader().getResource(resourcePath);
    try {
      return resourceUrl != null ? new File(resourceUrl.toURI()) : null;
    }
    catch (Exception e) {
      throw new IllegalArgumentException("Cannot resolve resource: " + resourcePath, e);
    }
  }

  public static String getFilePath(String resourcePath) {
    File file = getFile(resourcePath);
    return file != null ? file.getPath() : null;
  }
}
