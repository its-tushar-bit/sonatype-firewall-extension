/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.utils;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.io.Buffer;

/**
 * Various utility methods for detecting media types.
 */
public final class MediaTypeUtils
{
  // Reusing the same approach as used by DropWizard's AssertServlet to ensure consistency
  private static final MimeTypes mimeTypes = new MimeTypes();

  static {
    Map<String, String> extras = new HashMap<String, String>();
    extras.put("json", "application/json");
    mimeTypes.setMimeMap(extras);
  }

  private MediaTypeUtils() {
    // utility class
  }

  /**
   * Attempts to detect the media type based on the given name of the resource.
   * 
   * @param name The resource name
   * @return Detected media type
   */
  public static String byName(final String name) {
    String mediaType = "application/octet-stream";
    Buffer buffer = mimeTypes.getMimeByExtension(name);
    if (buffer != null) {
      mediaType = buffer.toString("UTF-8");
      if (mediaType.startsWith("text")) {
        mediaType += ";charset=UTF-8";
      }
    }
    return mediaType;
  }
}
