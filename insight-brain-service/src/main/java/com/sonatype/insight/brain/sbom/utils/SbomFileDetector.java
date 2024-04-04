/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.utils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;
import javax.inject.Named;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import com.sonatype.insight.brain.utils.AutoDeletingTempFile;
import com.sonatype.insight.scan.file.InvalidSbomException;
import com.sonatype.insight.scan.file.SbomFormat;
import com.sonatype.insight.scan.file.ThirdPartyUtils;
import com.sonatype.insight.scan.file.UnsupportedSbomException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.tika.Tika;
import org.cyclonedx.exception.ParseException;
import org.cyclonedx.model.Bom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spdx.library.InvalidSPDXAnalysisException;
import org.spdx.library.model.SpdxDocument;
import org.spdx.library.model.SpdxPackage;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;

@Named
public class SbomFileDetector
{
  private static final Logger log = LoggerFactory.getLogger(SbomFileDetector.class);

  public static final String SPEC_CYCLONEDX = "CycloneDx";

  public static final String SPEC_SPDX = "SPDX";

  public static final String SPDX_VERSION_PREFIX = "SPDX-";

  private final Set<String> supportedSbomMimeTypes = ImmutableSet.of(APPLICATION_XML, APPLICATION_JSON);

  private final Tika tika = new Tika();

  private final ObjectMapper objectMapper = new ObjectMapper();

  private final SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();

  public SbomDetectionResult getSbomDetectionResult(final InputStream sbomInputStream) {
    if (sbomInputStream == null) {
      return sbomDetectionErrorResult("provided content is not recognizable as an SBOM");
    }

    try (AutoDeletingTempFile tempFile = new AutoDeletingTempFile()) {
      Files.copy(sbomInputStream, tempFile.getPath(), REPLACE_EXISTING);
      return detect(tempFile.getPath().toFile());
    }
    catch (IOException e) {
      log.error("error detecting SBOM metadata", e);
      return sbomDetectionErrorResult("internal error in processing SBOM");
    }
  }

  public SbomDetectionResult getSbomDetectionResult(String sbomString) {
    if (StringUtils.isBlank(sbomString)) {
      return sbomDetectionErrorResult("provided content is not recognizable as an SBOM");
    }

    try (AutoDeletingTempFile tempFile = new AutoDeletingTempFile()) {
      Path file = Files.write(tempFile.getPath(), sbomString.getBytes(StandardCharsets.UTF_8));
      return detect(file.toFile());
    }
    catch (IOException e) {
      log.error("error detecting SBOM metadata", e);
      return sbomDetectionErrorResult("internal error in processing SBOM");
    }
  }

  public SbomDetectionResult getSbomDetectionResult(File sbomFile) {
    if (sbomFile == null) {
      return sbomDetectionErrorResult("invalid SBOM file input");
    }
    return detect(sbomFile);
  }

  private SbomDetectionResult detect(File sbomFile) {
    try {
      SbomDetectionResult result = new SbomDetectionResult();
      result.mimeType = tika.detect(sbomFile);
      String sbomStringContent = getSbomStringContent(sbomFile);
      if (TEXT_PLAIN.equals(result.mimeType)) {
        if (isPlainTextValidJson(sbomStringContent)) {
          result.mimeType = APPLICATION_JSON;
        }
        else if (isPlainTextValidXml(sbomStringContent)) {
          result.mimeType = APPLICATION_XML;
        }
      }

      if (supportedSbomMimeTypes.contains(result.mimeType)) {
        return attemptDetectingSbomFromContent(sbomStringContent, result);
      }
      result.errorMessage = "provided file type is not a supported SBOM file type";
      return result;
    }
    catch (IOException e) {
      log.debug("error detecting SBOM metadata", e);
      return sbomDetectionErrorResult("internal error in processing SBOM");
    }
  }

  private SbomDetectionResult sbomDetectionErrorResult(String errorMessage) {
    SbomDetectionResult result = new SbomDetectionResult();
    result.isSbom = false;
    result.errorMessage = errorMessage;
    return result;
  }

  private SbomDetectionResult attemptDetectingSbomFromContent(final String sbom, final SbomDetectionResult sbomResult) {
    if (ThirdPartyUtils.looksLikeCycloneDX(sbom)) {
      return tryDetectingAsCycloneDx(sbom, sbomResult);
    }
    else {
      try {
        return tryDetectingAsSpdx(sbom, sbomResult);
      }
      catch (IOException | InvalidSPDXAnalysisException e) {
        log.error("Not a valid/supported sbom file", e);
        sbomResult.errorMessage = "Not a valid/supported sbom file";
      }
    }
    return sbomResult;
  }

  private SbomDetectionResult tryDetectingAsSpdx(final String sbom, final SbomDetectionResult sbomResult)
      throws IOException, InvalidSPDXAnalysisException
  {
    try {
      SbomFormat sbomFormat = SbomFormat.forMimeType(sbomResult.mimeType);
      SpdxDocument spdxDocument =
          ThirdPartyUtils.parseAndValidateSpdx(sbom, Objects.requireNonNull(sbomFormat));
      sbomResult.isSbom = true;
      populateSpdxResult(sbomResult, sbomFormat, spdxDocument);
    }
    catch (UnsupportedSbomException e) {
      sbomResult.errorMessage = e.getMessage();
    }
    catch (InvalidSbomException e) {
      log.debug("error parsing content as sbom", e);
      sbomResult.errorMessage = "not a valid SPDX SBOM file";
    }
    return sbomResult;
  }

  private SbomDetectionResult tryDetectingAsCycloneDx(final String fileContent, final SbomDetectionResult sbomResult) {
    try {
      SbomFormat sbomFormat = SbomFormat.forMimeType(sbomResult.mimeType);
      Bom bom = ThirdPartyUtils.parseAndValidateCycloneDx(fileContent,
          Objects.requireNonNull(sbomFormat));
      sbomResult.isSbom = true;
      populateCycloneDxResult(sbomResult, sbomFormat, bom);
    }
    catch (UnsupportedSbomException e) {
      sbomResult.errorMessage = e.getMessage();
    }
    catch (IOException | ParseException e) {
      log.debug("error parsing content as sbom", e);
      sbomResult.errorMessage = "not a valid CycloneDx SBOM file";
    }
    return sbomResult;
  }

  private void populateSpdxResult(
      final SbomDetectionResult sbomResult,
      final SbomFormat sbomFormat,
      final SpdxDocument document)
      throws InvalidSPDXAnalysisException
  {
    sbomResult.summary = new SbomSummary();
    sbomResult.summary.serialNumber = SbomSpdxUtils.getOrGenerateSpdxSerialNumber(document);
    sbomResult.summary.specification = SPEC_SPDX;
    sbomResult.summary.version = StringUtils.replace(document.getSpecVersion(), SPDX_VERSION_PREFIX, "");
    sbomResult.summary.format = StringUtils.lowerCase(sbomFormat.toString());
    sbomResult.summary.componentCount = CollectionUtils.size(SbomSpdxUtils.getAllPackages(document));
    sbomResult.summary.vulnerabilityCount = CollectionUtils.size(SbomSpdxUtils.getAllVulnerabilities(document));
    SpdxPackage rootPackage = SbomSpdxUtils.getRootPackage(document);
    if (rootPackage != null) {
      sbomResult.summary.applicationName = rootPackage.getName().orElse(null);
      sbomResult.summary.applicationVersion = rootPackage.getVersionInfo().orElse(null);
    }
    sbomResult.summary.creationDetails = SbomSpdxUtils.getSbomCreationDetailsJson(document);
  }

  private static void populateCycloneDxResult(
      final SbomDetectionResult sbomResult,
      final SbomFormat sbomFormat,
      final Bom bom)
  {
    sbomResult.summary = new SbomSummary();
    sbomResult.summary.specification = SPEC_CYCLONEDX;
    sbomResult.summary.version = bom.getSpecVersion();
    sbomResult.summary.format = sbomFormat.toString();
    sbomResult.summary.componentCount = CollectionUtils.size(bom.getComponents());
    sbomResult.summary.vulnerabilityCount = CollectionUtils.size(bom.getVulnerabilities());
    sbomResult.summary.applicationName = SbomCycloneDxUtils.getApplicationNameSafely(bom);
    sbomResult.summary.applicationVersion = SbomCycloneDxUtils.getApplicationVersionSafely(bom);
    sbomResult.summary.serialNumber = SbomCycloneDxUtils.getOrGenerateSerialNumber(bom);
    sbomResult.summary.creationDetails = SbomCycloneDxUtils.getSbomCreationDetails(bom);
  }

  private boolean isPlainTextValidJson(String sbomContent) {
    try {
      return objectMapper.readTree(sbomContent) != null;
    }
    catch (IOException e) {
      log.debug("File content is not valid a JSON document");
    }
    return false;
  }

  private boolean isPlainTextValidXml(String sbomContent) {
    try {
      SAXParser saxParser = saxParserFactory.newSAXParser();
      saxParser.parse(new ByteArrayInputStream(sbomContent.getBytes(StandardCharsets.UTF_8)), new DefaultHandler());
      return true;
    }
    catch (ParserConfigurationException | IOException | SAXException e) {
      log.debug("File content is not valid a XML document");
    }
    return false;
  }

  private String getSbomStringContent(File sbomFile) throws IOException {
    return FileUtils.readFileToString(sbomFile, StandardCharsets.UTF_8);
  }
}
