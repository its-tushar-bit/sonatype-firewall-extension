/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.cyclonedx.CycloneDxSchema.Version;

public final class ThirdPartyUtils
{
  public static final Map<String, Version> CYCLONEDX_ACCEPTED_VERSIONS = ImmutableMap
      .of(Version.VERSION_11.getVersionString(), Version.VERSION_11,
          Version.VERSION_12.getVersionString(), Version.VERSION_12);
}
