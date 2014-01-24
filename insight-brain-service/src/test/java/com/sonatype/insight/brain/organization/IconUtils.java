/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.io.IOException;
import java.io.InputStream;

import org.codehaus.plexus.util.IOUtil;
import org.junit.Assert;

class IconUtils
{
  private static byte[] loadImage(String resource) throws IOException {
    InputStream iconStream = IconUtils.class.getResourceAsStream(resource);
    Assert.assertNotNull(iconStream);
    try {
      return IOUtil.toByteArray(iconStream);
    }
    finally {
      IOUtil.close(iconStream);
    }
  }

  public static byte[] loadInvalidIcon() throws IOException {
    return loadImage("/assets/assets/components/errorModal.html");
  }

  public static byte[] loadIcon(String name) throws IOException {
    return loadImage("/assets/assets/img/" + name);
  }
}
