/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.core.UriBuilder;

import com.sonatype.clm.dto.model.ProprietaryConfig;
import com.sonatype.insight.brain.api.v2.ApiSbomResource;
import com.sonatype.insight.brain.api.v2.dto.ApiThirdPartyScanTicketDTO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.proprietary.ProprietaryConfigService;
import com.sonatype.insight.brain.sbom.SbomSpecification;
import com.sonatype.insight.brain.scan.ScanResult;
import com.sonatype.insight.brain.scan.Scanner;
import com.sonatype.insight.scan.application.ScannerDriver;
import com.sonatype.insight.scan.file.SbomFormat;
import com.sonatype.insight.scan.model.ItemContentType;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.api.v2.service.ApiCycloneDxServiceV2.CWE_REGEX;

@Named
public class SbomMetadataUtils

{
  public static final String SBOM_IDENTIFICATION_SOURCE = "SBOM";

  private static final Logger log = LoggerFactory.getLogger(SbomMetadataUtils.class);

  private final ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO;

  private final ProductLicense productLicense;

  private final ProprietaryConfigService proprietaryConfigService;

  private final Scanner scanner;

  @Inject
  public SbomMetadataUtils(
      final ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO,
      final ProductLicense productLicense,
      final ProprietaryConfigService proprietaryConfigService,
      final Scanner scanner)
  {
    this.thirdPartySbomMetadataDAO = thirdPartySbomMetadataDAO;
    this.productLicense = productLicense;
    this.proprietaryConfigService = proprietaryConfigService;
    this.scanner = scanner;
  }

  public boolean hasMaxSbomLimitBeenReached() {
    long currentSbomFiles = thirdPartySbomMetadataDAO.getSbomCount();
    Integer licenseMaxSboms = productLicense.getMaxSboms();
    if (licenseMaxSboms != null && currentSbomFiles < licenseMaxSboms) {
      return false;
    }
    else {
      log.warn(
          "SBOM Manager has reached its licensed maximum of {} files. " +
              "Contact your account team to manage all your SBOMs.",
          licenseMaxSboms);
      return true;
    }
  }

  public boolean hasMaxActiveSbomLimitBeenReached() {
    long currentActiveSbomFiles = thirdPartySbomMetadataDAO.getActiveSbomCount();
    Integer licensedMaxSboms = productLicense.getMaxSboms();
    return licensedMaxSboms == null || currentActiveSbomFiles >= licensedMaxSboms;
  }

  public boolean hasSbomMetadata(final String scanId) {
    return thirdPartySbomMetadataDAO.hasSbomMetadata(scanId);
  }

  public ScanResult scanSbomInputStream(
      final Application app,
      final InputStream sbomInputStream,
      final SbomFormat sbomFormat,
      final ItemContentType itemContentType,
      final ScannerDriver scannerDriver)
  {
    try {
      String sbomContent = IOUtils.toString(sbomInputStream, StandardCharsets.UTF_8);
      return scanSbomContent(app, sbomContent, sbomFormat, itemContentType, scannerDriver);
    }
    catch (IOException e) {
      throw new UncheckedIOException("unable to read supplied sbom", e);
    }
  }

  public ScanResult scanBinaryFile(
      final Application app,
      final File binaryFile)
  {
    try {
      ProprietaryConfig proprietaryConfig =
          proprietaryConfigService.getProprietaryConfig(OwnerType.APPLICATION, app.getPublicId());
      return scanner.scan(binaryFile, binaryFile.getName(), app.getId(), proprietaryConfig);
    }
    catch (IOException e) {
      throw new UncheckedIOException("unable to read supplied file", e);
    }
  }

  public ScanResult scanSbomContent(
      final Application app,
      final String sbom,
      final SbomFormat sbomFormat,
      final ItemContentType itemContentType,
      ScannerDriver scannerDriver)
  {
    try {
      ProprietaryConfig proprietaryConfig =
          proprietaryConfigService.getProprietaryConfig(OwnerType.APPLICATION, app.getPublicId());
      return scanner.scanThirdPartyContent(sbom, app.getId(), itemContentType, SBOM_IDENTIFICATION_SOURCE, sbomFormat,
          proprietaryConfig, scannerDriver.getValue());
    }
    catch (IOException ex) {
      throw new UncheckedIOException("Error scanning sbom content", ex);
    }
  }

  public ApiThirdPartyScanTicketDTO createSbomImportTicket(final String applicationId) {
    ApiThirdPartyScanTicketDTO scanTicketDTO = new ApiThirdPartyScanTicketDTO();
    scanTicketDTO.requestId = UUID.randomUUID().toString().replace("-", "");
    scanTicketDTO.statusUrl = UriBuilder
        .fromResource(ApiSbomResource.class)
        .path(ApiSbomResource.SBOM_STATUS_PATH)
        .build(applicationId, scanTicketDTO.requestId)
        .toString();
    return scanTicketDTO;
  }

  public ItemContentType determineItemContentType(String sbomSpecification) {
    if (SbomSpecification.SPDX.toString().equals(sbomSpecification)) {
      return ItemContentType.SPDX;
    }
    else if (SbomSpecification.CYCLONEDX.toString().equals(sbomSpecification)) {
      return ItemContentType.SBOM;
    }
    else {
      return null;
    }
  }

  public static List<Integer> convertCwesStringToIntegerList(String cwesString) {
    return Arrays.stream(cwesString.split(","))
        .map((cwe) -> {
          Matcher cweMatcher = CWE_REGEX.matcher(cwe);
          if (cweMatcher.matches()) {
            return Integer.parseInt(cweMatcher.group(1));
          }
          else {
            return null;
          }
        })
        .filter(Objects::nonNull)
        .toList();
  }
}
