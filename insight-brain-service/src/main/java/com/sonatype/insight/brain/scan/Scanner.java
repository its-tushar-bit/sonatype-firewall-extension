/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scan;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.ProprietaryConfig;
import com.sonatype.insight.brain.dataaccess.ProprietaryConfigDAO;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.scan.client.ClientScanRequest;
import com.sonatype.insight.scan.client.ClientScanner;
import com.sonatype.insight.scan.config.ScanPropertiesLoader;
import com.sonatype.insight.scan.file.FileScanRequest;
import com.sonatype.insight.scan.file.FileScanner;
import com.sonatype.insight.scan.model.Scan;
import com.sonatype.insight.scan.model.ScanConfiguration;
import com.sonatype.insight.scan.model.io.ScanWriter;
import com.sonatype.insight.scan.model.io.ScanWriterFactory;

import org.codehaus.plexus.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Scans application bundles.
 * 
 * @since 1.8
 */
@Named
class Scanner
{
  private static final Logger log = LoggerFactory.getLogger(Scanner.class);

  private final ScanPropertiesLoader configLoader;

  private final ClientScanner clientScanner;

  private final FileScanner fileScanner;

  private final ScanWriterFactory writerFactory;

  private final ProprietaryConfigDAO proprietaryConfigDAO;

  @Inject
  public Scanner(ScanPropertiesLoader configLoader, ClientScanner clientScanner, FileScanner fileScanner,
      ScanWriterFactory writerFactory, InsightWork work)
  {
    this.configLoader = configLoader;
    this.clientScanner = clientScanner;
    this.fileScanner = fileScanner;
    this.writerFactory = writerFactory;
    proprietaryConfigDAO = new ProprietaryConfigDAO(work.getDataDir());
  }

  /**
   * Scans the specified target file and returns the resulting scan file.
   */
  public File scan(File target) throws IOException {
    File scanFile = File.createTempFile("clm-scan-", ".xml.gz");
    log.debug("Saving scan of {} to {}", target, scanFile);

    try {
      Scan scan = new Scan();
      scan.setConfiguration(new ScanConfiguration(getScanConfigProps()));
      try (ScanWriter writer = writerFactory.newWriter(scanFile)) {
        writer.openScan(scan);
        writer.writeConfiguration(scan.getConfiguration());
        scan.getSummary().setStartTime();
        clientScanner.scan(new ClientScanRequest(scan));
        fileScanner.scan(new FileScanRequest(scan, null, Arrays.asList(target), writer));
        scan.getSummary().setEndTime();
        writer.writeSummary(scan.getSummary());
        writer.closeScan();
      }
    }
    catch (RuntimeException | IOException e) {
      scanFile.delete();
      throw e;
    }

    return scanFile;
  }

  private Properties getScanConfigProps() throws IOException {
    Properties props = new Properties();
    props.setProperty("fileIncludes", "");
    props.setProperty("fileExcludes", "");
    props.setProperty("ipAddresses", "false");
    props.setProperty("hashJavaTypes", "true");
    ProprietaryConfig proprietaryConfig = proprietaryConfigDAO.get();
    if (proprietaryConfig != null) {
      props.put("proprietaryPackages", StringUtils.join(proprietaryConfig.getPackages().iterator(), ","));
    }
    configLoader.loadDefaults(props, null);
    configLoader.resolveAliases(props);
    return props;
  }
}
