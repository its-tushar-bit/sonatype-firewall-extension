/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.cyclonedx.BomParserFactory;
import org.cyclonedx.CycloneDxSchema.Version;
import org.cyclonedx.exception.ParseException;
import org.cyclonedx.model.Bom;
import org.cyclonedx.parsers.Parser;

public final class ThirdPartyUtils
{
  public static final Map<String, Version> CYCLONEDX_ACCEPTED_VERSIONS = ImmutableMap
      .of(Version.VERSION_11.getVersionString(), Version.VERSION_11,
          Version.VERSION_12.getVersionString(), Version.VERSION_12,
          Version.VERSION_13.getVersionString(), Version.VERSION_13);

  public static Bom parseBom(final String content) throws ParseException {
    byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
    Parser parser = BomParserFactory.createParser(bytes);
    return parser.parse(bytes);
  }
}
