/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;

import com.sonatype.clm.dto.model.ProprietaryConfig;
import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.proprietary.ProprietaryConfigService;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.scan.archive.RegexSelector;
import com.sonatype.insight.scan.archive.Selector.Selection;
import com.sonatype.insight.scan.archive.TFileUtils;
import com.sonatype.insight.scan.model.ClientScanType;
import com.sonatype.insight.scan.model.DirectoryScanItem;
import com.sonatype.insight.scan.model.Scan;
import com.sonatype.insight.scan.model.ScanItem;
import com.sonatype.insight.scan.model.io.ScanReader;
import com.sonatype.insight.scan.model.io.ScanWriter;
import com.sonatype.insight.scan.model.io.ScanWriterFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import de.schlichtherle.truezip.file.TArchiveDetector;
import de.schlichtherle.truezip.file.TFile;
import org.apache.commons.io.IOUtils;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.24
 */
@Named
public class ScanHandler
{
  private static final Logger log = LoggerFactory.getLogger(ScanHandler.class);

  public static final String TWISTLOCK_SCAN_FILENAME_SUFFIX = "-twistlock";

  private final InsightWork work;

  private final ScanUploader scanUploader;

  private final ScanReader scanReader;

  private final ScanWriterFactory scanWriterFactory;

  private final ProprietaryConfigService proprietaryConfigService;

  private final ApplicationDAO appDAO;

  @Inject
  public ScanHandler(InsightWork work,
                     ScanUploader scanUploader,
                     ScanReader scanReader,
                     ScanWriterFactory scanWriterFactory,
                     ProprietaryConfigService proprietaryConfigService,
                     ApplicationDAO appDAO)
  {
    this.work = work;
    this.scanUploader = scanUploader;
    this.scanReader = scanReader;
    this.scanWriterFactory = scanWriterFactory;
    this.proprietaryConfigService = proprietaryConfigService;
    this.appDAO = appDAO;
  }

  @Authorize(permission = Permission.EVALUATE_APPLICATION, anonymousAllowed = true)
  ScanReceipt handle(HttpServletRequest httpRequest,
                     @AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) String applicationPublicId,
                     ClientScanType clientScanType)
      throws IOException
  {
    long start = System.currentTimeMillis();
    log.debug("Received {} scan for application public id {}.", clientScanType, applicationPublicId);

    Application app = appDAO.getByPublicIdNotNull(applicationPublicId);

    File tempScanFile = saveScanFromHttpRequest(httpRequest, app, clientScanType);

    File tempTwistlockScanFile = null;
    if (ClientScanType.TWISTLOCK.equals(clientScanType)) {
      tempTwistlockScanFile = tempScanFile;

      ProprietaryConfig proprietaryConfig = proprietaryConfigService.getProprietaryConfig(OwnerType.APPLICATION,
          app.getPublicId());
      tempScanFile = convertTwistlockScan(tempTwistlockScanFile, proprietaryConfig);
    }

    ScanReceipt scanReceipt = scanUploader.upload(httpRequest, tempScanFile, app);
    if (ClientScanType.EXPANDED_COVERAGE.equals(clientScanType)) {
      Files.delete(tempScanFile.toPath());
    }
    else {
      File scanFile = work.getScanFile(app.getId(), scanReceipt.getScanId());
      FileUtils.rename(tempScanFile, scanFile);
      if (tempTwistlockScanFile != null) {
        FileUtils.rename(tempTwistlockScanFile, toTwistlockScanFilename(scanFile));
      }
    }

    log.debug("Handled {} scan id {} for application public id {} in {} ms.", clientScanType, scanReceipt.getScanId(),
        applicationPublicId, System.currentTimeMillis() - start);

    return scanReceipt;
  }

  private File convertTwistlockScan(File twistlockScanFile, ProprietaryConfig proprietaryConfig) throws IOException {
    long start = System.currentTimeMillis();

    File scanFile = FileUtils.createTempFile("temp-", ".xml.gz", twistlockScanFile.getParentFile());

    TwistlockScan twistlockScan = new TwistlockScan(twistlockScanFile);
    Scan scan = scanReader.read(IOUtils.toInputStream(twistlockScan.getScanXml(), "UTF-8"));
    try (ScanWriter writer = scanWriterFactory.newWriter(scanFile)) {
      writer.openScan(scan);
      writer.writeConfiguration(scan.getConfiguration());

      DirectoryScanItem directoryScanItem = new DirectoryScanItem();
      File dockerImageFakeFile = new File("DockerImage");
      directoryScanItem.setPath("DockerImage");
      writer.openDirectoryScanItem(directoryScanItem, dockerImageFakeFile);
      
      RegexSelector proprietaryRegexSelector = RegexSelector.forProprietaryRegexes(proprietaryConfig.getRegexes());

      TArchiveDetector archiveDetector = TFileUtils
          .getArchiveDetector(Collections.emptyMap(), null /* badExtensions */);
      ArrayNode scannedFiles = JsonUtils.parse(twistlockScan.getFilesJson());
      for (JsonNode scannedFile : scannedFiles) {
        String hash = scannedFile.get("sha1").asText();
        hash = hash.substring(0, 20);
        String path = scannedFile.get("path").asText();
  
        TFile tFile = new TFile(path, archiveDetector);
        ScanItem scanItem = tFile.isArchive() ? new DirectoryScanItem() : new ScanItem();
        scanItem.setSha1(hash);
        scanItem.setPath(path);
        boolean isProprietary = proprietaryRegexSelector.isSelected(path) == Selection.EXCLUDED;
        if (isProprietary) {
          scanItem.setProprietary(true);
        }

        writer.writeScanItem(scanItem);
      }
      writer.closeDirectoryScanItem(dockerImageFakeFile);
      writer.writeSummary(scan.getSummary());
      writer.closeScan();
    }

    log.debug("Converted {} scan in {} ms.", ClientScanType.TWISTLOCK, System.currentTimeMillis() - start);
    return scanFile;
  }

  private File toTwistlockScanFilename(File scanFile) {
    File path = scanFile.getParentFile();
    String twistlockFilename = scanFile.getName().replaceFirst("\\.", TWISTLOCK_SCAN_FILENAME_SUFFIX + ".");
    return new File(path, twistlockFilename);
  }

  /**
   * Saves the scan from the input stream in the given HTTP request to a temporary file in the location for scans for
   * the specified application.
   */
  private File saveScanFromHttpRequest(HttpServletRequest httpRequest, Application app, ClientScanType clientScanType)
      throws IOException
  {
    File scanDir = work.getScanDir(app.getId());
    scanDir.mkdirs();

    File scanFile = FileUtils.createTempFile("temp-",
        ClientScanType.TWISTLOCK.equals(clientScanType) ? ".zip" : ".xml.gz", scanDir);

    try (ServletInputStream is = httpRequest.getInputStream(); FileOutputStream os = new FileOutputStream(scanFile)) {
      IOUtil.copy(is, os);
    }

    return scanFile;
  }
}
