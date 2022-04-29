/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;
import org.cyclonedx.BomParserFactory;
import org.cyclonedx.CycloneDxSchema.Version;
import org.cyclonedx.exception.ParseException;
import org.cyclonedx.model.Bom;
import org.cyclonedx.parsers.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ThirdPartyUtils
{
  public static final String XML_SBOM = "XML";

  public static final String JSON_SBOM = "JSON";

  private static final Logger log = LoggerFactory.getLogger(ThirdPartyUtils.class);

  public static final Map<String, Version> CYCLONEDX_ACCEPTED_VERSIONS_XML = ImmutableMap
      .of(Version.VERSION_11.getVersionString(), Version.VERSION_11,
          Version.VERSION_12.getVersionString(), Version.VERSION_12,
          Version.VERSION_13.getVersionString(), Version.VERSION_13,
          Version.VERSION_14.getVersionString(), Version.VERSION_14);

  public static final Map<String, Version> CYCLONEDX_ACCEPTED_VERSIONS_JSON =
      ImmutableMap.of(Version.VERSION_14.getVersionString(), Version.VERSION_14);

  public static Bom parseBom(final String content) throws ParseException {
    byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
    Parser parser = BomParserFactory.createParser(bytes);
    return parser.parse(bytes);
  }

  public static Bom parseAndValidateSbom(final String content, String type) throws InvalidSbomException,
                                                                                   ParseException, IOException
  {
    byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
    Parser parser = BomParserFactory.createParser(bytes);

    Bom bom = parser.parse(bytes);

    validateCycloneDxVersion(type, bom);

    Version schemaVersion = getSchemaVersion(bom.getSpecVersion());
    List<ParseException> validate = parser.validate(bytes, schemaVersion);

    if (!validate.isEmpty()) {
      InvalidSbomException invalidSbomException = new InvalidSbomException("The sbom is not valid.");
      validate.forEach(invalidSbomException::addSuppressed);
      log.error(invalidSbomException.getMessage() + " There were " + invalidSbomException.getSuppressed().length +
          " errors." , invalidSbomException);
      throw invalidSbomException;
    }
    return bom;
  }

  public static Version getSchemaVersion(final String versionBom) {
    for (final Version version : Version.values()) {
      if (version.getVersionString().equals(versionBom)) {
        return version;
      }
    }
    return null;
  }

  public static void validateCycloneDxVersion(final String encodingType, final Bom bom) throws InvalidSbomException {
    if (StringUtils.isNotBlank(encodingType)) {
      if (ThirdPartyUtils.XML_SBOM.equalsIgnoreCase(encodingType)) {
        Version version = ThirdPartyUtils.CYCLONEDX_ACCEPTED_VERSIONS_XML.get(bom.getSpecVersion());
        if (version == null) {
          throw new InvalidSbomException("CycloneDX XML " + bom.getSpecVersion() + " version is not supported");
        }
      }
      else if (ThirdPartyUtils.JSON_SBOM.equalsIgnoreCase(encodingType)) {
        Version version = ThirdPartyUtils.CYCLONEDX_ACCEPTED_VERSIONS_JSON.get(bom.getSpecVersion());
        if (version == null) {
          throw new InvalidSbomException("CycloneDX JSON " + bom.getSpecVersion() + " version is not supported");
        }
      }
      else {
        throw new InvalidSbomException("CycloneDX content encodingType (" + encodingType + ") is not supported");
      }
    }
    else {
      throw new InvalidSbomException("Missing CycloneDX encoding type");
    }
  }
}
