/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scan;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Properties;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.ProprietaryConfig;
import com.sonatype.insight.brain.common.io.FileCleaner;
import com.sonatype.insight.brain.common.io.FileCleaner.FileDeletionException;
import com.sonatype.insight.scan.client.ClientScanRequest;
import com.sonatype.insight.scan.client.ClientScanner;
import com.sonatype.insight.scan.config.ScanPropertiesLoader;
import com.sonatype.insight.scan.file.FileScanRequest;
import com.sonatype.insight.scan.file.FileScanner;
import com.sonatype.insight.scan.file.ScanSession;
import com.sonatype.insight.scan.model.ItemContentType;
import com.sonatype.insight.scan.model.Scan;
import com.sonatype.insight.scan.model.ScanConfiguration;
import com.sonatype.insight.scan.model.ScanItem;
import com.sonatype.insight.scan.model.io.ScanWriter;
import com.sonatype.insight.scan.model.io.ScanWriterFactory;
import com.sonatype.insight.scan.util.HashUtils;

import org.codehaus.plexus.util.StringUtils;
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

  private final ScanPropertiesLoader configLoader;

  private final ClientScanner clientScanner;

  private final FileScanner fileScanner;

  private final ScanWriterFactory writerFactory;

  private final FileCleaner fileCleaner;

  @Inject
  public Scanner(ScanPropertiesLoader configLoader,
                 ClientScanner clientScanner,
                 FileScanner fileScanner,
                 ScanWriterFactory writerFactory,
                 FileCleaner fileCleaner)
  {
    this.configLoader = configLoader;
    this.clientScanner = clientScanner;
    this.fileScanner = fileScanner;
    this.writerFactory = writerFactory;
    this.fileCleaner = fileCleaner;
  }

  /**
   * Scans the specified target file and returns the resulting scan file, using the given directory as parent.
   */
  public ScanResult scan(File target, String filename, File scanDir, ProprietaryConfig proprietaryConfig)
      throws IOException
  {
    scanDir.mkdirs();
    File scanFile = File.createTempFile("temp-", ".xml.gz", scanDir);
    log.debug("Saving scan of {} to {}", target, scanFile);
    ScanResult scanResult = new ScanResult();
    scanResult.setScanFile(scanFile);
    try {
      Scan scan = new Scan();
      scan.setConfiguration(new ScanConfiguration(getScanConfigProps(proprietaryConfig)));
      try (ScanWriter writer = writerFactory.newWriter(scanFile)) {
        writer.openScan(scan);
        writer.writeConfiguration(scan.getConfiguration());
        scan.getSummary().setStartTime();
        ScanSession scanSession = new ScanSession(scan, writer);
        clientScanner.scan(new ClientScanRequest(scan));
        fileScanner.scan(new FileScanRequest(scanSession).addFile(target, filename, null));
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
        log.error("Could not delete scan file: {}", scanFile, fde);
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
      ProprietaryConfig proprietaryConfig) throws IOException
  {
    scanDir.mkdirs();
    File scanFile = File.createTempFile("temp-", ".xml.gz", scanDir);
    log.debug("Adding Sbom file to {}", scanFile);
    ScanResult scanResult = new ScanResult();
    scanResult.setScanFile(scanFile);
    try {
      Scan scan = new Scan();
      scan.setHasThirdPartyScanContent(true);
      scan.setConfiguration(new ScanConfiguration(getScanConfigProps(proprietaryConfig)));
      try (ScanWriter writer = writerFactory.newWriter(scanFile)) {
        writer.openScan(scan);
        writer.writeConfiguration(scan.getConfiguration());
        scan.getSummary().setStartTime();

        ScanItem scanItem = new ScanItem();
        scanItem.setContentType(contentType);
        scanItem.setPath(String.format("%s-bom.xml", source));
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
        log.error("Could not delete scan file: {}", scanFile, fde);
      }
      throw e;
    }
    return scanResult;
  }

  private String getHashForContent(String content) {
    String sha1 = HashUtils.hash(content, "SHA-1");
    return sha1.substring(0, Math.min(sha1.length(), 20));
  }

  private Properties getScanConfigProps(ProprietaryConfig proprietaryConfig) throws IOException {
    Properties props = new Properties();
    props.setProperty("fileIncludes", "");
    props.setProperty("fileExcludes", "");
    props.setProperty("ipAddresses", "false");
    props.setProperty("hashJavaTypes", "true");

    if (proprietaryConfig != null) {
      props.put("proprietaryPackages", StringUtils.join(proprietaryConfig.getPackages().iterator(), PACKAGE_DELIM));
      props.put("proprietaryRegexes", StringUtils.join(proprietaryConfig.getRegexes().iterator(), REGEX_DELIM));
    }
    configLoader.loadDefaults(props, null);
    configLoader.resolveAliases(props);
    return props;
  }
}
