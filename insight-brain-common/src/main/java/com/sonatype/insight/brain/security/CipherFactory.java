/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import org.sonatype.plexus.components.cipher.DefaultPlexusCipher;
import org.sonatype.plexus.components.cipher.PlexusCipher;

public class CipherFactory
{
  private CipherFactory() {
    // no-op
  }

  public static PlexusCipher createCipher() {
    if (FIPSModeDetector.isEnabled()) {
      return new FipsCipher();
    }
    return new DefaultPlexusCipher();
  }
}
