/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.UUID;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyScanDAO;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadataStatus;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyScan;
import com.sonatype.insight.brain.report.ApplicationReportPersistenceService;
import com.sonatype.insight.brain.sbom.SbomSpecification;
import com.sonatype.insight.brain.sbom.datastore.SbomEntity;
import com.sonatype.insight.brain.sbom.datastore.SbomPersistenceService;
import com.sonatype.insight.brain.sbom.export.SbomExportParams.ExportSpecification;
import com.sonatype.insight.brain.sbom.utils.SbomCycloneDxUtils;
import com.sonatype.insight.brain.sbom.utils.SbomDetectionResult;
import com.sonatype.insight.brain.scan.datastore.ScanEntity;
import com.sonatype.insight.brain.scan.datastore.ScanPersistenceService;
import com.sonatype.insight.brain.utils.CheckedIllegalArgumentException;
import com.sonatype.insight.brain.utils.FunctionWithException;
import com.sonatype.insight.brain.utils.SupplierWithException;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.InternalServerException;
import com.sonatype.insight.scan.file.SbomFormat;

import com.google.common.collect.Streams;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.codehaus.plexus.util.StringUtils;
import org.jooq.exception.DataAccessException;
import org.jooq.exception.IntegrityConstraintViolationException;
import org.postgresql.util.PSQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.db.jooq.DialectHelper.POSTGRES_UNIQUE_CONSTRAINT_VIOLATION;

/**
 * This class aims to have centralized responsibility for persisting high-level ThirdParty/SBOM data to the database and
 * long term disk storage. Its goal is to mediate all access to disk storage of SBOMs and all write operations to
 * ThirdPartySbomMetadata, ThirdPartyFile, and ThirdPartyScan. That said, it is very much a work-in-progress and much of
 * the SBOM-saving logic is still distributed throughout other classes such as ScanUploadService,
 * ThirdPartyScanResultsProcessor, and SbomResultsMerger. Hopefully future refactors will consolidate that logic. This
 * class also manages temporary storage locations for SBOM and binary files.
 */
@Named
@Singleton
public class ThirdPartyPersistenceService
{
  private static final Logger log = LoggerFactory.getLogger(ThirdPartyPersistenceService.class);

  private static final int MAX_SAVE_ATTEMPTS = 5;

  private static final int MAX_SLEEP_MILLIS_FOR_NEW_VERSION = 5;

  private static final DateTimeFormatter dtFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

  private final ThirdPartySbomMetadataDAO sbomMetadataDAO;

  private final ThirdPartyFileDAO thirdPartyFileDAO;

  private final ThirdPartyScanDAO thirdPartyScanDAO;

  private final SbomPersistenceService sbomPersistenceService;

  private final ApplicationReportPersistenceService applicationReportPersistenceService;

  private final ScanPersistenceService scanPersistenceService;

  /**
   * Paths coming into this class from outside, which get saved in various database columns and used in the construction
   * of some real filesystem paths. Some of these are user-provided and must be carefully sanitized, while others come
   * from scan.xml files, are not guaranteed to actually be fileystem paths (and thus cannot really be sanitized) and
   * must not be used in filesystem operations. This class helps keep track of whether a path is sanitized and safe to
   * use in filesystem operations.
   *
   * Note that ideally only TrustedAutoDeletingTempPath would be public, but it doesn't seem to be possible to make
   * the other nested classes private within an interface.
   */
  public sealed interface PersistencePath
  {
    /**
     * PersistencePath subtypes from which file extensions can be safely retrieved
     */
    sealed interface ExtensionSafePath
        extends PersistencePath
    {
      default String getExtension() {
        // Note that this method behaves the same across Windows and UNIX (aside from one exception case, see docs).
        // Both forward and backslashes are treated as directory separators on both.
        return FilenameUtils.getExtension(toString());
      }
    }

    /**
     * A path which could be a basename, a longer file path, or a string that isn't
     * a file path at all such as a container URI. Not safe for filesystem operations.
     */
    record UnsafePath(String scanPath)
        implements PersistencePath
    {
      @Override
      public String toString() {
        return scanPath;
      }
    }

    /**
     * A path from a scan.xml `path` attribute, which could be a basename or a longer file path but is expected to
     * be a syntactically valid file path. Safe for retrieving a file extension but not for using the full path in
     * filesystem operation.
     */
    static final class SanitizedSbomScanPath
        implements ExtensionSafePath
    {
      private final String scanPath;

      private final String extension;

      private SanitizedSbomScanPath(String scanPath) throws CheckedIllegalArgumentException {
        this.scanPath = scanPath;
        this.extension = FilenameUtils.getExtension(sanitizeUserPath(Path.of(scanPath)).toString());
      }

      @Override
      public String toString() {
        return scanPath;
      }

      @Override
      public String getExtension() {
        return extension;
      }

      /**
       * Sanitize the file path by normalizing it and ensuring the result is not empty or `.` and does not contain a
       * `..` segment
       */
      private static Path sanitizeUserPath(Path userFilePath) throws CheckedIllegalArgumentException {
        var normalizedPath = userFilePath.normalize();

        if (normalizedPath.toString().isEmpty() || Path.of(".").equals(normalizedPath)
            || Streams.stream(normalizedPath.iterator()).anyMatch(Path.of("..")::equals))
        {
          throw new CheckedIllegalArgumentException("Invalid filename: %s".formatted(userFilePath));
        }

        return normalizedPath;
      }
    }

    /**
     * A path provided directly by the user, for instance in a `Content-Disposition` header. Wrapping the user-provided
     * value in this class strips off directory information and ensures that the remaining basename (what this class
     * returns from `toString`) is a safe, sane value for use with IQ's internal filesystem operations.
     */
    static final class SanitizedUserPath
        implements ExtensionSafePath
    {
      private final Path sanitizedUserPath;

      private SanitizedUserPath(String userPath) throws CheckedIllegalArgumentException {
        this.sanitizedUserPath = getSafeFilename(Path.of(userPath));
      }

      @Override
      public String toString() {
        return sanitizedUserPath.toString();
      }

      /**
       * @return the basename from the given path, ensuring it is not empty, `.`, or `..`
       */
      private static Path getSafeFilename(Path userFilePath) throws CheckedIllegalArgumentException {
        Path filenamePath = userFilePath.getFileName();
        if (filenamePath.equals(Path.of("")) || filenamePath.equals(Path.of(".")) ||
            filenamePath.equals(Path.of("..")))
        {
          throw new CheckedIllegalArgumentException("Invalid filename: " + userFilePath);
        }
        else {
          return filenamePath;
        }
      }
    }
  }

  @Inject
  public ThirdPartyPersistenceService(
      final ThirdPartySbomMetadataDAO sbomMetadataDAO,
      final ThirdPartyFileDAO thirdPartyFileDAO,
      final ThirdPartyScanDAO thirdPartyScanDAO,
      final SbomPersistenceService sbomPersistenceService,
      final ApplicationReportPersistenceService applicationReportPersistenceService,
      final ScanPersistenceService scanPersistenceService)
  {
    this.sbomMetadataDAO = sbomMetadataDAO;
    this.thirdPartyFileDAO = thirdPartyFileDAO;
    this.thirdPartyScanDAO = thirdPartyScanDAO;
    this.sbomPersistenceService = sbomPersistenceService;
    this.applicationReportPersistenceService = applicationReportPersistenceService;
    this.scanPersistenceService = scanPersistenceService;
  }

  /**
   * Save the SBOM file to the database and disk storage. The value for the ThirdPartySbomMetadata.sbomVersion field is
   * pulled from the detectionResult. If no value is found there, a value based on the current timestamp is used. Saving
   * the record will be attempted several times in the event of database conflicts. Conflicts will be broken by
   * appending a timestamp to the specified version string. The version that ultimately gets saved is also added to the
   * AuditData. Actual SBOM files are saved to their permanent storage location, while binary files are saved to a
   * temporary location where they can be retrieved later for scanning.
   *
   * @param userFilename The filename/path provided by the user. Only the basename on this path will be considered (any
   *          directories will be ignored).
   *
   * @throws CheckedIllegalArgumentException if the basename of userFilename is empty, `.`, or `..`
   */
  public ImmutablePair<ThirdPartySbomMetadata, ThirdPartyFile> saveSbomManagerSbomOrBinary(
      SbomEntity sbomEntity,
      String userFilename,
      String applicationId,
      SbomDetectionResult detectionResult) throws IOException, CheckedIllegalArgumentException
  {
    return saveSbomManagerSbomOrBinaryFromUserUpload(
        sbomEntity,
        new PersistencePath.SanitizedUserPath(userFilename),
        applicationId,
        null,
        detectionResult);
  }

  /**
   * Save the SBOM file to the database and disk storage. Similar to the method above, except that preferredVersion will
   * be used as the version if supplied, falling back to the version from the file and then the timestamp if necessary.
   *
   * @param userFilename The filename/path provided by the user. Only the basename on this path will be considered (any
   *          directories will be ignored).
   *
   * @throws CheckedIllegalArgumentException if the basename of userFilename is empty, `.`, or `..`
   */
  public ImmutablePair<ThirdPartySbomMetadata, ThirdPartyFile> saveSbomManagerSbomOrBinary(
      SbomEntity sbomEntity,
      String userFilename,
      String applicationId,
      String preferredVersion,
      SbomDetectionResult detectionResult) throws IOException, CheckedIllegalArgumentException
  {
    return saveSbomManagerSbomOrBinaryFromUserUpload(
        sbomEntity,
        new PersistencePath.SanitizedUserPath(userFilename),
        applicationId,
        preferredVersion,
        detectionResult);
  }

  /**
   * Save the SBOM file to the database and disk storage. Similar to the method above, except the SBOM contents are
   * provided as a String instead of a Path. This method stores the basename of the "path" value from the scan.xml,
   * which must be a valid, safe file path.
   *
   * @param userPreferredVersion user-supplied version preference; null falls back to the version detected in the SBOM,
   *          then to a timestamp
   * @throws CheckedIllegalArgumentException if the basename of scanPath is empty, `.`, or `..`
   */
  public ImmutablePair<ThirdPartySbomMetadata, ThirdPartyFile> saveSbomManagerSbomFromScan(
      String sbomContents,
      String scanPath,
      String applicationId,
      String userPreferredVersion,
      SbomDetectionResult detectionResult) throws IOException, CheckedIllegalArgumentException
  {
    if (!detectionResult.isSbom) {
      throw new IllegalArgumentException("sbomContents must represent an SBOM, not a binary");
    }

    return saveSbomManagerSbom(
        () -> IOUtils.toInputStream(sbomContents, Charset.defaultCharset()),
        new PersistencePath.SanitizedSbomScanPath(scanPath),
        applicationId,
        userPreferredVersion,
        detectionResult);
  }

  /**
   * Save information about the binary to the database. The binary contents are NOT saved to a persistent temporary
   * location as this method is intended to be called during processing of an already-existing scan file, in which there
   * is no need for such temporary storage. This method stores the full "path" value from the scan.xml without sanity
   * checking that it is a safe file path. No actual files are saved using any part of that value.
   *
   * @param userPreferredVersion user-supplied version preference; null falls back to the version detected in the SBOM,
   *          then to a timestamp
   */
  public ImmutablePair<ThirdPartySbomMetadata, ThirdPartyFile> saveSbomManagerBinaryFromScan(
      String sbomContents,
      String scanPath,
      String applicationId,
      String userPreferredVersion,
      SbomDetectionResult detectionResult) throws IOException
  {
    if (detectionResult.isSbom) {
      throw new IllegalArgumentException("sbomContents must represent a binary, not an SBOM");
    }

    return saveSbomManagerBinaryFromScan(
        new PersistencePath.UnsafePath(scanPath),
        applicationId,
        userPreferredVersion,
        detectionResult);
  }

  /**
   * Save the ThirdPartyFile for the SBOM. This method supports the Lifecycle workflow (e.g. when an SBOM is detected
   * embedded in a scan.xml from a lifecycle scan of a binary) and so does not persist the SBOM contents to disk
   * or create a ThirdPartySbomMetadata record
   */
  public ThirdPartyFile saveLifecycleSbomFromScan(String scanPath) {
    try (var tx = thirdPartyFileDAO.createTransactionContext()) {
      tx.begin();
      var retval = saveThirdPartyFileToDB(tx, new PersistencePath.UnsafePath(scanPath));
      tx.commit();

      return retval;
    }
  }

  /**
   * Returns an InputStream of the contents of the SBOM file associated with the given ThirdPartySbomMetadata.
   *
   * @throws IllegalArgumentException if sbomMetadata does not have an associated persisted SBOM file (i.e. if
   *           sbomMetadata.getFilename() is null)
   */
  public InputStream getSbomContentsInputStream(
      ThirdPartySbomMetadata sbomMetadata) throws IOException, IllegalArgumentException
  {
    if (sbomMetadata.getFilename() != null) {
      var sbomEntity =
          sbomPersistenceService.getPermanentSbom(sbomMetadata.getApplicationId(), sbomMetadata.getFilename());
      return new GzipCompressorInputStream(sbomEntity.getInputStream());
    }
    else {
      // probably a binary upload that hasn't been processed yet
      throw new IllegalArgumentException("SBOM metadata does not have a persisted SBOM file");
    }
  }

  /**
   * Creates, saves, and returns the ThirdPartyScan record associating the given ThirdPartyFile with the given
   * scanRequestId.
   */
  public ThirdPartyScan associateWithScan(ThirdPartyFile thirdPartyFile, String scanRequestId) {
    try (var tx = thirdPartyScanDAO.createTransactionContext()) {
      tx.begin();

      var thirdPartyScan = new ThirdPartyScan(thirdPartyFile.getId(), scanRequestId, new Date());
      thirdPartyScanDAO.insert(tx, thirdPartyScan);

      tx.commit();
      return thirdPartyScan;
    }
  }

  /**
   * Write the SBOM/binary content from the input stream to a randomly-named file with the same file extension as
   * userFilename in the transient storage directory.
   *
   * @return an SbomEntity representing the written file, which should be removed when no longer needed
   */
  public SbomEntity writeToTransientStorage(
      InputStream sbomStream,
      String userFilename) throws IOException, CheckedIllegalArgumentException
  {
    var sanitizedPath = new PersistencePath.SanitizedUserPath(userFilename);

    SbomEntity transientSbom = sbomPersistenceService.getTransientSbom(sanitizedPath.toString());
    try (var outputStream = transientSbom.getOutputStream()) {
      IOUtils.copy(sbomStream, outputStream);
    }
    catch (IOException e) {
      deleteSbomDueToException(transientSbom, e);
      throw e;
    }

    log.debug("Uploaded SBOM or binary saved to transient storage at {}", transientSbom.getLocation());

    return transientSbom;
  }

  /**
   * Deletes the given SBOM entity from transient storage.
   */
  public void deleteSbomFromTransientStorage(SbomEntity sbomEntity) throws IOException {
    if (sbomEntity != null) {
      sbomPersistenceService.deleteSbom(sbomEntity);
    }
  }

  /**
   * @return the persistent temporary binary file associated with the given sbomMetadata and thirdPartyFile as an
   *         SBOM entity.
   *
   *         External callers should only read the file at this path and not modify or delete it. The actual directory
   *         structure of the path should also be treated as an implementation detail of this class and not inspected by
   *         external callers.
   *
   *         Implementation notes:
   *         Persistent temp files for binaries are stored in a directory/path named using the ThirdPartySbomMetadata
   *         id, with a
   *         filename matching their original filename as recorded in the ThirdPartyFile. Using the original name for
   *         the file
   *         is important for enabling the insight-scanner code to process it correctly.
   *
   * @throws IllegalArgumentException if the thirdPartyFile does not match the sbomMetadata or does not contain
   *           a sanitizable filename
   */
  public SbomEntity getBinaryPersistentTempFilePath(
      ThirdPartySbomMetadata sbomMetadata,
      ThirdPartyFile thirdPartyFile)
  {
    if (!sbomMetadata.getThirdPartyFileId().equals(thirdPartyFile.getId())) {
      throw new IllegalArgumentException(
          "ThirdPartyFile does not match ThirdPartySbomMetadata: %s vs %s"
              .formatted(sbomMetadata.getThirdPartyFileId(), thirdPartyFile.getId()));
    }

    PersistencePath.SanitizedUserPath sanitizedPath;
    try {
      sanitizedPath = new PersistencePath.SanitizedUserPath(thirdPartyFile.getFilename());
    }
    catch (CheckedIllegalArgumentException e) {
      throw new IllegalArgumentException(
          "Persistent temp file path cannot be constructed for unsafe path " + thirdPartyFile.getFilename(), e);
    }

    return sbomPersistenceService.getTemporarySbom(sanitizedPath.toString(), sbomMetadata.getId());
  }

  /**
   * Deletes the persistent temporary binary file associated with the given sbomMetadata.
   */
  public void deletePersistentTempBinary(
      ThirdPartySbomMetadata sbomMetadata,
      ThirdPartyFile thirdPartyFile) throws IOException
  {
    SbomEntity persistentTempFile = getBinaryPersistentTempFilePath(sbomMetadata, thirdPartyFile);
    sbomPersistenceService.deleteSbom(persistentTempFile);

    log.debug("Deleted persistent temporary binary file and directory for SBOM {} at {}",
        sbomMetadata.getId(), persistentTempFile.getLocation());
  }

  /**
   * Set the status on the ThirdPartySbomMetadata with the given id to PENDING. Atomically checks that it was currently
   * set to UPLOADED in the database and throws an exception if not.
   * Creates its own transaction context - use
   * {@link #setSbomMetadataStatusToPending(TransactionContext, ThirdPartySbomMetadata)} if you already have an active
   * transaction to avoid lock contention.
   */
  public void setSbomMetadataStatusToPending(ThirdPartySbomMetadata sbomMetadata) {
    try (var tx = sbomMetadataDAO.createTransactionContext()) {
      tx.begin();
      setSbomMetadataStatusToPendingInternal(tx, sbomMetadata);
      tx.commit();
    }
  }

  /**
   * Set the status on the ThirdPartySbomMetadata with the given id to PENDING, using the provided transaction context.
   * Atomically checks that it was currently set to UPLOADED in the database and throws an exception if not.
   * Use this overload when you already have an active transaction to avoid lock contention issues.
   */
  public void setSbomMetadataStatusToPending(TransactionContext tx, ThirdPartySbomMetadata sbomMetadata) {
    setSbomMetadataStatusToPendingInternal(tx, sbomMetadata);
  }

  private void setSbomMetadataStatusToPendingInternal(TransactionContext tx, ThirdPartySbomMetadata sbomMetadata) {
    var existing = sbomMetadataDAO.getByIdForUpdate(tx, sbomMetadata.getId());

    if (existing.getStatus() != ThirdPartySbomMetadataStatus.UPLOADED) {
      throw new IllegalStateException(
          "SBOM %s is not in the UPLOADED state, is in %s".formatted(sbomMetadata.getId(), existing.getStatus()));
    }

    sbomMetadata.setStatus(ThirdPartySbomMetadataStatus.PENDING);

    // this will merge the changes from sbomMetadata into `existing` and persist them to the DB
    sbomMetadataDAO.update(tx, sbomMetadata);
  }

  /**
   * Save the SBOM file generated from an uploaded binary, and store a reference to its file location in the
   * sbomMetadata.filename field. The filename is generated based on the original binary filename and the SBOM version.
   */
  public void saveSbomForBinary(InputStream sbomStream, ThirdPartySbomMetadata sbomMetadata) throws IOException {
    if (sbomMetadata.getFilename() != null) {
      throw new IllegalArgumentException("sbomMetadata must not already be associated with a permanently-saved file");
    }

    if (!sbomMetadata.getScanType().equals(SbomScanType.BINARY.name())) {
      throw new IllegalArgumentException("sbomMetadata must be for a binary scan");
    }

    var binaryFileName = sbomMetadata.getOriginalBinaryFileName();
    var binaryFilenameWithoutExtension = FilenameUtils.getBaseName(binaryFileName);
    var compressedBinaryFileName =
        "%s.%s.json.gz".formatted(binaryFilenameWithoutExtension, sbomMetadata.getSbomVersion());

    SbomEntity compressedSbomPath = null;
    try (var tx = sbomMetadataDAO.createTransactionContext()) {
      tx.begin();

      // We need to do this to seamlessly integrate into our SBOM exporter logic
      sbomMetadata.setFilename(compressedBinaryFileName);
      sbomMetadataDAO.update(tx, sbomMetadata);

      compressedSbomPath =
          sbomPersistenceService.doGetSbom(sbomMetadata.getApplicationId(), sbomMetadata.getFilename());
      if (compressedSbomPath.exists()) {
        throw new IOException("SBOM already exists: " + compressedSbomPath.getLocation());
      }

      try (var fileStream = compressedSbomPath.getOutputStream();
          var outputStream = new GzipCompressorOutputStream(fileStream))
      {
        IOUtils.copy(sbomStream, outputStream);
      }

      tx.commit();
    }
    catch (Exception e) {
      deleteSbomDueToException(compressedSbomPath, e);
      throw e;
    }
  }

  /**
   * Updates the `sbomVersion` field on the given ThirdPartySbomMetadata record to the specified value and saves it to
   * the database within the provided transaction context. If the new version is the same as the current version, no
   * changes are made.
   *
   * @throws CheckedIllegalArgumentException if applicationVersion is null, empty, or only whitespace
   */
  public void updateApplicationVersion(
      TransactionContext tx,
      ThirdPartySbomMetadata sbomMetadata,
      String applicationVersion) throws CheckedIllegalArgumentException
  {
    if (StringUtils.isBlank(applicationVersion)) {
      throw new CheckedIllegalArgumentException("applicationVersion must not be blank");
    }
    else if (!applicationVersion.equals(sbomMetadata.getSbomVersion())) {
      sbomMetadata.setSbomVersion(applicationVersion);
      sbomMetadataDAO.update(tx, sbomMetadata);

      var auditData = AuditData.get();
      auditData.setSbomVersion(sbomMetadata, SbomAction.UPDATE);
    }
  }

  /**
   * Deletes the given SBOM metadata entity and all known associated data including other related entities and files.
   */
  public void deleteSbomMetadataAndAssociatedFiles(ThirdPartySbomMetadata sbomMetadata) throws IOException {
    String applicationId = sbomMetadata.getApplicationId();
    String scanId;
    String filteredScanFile;

    try (TransactionContext tx = thirdPartyFileDAO.createTransactionContext()) {
      tx.begin();

      // Capture the scanId and filtered scan file name within the delete transaction, before the cascade delete
      // removes the ThirdPartyScan row, so the captured ids match the row actually being deleted even under a
      // concurrent re-scan of the same file (CLM-40930).
      ThirdPartyScan thirdPartyScan = thirdPartyScanDAO.getByThirdPartyFileId(tx, sbomMetadata.getThirdPartyFileId());
      scanId = thirdPartyScan == null ? null : thirdPartyScan.getScanId();
      filteredScanFile = thirdPartyScan == null ? null : thirdPartyScan.getFilteredScanFile();

      ThirdPartyFile thirdPartyFile = thirdPartyFileDAO.getById(sbomMetadata.getThirdPartyFileId());
      // Deleting the corresponding ThirdPartyFile will cascade to the SBOM metadata and all other child records.
      // Concurrent delete safety: if the row was already deleted by another transaction, skip gracefully.
      if (thirdPartyFile != null) {
        thirdPartyFileDAO.delete(tx, thirdPartyFile);
        deleteSbomFile(sbomMetadata);
        if (SbomScanType.BINARY.name().equals(sbomMetadata.getScanType())) {
          deletePersistentTempBinary(sbomMetadata, thirdPartyFile);
        }
      }
      tx.commit();
    }

    // Scan files and report files live in non-transactional storage (filesystem or S3). Delete them after the DB
    // commit so a storage failure is logged but cannot roll back the already-committed metadata delete (consistent
    // with deleteSbomFile behavior). Deleting an SBOM must remove it completely from the server — scan files and
    // reports (CLM-40930). Matches the cleanup patterns in ApplicationCleaner.delete and ReportPurger.purgeReport.
    deleteScanAndReportFilesForScan(applicationId, scanId, filteredScanFile);
  }

  /**
   * Best-effort removal of the scan files and policy-evaluation report files for the given scanId. A blank scanId
   * (e.g. an SBOM deleted before its evaluation completed) is skipped: nothing was persisted for it yet, and passing
   * a blank id to the storage layer could target the wrong path. Each deletion is independent and best-effort — a
   * failure is logged and neither prevents the other deletions nor rolls back the committed metadata delete.
   */
  private void deleteScanAndReportFilesForScan(String applicationId, String scanId, String filteredScanFile) {
    if (StringUtils.isBlank(scanId)) {
      log.debug("No scanId associated with SBOM for applicationId {}; skipping scan and report file cleanup",
          applicationId);
      return;
    }

    // Main scan file (scan-<scanId>.xml.gz)
    try {
      scanPersistenceService.deleteScan(applicationId, scanId);
    }
    catch (IOException e) {
      log.error("Failed to delete scan file for applicationId {} scanId {}: {}",
          applicationId, scanId, e.getMessage(), e);
    }

    // Filtered scan file (scan-<scanId>-filtered.xml.gz): per-scanId and not shared across scans, so it is safe to
    // delete here. (ReportPurger.purgeReport does not clean this file up — a separate, pre-existing gap.)
    if (StringUtils.isNotBlank(filteredScanFile)) {
      try {
        ScanEntity filteredScanEntity = scanPersistenceService.getScanByName(applicationId, filteredScanFile);
        scanPersistenceService.deleteScan(filteredScanEntity);
      }
      catch (IOException e) {
        log.error("Failed to delete filtered scan file {} for applicationId {}: {}",
            filteredScanFile, applicationId, e.getMessage(), e);
      }
    }

    // Policy-evaluation report files
    try {
      applicationReportPersistenceService.deleteReport(applicationId, scanId);
    }
    catch (IOException e) {
      log.error("Failed to delete report files for applicationId {} scanId {}: {}",
          applicationId, scanId, e.getMessage(), e);
    }
  }

  /**
   * Attempts to delete all SBOM temporary transient files that are older than the given date.
   * If it fails to delete one file, it will still attempt to delete the other files.
   * Any failures are logged but IOExceptions are not thrown by this method.
   */
  public void tryDeleteSbomTemporaryTransientFilesOlderThan(Date date) {
    try {
      sbomPersistenceService.deleteTransientSbomsOlderThan(date.toInstant());
    }
    catch (IOException e) {
      log.error("Failed to delete SBOM temporary transient files older than {}: {}", date, e.getMessage(), e);
    }
  }

  /**
   * Save the SBOM file or binary to the database and disk storage. SBOM files are saved to permanent storage, while
   * binary files are saved to a persistent temporary location where they can be retrieved later for scanning.
   */
  private ImmutablePair<ThirdPartySbomMetadata, ThirdPartyFile> saveSbomManagerSbomOrBinaryFromUserUpload(
      SbomEntity sbomEntity,
      PersistencePath.SanitizedUserPath userPath,
      String applicationId,
      String preferredVersion,
      SbomDetectionResult detectionResult) throws IOException
  {
    if (detectionResult.isSbom) {
      return saveSbomManagerSbom(
          sbomEntity::getInputStream,
          userPath,
          applicationId,
          preferredVersion,
          detectionResult);
    }
    else {
      return saveSbomManagerBinaryFromUserUpload(
          sbomEntity,
          userPath,
          applicationId,
          preferredVersion,
          detectionResult);
    }
  }

  /**
   * Save the SBOM (with retry logic) from either a user upload or a scan.xml
   */
  private ImmutablePair<ThirdPartySbomMetadata, ThirdPartyFile> saveSbomManagerSbom(
      SupplierWithException<InputStream, IOException> sbomContentStreamSupplier,
      PersistencePath.ExtensionSafePath userPath,
      String applicationId,
      String userPreferredVersion,
      SbomDetectionResult detectionResult) throws IOException
  {
    return trySaveInLoop(userPreferredVersion, detectionResult, (versionToSave) -> saveSbomWithVersion(
        sbomContentStreamSupplier,
        userPath,
        applicationId,
        versionToSave,
        detectionResult));
  }

  /**
   * Save the SBOM (with retry logic) from either a user upload or a scan.xml
   */
  private ImmutablePair<ThirdPartySbomMetadata, ThirdPartyFile> saveSbomManagerSbom(
      SupplierWithException<InputStream, IOException> sbomContentStreamSupplier,
      PersistencePath.SanitizedSbomScanPath userPath,
      String applicationId,
      String userPreferredVersion,
      SbomDetectionResult detectionResult) throws IOException
  {
    return trySaveInLoop(userPreferredVersion, detectionResult, (versionToSave) -> saveSbomWithVersion(
        sbomContentStreamSupplier,
        userPath,
        applicationId,
        versionToSave,
        detectionResult));
  }

  /**
   * Save the binary (with retry logic) from user upload only. This saves the DB records and saves the binary itself
   * to a persistent temp file.
   */
  private ImmutablePair<ThirdPartySbomMetadata, ThirdPartyFile> saveSbomManagerBinaryFromUserUpload(
      SbomEntity sbomEntity,
      PersistencePath.SanitizedUserPath userPath,
      String applicationId,
      String userPreferredVersion,
      SbomDetectionResult detectionResult) throws IOException
  {
    return trySaveInLoop(userPreferredVersion, detectionResult, (versionToSave) -> saveBinaryWithVersion(
        sbomEntity,
        userPath,
        applicationId,
        versionToSave,
        detectionResult));
  }

  /**
   * Save the binary (with retry logic) from scan.xml only. This does not save the binary to disk and thus
   * does not require a safe userPath.
   */
  private ImmutablePair<ThirdPartySbomMetadata, ThirdPartyFile> saveSbomManagerBinaryFromScan(
      PersistencePath userPath,
      String applicationId,
      String userPreferredVersion,
      SbomDetectionResult detectionResult) throws IOException
  {
    return trySaveInLoop(userPreferredVersion, detectionResult, (versionToSave) -> saveBinaryMetadataWithVersion(
        userPath,
        applicationId,
        versionToSave,
        detectionResult));
  }

  /**
   * The retry logic for persisting SBOMs. If there's a preferred version, try first to save with that and then
   * up to five more times with that version appended with timestamps to try to break conflicts. If there's no preferred
   * version, generate a timestamp version and try to save with that, generating (but not appending) new timestamps up
   * to five times to likewise break conflicts.
   */
  private ImmutablePair<ThirdPartySbomMetadata, ThirdPartyFile> trySaveInLoop(
      String userPreferredVersion,
      SbomDetectionResult detectionResult,
      FunctionWithException<String, ImmutablePair<ThirdPartySbomMetadata, ThirdPartyFile>, IOException> saveFunction) throws IOException
  {
    // Try to save using the user's preferred SBOM version if any, otherwise the version detected in the SBOM if any
    final Optional<String> preferredVersion = Optional.ofNullable(userPreferredVersion)
        .or(() -> Optional.ofNullable(detectionResult.summary).map(s -> s.applicationVersion))
        .filter(v -> !v.isBlank());

    String versionToSave = preferredVersion.orElse(getTimestampForVersion());

    Exception finalException = null;
    for (int i = 0; i < MAX_SAVE_ATTEMPTS; i++) {
      try {
        var metadataAndFile = saveFunction.apply(versionToSave);

        var auditData = AuditData.get();
        auditData.setSbomVersion(metadataAndFile.getLeft(), SbomAction.CREATE);
        auditData.setStageId(StageTypes.COMPLIANCE.getId());
        return metadataAndFile;
      }
      catch (DataAccessException e) {
        // Handle unique constraint violations for PostgreSQL and H2
        if ((e.getCause() instanceof PSQLException psqlEx
            && POSTGRES_UNIQUE_CONSTRAINT_VIOLATION.equals(psqlEx.getSQLState()))
            || e instanceof IntegrityConstraintViolationException
            || e.getCause() instanceof IntegrityConstraintViolationException)
        {
          finalException = e;

          var oldVersionToSave = versionToSave;
          sleepForNewVersionTimestamp();
          versionToSave = getNewHopefullyUniqueVersion(preferredVersion);

          log.debug("Version conflict saving SBOM with version {}, retrying with new version {} ",
              oldVersionToSave, versionToSave);
        }
        else {
          throw e;
        }
      }
    }

    throw new InternalServerException("Failed to save SBOM", finalException);
  }

  private ImmutablePair<ThirdPartySbomMetadata, ThirdPartyFile> saveSbomWithVersion(
      SupplierWithException<InputStream, IOException> sbomContentStreamSupplier,
      PersistencePath.ExtensionSafePath userPath,
      String applicationId,
      String applicationVersion,
      SbomDetectionResult detectionResult) throws IOException
  {
    SbomEntity sbomEntity1 = null;
    try (var tx = sbomMetadataDAO.createTransactionContext()) {
      tx.begin();

      sbomEntity1 = writeSbomToPermanentStorage(sbomContentStreamSupplier, applicationId, userPath);

      var retval =
          saveSbomMetadataToDB(
              tx,
              userPath,
              sbomEntity1.getName(),
              applicationId,
              applicationVersion,
              SbomScanType.SBOM,
              detectionResult);

      tx.commit();
      return retval;
    }
    catch (Exception e) {
      deleteSbomDueToException(sbomEntity1, e);
      throw e;
    }
  }

  /**
   * Save the ThirdPartySbomMetadata and ThirdPartyFile records for a binary scan coming from a user upload, and save
   * the binary itself to disk in persistent temp storage.
   */
  private ImmutablePair<ThirdPartySbomMetadata, ThirdPartyFile> saveBinaryWithVersion(
      SbomEntity sbomEntity,
      PersistencePath.SanitizedUserPath userPath,
      String applicationId,
      String applicationVersion,
      SbomDetectionResult detectionResult) throws IOException
  {
    SbomEntity persistentTempSbom = null;
    try (var tx = sbomMetadataDAO.createTransactionContext()) {
      tx.begin();

      var retval = saveBinaryMetadataWithVersion(
          tx,
          userPath,
          applicationId,
          applicationVersion,
          detectionResult);
      var sbomMetadata = retval.getLeft();

      persistentTempSbom = writeBinaryToPersistentTempStorage(
          sbomEntity,
          sbomMetadata,
          userPath);

      tx.commit();
      return retval;
    }
    catch (Exception e) {
      deleteSbomDueToException(persistentTempSbom, e);
      throw e;
    }
  }

  /**
   * Save the ThirdPartySbomMetadata and ThirdPartyFile records for a binary scan coming from a scan.xml. In this
   * case, the binary does not get saved to the persistent temp file as it is already available in the scan file.
   */
  private ImmutablePair<ThirdPartySbomMetadata, ThirdPartyFile> saveBinaryMetadataWithVersion(
      PersistencePath userPath,
      String applicationId,
      String applicationVersion,
      SbomDetectionResult detectionResult)
  {
    try (var tx = sbomMetadataDAO.createTransactionContext()) {
      tx.begin();

      var retval = saveBinaryMetadataWithVersion(
          tx,
          userPath,
          applicationId,
          applicationVersion,
          detectionResult);

      tx.commit();
      return retval;
    }
  }

  /**
   * Within the provided transaction, save the ThirdPartySbomMetadata and ThirdPartyFile records for the binary file
   */
  private ImmutablePair<ThirdPartySbomMetadata, ThirdPartyFile> saveBinaryMetadataWithVersion(
      TransactionContext tx,
      PersistencePath userPath,
      String applicationId,
      String applicationVersion,
      SbomDetectionResult detectionResult)
  {
    var retval = saveSbomMetadataToDB(
        tx,
        userPath,
        null,
        applicationId,
        applicationVersion,
        SbomScanType.BINARY,
        detectionResult);

    var sbomMetadata = retval.getLeft();
    sbomMetadata.setOriginalBinaryFileName(userPath.toString());
    sbomMetadataDAO.update(tx, sbomMetadata);

    return retval;
  }

  /**
   * Save the SBOM contents to disk, gzipped, in the permanent storage directory
   */
  private SbomEntity writeSbomToPermanentStorage(
      SupplierWithException<InputStream, IOException> sbomContentStreamSupplier,
      String applicationId,
      PersistencePath.ExtensionSafePath userPath) throws IOException
  {
    SbomEntity permanentSbom = null;
    try {
      String randomUUID = UUID.randomUUID().toString().replace("-", "");
      String extension = userPath.getExtension();
      String fileName = randomUUID + (extension == null ? "" : "." + extension) + ".gz";

      permanentSbom = sbomPersistenceService.doGetSbom(applicationId, fileName);
      if (permanentSbom.exists()) {
        throw new IOException("SBOM already exists: " + permanentSbom.getLocation());
      }

      try (InputStream sbomStream = sbomContentStreamSupplier.get();
          OutputStream outputStream = permanentSbom.getOutputStream();
          GzipCompressorOutputStream compressorOutputStream = new GzipCompressorOutputStream(outputStream))
      {
        IOUtils.copy(sbomStream, compressorOutputStream);
      }

      return permanentSbom;
    }
    catch (IOException e) {
      deleteSbomDueToException(permanentSbom, e);
      throw e;
    }
  }

  /**
   * Save the SBOM contents to disk, raw, in the persistent temp storage directory
   */
  private SbomEntity writeBinaryToPersistentTempStorage(
      SbomEntity sbomEntity, // binaryTransientPath
      ThirdPartySbomMetadata sbomMetadata,
      PersistencePath.SanitizedUserPath userPath) throws IOException
  {
    SbomEntity persistentTempSbom = null;
    try {
      persistentTempSbom =
          sbomPersistenceService.saveTemporarySbom(sbomEntity, userPath.toString(), sbomMetadata.getId());
    }
    catch (IOException e) {
      deleteSbomDueToException(persistentTempSbom, e);
      throw e;
    }

    return persistentTempSbom;
  }

  /**
   * Within the provided transaction, save the ThirdPartySbomMetadata and ThirdPartyFile records for the binary or SBOM.
   */
  private ImmutablePair<ThirdPartySbomMetadata, ThirdPartyFile> saveSbomMetadataToDB(
      TransactionContext tx,
      PersistencePath userPath,
      String serverFilePath,
      String applicationId,
      String applicationVersion,
      SbomScanType scanType,
      SbomDetectionResult detectionResult)
  {
    var thirdPartyFile = saveThirdPartyFileToDB(tx, userPath);

    var sbomMetadata = new ThirdPartySbomMetadata();
    var summary = detectionResult.summary;

    sbomMetadata.setApplicationId(applicationId);
    sbomMetadata.setThirdPartyFileId(thirdPartyFile.getId());
    sbomMetadata.setFilename(serverFilePath);
    sbomMetadata.setSbomVersion(applicationVersion);
    sbomMetadata.setIsValid(detectionResult.isValid);
    sbomMetadata.setCreatedAt(new Date());
    sbomMetadata.setStatus(ThirdPartySbomMetadataStatus.UPLOADED);
    sbomMetadata.setScanType(scanType.name());

    if (summary != null) {
      sbomMetadata.setSerialNumber(detectionResult.summary.serialNumber);
      sbomMetadata.setSpec(detectionResult.summary.specification);
      sbomMetadata.setSpecFormat(detectionResult.summary.format);
      sbomMetadata.setSpecVersion(detectionResult.summary.version);
      sbomMetadata.setMetadataJson(detectionResult.summary.creationDetails);
    }
    else if (scanType == SbomScanType.BINARY) {
      // defaults for binary scans
      sbomMetadata.setSpec(SbomSpecification.CYCLONEDX.toString());
      sbomMetadata.setSpecFormat(SbomFormat.JSON.toString());
      sbomMetadata.setSpecVersion(ExportSpecification.DEFAULT.getVersion());
      sbomMetadata.setMetadataJson(SbomCycloneDxUtils.getGenericSbomCreationDetailsAsString());
    }

    sbomMetadataDAO.insert(tx, sbomMetadata);

    return new ImmutablePair<>(sbomMetadata, thirdPartyFile);
  }

  /**
   * Within the provided transaction, save the ThirdPartyFile records for the binary or SBOM.
   */
  private ThirdPartyFile saveThirdPartyFileToDB(TransactionContext tx, PersistencePath userPath) {
    var thirdPartyFile = new ThirdPartyFile(userPath.toString(), new Date());
    thirdPartyFileDAO.insert(tx, thirdPartyFile);
    return thirdPartyFile;
  }

  private void deleteSbomDueToException(SbomEntity sbomEntity, Exception e) {
    if (sbomEntity != null) {
      try {
        sbomPersistenceService.deleteSbom(sbomEntity);
      }
      catch (IOException ioe) {
        e.addSuppressed(ioe);
      }
    }
  }

  /**
   * Sleep for a random time between 1 and 5 milliseconds in order to get a new, hopefully unique timestamp.
   * Returns immediately if interrupted.
   */
  private void sleepForNewVersionTimestamp() {
    try {
      Thread.sleep((Double.doubleToLongBits(Math.random()) % MAX_SLEEP_MILLIS_FOR_NEW_VERSION) + 1);
    }
    catch (InterruptedException ie) {
      Thread.interrupted(); // acknowledge and continue
    }
  }

  /**
   * @return the current timestamp in the format yyyyMMddHHmmssSSS, used in various places in this class
   *         for auto-generated and disambiguated version strings
   */
  private String getTimestampForVersion() {
    return dtFormatter.format(LocalDateTime.now());
  }

  /**
   * @return a hopefully unique version string based on the given versionFromSbomFile (if non-null)
   *         and the current timestamp
   */
  private String getNewHopefullyUniqueVersion(Optional<String> versionFromSbomFile) {
    var versionJoiner = new StringJoiner("_");
    versionFromSbomFile.ifPresent(versionJoiner::add);
    versionJoiner.add(getTimestampForVersion());
    return versionJoiner.toString();
  }

  private void deleteSbomFile(ThirdPartySbomMetadata sbomMetadata) throws IOException {
    if (sbomMetadata.getFilename() != null) {
      sbomPersistenceService.deleteSbom(sbomMetadata.getApplicationId(), sbomMetadata.getFilename());
    }
  }
}
