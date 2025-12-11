/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.sbom.datastore;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.common.io.FileCleaner;
import com.sonatype.insight.brain.service.InsightWork;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * File-based implementation of the SbomPersistenceService using InsightWork.
 */
@Named
@Singleton
public class FileSbomPersistenceService
    extends SbomPersistenceService
{
  private static final Logger log = LoggerFactory.getLogger(FileSbomPersistenceService.class);

  private final InsightWork insightWork;

  private final FileCleaner fileCleaner;

  @Inject
  public FileSbomPersistenceService(final InsightWork insightWork, final FileCleaner fileCleaner) {
    this.insightWork = insightWork;
    this.fileCleaner = fileCleaner;
  }

  @Override
  public SbomEntity doGetSbom(final String appId, final String fileName) {
    Path sbomDir = insightWork.getSbomDir(appId).toPath();
    Path sbomPath = sbomDir.resolve(fileName);
    return new FileSbomEntity(sbomPath, appId, fileName);
  }

  @Override
  public SbomEntity getTemporarySbom(final String fileName, final String prefix) {
    Path sbomDir = insightWork.getSbomPersistentTempDir().toPath();
    Path prefixPath = prefix != null ? sbomDir.resolve(prefix) : sbomDir;
    Path sbomPath = prefixPath.resolve(fileName);
    return new FileSbomEntity(sbomPath, null, fileName);
  }

  @Override
  public SbomEntity getTransientSbom(String fileName) throws IOException {
    Path sbomDir = insightWork.getSbomTransientDir().toPath();

    String extension = FilenameUtils.getExtension(fileName);
    Path sbomPath = Files.createTempFile(sbomDir, null, extension == null ? null : "." + extension);
    return new FileSbomEntity(sbomPath, null, sbomPath.getFileName().toString());
  }

  @Override
  public SbomEntity saveTemporarySbom(
      final SbomEntity sbomEntity,
      final String fileName,
      final String prefix) throws IOException
  {
    Path sbomDir = insightWork.getSbomPersistentTempDir().toPath();
    Path prefixPath = prefix != null ? sbomDir.resolve(prefix) : sbomDir;
    Files.createDirectories(prefixPath);

    Path persistentTempSbomFilePath = prefixPath.resolve(fileName);

    // For efficiency, try to make a hard link first, and fall back to copying if that fails.
    // Hard links are generally only supported on UNIX (Windows can also do them but requires admin privileges)
    boolean hardLinked = false;
    try {
      Files.createLink(persistentTempSbomFilePath, sbomEntity.getPath().toAbsolutePath());
      hardLinked = true;
    }
    catch (Exception e) {
      log.trace("Failed to create hard link from {} to {}", sbomEntity.getLocation(), persistentTempSbomFilePath, e);
      // fall through to copy
    }

    if (!hardLinked) {
      Files.copy(sbomEntity.getPath().toAbsolutePath(), persistentTempSbomFilePath);
    }

    log.debug("Copied SBOM file from {} to {}", sbomEntity.getLocation(), persistentTempSbomFilePath);
    return new FileSbomEntity(persistentTempSbomFilePath, sbomEntity.getAppId(), fileName);
  }

  @Override
  public void deleteSbom(final SbomEntity sbomEntity) throws IOException {
    boolean deleted = Files.deleteIfExists(sbomEntity.getPath());
    if (deleted) {
      // Delete the parent directory if it is empty
      Path parent = sbomEntity.getPath().getParent();
      if (parent != null && Files.isDirectory(parent)) {
        try (Stream<Path> entries = Files.list(parent)) {
          if (entries.findAny().isEmpty()) {
            Files.delete(parent);
          }
        }
      }
    }
  }

  @Override
  public void deleteSbom(final String appId, final String fileName) throws IOException {
    Files.deleteIfExists(insightWork.getSbomDir(appId).toPath().resolve(fileName));
  }

  @Override
  public void deleteSbomsFor(final String appId) throws IOException {
    fileCleaner.delete(insightWork.getSbomDir(appId, false));
  }

  @Override
  public void deleteTransientSbomsOlderThan(final Instant instant) throws IOException {
    Path transientDir = insightWork.getSbomTransientDir().toPath();

    try (Stream<Path> paths = Files.walk(transientDir)) {
      paths
          .filter(Files::isRegularFile)
          .filter(path -> isOlderThan(path, instant))
          .forEach(path -> {
            try {
              Files.deleteIfExists(path);
            }
            catch (IOException e) {
              log.error("Failed to delete file {}.", path, e);
            }
          });
    }
  }

  @Override
  public void moveSbomEntity(final SbomEntity from, final SbomEntity to) throws IOException {
    FileSbomEntity fromFile = (FileSbomEntity) from;
    FileSbomEntity toFile = (FileSbomEntity) to;
    Files.createDirectories(toFile.getPath().getParent());
    Files.move(fromFile.path(), toFile.path(), StandardCopyOption.REPLACE_EXISTING);
  }

  private boolean isOlderThan(final Path path, final Instant instant) {
    try {
      FileTime lastModifiedTime = Files.getLastModifiedTime(path);
      return lastModifiedTime.toInstant().compareTo(instant) <= 0;
    }
    catch (IOException e) {
      log.warn("Could not get last modified time for file {}.", path, e);
      return false;
    }
  }
}
