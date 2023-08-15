/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;
import org.cyclonedx.BomParserFactory;
import org.cyclonedx.CycloneDxSchema.Version;
import org.cyclonedx.exception.ParseException;
import org.cyclonedx.model.Bom;
import org.cyclonedx.parsers.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spdx.jacksonstore.MultiFormatStore;
import org.spdx.jacksonstore.MultiFormatStore.Format;
import org.spdx.jacksonstore.MultiFormatStore.Verbose;
import org.spdx.library.DefaultModelStore;
import org.spdx.library.InvalidSPDXAnalysisException;
import org.spdx.library.model.SpdxDocument;
import org.spdx.storage.IModelStore;
import org.spdx.storage.simple.InMemSpdxStore;

public final class ThirdPartyUtils
{
  public enum SbomFormat
  {
    XML, JSON;
  }

  private static final Logger log = LoggerFactory.getLogger(ThirdPartyUtils.class);

  public static final Map<String, Version> CYCLONEDX_ACCEPTED_VERSIONS_XML = ImmutableMap
      .of(Version.VERSION_11.getVersionString(), Version.VERSION_11,
          Version.VERSION_12.getVersionString(), Version.VERSION_12,
          Version.VERSION_13.getVersionString(), Version.VERSION_13,
          Version.VERSION_14.getVersionString(), Version.VERSION_14);

  public static final Map<String, Version> CYCLONEDX_ACCEPTED_VERSIONS_JSON =
      ImmutableMap.of(Version.VERSION_14.getVersionString(), Version.VERSION_14);

  public static final Map<String, String> SPDX_ACCEPTED_VERSIONS =
      ImmutableMap.of("SPDX-2.3", "2.3");

  public static Bom parseBom(final String content) throws ParseException {
    byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
    Parser parser = BomParserFactory.createParser(bytes);
    return parser.parse(bytes);
  }

  public static Bom parseAndValidateCycloneDx(final String content, SbomFormat format)
      throws InvalidSbomException, ParseException, IOException
  {
    byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
    Parser parser = BomParserFactory.createParser(bytes);

    Bom bom = parser.parse(bytes);

    validateCycloneDxVersion(format, bom);

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

  public static SpdxDocument parseAndValidateSpdx(final String content, SbomFormat sbomFormat)
      throws InvalidSPDXAnalysisException, InvalidSbomException, IOException
  {
    Format format = sbomFormat == SbomFormat.JSON ? Format.JSON : Format.XML;
    DefaultModelStore.reset();
    IModelStore modelStore = new InMemSpdxStore();

    String uri;
    try (MultiFormatStore multiFormatStore = new MultiFormatStore(modelStore, format, Verbose.COMPACT);
         InputStream in = new BufferedInputStream(new ByteArrayInputStream(content.getBytes()))) {
      uri = multiFormatStore.deSerialize(in, true);
    }
    catch (Exception e) {
      throw new IOException("Resource cannot be closed", e);
    }
    SpdxDocument spdxDocument = new SpdxDocument(modelStore, uri, DefaultModelStore.getDefaultCopyManager(), true);

    validateSpdxVersion(sbomFormat, spdxDocument);

    List<String> verificationErrors = filerOutDeprecationWarnings(spdxDocument.verify());

    if (!verificationErrors.isEmpty()) {
      InvalidSbomException invalidSbomException = new InvalidSbomException("The sbom is not valid.");
      // the "Relationship error: " prefix is added sometimes multiple times and doesn't bring any value in itself
      verificationErrors.forEach(ve -> invalidSbomException.addSuppressed(
          new InvalidSPDXAnalysisException(ve.replace("Relationship error: ", ""))));
      log.error(invalidSbomException.getMessage() + " There were " + verificationErrors.size() +
          " errors." , invalidSbomException);
      throw invalidSbomException;
    }
    return spdxDocument;
  }

  private static final Pattern DEPRECATION_PATTERN =
      Pattern.compile(".*Relationship error: [^\\s]+ is deprecated\\..*");

  private static List<String> filerOutDeprecationWarnings(final List<String> verificationErrors) {
    return verificationErrors.stream()
        .filter(s -> !DEPRECATION_PATTERN.matcher(s).matches())
        .collect(Collectors.toList());
  }

  public static Version getSchemaVersion(final String versionBom) {
    for (final Version version : Version.values()) {
      if (version.getVersionString().equals(versionBom)) {
        return version;
      }
    }
    return null;
  }

  public static void validateCycloneDxVersion(final SbomFormat format, final Bom bom) throws InvalidSbomException {
    if (format != null) {
      if (format == SbomFormat.XML) {
        Version version = ThirdPartyUtils.CYCLONEDX_ACCEPTED_VERSIONS_XML.get(bom.getSpecVersion());
        if (version == null) {
          throw new InvalidSbomException("CycloneDX XML " + bom.getSpecVersion() + " version is not supported");
        }
      }
      else if (format == SbomFormat.JSON) {
        Version version = ThirdPartyUtils.CYCLONEDX_ACCEPTED_VERSIONS_JSON.get(bom.getSpecVersion());
        if (version == null) {
          throw new InvalidSbomException("CycloneDX JSON " + bom.getSpecVersion() + " version is not supported");
        }
      }
      else {
        throw new InvalidSbomException("CycloneDX content encodingType (" + format + ") is not supported");
      }
    }
    else {
      throw new InvalidSbomException("Missing CycloneDX encoding type");
    }
  }

  public static void validateSpdxVersion(SbomFormat format,  SpdxDocument spdxDocument) throws InvalidSbomException {
    if (format != null) {
      try {
        final String specVersion = spdxDocument.getSpecVersion();
        if (StringUtils.isBlank(specVersion)) {
          throw new InvalidSbomException("SPDX version is not specified");
        }
        if (!ThirdPartyUtils.SPDX_ACCEPTED_VERSIONS.containsKey(specVersion)) {
          throw new InvalidSbomException("SPDX " + specVersion.replace("SPDX-", "") + " version is not supported");
        }
      }
      catch (InvalidSPDXAnalysisException e) {
        throw new InvalidSbomException("SPDX version is not specified");
      }
    }
    else {
      throw new InvalidSbomException("Missing SPDX encoding type");
    }
  }
}
