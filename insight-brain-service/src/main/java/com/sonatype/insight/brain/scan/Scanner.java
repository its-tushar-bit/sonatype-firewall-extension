/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scan;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.ProprietaryConfig;
import com.sonatype.insight.brain.common.io.FileCleaner;
import com.sonatype.insight.brain.common.io.FileCleaner.FileDeletionException;
import com.sonatype.insight.brain.features.FeaturesService;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.license.model.Feature;
import com.sonatype.insight.scan.client.ClientScanRequest;
import com.sonatype.insight.scan.client.ClientScanner;
import com.sonatype.insight.scan.config.ScanPropertiesLoader;
import com.sonatype.insight.scan.file.FileScanRequest;
import com.sonatype.insight.scan.file.FileScanner;
import com.sonatype.insight.scan.file.ScanSession;
import com.sonatype.insight.scan.file.ThirdPartyUtils.SbomFormat;
import com.sonatype.insight.scan.model.ItemContentType;
import com.sonatype.insight.scan.model.Scan;
import com.sonatype.insight.scan.model.ScanConfiguration;
import com.sonatype.insight.scan.model.ScanItem;
import com.sonatype.insight.scan.model.ScanMetadata;
import com.sonatype.insight.scan.model.io.ScanWriter;
import com.sonatype.insight.scan.model.io.ScanWriterFactory;
import com.sonatype.insight.scan.util.HashUtils;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.clm.dto.model.ProprietaryConfig.PACKAGE_DELIM;
import static com.sonatype.clm.dto.model.ProprietaryConfig.REGEX_DELIM;

/**
 * Scans application bundles.
 *
 * @since 1.8
 */
@Named
public class Scanner
{
  private static final Logger log = LoggerFactory.getLogger(Scanner.class);

  private static final String TEMP_SCAN_PREFIX = "temp-";

  private static final String SCAN_SUFFIX = ".xml.gz";

  private static final String COULD_NOT_DELETE_SCAN_FILE = "Could not delete scan file: {}";

  private final ScanPropertiesLoader configLoader;

  private final ClientScanner clientScanner;

  private final FileScanner fileScanner;

  private final ScanWriterFactory writerFactory;

  private final FileCleaner fileCleaner;

  private final FeaturesService featuresService;

  @Inject
  public Scanner(
      ScanPropertiesLoader configLoader,
      ClientScanner clientScanner,
      FileScanner fileScanner,
      ScanWriterFactory writerFactory,
      FileCleaner fileCleaner,
      FeaturesService featuresService)
  {
    this.configLoader = configLoader;
    this.clientScanner = clientScanner;
    this.fileScanner = fileScanner;
    this.writerFactory = writerFactory;
    this.fileCleaner = fileCleaner;
    this.featuresService = featuresService;
  }

  /**
   * Scans the specified target file and returns the resulting scan file, using the given directory as parent.
   * 
   * @param scanTarget The binary to be scanned
   * @param filename The name of the binary to be scanned. This is not necessarily the same as the target's file name.
   *          When the binary to be scanned is uploaded via the UI, it is saved in a temporary file (this is the target
   *          that will be scanned). The filename parameter holds the name of the file that was uploaded via the UI.
   * @param scanDir The directory where to store the scan file.
   */
  public ScanResult scan(File scanTarget, String filename, File scanDir, ProprietaryConfig proprietaryConfig)
      throws IOException
  {
    return scan(Collections.singletonList(scanTarget), filename, scanDir, proprietaryConfig,
        null /* scanConfiguration */, null /* scanMetadata */);
  }

  /**
   * @param scanMetadata Allows the caller to provide the commit hash that should be associated with the scan, if it's
   *          available (as should be the case for source control scanning, for example)
   */
  public ScanResult scan(
      List<File> scanTargets,
      File scanDir,
      ProprietaryConfig proprietaryConfig,
      ScanConfiguration inputScanConfiguration,
      ScanMetadata scanMetadata)
      throws IOException
  {
    return scan(scanTargets, null /* filename */, scanDir, proprietaryConfig, inputScanConfiguration, scanMetadata);
  }

  private ScanResult scan(
      List<File> scanTargets,
      String filename,
      File scanDir,
      ProprietaryConfig proprietaryConfig,
      ScanConfiguration inputScanConfiguration,
      ScanMetadata scanMetadata) throws IOException
  {
    Files.createDirectories(scanDir.toPath());
    File scanFile = Files.createTempFile(scanDir.toPath(), TEMP_SCAN_PREFIX, SCAN_SUFFIX).toFile();
    log.debug("Saving scan of {} to {}", scanTargets, scanFile);
    ScanResult scanResult = new ScanResult();
    scanResult.setScanFile(scanFile);
    try {
      Scan scan = new Scan();

      ScanConfiguration scanConfiguration = buildScanConfiguration(proprietaryConfig, inputScanConfiguration);
      scan.setConfiguration(scanConfiguration);
      scan.setMetadata(scanMetadata);

      try (ScanWriter writer = writerFactory.newWriter(scanFile)) {
        writer.openScan(scan);
        writer.writeConfiguration(scan.getConfiguration());
        writer.writeMetadata(scanMetadata);
        scan.getSummary().setStartTime();
        ScanSession scanSession = new ScanSession(scan, writer);
        Set<Feature> features = featuresService.getFeatures();
        scanSession.setLicensedFeatures(features.stream().map(Feature::getId).collect(Collectors.toSet()));
        clientScanner.scan(new ClientScanRequest(scan));
        if (filename != null && scanTargets.size() == 1) {
          fileScanner.scan(new FileScanRequest(scanSession).addFile(scanTargets.get(0), filename, null));
        }
        else {
          fileScanner.scan(new FileScanRequest(scanSession).addFiles(scanTargets));
        }
        scan.getSummary().setEndTime();
        writer.writeSummary(scan.getSummary());
        writer.closeScan();
        scanResult.setHasThirdPartyScanContent(scan.hasThirdPartyScanContent());
      }
    }
    catch (RuntimeException | IOException e) {
      try {
        fileCleaner.delete(scanFile);
      }
      catch (FileDeletionException fde) {
        log.error(COULD_NOT_DELETE_SCAN_FILE, scanFile, fde);
      }
      throw e;
    }
    return scanResult;
  }

  public ScanResult scanContent(
      String content,
      File scanDir,
      ItemContentType contentType,
      String source,
      SbomFormat format,
      ProprietaryConfig proprietaryConfig,
      String scannerDriver) throws IOException
  {
    Files.createDirectories(scanDir.toPath());
    File scanFile = Files.createTempFile(scanDir.toPath(), TEMP_SCAN_PREFIX, SCAN_SUFFIX).toFile();
    log.debug("Adding Sbom file to {}", scanFile);
    ScanResult scanResult = new ScanResult();
    scanResult.setScanFile(scanFile);
    try {
      Scan scan = new Scan();
      scan.setHasThirdPartyScanContent(true);
      scan.setConfiguration(buildScanConfiguration(proprietaryConfig, null));
      try (ScanWriter writer = writerFactory.newWriter(scanFile)) {
        writer.openScan(scan);
        writer.writeConfiguration(scan.getConfiguration());
        scan.getSummary().setScannerDriver(scannerDriver);
        scan.getSummary().putClientInfo("insight.scannerDriver", scannerDriver);
        scan.getSummary().setStartTime();

        ScanItem scanItem = new ScanItem();
        scanItem.setContentType(contentType);
        if (contentType == ItemContentType.SPDX) {
          scanItem.setPath(String.format("%s.spdx.%s", source, format.name().toLowerCase(Locale.ROOT)));
        }
        else {
          scanItem.setPath(String.format("%s-bom.%s", source, format.name().toLowerCase(Locale.ROOT)));
        }
        scanItem.setContent(content);
        scanItem.setLastModified(new Date().getTime());
        scanItem.setSha1(getHashForContent(content));

        writer.writeScanItem(scanItem);

        scan.getSummary().setEndTime();
        writer.writeSummary(scan.getSummary());
        writer.closeScan();
        scanResult.setHasThirdPartyScanContent(true);
      }
    }
    catch (RuntimeException | IOException e) {
      try {
        fileCleaner.delete(scanFile);
      }
      catch (FileDeletionException fde) {
        log.error(COULD_NOT_DELETE_SCAN_FILE, scanFile, fde);
      }
      throw e;
    }
    return scanResult;
  }

  private String getHashForContent(String content) {
    String sha1 = HashUtils.hash(content, "SHA-1");
    return sha1.substring(0, Math.min(sha1.length(), 20));
  }

  private ScanConfiguration buildScanConfiguration(
      ProprietaryConfig proprietaryConfig,
      ScanConfiguration inputScanConfiguration) throws IOException
  {
    Properties props = new Properties();
    props.setProperty("fileIncludes", "");
    props.setProperty("fileExcludes", "");
    props.setProperty("ipAddresses", "false");
    props.setProperty("hashJavaTypes", "true");

    if (proprietaryConfig != null) {
      props.put("proprietaryPackages", StringUtils.join(proprietaryConfig.getPackages().iterator(), PACKAGE_DELIM));
      props.put("proprietaryRegexes", StringUtils.join(proprietaryConfig.getRegexes().iterator(), REGEX_DELIM));
    }
    if (SystemConfigurationPropertyFeature.BUILT_FROM_SOURCE.isEnabled()) {
      props.put("includeSha256", "true");
    }
    configLoader.loadDefaults(props, null);
    configLoader.resolveAliases(props);
    
    if (inputScanConfiguration != null) {
      props.putAll(inputScanConfiguration.getProperties());
    }

    return new ScanConfiguration(props);
  }
}
