/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.scan.cli;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.scan.client.ClientScanRequest;
import com.sonatype.insight.scan.client.ClientScanner;
import com.sonatype.insight.scan.config.ScanPropertiesLoader;
import com.sonatype.insight.scan.file.FileScanRequest;
import com.sonatype.insight.scan.file.FileScanner;
import com.sonatype.insight.scan.file.ScanSession;
import com.sonatype.insight.scan.model.Scan;
import com.sonatype.insight.scan.model.ScanConfiguration;
import com.sonatype.insight.scan.model.io.ScanWriter;
import com.sonatype.insight.scan.model.io.ScanWriterFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class Scanner
{
  private static final Logger log = LoggerFactory.getLogger(Scanner.class);

  private final ScanPropertiesLoader configLoader;

  private final ClientScanner clientScanner;

  private final FileScanner fileScanner;

  private final ScanWriterFactory writerFactory;

  @Inject
  public Scanner(ScanPropertiesLoader configLoader,
                 ClientScanner clientScanner,
                 FileScanner fileScanner,
                 ScanWriterFactory writerFactory)
  {
    this.configLoader = configLoader;
    this.clientScanner = clientScanner;
    this.fileScanner = fileScanner;
    this.writerFactory = writerFactory;
  }

  public void scan(File scanFile, List<File> targets, Properties config) throws IOException {
    log.info("Starting scan...");

    Scan scan = new Scan();
    scan.setConfiguration(new ScanConfiguration(getScanConfigProps(config)));
    try (ScanWriter writer = writerFactory.newWriter(scanFile)) {
      writer.openScan(scan);
      writer.writeConfiguration(scan.getConfiguration());
      scan.getSummary().setStartTime();
      ScanSession scanSession = new ScanSession(scan, writer);
      clientScanner.scan(new ClientScanRequest(scan));
      fileScanner.scan(new FileScanRequest(scanSession, targets));
      scan.getSummary().setEndTime();
      writer.writeSummary(scan.getSummary());
      writer.closeScan();
      writer.close();
    }
    log.info("Fingerprinting completed in {} seconds for {} archives, {} total files",
        scan.getSummary().getElapsedSeconds(), scan.getSummary().getArchives(), scan.getSummary().getFiles());
  }

  private Properties getScanConfigProps(Properties properties) throws IOException {
    Properties props = new Properties();
    props.putAll(properties);
    configLoader.loadDefaults(props, "configuration.properties");
    configLoader.resolveAliases(props);
    return props;
  }
}
