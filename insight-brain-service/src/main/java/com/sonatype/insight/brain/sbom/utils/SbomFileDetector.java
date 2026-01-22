/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.utils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import jakarta.inject.Named;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.sbom.SbomSpecification;
import com.sonatype.insight.brain.utils.AutoDeletingTempFile;
import com.sonatype.insight.scan.file.SbomFormat;
import com.sonatype.insight.scan.file.SbomProcessingException;
import com.sonatype.insight.scan.file.SbomValidationException;
import com.sonatype.insight.scan.file.ThirdPartyUtils;
import com.sonatype.insight.scan.file.UnsupportedSbomException;
import com.sonatype.insight.scan.file.ValidationException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.tika.Tika;
import org.cyclonedx.exception.ParseException;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spdx.library.InvalidSPDXAnalysisException;
import org.spdx.library.model.SpdxDocument;
import org.spdx.library.model.SpdxPackage;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.MediaType.APPLICATION_XML;
import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;

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

  public SbomDetectionResult getSbomDetectionResult(
      String sbomString,
      String originalFilename,
      boolean ignoreValidationError)
  {
    if (StringUtils.isBlank(sbomString)) {
      return sbomDetectionErrorResult("Provided content is not recognizable as an SBOM.", null);
    }

    try (AutoDeletingTempFile tempFile = new AutoDeletingTempFile()) {
      Path path = Files.writeString(tempFile.getPath(), sbomString);
      return getSbomDetectionResult(path, originalFilename, ignoreValidationError);
    }
    catch (IOException e) {
      log.error("error detecting SBOM metadata", e);
      return sbomDetectionErrorResult("Internal error in processing SBOM.", e);
    }
  }

  public SbomDetectionResult getSbomDetectionResult(
      final Path sbomPath,
      final String originalFilename,
      final boolean ignoreValidationError)
  {
    File sbomFile = sbomPath == null ? null : sbomPath.toFile();

    if (sbomFile == null || !sbomFile.exists() || sbomFile.length() == 0) {
      return sbomDetectionErrorResult("Invalid SBOM file input.", null);
    }

    SbomDetectionResult result;
    try {
      result = new SbomDetectionResult();
      result.mimeType = tika.detect(sbomFile);
      result.filename = originalFilename;
      if (TEXT_PLAIN.equals(result.mimeType)) {
        String sbomStringContent = getSbomStringContent(sbomFile);
        if (isPlainTextValidJson(sbomStringContent)) {
          result.mimeType = APPLICATION_JSON;
          attemptDetectingSbomFromContent(sbomStringContent, result, ignoreValidationError);
        }
        else if (isPlainTextValidXml(sbomStringContent)) {
          result.mimeType = APPLICATION_XML;
          attemptDetectingSbomFromContent(sbomStringContent, result, ignoreValidationError);
        }
        else {
          result.errorMessage = PROVIDED_FILE_NOT_SUPPORTED_ERROR;
        }
      }
      else if (supportedSbomMimeTypes.contains(result.mimeType)) {
        attemptDetectingSbomFromContent(getSbomStringContent(sbomFile), result, ignoreValidationError);
      }
      else {
        result.errorMessage = PROVIDED_FILE_NOT_SUPPORTED_ERROR;
      }
    }
    catch (IOException e) {
      log.debug("error detecting SBOM metadata", e);
      result = sbomDetectionErrorResult("Internal error in processing SBOM.", e);
    }

    return result;
  }

  private SbomDetectionResult sbomDetectionErrorResult(final String errorMessage, final Exception e) {
    SbomDetectionResult result = new SbomDetectionResult();
    result.errorMessage = errorMessage;
    result.validationErrors = getErrors(e);
    return result;
  }

  private SbomDetectionResult attemptDetectingSbomFromContent(
      final String sbom,
      final SbomDetectionResult sbomResult,
      final boolean ignoreValidationError)
  {
    if (ThirdPartyUtils.looksLikeCycloneDX(sbom)) {
      return tryDetectingAsCycloneDx(sbom, sbomResult, ignoreValidationError);
    }
    else if (SbomSpdxUtils.looksLikeSpdxDocument(sbom)) {
      return tryDetectingAsSpdx(sbom, sbomResult, ignoreValidationError);
    }
    else {
      sbomResult.errorMessage = "Not a valid/supported SBOM file.";
    }
    return sbomResult;
  }

  private SbomDetectionResult tryDetectingAsSpdx(
      final String sbom,
      final SbomDetectionResult sbomResult,
      final boolean ignoreValidationError)
  {
    SbomFormat sbomFormat = SbomFormat.forMimeType(sbomResult.mimeType);
    try {
      sbomResult.isSbom = true;

      SpdxDocument spdxDocument;
      try {
        spdxDocument = ThirdPartyUtils.parseAndValidateSpdx(sbom, Objects.requireNonNull(sbomFormat));
        sbomResult.isValid = true;
      }
      catch (SbomValidationException e) {
        log.debug("Error validating SPDX SBOM, file name: {}, scan type: {}", sbomResult.filename, "SBOM", e);
        sbomResult.isValid = false;
        sbomResult.isValidationErrorIgnorable = true;

        spdxDocument = ThirdPartyUtils.parseSpdxWithNoValidation(sbom, Objects.requireNonNull(sbomFormat));

        if (!shouldIgnoreValidationError(ignoreValidationError)) {
          sbomResult.errorMessage = "Not a valid SPDX SBOM file.";
          sbomResult.validationErrors = getErrors(e);
        }
      }

      populateSpdxResult(sbomResult, sbomFormat, spdxDocument);
    }
    catch (UnsupportedSbomException e) {
      log.debug("Error validating SPDX SBOM, file name: {}, scan type: {}", sbomResult.filename, "SBOM", e);
      sbomResult.isValid = false;
      sbomResult.isValidationErrorIgnorable = false;
      sbomResult.errorMessage = e.getMessage();
      sbomResult.validationErrors = getErrors(e);
    }
    catch (SbomProcessingException | InvalidSPDXAnalysisException e) {
      log.debug("Error validating SPDX SBOM, file name: {}, scan type: {}", sbomResult.filename, "SBOM", e);
      sbomResult.isValid = false;
      sbomResult.isValidationErrorIgnorable = false;
      sbomResult.errorMessage = "Not a valid SPDX SBOM file.";
      sbomResult.validationErrors = getErrors(e);
      sbomResult.summary = new SbomSummary();
      sbomResult.summary.specification = SbomSpecification.SPDX.toString();
      sbomResult.summary.format = sbomFormat.toString();
    }
    return sbomResult;
  }

  private SbomDetectionResult tryDetectingAsCycloneDx(
      final String fileContent,
      final SbomDetectionResult sbomResult,
      final boolean ignoreValidationError)
  {
    SbomFormat sbomFormat = SbomFormat.forMimeType(sbomResult.mimeType);
    try {
      sbomResult.isSbom = true;

      Bom bom;
      try {
        bom = ThirdPartyUtils.parseAndValidateCycloneDx(fileContent, Objects.requireNonNull(sbomFormat));
        sbomResult.isValid = true;
      }
      catch (SbomValidationException e) {
        log.debug("Error validating CycloneDX SBOM, file name: {}, scan type: {}", sbomResult.filename, "SBOM", e);
        sbomResult.isValid = false;
        sbomResult.isValidationErrorIgnorable = true;

        bom = ThirdPartyUtils.parseCycloneDxWithNoValidation(fileContent, Objects.requireNonNull(sbomFormat));

        if (!shouldIgnoreValidationError(ignoreValidationError)) {
          sbomResult.errorMessage = "Not a valid CycloneDX SBOM file.";
          sbomResult.validationErrors = getErrors(e);
        }
      }

      populateCycloneDxResult(sbomResult, sbomFormat, bom);
    }
    catch (UnsupportedSbomException e) {
      log.debug("Error validating CycloneDX SBOM, file name: {}, scan type: {}",
          sbomResult.filename, "SBOM", e);
      sbomResult.isValid = false;
      sbomResult.isValidationErrorIgnorable = false;
      sbomResult.errorMessage = e.getMessage();
      sbomResult.validationErrors = getErrors(e);
    }
    catch (SbomProcessingException e) {
      log.debug("Error validating CycloneDX SBOM, file name: {}, scan type: {}", sbomResult.filename, "SBOM", e);
      sbomResult.isValid = false;
      sbomResult.isValidationErrorIgnorable = false;
      sbomResult.errorMessage = "Not a valid CycloneDX SBOM file.";
      sbomResult.validationErrors = getErrors(e);
      sbomResult.summary = new SbomSummary();
      sbomResult.summary.specification = SbomSpecification.CYCLONEDX.toString();
      sbomResult.summary.format = sbomFormat.toString();
    }
    return sbomResult;
  }

  private boolean shouldIgnoreValidationError(final boolean ignoreValidationError) {
    return ignoreValidationError || SystemConfigurationPropertyFeature.SKIP_SBOM_IMPORT_VALIDATION.isEnabled();
  }

  private List<String> getErrors(final Throwable t) {
    if (t == null) {
      return null;
    }
    List<ValidationException> validationExceptions = new ArrayList<>();
    populateErrors(new HashSet<>(), t, validationExceptions);
    removeDuplicates(validationExceptions);
    if (validationExceptions.isEmpty()) {
      return null;
    }
    return validationExceptions.stream().map(ValidationException::getMessage).toList();
  }

  private void populateErrors(
      final Set<Throwable> processed,
      final Throwable t,
      final List<ValidationException> validationExceptions)
  {
    if (!processed.add(t)) {
      return;
    }
    if (t instanceof ValidationException validationException) {
      validationExceptions.add(validationException);
    }
    else if (t instanceof JsonParseException jsonParseException) {
      validationExceptions.add(new ValidationException(jsonParseException));
    }
    else if (t instanceof SAXParseException saxParseException) {
      validationExceptions.add(new ValidationException(saxParseException));
    }
    else if (t instanceof ParseException ||
        t instanceof InvalidSPDXAnalysisException ||
        t instanceof JSONException
    ) {
      validationExceptions.add(new ValidationException(t));
    }
    for (Throwable child : t.getSuppressed()) {
      populateErrors(processed, child, validationExceptions);
    }
    if (t.getCause() != null) {
      populateErrors(processed, t.getCause(), validationExceptions);
    }
  }

  private void removeDuplicates(final List<ValidationException> validationExceptions) {
    for (int i = validationExceptions.size() - 1; i >= 0; i--) {
      ValidationException validationException1 = validationExceptions.get(i);
      for (int j = i - 1; j >= 0; j--) {
        ValidationException validationException2 = validationExceptions.get(j);
        int redundantIndex = getRedundantIndex(validationException1, validationException2);
        if (redundantIndex == 0) {
          validationExceptions.remove(i);
          break;
        }
        else if (redundantIndex == 1) {
          validationExceptions.remove(j);
          i--;
        }
      }
    }
  }

  private int getRedundantIndex(
      final ValidationException validationException,
      final ValidationException otherValidationException)
  {
    String errorMessage = validationException.getOriginalMessage();
    String otherErrorMessage = otherValidationException.getOriginalMessage();

    // The error messages are not similar enough for either to be considered redundant
    if (!errorMessage.contains(otherErrorMessage) && !otherErrorMessage.contains(errorMessage)) {
      return -1;
    }

    // The errors have different locations
    if (validationException.getLine() != null && otherValidationException.getLine() != null &&
        !Objects.equals(validationException.getLine(), otherValidationException.getLine())) {
      return -1;
    }
    if (validationException.getColumn() != null && otherValidationException.getColumn() != null &&
        !Objects.equals(validationException.getColumn(), otherValidationException.getColumn())) {
      return -1;
    }
    if (validationException.getPath() != null && otherValidationException.getPath() != null &&
        !Objects.equals(validationException.getPath(), otherValidationException.getPath())) {
      return -1;
    }

    // Which error has less information
    return countParts(validationException) < countParts(otherValidationException) ? 0 : 1;
  }

  private int countParts(final ValidationException validationException) {
    int count = 0;
    if (validationException.getLine() != null) {
      count++;
    }
    if (validationException.getColumn() != null) {
      count++;
    }
    if (validationException.getPath() != null) {
      count++;
    }
    if (validationException.getOriginalMessage() != null) {
      count++;
    }
    return count;
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
    sbomResult.summary.vulnerabilityCount = calculateNumberOfVulnerabilities(bom);
    sbomResult.summary.applicationName = SbomCycloneDxUtils.getApplicationNameSafely(bom);
    sbomResult.summary.applicationVersion = SbomCycloneDxUtils.getApplicationVersionSafely(bom);
    sbomResult.summary.serialNumber = SbomCycloneDxUtils.getOrGenerateSerialNumber(bom);
    sbomResult.summary.creationDetails = SbomCycloneDxUtils.getSbomCreationDetailsJson(bom);
  }

  private boolean isPlainTextValidJson(final String sbomContent) {
    try {
      return objectMapper.readTree(sbomContent) != null;
    }
    catch (IOException e) {
      log.debug("File content is not valid a JSON document");
    }
    return false;
  }

  private static int calculateNumberOfVulnerabilities(Bom bom) {
    // Count the number of vulnerabilities extensions. This was added to CDX older versions up to 1.3.
    // Starting with 1.4, the current vulnerabilities attribute was added to the spec.
    // For the calculation we add both counts, because they can't be in the SBOM at the same time.
    int vulnerabilitiesExtensionsCount = 0;
    if (bom.getComponents() != null) {
      vulnerabilitiesExtensionsCount = bom.getComponents().stream()
          .map(Component::getExtensions)
          .filter(MapUtils::isNotEmpty)
          .map(m -> m.get(SbomCycloneDxUtils.VULNERABILITY_KEY))
          .filter(Objects::nonNull)
          .mapToInt(extension -> extension.getExtensions().size())
          .sum();
    }
    return CollectionUtils.size(bom.getVulnerabilities()) + vulnerabilitiesExtensionsCount;
  }

  @VisibleForTesting
  boolean isPlainTextValidXml(final String sbomContent) {
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

  private String getSbomStringContent(final File sbomFile) throws IOException {
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
