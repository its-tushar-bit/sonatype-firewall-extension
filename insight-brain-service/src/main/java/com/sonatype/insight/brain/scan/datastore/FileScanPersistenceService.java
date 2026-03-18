/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.scan.datastore;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.common.io.FileCleaner;
import com.sonatype.insight.brain.common.io.FileCleaner.FileDeletionException;
import com.sonatype.insight.brain.service.InsightWork;

import org.codehaus.plexus.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class FileScanPersistenceService
    extends ScanPersistenceService
{
  private static final Logger log = LoggerFactory.getLogger(FileScanPersistenceService.class);

  private final InsightWork work;

  private final FileCleaner fileCleaner;

  @Inject
  public FileScanPersistenceService(final InsightWork work, final FileCleaner fileCleaner) {
    this.work = work;
    this.fileCleaner = fileCleaner;
  }

  @Override
  public ScanEntity doGetScan(final String appId, final String scanId) {
    return new FileScanEntity(getFile(appId, scanFileName(scanId)).toPath(), appId);
  }

  private File getFile(final String appId, final String name) {
    return new File(work.getScanDir(appId), name);
  }

  private static String scanFileName(final String scanId) {
    return "scan-" + scanId + ".xml.gz";
  }

  @Override
  public ScanEntity createTempScan(final String appId) throws IOException {
    File scanDir = work.getScanDir(appId);
    Files.createDirectories(scanDir.toPath());
    return new FileScanEntity(FileUtils.createTempFile("temp-", ".xml.gz", scanDir).toPath(), appId);
  }

  @Override
  public void moveTempScan(
      final ScanEntity tempScanEntity,
      final String appId,
      final String scanId) throws IOException
  {
    Path sourcePath = ((FileScanEntity) tempScanEntity).path();
    Path destinationPath = getFile(appId, scanFileName(scanId)).toPath();
    Files.createDirectories(destinationPath.getParent());
    Files.move(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
  }

  @Override
  public ScanEntity getScanByName(final String appId, final String name) {
    return new FileScanEntity(getFile(appId, name).toPath(), appId);
  }

  @Override
  public void copyScanFile(final ScanEntity source, final ScanEntity destination) throws IOException {
    Path sourcePath = ((FileScanEntity) source).path();
    Path destinationPath = ((FileScanEntity) destination).path();
    Files.createDirectories(destinationPath.getParent());
    Files.copy(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
  }

  @Override
  public void deleteScansFor(final String appId) throws FileDeletionException {
    fileCleaner.delete(work.getScanDir(appId));
  }

  @Override
  public Stream<ScanEntity> allScanFilesFor(final String appId) {
    File scanDir = work.getScanDir(appId);
    if (scanDir == null || !scanDir.exists() || !scanDir.isDirectory()) {
      log.info("There is no scan directory for application with ID {}.", appId);
      return Stream.empty();
    }
    // Don't use Files.list() as that may result in a lot of file handlers being used and not released.
    // See https://stackoverflow.com/questions/36990053/
    // resource-leak-in-files-listpath-dir-when-stream-is-not-explicitly-closed
    File[] scanFiles = scanDir.listFiles();
    if (scanFiles == null || scanFiles.length == 0) {
      log.info("There are no scan files for application with ID {}.", appId);
      return Stream.empty();
    }
    return Stream.of(scanFiles).map(file -> new FileScanEntity(file.toPath(), appId));
  }

  @Override
  public void deleteScan(final String appId, final String scanId) throws IOException {
    Files.deleteIfExists(getFile(appId, scanFileName(scanId)).toPath());
  }

  @Override
  public Class<? extends ScanEntity> getScanEntityClass() {
    return FileScanEntity.class;
  }
}
