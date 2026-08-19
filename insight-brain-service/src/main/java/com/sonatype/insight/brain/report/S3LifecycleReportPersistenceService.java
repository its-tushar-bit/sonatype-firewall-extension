/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.report;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.api.experimental.ApiVulnerabilitySignatureService;
import com.sonatype.insight.brain.aws.s3.S3OutputStream;
import com.sonatype.insight.brain.aws.s3.S3Utils;
import com.sonatype.insight.brain.report.LifecycleReport.ReportFile;
import com.sonatype.insight.brain.report.LifecycleReport.ReportFileLocationType;
import com.sonatype.insight.brain.service.CopyStorageService;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.config.StorageConfig.S3DataStoreConfig;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Object;

import static com.sonatype.insight.brain.aws.s3.S3ExceptionUtil.wrapS3Exception;
import static java.util.Objects.requireNonNull;

@Named
@Singleton
public class S3LifecycleReportPersistenceService
    extends LifecycleReportPersistenceService
{
  private static final Logger log = LoggerFactory.getLogger(S3LifecycleReportPersistenceService.class);

  private static final String APP_WIDE_BASE_FORMAT = "report/%s/";

  private static final String BASE_FORMAT = APP_WIDE_BASE_FORMAT + "%s/";

  private static final String SPECIAL_FILE_FORMAT = BASE_FORMAT + "%s";

  private static final String ADDITIONAL_KEY_PREFIX = BASE_FORMAT + "additional.files/";

  private static final String ADDITIONAL_KEY_FORMAT = ADDITIONAL_KEY_PREFIX + "%s";

  private static final String KEY_PREFIX = BASE_FORMAT + "report.files/";

  private static final String KEY_FORMAT = KEY_PREFIX + "%s";

  private static final String CACHE_KEY_PREFIX = BASE_FORMAT + "report.cache/";

  private static final String CACHE_KEY_FORMAT = CACHE_KEY_PREFIX + "%s";

  private static final String ZIP_FILENAME = "report.zip";

  private static final String ZIP_KEY_FORMAT = BASE_FORMAT + ZIP_FILENAME;

  private static final String PDF_FILENAME = "report.pdf";

  private static final String VULNERABILITY_SIGNATURE_FILENAME =
      ApiVulnerabilitySignatureService.VULNERABILITY_SIGNATURE_JSON_FILENAME;

  private final S3Client s3Client;

  private final S3AsyncClient s3AsyncClient;

  private final S3DataStoreConfig s3DataStoreConfig;

  private class S3ReportEntity
      implements ReportEntity
  {
    protected final S3ObjectKey key;

    /**
     * Cached metadata for this entity to reduce S3 API calls.
     * <ul>
     * <li>{@code null} - metadata has not been fetched yet</li>
     * <li>{@link Optional#empty()} - entity does not exist in S3</li>
     * <li>{@link Optional#of(Metadata)} - entity exists with the cached metadata</li>
     * </ul>
     */
    private volatile Optional<Metadata> metadata;

    public S3ReportEntity(final S3ObjectKey key) {
      this.key = key;
    }

    /**
     * Constructor that accepts pre-fetched metadata from ListObjectsV2 to avoid additional headObject calls.
     *
     * @param key the S3 object key
     * @param metadata metadata from a previous S3 operation (e.g., ListObjectsV2)
     */
    public S3ReportEntity(final S3ObjectKey key, final Optional<Metadata> metadata) {
      this.key = key;
      this.metadata = metadata;
    }

    @Override
    @WithSpan
    public boolean exists() throws IOException {
      return getS3Metadata().isPresent();
    }

    @Override
    @SuppressWarnings("OptionalAssignedToNull")
    public boolean exists(final MetadataSource source) throws IOException {
      if (source == MetadataSource.CACHED && metadata != null) {
        return metadata.isPresent();
      }
      return exists();
    }

    @Override
    public long getTime() throws IOException {
      return getS3Metadata().orElseThrow(() -> new IOException("File does not exist")).lastModifiedEpochTime();
    }

    @Override
    @SuppressWarnings("OptionalAssignedToNull")
    public long getTime(final MetadataSource source) throws IOException {
      if (source == MetadataSource.CACHED && metadata != null) {
        Long time = metadata.orElseThrow(() -> new IOException("File does not exist")).lastModifiedEpochTime();
        if (time != null) {
          return time;
        }
      }
      return getTime();
    }

    @Override
    public String getName() {
      return key.objectName();
    }

    @Override
    public long length() throws IOException {
      return getS3Metadata().orElseThrow(() -> new IOException("File does not exist")).sizeInBytes();
    }

    @Override
    @SuppressWarnings("OptionalAssignedToNull")
    public long length(final MetadataSource source) throws IOException {
      if (source == MetadataSource.CACHED && metadata != null) {
        Long length = metadata.orElseThrow(() -> new IOException("File does not exist")).sizeInBytes();
        if (length != null) {
          return length;
        }
      }
      return length();
    }

    @Override
    public Optional<Metadata> getMetadata(final MetadataAttribute... metadataAttributes) throws IOException {
      return getS3Metadata();
    }

    @Override
    @SuppressWarnings("OptionalAssignedToNull")
    public Optional<Metadata> getMetadata(
        final MetadataSource source,
        final MetadataAttribute... metadataAttributes) throws IOException
    {
      if (source == MetadataSource.CACHED && metadata != null) {
        return metadata;
      }
      return getMetadata(metadataAttributes);
    }

    @Override
    public void setMetadata(final Optional<Metadata> metadata) {
      this.metadata = metadata;
    }

    @Override
    @WithSpan
    public OutputStream getOutputStream() {
      return new S3OutputStream(
          s3AsyncClient,
          key.toString(),
          s3DataStoreConfig.getBucketName(),
          s3DataStoreConfig.getServerSideEncryption());
    }

    @Override
    @WithSpan
    public InputStream getInputStream() throws IOException {
      GetObjectRequest getObjectRequest =
          GetObjectRequest.builder().bucket(s3DataStoreConfig.getBucketName()).key(key.toString()).build();
      return wrapS3Exception(() -> s3Client.getObject(getObjectRequest));
    }

    @Override
    public Class<? extends LifecycleReportPersistenceService> getLifecycleReportPersistenceServiceClass() {
      return S3LifecycleReportPersistenceService.class;
    }

    protected Optional<Metadata> getS3Metadata() throws IOException {
      try {
        S3Object s3Object = wrapS3Exception(() -> {
          HeadObjectRequest request = HeadObjectRequest.builder()
              .bucket(s3DataStoreConfig.getBucketName())
              .key(key.toString())
              .build();

          HeadObjectResponse response = s3Client.headObject(request);

          return S3Object.builder()
              .size(response.contentLength())
              .lastModified(response.lastModified())
              .build();
        });
        metadata = Optional.of(new Metadata(s3Object.lastModified().toEpochMilli(), s3Object.size()));
        return metadata;
      }
      catch (IOException e) {
        if (e.getCause() instanceof NoSuchKeyException) {
          metadata = Optional.empty();
          return metadata;
        }
        else {
          throw e;
        }
      }
    }
  }

  private class S3PdfEntity
      extends S3ReportEntity
      implements ReportPdfEntity
  {
    public S3PdfEntity(final S3ObjectKey key) {
      super(key);
    }

    @Override
    @WithSpan
    public void deleteIfExists() throws IOException {
      log.debug("Deleting PDF at S3 key '{}'", key);
      deleteByKey(key);
    }
  }

  @Inject
  public S3LifecycleReportPersistenceService(
      @Nullable final S3Client s3Client,
      @Nullable final S3AsyncClient s3AsyncClient,
      final InsightConfig insightConfig)
  {
    this.s3Client = s3Client;
    this.s3AsyncClient = s3AsyncClient;
    this.s3DataStoreConfig = insightConfig.getStorage().getS3Config();
    if (s3DataStoreConfig != null) {
      requireNonNull(s3Client);
      requireNonNull(s3DataStoreConfig.getBucketName());
      requireNonNull(s3DataStoreConfig.getObjectKeyPrefix());
    }
  }

  @Override
  @WithSpan
  protected ReportEntity doGetReportEntity(
      final String ownerId,
      final String scanId,
      final String name) throws IOException
  {
    ReportEntity entity = getAdditionalReportEntity(ownerId, scanId, name);
    ReportFile reportFile = ReportFile.fromName(name);
    boolean isUnknownOrAdditional =
        reportFile == null || reportFile.getLocationTypes().contains(ReportFileLocationType.ADDITIONAL);
    if (isUnknownOrAdditional && entity.exists()) {
      return entity;
    }
    else {
      return getOrCreateCacheReportEntity(ownerId, scanId, name);
    }
  }

  @Override
  @WithSpan
  public Stream<ReportEntity> getAllReportEntities(
      final String ownerId,
      final String scanId) throws IOException
  {
    Set<S3ReportEntity> additionalEntities = getAdditionalEntities(ownerId, scanId);
    Set<String> namesAlreadySeen = new HashSet<>();
    additionalEntities.stream().map(ReportEntity::getName).forEach(namesAlreadySeen::add);
    Set<S3ReportEntity> localEntities = getLocalCopyEntities(ownerId, scanId, namesAlreadySeen);
    localEntities.stream().map(ReportEntity::getName).forEach(namesAlreadySeen::add);

    return Stream.concat(
        additionalEntities.stream(),
        Stream.concat(
            localEntities.stream(),
            getOriginalEntities(ownerId, scanId, namesAlreadySeen)));
  }

  @Override
  @WithSpan
  public void saveOriginalReport(
      final String ownerId,
      final String scanId,
      final InputStream reportZipContents) throws IOException
  {
    if (reportExists(ownerId, scanId)) {
      throw new IOException("Report already exists for ownerId '" + ownerId + "' scanId '" + scanId + "'");
    }

    try (var zipInputStream = new ZipInputStream(reportZipContents)) {
      ZipEntry entry;
      while ((entry = zipInputStream.getNextEntry()) != null) {
        String name = entry.getName();
        if (name.endsWith("/")) {
          continue;
        }

        saveOriginalReportFile(ownerId, scanId, name, zipInputStream);
      }
    }
    catch (IOException e) {
      log.error("Error saving original report files to S3 for ownerId '{}' scanId '{}'",
          ownerId, scanId, e);

      try {
        deleteReport(ownerId, scanId);
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
      final String ownerId,
      final String scanId,
      final Stream<ReportEntity> originalReportEntities) throws IOException
  {
    Iterator<ReportEntity> iterator = originalReportEntities.iterator();
    while (iterator.hasNext()) {
      ReportEntity originalReportEntity = iterator.next();
      try (InputStream inputStream = originalReportEntity.getInputStream()) {
        saveOriginalReportFile(ownerId, scanId, originalReportEntity.getName(), inputStream);
      }
    }
  }

  @Override
  @WithSpan
  protected void doSaveReportFile(
      final String ownerId,
      final String scanId,
      final String name,
      final InputStream contents) throws IOException
  {
    saveReportFile(getCacheKey(ownerId, scanId, name), contents);
  }

  @Override
  @WithSpan
  protected void doSaveAdditionalReportFile(
      final String ownerId,
      final String scanId,
      final String name,
      final InputStream contents) throws IOException
  {
    saveReportFile(getAdditionalObjectKey(ownerId, scanId, name), contents);
  }

  @Override
  public ReportPdfEntity getPdfEntity(final String ownerId, final String scanId) {
    return new S3PdfEntity(new S3ObjectKey(SPECIAL_FILE_FORMAT, ownerId, scanId, PDF_FILENAME,
        s3DataStoreConfig.getObjectKeyPrefix()));
  }

  @Override
  public ReportEntity getVulnerabilitySignaturesEntity(final String ownerId, final String scanId) {
    var key =
        new S3ObjectKey(SPECIAL_FILE_FORMAT, ownerId, scanId, VULNERABILITY_SIGNATURE_FILENAME,
            s3DataStoreConfig.getObjectKeyPrefix());
    return new S3ReportEntity(key);
  }

  @Override
  public String getReportLocation(final String ownerId, final String scanId) {
    var key = new S3ObjectKey(BASE_FORMAT, ownerId, scanId, "", s3DataStoreConfig.getObjectKeyPrefix());

    // There appears to be no actual way in the S3 API to construct this string more safely, so we have to just use
    // string operations
    return "s3://%s/%s".formatted(s3DataStoreConfig.getBucketName(), key.toString());
  }

  @Override
  @WithSpan
  public boolean reportExists(final String ownerId, final String scanId) throws IOException {
    try (var objects = S3Utils.getS3Objects(s3Client, s3DataStoreConfig.getBucketName(),
        s3DataStoreConfig.getObjectKeyPrefix() + String.format(BASE_FORMAT, ownerId, scanId)))
    {
      Set<S3Object> s3Objects = objects.collect(Collectors.toSet());
      if (s3Objects.stream().anyMatch(s3Object -> s3Object.key().endsWith(CopyStorageService.COPY_MARKER))) {
        return false;
      }
      // Do we have at least one object saved for this app and scan which isn't the copy marker
      return !s3Objects.isEmpty();
    }
  }

  @Override
  @WithSpan
  public void deleteReport(final String ownerId, final String scanId) throws IOException {
    log.debug("Deleting report files in S3 for ownerId '{}' scanId '{}'", ownerId, scanId);
    deleteAllWithPrefix(String.format(BASE_FORMAT, ownerId, scanId));
  }

  @Override
  @WithSpan
  public void deleteReports(final String ownerId) throws IOException {
    log.debug("Deleting ALL report files in S3 for ownerId '{}'", ownerId);
    deleteAllWithPrefix(String.format(APP_WIDE_BASE_FORMAT, ownerId));
  }

  @Override
  public Class<? extends ReportEntity> getReportEntityClass() {
    return S3ReportEntity.class;
  }

  @Override
  @WithSpan
  public void deleteReportEntity(final ReportEntity reportEntity) throws IOException {
    S3ReportEntity s3ReportEntity = (S3ReportEntity) reportEntity;
    deleteByKey(s3ReportEntity.key);
  }

  private ReportEntity getOrCreateCacheReportEntity(
      final String ownerId,
      final String scanId,
      final String name) throws IOException
  {
    ReportEntity entity = getCacheReportEntity(ownerId, scanId, name);

    if (!entity.exists()) {
      if (createLocalCopyFromOriginal(ownerId, scanId, name)) {
        // Set the metadata to indicate the entity exists
        entity.setMetadata(Optional.of(new Metadata(null, null)));
      }
    }

    return entity;
  }

  private ReportEntity getAdditionalReportEntity(
      final String ownerId,
      final String scanId,
      final String name)
  {
    return new S3ReportEntity(getAdditionalObjectKey(ownerId, scanId, name));
  }

  private ReportEntity getCacheReportEntity(
      final String ownerId,
      final String scanId,
      final String name)
  {
    return new S3ReportEntity(getCacheKey(ownerId, scanId, name));
  }

  /**
   * Creates a cache copy of the original file. If an original file by this name does not exist, does nothing.
   * If a report.zip file exists, it will be extracted first.
   */
  @WithSpan
  private boolean createLocalCopyFromOriginal(
      final String ownerId,
      final String scanId,
      final String name) throws IOException
  {
    boolean[] copied = {false};

    var request = CopyObjectRequest.builder()
        .sourceBucket(s3DataStoreConfig.getBucketName())
        .sourceKey(getOriginalKey(ownerId, scanId, name).toString())
        .destinationBucket(s3DataStoreConfig.getBucketName())
        .destinationKey(getCacheKey(ownerId, scanId, name).toString())
        .serverSideEncryption(s3DataStoreConfig.getServerSideEncryption())
        .build();

    wrapS3Exception(() -> {
      try {
        s3Client.copyObject(request);
        copied[0] = true;
      }
      catch (NoSuchKeyException e) {
        try {
          S3ObjectKey zipKey = getZipKey(ownerId, scanId);
          String zipKeyString = zipKey.toString();
          if (S3Utils.exists(s3Client, s3DataStoreConfig.getBucketName(), zipKeyString)) {
            log.info("Original file '{}' not found for ownerId '{}' scanId '{}', but report.zip exists. " +
                "Extracting zip file to report.files/", name, ownerId, scanId);
            extractZipFileToS3(ownerId, scanId);
            try {
              s3Client.copyObject(request);
              copied[0] = true;
            }
            catch (NoSuchKeyException ex) {
              log.info("File '{}' not found in extracted contents for ownerId '{}' scanId '{}'",
                  name, ownerId, scanId);
            }
            deleteByKey(zipKey);
            log.info("Deleted report.zip for ownerId '{}' scanId '{}'", ownerId, scanId);
          }
        }
        catch (IOException ex) {
          throw new RuntimeException(ex);
        }
      }
    });
    return copied[0];
  }

  /**
   * Extracts a report.zip file from S3 to the report.files/ folder in S3.
   * The zip file is preserved for transactional safety - caller is responsible for deletion after success.
   */
  @WithSpan
  private void extractZipFileToS3(final String ownerId, final String scanId) throws IOException {
    S3ObjectKey zipKey = getZipKey(ownerId, scanId);
    log.info("Starting extraction of report.zip for ownerId '{}' scanId '{}'", ownerId, scanId);

    GetObjectRequest getRequest = GetObjectRequest.builder()
        .bucket(s3DataStoreConfig.getBucketName())
        .key(zipKey.toString())
        .build();

    try (InputStream zipInputStream = wrapS3Exception(() -> s3Client.getObject(getRequest));
        ZipInputStream zis = new ZipInputStream(zipInputStream))
    {

      ZipEntry entry;
      while ((entry = zis.getNextEntry()) != null) {
        String entryName = entry.getName();
        if (entryName.endsWith("/")) {
          continue;
        }

        log.debug("Extracting file '{}' from report.zip for ownerId '{}' scanId '{}'",
            entryName, ownerId, scanId);
        saveOriginalReportFile(ownerId, scanId, entryName, zis);
      }

      log.info("Successfully extracted report.zip for ownerId '{}' scanId '{}'", ownerId, scanId);
    }
    catch (IOException e) {
      log.error("Error extracting report.zip for ownerId '{}' scanId '{}'. " +
          "Zip file will remain in S3 for retry.", ownerId, scanId, e);
      throw e;
    }
  }

  private void deleteByKey(S3ObjectKey key) throws IOException {
    var request = DeleteObjectRequest.builder().bucket(s3DataStoreConfig.getBucketName()).key(key.toString()).build();
    wrapS3Exception(() -> s3Client.deleteObject(request));
  }

  private void deleteAllWithPrefix(final String keySubPrefix) throws IOException {
    S3Utils.deleteAllWithPrefix(s3Client, s3DataStoreConfig.getBucketName(),
        s3DataStoreConfig.getObjectKeyPrefix() + keySubPrefix);
  }

  private void saveOriginalReportFile(
      final String ownerId,
      final String scanId,
      final String name,
      final InputStream contents) throws IOException
  {
    saveReportFile(getOriginalKey(ownerId, scanId, name), contents);
  }

  @Override
  @WithSpan
  public void moveReport(
      final String appId,
      final String sourceScanId,
      final String destinationScanId) throws IOException
  {
    deleteReport(appId, destinationScanId);
    Set<S3Object> s3Objects;
    String sourceBasePrefix = s3DataStoreConfig.getObjectKeyPrefix() + String.format(BASE_FORMAT, appId, sourceScanId);
    try (var objects = S3Utils.getS3Objects(s3Client, s3DataStoreConfig.getBucketName(), sourceBasePrefix)) {
      s3Objects = objects.collect(Collectors.toSet());
    }
    String targetBasePrefix =
        s3DataStoreConfig.getObjectKeyPrefix() + String.format(BASE_FORMAT, appId, destinationScanId);
    for (S3Object s3Object : s3Objects) {
      String sourceKey = s3Object.key();
      String targetKey = targetBasePrefix + sourceKey.substring(sourceBasePrefix.length());
      CopyObjectRequest copyRequest = CopyObjectRequest.builder()
          .sourceBucket(s3DataStoreConfig.getBucketName())
          .sourceKey(sourceKey)
          .destinationBucket(s3DataStoreConfig.getBucketName())
          .destinationKey(targetKey)
          .serverSideEncryption(s3DataStoreConfig.getServerSideEncryption())
          .build();
      wrapS3Exception(() -> s3Client.copyObject(copyRequest));
    }
    deleteReport(appId, sourceScanId);
  }

  private void saveReportFile(final S3ObjectKey key, final InputStream contents) throws IOException {
    try (OutputStream outputStream = new S3ReportEntity(key).getOutputStream()) {
      log.debug("Saving report file to S3: {}", key);
      contents.transferTo(outputStream);
    }
  }

  private Set<S3ReportEntity> getAdditionalEntities(
      final String ownerId,
      final String scanId) throws IOException
  {
    String prefix = s3DataStoreConfig.getObjectKeyPrefix() + ADDITIONAL_KEY_PREFIX.formatted(ownerId, scanId);
    Function<String, S3ObjectKey> keyParser =
        key -> getAdditionalObjectKey(ownerId, scanId, StringUtils.removeStart(key, prefix));

    try (Stream<S3ReportEntity> entities = getEntities(prefix, keyParser)) {
      return entities.collect(Collectors.toSet());
    }
  }

  private Set<S3ReportEntity> getLocalCopyEntities(
      final String ownerId,
      final String scanId,
      final Set<String> excludeNames) throws IOException
  {
    String prefix = s3DataStoreConfig.getObjectKeyPrefix() + CACHE_KEY_PREFIX.formatted(ownerId, scanId);
    Function<String, S3ObjectKey> keyParser =
        key -> getCacheKey(ownerId, scanId, StringUtils.removeStart(key, prefix));

    try (Stream<S3ReportEntity> entities = getEntities(prefix, keyParser)) {
      return entities
          .filter(entity -> !excludeNames.contains(entity.getName()))
          .collect(Collectors.toSet());
    }
  }

  @Override
  @WithSpan
  public Stream<ReportEntity> getOriginalReportEntities(
      final String ownerId,
      final String scanId) throws IOException
  {
    Stream<S3ReportEntity> originalEntities = getOriginalEntities(ownerId, scanId, Collections.emptySet());
    return originalEntities.map(Function.identity());
  }

  private Stream<S3ReportEntity> getOriginalEntities(
      final String ownerId,
      final String scanId,
      final Set<String> excludeNames) throws IOException
  {
    String prefix = s3DataStoreConfig.getObjectKeyPrefix() + KEY_PREFIX.formatted(ownerId, scanId);
    return getEntities(
        prefix,
        key -> getOriginalKey(ownerId, scanId, StringUtils.removeStart(key, prefix)))
            .filter(entity -> !excludeNames.contains(entity.getName()));
  }

  /**
   * @return a Stream of S3ReportEntity objects for all objects in the specified keySubPrefix under the keyPrefix
   *         instance variable.
   *
   * @param keyParser a function that takes a full S3 key string starting with the prefix and returns a parsed
   *          S3ObjectKey
   */
  private Stream<S3ReportEntity> getEntities(
      final String prefix,
      final Function<String, S3ObjectKey> keyParser) throws IOException
  {
    return S3Utils.getS3Objects(s3Client, s3DataStoreConfig.getBucketName(), prefix)
        .map(s3Object -> {
          // Extract metadata from ListObjectsV2 response to avoid additional headObject calls
          Metadata metadata = new Metadata(
              s3Object.lastModified().toEpochMilli(),
              s3Object.size());
          return new S3ReportEntity(keyParser.apply(s3Object.key()), Optional.of(metadata));
        });
  }

  /**
   * @return a key referring to the specified object within the original report files downloaded from HDS
   */
  private S3ObjectKey getOriginalKey(final String ownerId, final String scanId, final String objectName) {
    return new S3ObjectKey(KEY_FORMAT, ownerId, scanId, objectName, s3DataStoreConfig.getObjectKeyPrefix());
  }

  /**
   * @return a key referring to the report.zip file
   */
  private S3ObjectKey getZipKey(final String ownerId, final String scanId) {
    return new S3ObjectKey(ZIP_KEY_FORMAT, ownerId, scanId, "", s3DataStoreConfig.getObjectKeyPrefix());
  }

  /**
   * @return a key referring to the modified/modifiable/restorable copy of the specified object
   */
  private S3ObjectKey getCacheKey(
      final String ownerId,
      final String scanId,
      final String objectName)
  {
    return new S3ObjectKey(CACHE_KEY_FORMAT, ownerId, scanId, objectName, s3DataStoreConfig.getObjectKeyPrefix());
  }

  /**
   * @return a key referring to the specified "additional" object
   */
  private S3ObjectKey getAdditionalObjectKey(
      final String ownerId,
      final String scanId,
      final String objectName)
  {
    return new S3ObjectKey(ADDITIONAL_KEY_FORMAT, ownerId, scanId, objectName,
        s3DataStoreConfig.getObjectKeyPrefix());
  }
}
