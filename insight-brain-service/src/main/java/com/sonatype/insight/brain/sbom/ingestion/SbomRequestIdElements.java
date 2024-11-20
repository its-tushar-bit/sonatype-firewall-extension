/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.ingestion;

import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.regex.Pattern;

import com.sonatype.insight.brain.thirdparty.SbomScanType;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.scan.file.SbomFormat;
import com.sonatype.insight.scan.model.ItemContentType;

import org.apache.commons.lang3.StringUtils;

public class SbomRequestIdElements
{
  private static final Pattern UUID_HEX_PATTERN = Pattern.compile("^\\p{XDigit}{32}$");

  private SbomScanType scanType;

  private String fileNameUUID;

  private String originalFileName;

  private SbomFormat sbomFormat;

  private ItemContentType contentType;

  private boolean isSbomValid;

  public SbomRequestIdElements(final String fileNameUUID, final String originalFileName) {
    this.scanType = SbomScanType.BINARY;
    this.fileNameUUID = fileNameUUID;
    this.originalFileName = originalFileName;
  }

  public SbomRequestIdElements(
      final String fileNameUUID,
      final String originalFileName,
      final SbomFormat sbomFormat,
      final ItemContentType contentType,
      final boolean isSbomValid)
  {
    this.scanType = SbomScanType.SBOM;
    this.fileNameUUID = fileNameUUID;
    this.originalFileName = originalFileName;
    this.sbomFormat = sbomFormat;
    this.contentType = contentType;
    this.isSbomValid = isSbomValid;
  }

  public String encodeRequestId() {
    String filenameToUseForRequestId;

    if (scanType == SbomScanType.BINARY) {
      filenameToUseForRequestId = String.format("%s-%s-%s", scanType.name(), fileNameUUID, originalFileName);
    }
    else {
      filenameToUseForRequestId = String.format("%s-%b-%s-%s-%s-%s",
          scanType.name(), isSbomValid, sbomFormat, contentType.name(), fileNameUUID, originalFileName);
    }

    return Base64.getEncoder().encodeToString(filenameToUseForRequestId.getBytes());
  }

  public static SbomRequestIdElements decodeFromRequestId(final String requestId) {
    if (StringUtils.isEmpty(requestId)) {
      return null;
    }

    String decodedRequestId;
    try {
      decodedRequestId = new String(Base64.getDecoder().decode(requestId));
    }
    catch (IllegalArgumentException ex) {
      throw new BadRequestException("The provided requestId " + requestId + " is not valid.");
    }

    String[] requestElements = decodedRequestId.split("-");
    List<String> elementsList = Arrays.asList(requestElements);

    if (requestElements[0].equals(SbomScanType.SBOM.name())) {
      String fileNameUUID = requestElements[4];
      String originalFileName = String.join("-", elementsList.subList(5, elementsList.size()));

      validateFilename(requestId, String.format("%s-%s", fileNameUUID, originalFileName));
      validateFileNameUUID(requestId, fileNameUUID);

      try {
        return new SbomRequestIdElements(fileNameUUID, originalFileName, SbomFormat.forString(requestElements[2]),
            ItemContentType.valueOf(requestElements[3]), Boolean.parseBoolean(requestElements[1]));
      }
      catch (IllegalArgumentException ex) {
        throw new BadRequestException("The provided requestId " + requestId + " is not valid.");
      }

    }
    else if (requestElements[0].equals(SbomScanType.BINARY.name())) {
      String fileNameUUID = requestElements[1];
      String originalFileName = String.join("-", elementsList.subList(2, elementsList.size()));

      validateFilename(requestId, originalFileName);
      validateFileNameUUID(requestId, fileNameUUID);

      return new SbomRequestIdElements(fileNameUUID, originalFileName);
    }
    else {
      throw new BadRequestException("The provided requestId " + requestId + " is not valid.");
    }
  }

  private static void validateFilename(final String requestId, final String originalFileName) {
    if (StringUtils.isBlank(originalFileName) || originalFileName.contains("/") || originalFileName.contains("\\")) {
      throw new BadRequestException("The provided requestId " + requestId + " is not valid.");
    }
  }

  private static void validateFileNameUUID(final String requestId, final String fileNameUUID) {
    if (StringUtils.isBlank(fileNameUUID) || !UUID_HEX_PATTERN.matcher(fileNameUUID).matches()) {
      throw new BadRequestException("The provided requestId " + requestId + " is not valid.");
    }
  }

  public SbomScanType getScanType() {
    return scanType;
  }

  public void setScanType(final SbomScanType scanType) {
    this.scanType = scanType;
  }

  public String getOriginalFileName() {
    return originalFileName;
  }

  public void setOriginalFileName(final String originalFileName) {
    this.originalFileName = originalFileName;
  }

  public String getFileNameUUID() {
    return fileNameUUID;
  }

  public void setFileNameUUID(final String fileNameUUID) {
    this.fileNameUUID = fileNameUUID;
  }

  public String getStoredFileName() {
    return String.format("%s-%s", fileNameUUID, originalFileName);
  }

  public SbomFormat getSbomFormat() {
    return sbomFormat;
  }

  public void setSbomFormat(final SbomFormat sbomFormat) {
    this.sbomFormat = sbomFormat;
  }

  public ItemContentType getContentType() {
    return contentType;
  }

  public void setContentType(final ItemContentType contentType) {
    this.contentType = contentType;
  }

  public boolean isSbomValid() {
    return isSbomValid;
  }

  public void setIsSbomValid(final boolean isSbomValid) {
    this.isSbomValid = isSbomValid;
  }
}
