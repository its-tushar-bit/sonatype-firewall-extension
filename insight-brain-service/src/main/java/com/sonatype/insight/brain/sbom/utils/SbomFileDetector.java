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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import javax.inject.Named;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.sbom.SbomSpecification;
import com.sonatype.insight.brain.utils.AutoDeletingTempFile;
import com.sonatype.insight.scan.file.InvalidSbomException;
import com.sonatype.insight.scan.file.SbomFormat;
import com.sonatype.insight.scan.file.ThirdPartyUtils;
import com.sonatype.insight.scan.file.UnsupportedSbomException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
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
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.helpers.DefaultHandler;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;

@Named
public class SbomFileDetector
{
  private static final Logger log = LoggerFactory.getLogger(SbomFileDetector.class);

  public static final String SPDX_VERSION_PREFIX = "SPDX-";

  public static final String PROVIDED_FILE_NOT_SUPPORTED_ERROR =
      "Provided file type is not a supported SBOM file type.";

  private final Set<String> supportedSbomMimeTypes = ImmutableSet.of(APPLICATION_XML, APPLICATION_JSON);

  private final Tika tika = new Tika();

  private final ObjectMapper objectMapper = new ObjectMapper();

  private static final SAXParserFactory saxParserFactory;

  public SbomDetectionResult getSbomDetectionResult(final InputStream sbomInputStream) {
    if (sbomInputStream == null) {
      return sbomDetectionErrorResult("Provided content is not recognizable as an SBOM.", null);
    }

    try (AutoDeletingTempFile tempFile = new AutoDeletingTempFile()) {
      Files.copy(sbomInputStream, tempFile.getPath(), REPLACE_EXISTING);
      return detect(tempFile.getPath().toFile());
    }
    catch (IOException e) {
      log.error("error detecting SBOM metadata", e);
      return sbomDetectionErrorResult("Internal error in processing SBOM.", e);
    }
  }

  public SbomDetectionResult getSbomDetectionResult(String sbomString) {
    if (StringUtils.isBlank(sbomString)) {
      return sbomDetectionErrorResult("Provided content is not recognizable as an SBOM.", null);
    }

    try (AutoDeletingTempFile tempFile = new AutoDeletingTempFile()) {
      Path file = Files.writeString(tempFile.getPath(), sbomString);
      return detect(file.toFile());
    }
    catch (IOException e) {
      log.error("error detecting SBOM metadata", e);
      return sbomDetectionErrorResult("Internal error in processing SBOM.", e);
    }
  }

  public SbomDetectionResult getSbomDetectionResult(File sbomFile) {
    return detect(sbomFile);
  }

  private SbomDetectionResult detect(File sbomFile) {
    if (sbomFile == null || !sbomFile.exists() || sbomFile.length() == 0) {
      return sbomDetectionErrorResult("Invalid SBOM file input.", null);
    }

    SbomDetectionResult result;
    try {
      result = new SbomDetectionResult();
      result.isBinary = true;
      result.mimeType = tika.detect(sbomFile);
      if (TEXT_PLAIN.equals(result.mimeType)) {
        String sbomStringContent = getSbomStringContent(sbomFile);
        if (isPlainTextValidJson(sbomStringContent)) {
          result.mimeType = APPLICATION_JSON;
          attemptDetectingSbomFromContent(sbomStringContent, result);
        }
        else if (isPlainTextValidXml(sbomStringContent)) {
          result.mimeType = APPLICATION_XML;
          attemptDetectingSbomFromContent(sbomStringContent, result);
        }
        else {
          result.errorMessage = PROVIDED_FILE_NOT_SUPPORTED_ERROR;
        }
      }
      else if (supportedSbomMimeTypes.contains(result.mimeType)) {
        attemptDetectingSbomFromContent(getSbomStringContent(sbomFile), result);
      }
      else {
        result.errorMessage = PROVIDED_FILE_NOT_SUPPORTED_ERROR;
      }
    }
    catch (IOException e) {
      log.debug("error detecting SBOM metadata", e);
      result = sbomDetectionErrorResult("Internal error in processing SBOM.", e);
    }
    // If there are any SBOM validation errors from parsing
    // then we should set this to be an SBOM scan rather than a binary scan
    if (result.validationErrors != null) {
      result.isBinary = false;
    }
    return result;
  }

  private SbomDetectionResult sbomDetectionErrorResult(String errorMessage, Exception e) {
    SbomDetectionResult result = new SbomDetectionResult();
    result.errorMessage = errorMessage;
    result.validationErrors = getErrors(e);
    return result;
  }

  private SbomDetectionResult attemptDetectingSbomFromContent(final String sbom, final SbomDetectionResult sbomResult) {
    if (ThirdPartyUtils.looksLikeCycloneDX(sbom)) {
      return tryDetectingAsCycloneDx(sbom, sbomResult);
    }
    else if (SbomSpdxUtils.looksLikeSpdxDocument(sbom)) {
      try {
        return tryDetectingAsSpdx(sbom, sbomResult);
      }
      catch (IOException | InvalidSPDXAnalysisException e) {
        log.error("Not a valid/supported sbom file.", e);
        sbomResult.errorMessage = "Not a valid/supported sbom file.";
        sbomResult.validationErrors = getErrors(e);
      }
    }
    else {
      sbomResult.errorMessage = "Not a valid/supported sbom file.";
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
      sbomResult.validationErrors = getErrors(e);
    }
    catch (InvalidSbomException e) {
      log.debug("error parsing content as sbom", e);
      sbomResult.errorMessage = "Not a valid SPDX SBOM file.";
      sbomResult.validationErrors = getErrors(e);
    }
    return sbomResult;
  }

  private SbomDetectionResult tryDetectingAsCycloneDx(final String fileContent, final SbomDetectionResult sbomResult) {
    try {
      SbomFormat sbomFormat = SbomFormat.forMimeType(sbomResult.mimeType);

      Bom bom;
      try {
        bom = ThirdPartyUtils.parseAndValidateCycloneDx(fileContent,
            Objects.requireNonNull(sbomFormat));
      }
      catch (InvalidSbomException ex) {
        if (SystemConfigurationPropertyFeature.SKIP_SBOM_IMPORT_VALIDATION.isEnabled()) {
          log.info("Validation was skipped due to system property skipSbomImportValidation being enabled.");
          bom = ThirdPartyUtils.parseCycloneDxWithNoValidation(fileContent,
              Objects.requireNonNull(sbomFormat));
          log.info("SBOM validation skipped per configuration");
        }
        else {
          throw ex;
        }
      }

      sbomResult.isSbom = true;
      populateCycloneDxResult(sbomResult, sbomFormat, bom);
    }
    catch (UnsupportedSbomException e) {
      sbomResult.errorMessage = e.getMessage();
      sbomResult.validationErrors = getErrors(e);
    }
    catch (IOException | ParseException | InvalidSbomException e) {
      log.debug("error parsing content as sbom", e);
      sbomResult.errorMessage = "Not a valid CycloneDx SBOM file.";
      sbomResult.validationErrors = getErrors(e);
    }
    return sbomResult;
  }

  private List<String> getErrors(final Throwable t) {
    if (t == null) {
      return null;
    }
    List<String> errors = new ArrayList<>();
    populateErrors(new HashSet<>(), t, errors);
    if (errors.isEmpty()) {
      return null;
    }
    return errors;
  }

  private void populateErrors(final Set<Throwable> processed, final Throwable t, final List<String> errors) {
    if (!processed.add(t)) {
      return;
    }
    if (t instanceof ParseException || t instanceof InvalidSPDXAnalysisException) {
      errors.add(t.getMessage());
    }
    for (Throwable child : t.getSuppressed()) {
      populateErrors(processed, child, errors);
    }
    if (t.getCause() != null) {
      populateErrors(processed, t.getCause(), errors);
    }
  }

  private void populateSpdxResult(
      final SbomDetectionResult sbomResult,
      final SbomFormat sbomFormat,
      final SpdxDocument document)
      throws InvalidSPDXAnalysisException
  {
    sbomResult.summary = new SbomSummary();
    sbomResult.summary.serialNumber = SbomSpdxUtils.getOrGenerateSpdxSerialNumber(document);
    sbomResult.summary.specification = SbomSpecification.SPDX.toString();
    sbomResult.summary.version = StringUtils.replace(document.getSpecVersion(), SPDX_VERSION_PREFIX, "");
    sbomResult.summary.format = StringUtils.lowerCase(sbomFormat.toString(), Locale.ROOT);
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
    sbomResult.summary.specification = SbomSpecification.CYCLONEDX.toString();
    sbomResult.summary.version = bom.getSpecVersion();
    sbomResult.summary.format = sbomFormat.toString();
    sbomResult.summary.componentCount = CollectionUtils.size(bom.getComponents());
    sbomResult.summary.vulnerabilityCount = CollectionUtils.size(bom.getVulnerabilities());
    sbomResult.summary.applicationName = SbomCycloneDxUtils.getApplicationNameSafely(bom);
    sbomResult.summary.applicationVersion = SbomCycloneDxUtils.getApplicationVersionSafely(bom);
    sbomResult.summary.serialNumber = SbomCycloneDxUtils.getOrGenerateSerialNumber(bom);
    sbomResult.summary.creationDetails = SbomCycloneDxUtils.getSbomCreationDetailsJson(bom);
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

  @VisibleForTesting
  boolean isPlainTextValidXml(String sbomContent) {
    try {
      SAXParser saxParser = saxParserFactory.newSAXParser();
      saxParser.parse(new ByteArrayInputStream(sbomContent.getBytes(StandardCharsets.UTF_8)), new DefaultHandler());
      return true;
    }
    catch (ParserConfigurationException | IOException | SAXException e) {
      log.debug("File content is not valid a XML document. {}", e.getMessage());
    }
    return false;
  }

  private String getSbomStringContent(File sbomFile) throws IOException {
    return FileUtils.readFileToString(sbomFile, StandardCharsets.UTF_8);
  }

  static {
    saxParserFactory = SAXParserFactory.newInstance();
    saxParserFactory.setNamespaceAware(true);
    try {
      saxParserFactory.setFeature("http://xml.org/sax/features/external-general-entities", false);
      saxParserFactory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
      saxParserFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    }
    catch (SAXNotSupportedException | SAXNotRecognizedException | ParserConfigurationException e) {
      log.debug("Error configuring SAXParserFactory", e);
    }
  }
}
