/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.report;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.api.experimental.ApiVulnerabilitySignatureService;
import com.sonatype.insight.brain.common.io.FileCleaner;
import com.sonatype.insight.brain.report.ApplicationReport.ReportFile;
import com.sonatype.insight.brain.report.ApplicationReport.ReportFileLocationType;
import com.sonatype.insight.brain.report.pdf.PdfGenerator;
import com.sonatype.insight.brain.service.CopyStorageService;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.utils.AutoDeletingTempFile;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Backend for persisting file-based parts of an application report to the local filesystem
 */
@Named
@Singleton
public class FileApplicationReportPersistenceService
    extends ApplicationReportPersistenceService
{
  private static final Logger log = LoggerFactory.getLogger(FileApplicationReportPersistenceService.class);

  private static final String PDF_FILENAME = PdfGenerator.REPORT_FILE_NAME;

  private static final String VULNERABILITY_SIGNATURE_FILENAME =
      ApiVulnerabilitySignatureService.VULNERABILITY_SIGNATURE_JSON_FILENAME;

  private static final String ZIP_FILENAME = "report.zip";

  private static final String LOCAL_COPY_DIRECTORY_NAME = "report.cache";

  private static final String ADDITIONAL_FILES_DIRECTORY_NAME = "additional.files";

  /**
   * A ReportEntity derived from a file on the local filesystem
   */
  private static class FileReportEntity
      implements ReportEntity
  {
    /**
     * The directory from which the entity's "name" should be interpreted as a relative path. In practice, either the
     * cache directory, the additional files directory, or in the case of a PDF, the overall report directory.
     */
    protected final Path reportBaseDir;

    protected final Path entityPath;

    public FileReportEntity(Path reportBaseDir, Path entityPath) {
      this.reportBaseDir = reportBaseDir;
      this.entityPath = entityPath;

      validate();
    }

    @Override
    @WithSpan
    public boolean exists() {
      return Files.exists(entityPath);
    }

    @Override
    @WithSpan
    public long getTime() throws IOException {
      return Files.getLastModifiedTime(entityPath).toMillis();
    }

    @Override
    public String getName() {
      return reportBaseDir.relativize(entityPath).toString();
    }

    @Override
    @WithSpan
    public long length() throws IOException {
      return Files.size(entityPath);
    }

    @Override
    public Optional<Metadata> getMetadata(final MetadataAttribute... metadataAttributes) throws IOException {
      if (!exists()) {
        return Optional.empty();
      }
      if (metadataAttributes.length == 0) {
        return Optional.of(new Metadata(null, null));
      }
      String attributeNames = Set.of(metadataAttributes)
          .stream()
          .map(MetadataAttribute::getFileAttributeName)
          .collect(Collectors.joining(","));
      Map<String, Object> fileAttributes = Files.readAttributes(entityPath, attributeNames);
      return Optional.of(new Metadata(
          ((FileTime) fileAttributes.get(MetadataAttribute.LAST_MODIFIED_EPOCH_TIME.getFileAttributeName())).toMillis(),
          (Long) fileAttributes.get(MetadataAttribute.SIZE_IN_BYTES.getFileAttributeName())));
    }

    @Override
    @WithSpan
    public OutputStream getOutputStream() throws IOException {
      return Files.newOutputStream(entityPath);
    }

    @Override
    @WithSpan
    public InputStream getInputStream() throws IOException {
      return Files.newInputStream(entityPath);
    }

    @Override
    public Class<? extends ApplicationReportPersistenceService> getApplicationReportPersistenceServiceClass() {
      return FileApplicationReportPersistenceService.class;
    }

    private void validate() {
      if (!entityPath.startsWith(reportBaseDir)) {
        throw new IllegalArgumentException("Path " + entityPath + " is not within " + reportBaseDir);
      }
    }
  }

  /**
   * A ReportEntity derived from a file stored within the original report zip. These entities are read-only, and are
   * only valid as long as the zipFile is open.
   */
  private static class ZipFileReportEntity
      implements ReportEntity
  {
    private final Path pathToZip;

    private final ZipFile zipFile;

    private final ZipEntry entry;

    public ZipFileReportEntity(final Path pathToZip, final ZipFile zipFile, final ZipEntry entry) {
      this.pathToZip = pathToZip;
      this.zipFile = zipFile;
      this.entry = entry;
    }

    @Override
    public boolean exists() {
      // this service only creates these entities if the entry exists
      return true;
    }

    @Override
    @WithSpan
    public long getTime() throws IOException {
      long zipModifiedTime = Files.getLastModifiedTime(pathToZip).toMillis();
      long fileModifiedTime = entry.getLastModifiedTime().toMillis();
      return Math.max(zipModifiedTime, fileModifiedTime);
    }

    @Override
    public String getName() {
      return entry.getName();
    }

    @Override
    public long length() {
      return entry.getSize();
    }

    @Override
    public Optional<Metadata> getMetadata(final MetadataAttribute... metadataAttributes) throws IOException {
      if (!exists()) {
        return Optional.empty();
      }
      Set<MetadataAttribute> metadataAttributesSet = Set.of(metadataAttributes);
      return Optional.of(new Metadata(
          metadataAttributesSet.contains(MetadataAttribute.LAST_MODIFIED_EPOCH_TIME) ? getTime() : null,
          metadataAttributesSet.contains(MetadataAttribute.SIZE_IN_BYTES) ? length() : null));
    }

    @Override
    @WithSpan
    public OutputStream getOutputStream() {
      throw new UnsupportedOperationException("Overwriting original report files is not supported");
    }

    @Override
    @WithSpan
    public InputStream getInputStream() throws IOException {
      return zipFile.getInputStream(entry);
    }

    @Override
    public Class<? extends ApplicationReportPersistenceService> getApplicationReportPersistenceServiceClass() {
      return FileApplicationReportPersistenceService.class;
    }
  }

  private static class FileReportPdfEntity
      extends FileReportEntity
      implements ReportPdfEntity
  {
    public FileReportPdfEntity(final Path reportBaseDir) {
      super(reportBaseDir, reportBaseDir.resolve(PDF_FILENAME));
    }

    @Override
    @WithSpan
    public void deleteIfExists() throws IOException {
      Files.deleteIfExists(entityPath);
    }
  }

  private final InsightConfig insightConfig;

  private final FileCleaner fileCleaner;

  @Inject
  public FileApplicationReportPersistenceService(final InsightConfig insightConfig, final FileCleaner fileCleaner) {
    this.insightConfig = insightConfig;
    this.fileCleaner = fileCleaner;
  }

  @Override
  @WithSpan
  protected ReportEntity doGetReportEntity(
      final String applicationId,
      final String scanId,
      final String name) throws IOException
  {
    ReportEntity entity = getAdditionalReportEntity(applicationId, scanId, name);
    ReportFile reportFile = ReportFile.fromName(name);
    boolean isUnknownOrAdditional =
        reportFile == null || reportFile.getLocationTypes().contains(ReportFileLocationType.ADDITIONAL);
    if (isUnknownOrAdditional && entity.exists()) {
      return entity;
    }
    else {
      return getOrCreateLocalCopyReportEntity(applicationId, scanId, name);
    }
  }

  @Override
  @WithSpan
  public Stream<ReportEntity> getAllReportEntities(
      final String applicationId,
      final String scanId) throws IOException
  {
    Set<ReportEntity> additionalEntities = getAdditionalEntities(applicationId, scanId);
    Set<String> namesAlreadySeen = new HashSet<>();
    additionalEntities.stream().map(ReportEntity::getName).forEach(namesAlreadySeen::add);
    Set<ReportEntity> localEntities = getLocalCopyEntities(applicationId, scanId, namesAlreadySeen);
    localEntities.stream().map(ReportEntity::getName).forEach(namesAlreadySeen::add);

    return Stream.concat(
        additionalEntities.stream(),
        Stream.concat(
            localEntities.stream(),
            getOriginalEntities(applicationId, scanId, namesAlreadySeen)));
  }

  @Override
  @WithSpan
  public Stream<ReportEntity> getOriginalReportEntities(
      final String applicationId,
      final String scanId) throws IOException
  {
    return getOriginalEntities(applicationId, scanId, Collections.emptySet());
  }

  @Override
  @WithSpan
  public void saveOriginalReport(
      final String applicationId,
      final String scanId,
      final InputStream reportZipContents) throws IOException
  {
    Path finalPath = getZipPath(applicationId, scanId);
    Path parent = finalPath.getParent();

    try {
      Files.createDirectories(parent);
      try (AutoDeletingTempFile tempFile = new AutoDeletingTempFile(parent, "temp-", "zip")) {
        Path tempPath = tempFile.getPath();
        log.debug("Saving original report for application {} and scan {} to {}",
            applicationId, scanId, tempPath);

        Files.copy(reportZipContents, tempPath, StandardCopyOption.REPLACE_EXISTING);
        log.debug("Original report for application {} and scan {} written to temp file {} successfully",
            applicationId, scanId, tempPath);

        Files.move(tempPath, finalPath, StandardCopyOption.REPLACE_EXISTING);
        log.debug("Original report for application {} and scan {} moved to permanent location: {}",
            applicationId, scanId, finalPath);
      }
    }
    catch (IOException e) {
      try {
        Files.deleteIfExists(finalPath);
      }
      catch (IOException e2) {
        e.addSuppressed(e2);
      }
      throw e;
    }
  }

  @Override
  @WithSpan
  public void saveOriginalReportEntities(
      final String applicationId,
      final String scanId,
      final Stream<ReportEntity> originalReportEntities) throws IOException
  {
    Path zipPath = getZipPath(applicationId, scanId);
    try {
      Files.createDirectories(zipPath.getParent());
      try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipPath))) {
        Iterator<ReportEntity> iterator = originalReportEntities.iterator();
        while (iterator.hasNext()) {
          ReportEntity originalReportEntity = iterator.next();
          try (InputStream in = originalReportEntity.getInputStream()) {
            String entryName = originalReportEntity.getName();
            zos.putNextEntry(new ZipEntry(entryName));
            in.transferTo(zos);
            zos.closeEntry();
          }
        }
      }
    }
    catch (IOException e) {
      try {
        Files.deleteIfExists(zipPath);
      }
      catch (IOException e2) {
        e.addSuppressed(e2);
      }
      throw e;
    }
  }

  @Override
  @WithSpan
  public void moveReport(
      final String appId,
      final String sourceScanId,
      final String destinationScanId) throws IOException
  {
    final Path sourceReportDirPath = getReportDirPath(appId, sourceScanId);
    final Path destinationReportDirPath = getReportDirPath(appId, destinationScanId);
    deleteReport(appId, destinationScanId);
    Files.move(sourceReportDirPath, destinationReportDirPath);
  }

  @Override
  @WithSpan
  protected void doSaveReportFile(
      final String applicationId,
      final String scanId,
      final String name,
      final InputStream contents) throws IOException
  {
    Path localCopyPath = getLocalCopyPath(applicationId, scanId, name);

    Files.createDirectories(localCopyPath.getParent());
    Files.copy(contents, localCopyPath, StandardCopyOption.REPLACE_EXISTING);
  }

  @Override
  @WithSpan
  protected void doSaveAdditionalReportFile(
      final String applicationId,
      final String scanId,
      final String name,
      final InputStream contents) throws IOException
  {
    Path localCopyPath = getAdditionalFilesPath(applicationId, scanId, name);

    Files.createDirectories(localCopyPath.getParent());
    Files.copy(contents, localCopyPath, StandardCopyOption.REPLACE_EXISTING);
  }

  @Override
  @WithSpan
  public ReportPdfEntity getPdfEntity(final String applicationId, final String scanId) {
    return new FileReportPdfEntity(getReportDirPath(applicationId, scanId));
  }

  @Override
  @WithSpan
  public BaseReportEntity getVulnerabilitySignaturesEntity(final String applicationId, final String scanId) {
    Path reportDirPath = getReportDirPath(applicationId, scanId);
    return new FileReportEntity(reportDirPath, reportDirPath.resolve(VULNERABILITY_SIGNATURE_FILENAME));
  }

  @Override
  public String getReportLocation(final String applicationId, final String scanId) {
    return getReportDirPath(applicationId, scanId).toString();
  }

  @Override
  @WithSpan
  public boolean reportExists(final String applicationId, final String scanId) {
    if (Files.exists(getAdditionalFilesPath(applicationId, scanId).resolve(CopyStorageService.COPY_MARKER))) {
      return false;
    }
    return Files.exists(getZipPath(applicationId, scanId));
  }

  @Override
  @WithSpan
  public void deleteReport(final String applicationId, final String scanId) throws IOException {
    fileCleaner.delete(getReportDirPath(applicationId, scanId).toFile());
  }

  @Override
  @WithSpan
  public void deleteReports(final String applicationId) throws IOException {
    fileCleaner.delete(getReportsForApplicationPath(applicationId).toFile());
  }

  @Override
  @WithSpan
  public void deleteReportEntity(final ReportEntity reportEntity) throws IOException {
    FileReportEntity fileReportEntity = (FileReportEntity) reportEntity;
    Files.deleteIfExists(fileReportEntity.entityPath);
  }

  private ReportEntity getAdditionalReportEntity(
      final String applicationId,
      final String scanId,
      final String name)
  {
    Path entityPath = getAdditionalFilesPath(applicationId, scanId, name);
    return new FileReportEntity(getAdditionalFilesPath(applicationId, scanId), entityPath);
  }

  private ReportEntity getOrCreateLocalCopyReportEntity(
      final String applicationId,
      final String scanId,
      final String name) throws IOException
  {
    ReportEntity entity = getLocalCopyReportEntity(applicationId, scanId, name);

    if (!entity.exists()) {
      try {
        createLocalCopyFromOriginal(applicationId, scanId, name);
      }
      catch (java.nio.file.FileAlreadyExistsException ignored) {
        // concurrent request already created the file — safe to proceed
      }
    }

    return entity;
  }

  private ReportEntity getLocalCopyReportEntity(
      final String applicationId,
      final String scanId,
      final String name)
  {
    Path entityPath = getLocalCopyPath(applicationId, scanId, name);
    return new FileReportEntity(getLocalCopyPath(applicationId, scanId), entityPath);
  }

  /**
   * Creates a local copy of the original file. If an original file by this name does not exist, does nothing.
   */
  @WithSpan
  private void createLocalCopyFromOriginal(
      final String applicationId,
      final String scanId,
      final String name) throws IOException
  {
    Path localCopyPath = getLocalCopyPath(applicationId, scanId, name);
    Files.createDirectories(localCopyPath.getParent());

    try (var zipFile = openZipFile(applicationId, scanId)) {
      ZipEntry entry = zipFile.getEntry(name);
      if (entry != null) {
        Files.copy(zipFile.getInputStream(entry), localCopyPath, StandardCopyOption.REPLACE_EXISTING);
      }
    }
  }

  private Path getReportsRootPath() {
    return insightConfig.getClusterDirectory().toPath().resolve("report");
  }

  private Path getReportsForApplicationPath(final String applicationId) {
    return getReportsRootPath().resolve(applicationId);
  }

  private Path getReportDirPath(final String applicationId, final String scanId) {
    return getReportsForApplicationPath(applicationId).resolve(scanId);
  }

  private Path getZipPath(final String applicationId, final String scanId) {
    return getReportDirPath(applicationId, scanId).resolve(ZIP_FILENAME);
  }

  private Path getLocalCopyPath(final String applicationId, final String scanId) {
    return getReportDirPath(applicationId, scanId).resolve(LOCAL_COPY_DIRECTORY_NAME);
  }

  private Path getLocalCopyPath(final String applicationId, final String scanId, final String name) {
    return getLocalCopyPath(applicationId, scanId).resolve(name);
  }

  private Path getAdditionalFilesPath(final String applicationId, final String scanId) {
    return getReportDirPath(applicationId, scanId).resolve(ADDITIONAL_FILES_DIRECTORY_NAME);
  }

  private Path getAdditionalFilesPath(final String applicationId, final String scanId, final String name) {
    return getAdditionalFilesPath(applicationId, scanId).resolve(name);
  }

  private ZipFile openZipFile(final String applicationId, final String scanId) throws IOException {
    return openZipFile(getZipPath(applicationId, scanId));
  }

  private static ZipFile openZipFile(final Path pathToZip) throws IOException {
    return new ZipFile(pathToZip.toFile());
  }

  @WithSpan
  private Set<ReportEntity> getAdditionalEntities(
      final String applicationId,
      final String scanId) throws IOException
  {
    Path additionalFilesDir = getAdditionalFilesPath(applicationId, scanId);

    if (Files.exists(additionalFilesDir)) {
      try (var fileStream = Files.walk(additionalFilesDir)) {
        return fileStream
            .filter(Files::isRegularFile)
            .map(path -> new FileReportEntity(additionalFilesDir, path))
            .collect(Collectors.toSet());
      }
      catch (UncheckedIOException e) {
        throw e.getCause();
      }
    }
    else {
      return Set.of();
    }
  }

  @WithSpan
  private Set<ReportEntity> getLocalCopyEntities(
      final String applicationId,
      final String scanId,
      final Set<String> excludeNames) throws IOException
  {
    Path localCopiesDir = getLocalCopyPath(applicationId, scanId);

    if (Files.exists(localCopiesDir)) {
      try (var fileStream = Files.walk(localCopiesDir)) {
        return fileStream
            .filter(Files::isRegularFile)
            .filter(path -> !excludeNames.contains(localCopiesDir.relativize(path).toString()))
            .map(path -> new FileReportEntity(localCopiesDir, path))
            .collect(Collectors.toSet());
      }
      catch (UncheckedIOException e) {
        throw e.getCause();
      }
    }
    else {
      return Set.of();
    }
  }

  @WithSpan
  private Stream<ReportEntity> getOriginalEntities(
      final String applicationId,
      final String scanId,
      final Set<String> excludeNames) throws IOException
  {
    Path pathToZip = getZipPath(applicationId, scanId);

    if (Files.exists(pathToZip)) {
      // doesn't close until the returned stream is closed
      ZipFile zipFile = openZipFile(pathToZip);

      try {
        Predicate<ZipEntry> isDirectory = ZipEntry::isDirectory;
        return zipFile.stream()
            .filter(isDirectory.negate())
            .filter(entry -> !excludeNames.contains(entry.getName()))
            .map(entry -> {
              ReportEntity entity = new ZipFileReportEntity(pathToZip, zipFile, entry);
              return entity;
            })
            .onClose(() -> {
              try {
                zipFile.close();
              }
              catch (IOException e) {
                throw new UncheckedIOException(e);
              }
            });
      }
      catch (Exception e) {
        try {
          // if the stream doesn't get returned, its close will never get called by surrounding code, so we need
          // to go ahead and close zipFile.
          zipFile.close();
        }
        catch (IOException e2) {
          e.addSuppressed(e2);
        }

        throw e;
      }
    }
    else {
      return Stream.empty();
    }
  }

  @Override
  public boolean supportsTrash() {
    return true;
  }

  @Override
  public Class<? extends ReportEntity> getReportEntityClass() {
    return FileReportEntity.class;
  }
}
