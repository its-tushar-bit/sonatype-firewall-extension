/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

import static org.assertj.core.api.Assertions.assertThat;

public class IconUtils
{
  private static byte[] loadImage(String resource) throws IOException {
    try (InputStream iconStream = IconUtils.class.getResourceAsStream(resource)) {
      assertThat(iconStream).as("Missing resource: " + resource).isNotNull();
      return IOUtils.toByteArray(iconStream);
    }
  }

  public static byte[] loadInvalidIcon() throws IOException {
    return loadImage("/IconUtils/index.html");
  }

  public static byte[] loadIconFromProductAssets(String name) throws IOException {
    return loadImage("/assets/img/" + name);
  }
}
