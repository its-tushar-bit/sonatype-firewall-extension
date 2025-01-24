/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Date;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyScanDAO;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadataStatus;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyScan;
import com.sonatype.insight.brain.sbom.SbomSpecification;
import com.sonatype.insight.brain.sbom.export.SbomExportParams.ExportSpecification;
import com.sonatype.insight.brain.sbom.utils.SbomCycloneDxUtils;
import com.sonatype.insight.brain.sbom.utils.SbomDetectionResult;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.utils.AutoDeletingTempFile;
import com.sonatype.insight.brain.utils.CheckedIllegalArgumentException;
import com.sonatype.insight.brain.utils.FunctionWithException;
import com.sonatype.insight.brain.utils.SupplierWithException;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.InternalServerException;
import com.sonatype.insight.scan.file.SbomFormat;

import com.google.common.collect.Streams;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.RollbackException;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.AgeFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.codehaus.plexus.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

  private final InsightWork insightWork;

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
      private final Path sanitizedPath;

      private SanitizedSbomScanPath(String scanPath) throws CheckedIllegalArgumentException {
        this.sanitizedPath = sanitizeUserPath(Path.of(scanPath));
      }

      @Override
      public String toString() {
        return sanitizedPath.toString();
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
    }

    /**
     * AutoDeletingTempFile instances whose paths are generated safely from sanitized user input and which can be
     * trusted
     */
    public static final class TrustedAutoDeletingTempPath
        extends AutoDeletingTempFile
        implements ExtensionSafePath
    {
      private TrustedAutoDeletingTempPath(Path dir, ExtensionSafePath userPath) throws IOException {
        this(dir, userPath.getExtension());
      }

      // This is a separate constructor just to make the expression within the super call more readable
      private TrustedAutoDeletingTempPath(Path dir, String extension) throws IOException {
        super(dir, extension.isBlank() ? null : extension);
      }
    }
  }

  @Inject
  public ThirdPartyPersistenceService(
      final ThirdPartySbomMetadataDAO sbomMetadataDAO,
      final ThirdPartyFileDAO thirdPartyFileDAO,
      final ThirdPartyScanDAO thirdPartyScanDAO,
      final InsightWork insightWork)
  {
    this.sbomMetadataDAO = sbomMetadataDAO;
    this.thirdPartyFileDAO = thirdPartyFileDAO;
    this.thirdPartyScanDAO = thirdPartyScanDAO;
    this.insightWork = insightWork;
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
   * directories will be ignored).
   *
   * @throws CheckedIllegalArgumentException if the basename of userFilename is empty, `.`, or `..`
   */
  public ImmutablePair<ThirdPartySbomMetadata, ThirdPartyFile> saveSbomManagerSbomOrBinary(
      PersistencePath.TrustedAutoDeletingTempPath sbomPath,
      String userFilename,
      String applicationId,
      SbomDetectionResult detectionResult) throws IOException, CheckedIllegalArgumentException
  {
    return saveSbomManagerSbomOrBinaryFromUserUpload(
        sbomPath,
        new PersistencePath.SanitizedUserPath(userFilename),
        applicationId,
        null,
        detectionResult
    );
  }

  /**
   * Save the SBOM file to the database and disk storage. Similar to the method above, except that preferredVersion will
   * be used as the version if supplied, falling back to the version from the file and then the timestamp if necessary.
   *
   * @param userFilename The filename/path provided by the user. Only the basename on this path will be considered (any
   * directories will be ignored).
   *
   * @throws CheckedIllegalArgumentException if the basename of userFilename is empty, `.`, or `..`
   */
  public ImmutablePair<ThirdPartySbomMetadata, ThirdPartyFile> saveSbomManagerSbomOrBinary(
      PersistencePath.TrustedAutoDeletingTempPath sbomPath,
      String userFilename,
      String applicationId,
      String preferredVersion,
      SbomDetectionResult detectionResult) throws IOException, CheckedIllegalArgumentException
  {
    return saveSbomManagerSbomOrBinaryFromUserUpload(
        sbomPath,
        new PersistencePath.SanitizedUserPath(userFilename),
        applicationId,
        preferredVersion,
        detectionResult
    );
  }

  /**
   * Save the SBOM file to the database and disk storage. Similar to the method above, except the SBOM contents are
   * provided as a String instead of a Path. This method stores the basename of the "path" value from the scan.xml,
   * which must be a valid, safe file path.
   *
   * @throws CheckedIllegalArgumentException if the basename of scanPath is empty, `.`, or `..`
   */
  public ImmutablePair<ThirdPartySbomMetadata, ThirdPartyFile> saveSbomManagerSbomFromScan(
      String sbomContents,
      String scanPath,
      String applicationId,
      SbomDetectionResult detectionResult) throws IOException, CheckedIllegalArgumentException
  {
    if (!detectionResult.isSbom) {
      throw new IllegalArgumentException("sbomContents must represent an SBOM, not a binary");
    }

    return saveSbomManagerSbom(
        () -> IOUtils.toInputStream(sbomContents, Charset.defaultCharset()),
        new PersistencePath.SanitizedSbomScanPath(scanPath),
        applicationId,
        null,
        detectionResult
    );
  }

  /**
   * Save information about the binary to the database.  The binary contents are NOT saved to a persistent temporary
   * location as this method is intended to be called during processing of an already-existing scan file, in which there
   * is no need for such temporary storage. This method stores the full "path" value from the scan.xml without sanity
   * checking that it is a safe file path. No actual files are saved using any part of that value.
   */
  public ImmutablePair<ThirdPartySbomMetadata, ThirdPartyFile> saveSbomManagerBinaryFromScan(
      String sbomContents,
      String scanPath,
      String applicationId,
      SbomDetectionResult detectionResult) throws IOException
  {
    if (detectionResult.isSbom) {
      throw new IllegalArgumentException("sbomContents must represent a binary, not an SBOM");
    }

    return saveSbomManagerBinaryFromScan(
        new PersistencePath.UnsafePath(scanPath),
        applicationId,
        null,
        detectionResult
    );
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
   * @throws IllegalArgumentException if sbomMetadata does not have an associated persisted SBOM file (i.e. if
   * sbomMetadata.getFilename() is null)
   */
  public InputStream getSbomContentsInputStream(
      ThirdPartySbomMetadata sbomMetadata) throws IOException, IllegalArgumentException
  {
    if (sbomMetadata.getFilename() != null) {
      var sbomPath = getSbomPermanentStoragePath(sbomMetadata);
      var fileInputStream = Files.newInputStream(sbomPath);
      return new GzipCompressorInputStream(fileInputStream);
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
   * @return an AutoDeletingTempFile representing the written file, which should be used within try-with-resources
   * in order to delete when no longer needed
   */
  public PersistencePath.TrustedAutoDeletingTempPath writeToTransientStorage(
      InputStream sbomStream,
      String userFilename) throws IOException, CheckedIllegalArgumentException
  {
    var sanitizedPath = new PersistencePath.SanitizedUserPath(userFilename);
    var tempFile = new PersistencePath.TrustedAutoDeletingTempPath(
        insightWork.getSbomTransientDir().toPath(),
        sanitizedPath
    );

    try (var outputStream = Files.newOutputStream(tempFile.getPath())) {
      IOUtils.copy(sbomStream, outputStream);
    }
    catch (IOException e) {
      try {
        tempFile.close();
      }
      catch (IOException e2) {
        e.addSuppressed(e2);
      }

      throw e;
    }

    log.debug("Uploaded SBOM or binary saved to transient storage at {}", tempFile.getPath());

    return tempFile;
  }

  /**
   * @return the path to the persistent temporary binary file associated with the given sbomMetadata and thirdPartyFile.
   *
   * External callers should only read the file at this path and not modify or delete it.  The actual directory
   * structure of the path should also be treated as an implementation detail of this class and not inspected by
   * external callers.
   *
   * Implementation notes:
   * Persistent temp files for binaries are stored in a directory named using the ThirdPartySbomMetadata id, with a
   * filename matching their original filename as recorded in the ThirdPartyFile. Using the original name for the file
   * is important for enabling the insight-scanner code to process it correctly.
   *
   * @throws IllegalArgumentException if the thirdPartyFile does not match the sbomMetadata or does not contain
   * a sanitizable filename
   */
  public Path getBinaryPersistentTempFilePath(ThirdPartySbomMetadata sbomMetadata, ThirdPartyFile thirdPartyFile) {
    if (!sbomMetadata.getThirdPartyFileId().equals(thirdPartyFile.getId())) {
      throw new IllegalArgumentException(
          "ThirdPartyFile does not match ThirdPartySbomMetadata: %s vs %s"
              .formatted(sbomMetadata.getThirdPartyFileId(), thirdPartyFile.getId())
      );
    }

    PersistencePath.SanitizedUserPath sanitizedPath;
    try {
      sanitizedPath = new PersistencePath.SanitizedUserPath(thirdPartyFile.getFilename());
    }
    catch (CheckedIllegalArgumentException e) {
      throw new IllegalArgumentException(
          "Peristent temp file path cannot be constructed for unsafe path " + thirdPartyFile.getFilename(), e);
    }

    return getBinaryPersistentTempFilePath(sbomMetadata, sanitizedPath);
  }

  /**
   * Deletes the persistent temporary binary file associated with the given sbomMetadata.
   */
  public void deletePersistentTempBinary(
      ThirdPartySbomMetadata sbomMetadata,
      ThirdPartyFile thirdPartyFile) throws IOException
  {
    var filePath = getBinaryPersistentTempFilePath(sbomMetadata, thirdPartyFile);

    Files.deleteIfExists(filePath);

    // directory is unique to this sbomMetadata and should contain no other files
    Files.deleteIfExists(filePath.getParent());
    log.debug("Deleted persistent temporary binary file and directory for SBOM {} at {}",
        sbomMetadata.getId(), filePath);
  }

  /**
   * Set the status on the ThirdPartySbomMetadata with the given id to PENDING. Atomically checks that it was currently
   * set to UPLOADED in the database and throws an exception if not
   */
  public void setSbomMetadataStatusToPending(ThirdPartySbomMetadata sbomMetadata) {
    try (var tx = sbomMetadataDAO.createTransactionContext()) {
      tx.begin();

      var existing = sbomMetadataDAO.getByIdForUpdate(tx, sbomMetadata.getId());

      if (existing.getStatus() != ThirdPartySbomMetadataStatus.UPLOADED) {
        throw new IllegalStateException(
            "SBOM %s is not in the UPLOADED state, is in %s".formatted(sbomMetadata.getId(), existing.getStatus()));
      }

      sbomMetadata.setStatus(ThirdPartySbomMetadataStatus.PENDING);

      // this will merge the changes from sbomMetadata into `existing` and persist them to the DB
      sbomMetadataDAO.update(tx, sbomMetadata);
      tx.commit();
    }
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

    Path compressedSbomPath = null;
    try (var tx = sbomMetadataDAO.createTransactionContext()) {
      tx.begin();

      // We need to do this to seamlessly integrate into our SBOM exporter logic
      sbomMetadata.setFilename(compressedBinaryFileName);
      sbomMetadataDAO.update(tx, sbomMetadata);

      compressedSbomPath = getSbomPermanentStoragePath(sbomMetadata);

      try (var fileStream = Files.newOutputStream(compressedSbomPath, StandardOpenOption.CREATE_NEW);
          var outputStream = new GzipCompressorOutputStream(fileStream)) {
        IOUtils.copy(sbomStream, outputStream);
      }

      tx.commit();
    }
    catch (Exception e) {
      deleteFileDueToException(compressedSbomPath, e);
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
    try (TransactionContext tx = thirdPartyFileDAO.createTransactionContext()) {
      tx.begin();
      ThirdPartyFile thirdPartyFile = thirdPartyFileDAO.getById(sbomMetadata.getThirdPartyFileId());
      // Deleting the corresponding ThirdPartyFile will cascade to the SBOM metadata and all other child records
      thirdPartyFileDAO.delete(tx, thirdPartyFile);
      deleteSbomFile(sbomMetadata);
      if (SbomScanType.BINARY.name().equals(sbomMetadata.getScanType())) {
        deletePersistentTempBinary(sbomMetadata, thirdPartyFile);
      }
      tx.commit();
    }
  }

  /**
   * Attempts to delete all SBOM temporary transient files that are older than the given date. 
   * If it fails to delete one file, it will still attempt to delete the other files.
   * Any failures are logged but IOExceptions are not thrown by this method.
   */
  public void tryDeleteSbomTemporaryTransientFilesOlderThan(Date date) {
    Collection<File> files = FileUtils.listFiles(
        insightWork.getSbomTransientDir(),
        new AgeFileFilter(date),
        TrueFileFilter.INSTANCE
    );
    for (File file : files) {
      tryDeleteFileIfExists(file.toPath());
    }
  }

  /**
   * Save the SBOM file or binary to the database and disk storage. SBOM files are saved to permanent storage, while
   * binary files are saved to a persistent temporary location where they can be retrieved later for scanning.
   */
  private ImmutablePair<ThirdPartySbomMetadata, ThirdPartyFile> saveSbomManagerSbomOrBinaryFromUserUpload(
      PersistencePath.TrustedAutoDeletingTempPath sbomPath,
      PersistencePath.SanitizedUserPath userPath,
      String applicationId,
      String preferredVersion,
      SbomDetectionResult detectionResult) throws IOException
  {
    if (detectionResult.isSbom) {
      return saveSbomManagerSbom(
          () -> Files.newInputStream(sbomPath.getPath()),
          userPath,
          applicationId,
          preferredVersion,
          detectionResult
      );
    }
    else {
      return saveSbomManagerBinaryFromUserUpload(
          sbomPath,
          userPath,
          applicationId,
          preferredVersion,
          detectionResult
      );
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
    return trySaveInLoop(userPreferredVersion, detectionResult, (versionToSave) -> {
      return saveSbomWithVersion(
          sbomContentStreamSupplier,
          userPath,
          applicationId,
          versionToSave,
          detectionResult
      );
    });
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
    return trySaveInLoop(userPreferredVersion, detectionResult, (versionToSave) -> {
      return saveSbomWithVersion(
          sbomContentStreamSupplier,
          userPath,
          applicationId,
          versionToSave,
          detectionResult
      );
    });
  }

  /**
   * Save the binary (with retry logic) from user upload only. This saves the DB records and saves the binary itself
   * to a persistent temp file.
   */
  private ImmutablePair<ThirdPartySbomMetadata, ThirdPartyFile> saveSbomManagerBinaryFromUserUpload(
      PersistencePath.TrustedAutoDeletingTempPath binaryPath,
      PersistencePath.SanitizedUserPath userPath,
      String applicationId,
      String userPreferredVersion,
      SbomDetectionResult detectionResult) throws IOException
  {
    return trySaveInLoop(userPreferredVersion, detectionResult, (versionToSave) -> saveBinaryWithVersion(
        binaryPath,
        userPath,
        applicationId,
        versionToSave,
        detectionResult
    ));
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
        detectionResult
    ));
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
      FunctionWithException<String, ImmutablePair<ThirdPartySbomMetadata, ThirdPartyFile>, IOException> saveFunction)
      throws IOException
  {
    //Try to save using the user's preferred SBOM version if any, otherwise the version detected in the SBOM if any
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
      catch (EntityExistsException | RollbackException e) {
        if (e instanceof EntityExistsException || e.getCause() instanceof EntityExistsException) {
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
    Path sbomPath = null;
    try (var tx = sbomMetadataDAO.createTransactionContext()) {
      tx.begin();

      sbomPath = writeSbomToPermanentStorage(sbomContentStreamSupplier, applicationId, userPath);

      var retval =
          saveSbomMetadataToDB(
              tx,
              userPath,
              sbomPath.getFileName(),
              applicationId,
              applicationVersion,
              SbomScanType.SBOM,
              detectionResult
          );

      tx.commit();
      return retval;
    }
    catch (Exception e) {
      deleteFileDueToException(sbomPath, e);
      throw e;
    }
  }

  /**
   * Save the ThirdPartySbomMetadata and ThirdPartyFile records for a binary scan coming from a user upload, and save
   * the binary itself to disk in persistent temp storage.
   */
  private ImmutablePair<ThirdPartySbomMetadata, ThirdPartyFile> saveBinaryWithVersion(
      PersistencePath.TrustedAutoDeletingTempPath binaryPath,
      PersistencePath.SanitizedUserPath userPath,
      String applicationId,
      String applicationVersion,
      SbomDetectionResult detectionResult) throws IOException
  {
    Path persistentTempStoragePath = null;
    try (var tx = sbomMetadataDAO.createTransactionContext()) {
      tx.begin();

      var retval = saveBinaryMetadataWithVersion(
          tx,
          userPath,
          applicationId,
          applicationVersion,
          detectionResult
      );
      var sbomMetadata = retval.getLeft();

      persistentTempStoragePath = writeBinaryToPersistentTempStorage(
          binaryPath,
          sbomMetadata,
          userPath
      );

      tx.commit();
      return retval;
    }
    catch (Exception e) {
      deleteFileDueToException(persistentTempStoragePath, e);
      if (persistentTempStoragePath != null) {
        deleteFileDueToException(persistentTempStoragePath.getParent(), e);
      }
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
          detectionResult
      );

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
        detectionResult
    );

    var sbomMetadata = retval.getLeft();
    sbomMetadata.setOriginalBinaryFileName(userPath.toString());
    sbomMetadataDAO.update(tx, sbomMetadata);

    return retval;
  }

  /**
   * Save the SBOM contents to disk, gzipped, in the permanent storage directory
   */
  private Path writeSbomToPermanentStorage(
      SupplierWithException<InputStream, IOException> sbomContentStreamSupplier,
      String applicationId,
      PersistencePath.ExtensionSafePath userPath) throws IOException
  {
    Path sbomPath = null;
    try {
      File sbomDir = insightWork.getSbomDir(applicationId);
      Files.createDirectories(sbomDir.toPath().normalize());

      String randomUUID = UUID.randomUUID().toString().replace("-", "");
      String extension = userPath.getExtension();
      String basename = randomUUID + (extension == null ? "" : "." + extension) + ".gz";
      sbomPath = sbomDir.toPath().normalize().resolve(basename);

      try (InputStream sbomStream = sbomContentStreamSupplier.get();
          OutputStream fileOutputStream = Files.newOutputStream(sbomPath);
          GzipCompressorOutputStream outputStream = new GzipCompressorOutputStream(fileOutputStream)) {
        IOUtils.copy(sbomStream, outputStream);
      }

      return sbomPath;
    }
    catch (IOException e) {
      deleteFileDueToException(sbomPath, e);
      throw e;
    }
  }

  /**
   * Save the SBOM contents to disk, raw, in the persistent temp storage directory
   */
  private Path writeBinaryToPersistentTempStorage(
      PersistencePath.TrustedAutoDeletingTempPath binaryTransientPath,
      ThirdPartySbomMetadata sbomMetadata,
      PersistencePath.SanitizedUserPath userPath) throws IOException
  {
    Path dest = getBinaryPersistentTempFilePath(sbomMetadata, userPath);
    Path binaryPath = binaryTransientPath.getPath();

    try {
      Files.createDirectories(dest.getParent());

      // For efficiency, try to make a hard link first, and fall back to copying if that fails.
      // Hard links are generally only supported on UNIX (Windows can also do them but requires admin privileges)
      boolean hardLinked = false;
      try {
        Files.createLink(dest, binaryPath);
        hardLinked = true;
      }
      catch (Exception e) {
        log.trace("Failed to create hard link from {} to {}", binaryPath, dest, e);
        // fall through to copy
      }

      if (!hardLinked) {
        Files.copy(binaryPath, dest);
      }

      log.debug("Saved binary file for SBOM {} to {}", sbomMetadata.getId(), dest);
    }
    catch (IOException e) {
      deleteFileDueToException(dest, e);
      throw e;
    }

    return dest;
  }

  /**
   * Within the provided transaction, save the ThirdPartySbomMetadata and ThirdPartyFile records for the binary or SBOM.
   */
  private ImmutablePair<ThirdPartySbomMetadata, ThirdPartyFile> saveSbomMetadataToDB(
      TransactionContext tx,
      PersistencePath userPath,
      Path serverFilePath,
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
    sbomMetadata.setFilename(serverFilePath == null ? null : serverFilePath.toString());
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

  private void deleteFileDueToException(Path path, Exception e) {
    if (path != null) {
      try {
        Files.deleteIfExists(path);
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
   * for auto-generated and disambiguated version strings
   */
  private String getTimestampForVersion() {
    return dtFormatter.format(LocalDateTime.now());
  }

  /**
   * @return a hopefully unique version string based on the given versionFromSbomFile (if non-null)
   * and the current timestamp
   */
  private String getNewHopefullyUniqueVersion(Optional<String> versionFromSbomFile) {
    var versionJoiner = new StringJoiner("_");
    versionFromSbomFile.ifPresent(versionJoiner::add);
    versionJoiner.add(getTimestampForVersion());
    return versionJoiner.toString();
  }

  /**
   * @return the basename from the given path, ensuring it is not empty, `.`, or `..`
   */
  private static Path getSafeFilename(Path userFilePath) throws CheckedIllegalArgumentException {
    Path filenamePath = userFilePath.getFileName();
    if (filenamePath.equals(Path.of("")) || filenamePath.equals(Path.of(".")) || filenamePath.equals(Path.of(".."))) {
      throw new CheckedIllegalArgumentException("Invalid filename: " + userFilePath);
    }
    else {
      return filenamePath;
    }
  }

  /**
   * Sanitize the file path by normalizing it and ensuring the result is not empty or `.` and does not contain a `..`
   * segment
   */
  private static Path sanitizeUserPath(Path userFilePath) throws CheckedIllegalArgumentException {
    var normalizedPath = userFilePath.normalize();

    if (normalizedPath.toString().isEmpty() || Path.of(".").equals(normalizedPath)
        || Streams.stream(normalizedPath.iterator()).anyMatch(Path.of("..")::equals)) {
      throw new CheckedIllegalArgumentException("Invalid filename: %s".formatted(userFilePath));
    }

    return normalizedPath;
  }

  private Path getSbomPermanentStoragePath(ThirdPartySbomMetadata sbomMetadata) {
    return insightWork.getSbomDir(sbomMetadata.getApplicationId()).toPath().resolve(sbomMetadata.getFilename());
  }

  private Path getBinaryPersistentTempFilePath(
      ThirdPartySbomMetadata sbomMetadata,
      PersistencePath.SanitizedUserPath userPath)
  {
    return insightWork.getSbomPersistentTempDir().toPath()
        .resolve(sbomMetadata.getId())
        .resolve(userPath.toString());
  }

  private void deleteSbomFile(ThirdPartySbomMetadata sbomMetadata) throws IOException {
    if (sbomMetadata.getFilename() != null) {
      Files.deleteIfExists(getSbomPermanentStoragePath(sbomMetadata));
    }
  }

  private void tryDeleteFileIfExists(Path path) {
    try {
      Files.deleteIfExists(path);
    }
    catch (IOException e) {
      log.error("Failed to delete file {}.", path, e);
    }
  }
}
