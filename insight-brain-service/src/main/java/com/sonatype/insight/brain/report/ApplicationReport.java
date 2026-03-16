/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.report;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.tenancy.TenantAwareSupplier;
import com.sonatype.insight.brain.tenancy.TenantReference;
import com.sonatype.insight.brain.tenancy.TenantThreadPoolExecutor;
import com.sonatype.insight.brain.thirdparty.ThirdPartyApplicationReportDTO;
import com.sonatype.insight.brain.utils.DefaultExecutorThreadPools;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.databind.node.ContainerNode;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.commons.io.input.CharSequenceInputStream;

import static com.sonatype.insight.brain.report.ApplicationReport.ReportFile.INDEX_HTML;
import static com.sonatype.insight.brain.report.ApplicationReport.ReportFile.LICENSES_JSON;
import static com.sonatype.insight.brain.report.ApplicationReport.ReportFile.SECURITY_JSON;
import static com.sonatype.insight.brain.report.ApplicationReport.ReportFile.TEMPLATE_PROPERTIES;
import static com.sonatype.insight.brain.report.ApplicationReport.ReportFile.THIRD_PARTY_BOM_JSON;
import static com.sonatype.insight.brain.report.ApplicationReport.ReportFile.THIRD_PARTY_LICENSE_JSON;
import static com.sonatype.insight.brain.report.ApplicationReport.ReportFile.THIRD_PARTY_SECURITY_JSON;
import static com.sonatype.insight.brain.report.ApplicationReport.ReportFileLocationType.ADDITIONAL;
import static com.sonatype.insight.brain.report.ApplicationReport.ReportFileLocationType.CACHE;
import static com.sonatype.insight.brain.report.ApplicationReport.ReportFileLocationType.ORIGINAL;

public class ApplicationReport
{
  private static final int REPORT_ENTITY_LOADING_THREADS_MIN = 1;

  private static final int REPORT_ENTITY_LOADING_THREADS_MAX = Integer.MAX_VALUE;

  private static final int REPORT_ENTITY_LOADING_THREADS_DEFAULT = 5;

  private static final String REPORT_ENTITY_LOADING_THREADS = "reportEntityLoadingThreads";

  private static final TenantReference<TenantThreadPoolExecutor> reportEntityLoadingExecutors =
      new TenantReference<>(() -> {
        int reportEntityLoadingThreadCount = DefaultExecutorThreadPools.getThreadCount(
            REPORT_ENTITY_LOADING_THREADS_MIN,
            REPORT_ENTITY_LOADING_THREADS_MAX,
            REPORT_ENTITY_LOADING_THREADS_DEFAULT,
            REPORT_ENTITY_LOADING_THREADS);
        TenantThreadPoolExecutor tenantThreadPoolExecutor = new TenantThreadPoolExecutor(
            reportEntityLoadingThreadCount,
            reportEntityLoadingThreadCount,
            5L,
            TimeUnit.SECONDS,
            new SynchronousQueue<>(),
            new ThreadFactoryBuilder().setNameFormat("ReportEntityLoading-%d").build(),
            new ThreadPoolExecutor.CallerRunsPolicy(),
            "report_entity_loading",
            ApplicationReport.class.getSimpleName());
        tenantThreadPoolExecutor.allowCoreThreadTimeOut(true);
        return tenantThreadPoolExecutor;
      });

  public enum ReportType
  {
    FULL,
    ERROR
  }

  public enum ReportFileLocationType
  {
    /**
     * Files that are part of the original report.zip
     */
    ORIGINAL,

    /**
     * Files that are created/copied to the cache
     */
    CACHE,

    /**
     * Files that are created and considered as additional
     */
    ADDITIONAL
  }

  /**
   * See also com.sonatype.insight.scan.application.ApplicationReportGenerator.
   * As-is we copy any original file requested to the cache
   * see FileApplicationReportPersistenceService.getOrCreateLocalCopyReportEntity
   * and S3ApplicationReportPersistenceService.getOrCreateCacheReportEntity.
   * This is not an exhaustive list as some files with dynamic names can be added to the report.zip e.g.
   * <a href=
   * "https://github.com/sonatype/hosted-data-services/blob/926c58ea6000cb08b7a5309cf44e7878c529ce62/insight-scan-processor/src/main/java/com/sonatype/insight/scan/application/ApplicationReportGenerator.java#L389">here</a>
   */
  public enum ReportFile
  {
    BOM_JSON("bom.json", Set.of(ORIGINAL, CACHE)),
    COMPONENTS_JSON("components.json", Set.of(ORIGINAL, CACHE)),
    DATA_JSON("data.json", Set.of(ORIGINAL, CACHE)),
    DEPENDENCIES_JSON("dependencies.json", Set.of(ORIGINAL, CACHE)),
    INDEX_HTML("index.html", Set.of(ORIGINAL, CACHE)),
    LICENSES_JSON("licenses.json", Set.of(ORIGINAL, CACHE)),
    LICENSE_THREATS_JSON("licensethreats.json", Set.of(ORIGINAL, CACHE)),
    PARTIAL_MATCHED_JSON("partialmatched.json", Set.of(ORIGINAL, CACHE)),
    POPULARITY_JSON("popularity.json", Set.of(ORIGINAL, CACHE)),
    SECURITY_JSON("security.json", Set.of(ORIGINAL, CACHE)),
    SUMMARY_JSON("summary.json", Set.of(ORIGINAL, CACHE)),
    TEMPLATE_PROPERTIES("template.properties", Set.of(ORIGINAL, CACHE)),
    UNKNOWN_JS_JSON("unknownjs.json", Set.of(ORIGINAL, CACHE)),
    // policyalerts.json and policythreats.json are only generated by IQ since they're based on policies
    POLICY_ALERTS("policyalerts.json", Set.of(CACHE)),
    POLICY_THREATS("policythreats.json", Set.of(CACHE)),
    // HDS, under some conditions, can also add thirdparty-bom.json and thirdparty-security.json to the report.zip
    THIRD_PARTY_BOM_JSON("thirdparty-bom.json", Set.of(ORIGINAL, CACHE, ADDITIONAL)),
    THIRD_PARTY_SECURITY_JSON("thirdparty-security.json", Set.of(ORIGINAL, CACHE, ADDITIONAL)),
    THIRD_PARTY_LICENSE_JSON("thirdparty-license.json", Set.of(ADDITIONAL));

    private static final Map<String, ReportFile> BY_NAME = new HashMap<>();

    static {
      for (ReportFile file : values()) {
        BY_NAME.put(file.name, file);
      }
    }

    public static ReportFile fromName(String name) {
      return BY_NAME.get(name);
    }

    private final String name;

    private final Set<ReportFileLocationType> locationTypes;

    ReportFile(final String name, final Set<ReportFileLocationType> locationTypes) {
      this.name = name;
      this.locationTypes = locationTypes;
    }

    public String getName() {
      return name;
    }

    public Set<ReportFileLocationType> getLocationTypes() {
      return locationTypes;
    }
  }

  private final ApplicationReportPersistenceService persistenceService;

  private final Application application;

  private final String scanId;

  public ApplicationReport(
      ApplicationReportPersistenceService persistenceService,
      Application application,
      String scanId)
  {
    this.persistenceService = persistenceService;
    this.application = application;
    this.scanId = scanId;
  }

  public ReportEntry getEntry(final String name) throws IOException {
    ReportEntity entity = getEntity(name);
    if (entity.exists(MetadataSource.CACHED)) {
      try (var stream = entity.getInputStream()) {
        return new ReportEntry(name, entity.getTime(MetadataSource.CACHED), stream.readAllBytes());
      }
    }
    else {
      return null;
    }
  }

  public ReportEntity getEntity(final String name) throws IOException {
    return persistenceService.getReportEntity(application.getId(), scanId, name);
  }

  /**
   * @return a stream of all entities in the report for the application and scan. This stream must be closed!
   */
  public Stream<ReportEntity> getAllEntities() throws IOException {
    return persistenceService.getAllReportEntities(application.getId(), scanId);
  }

  public void putEntry(String name, byte[] buf) throws IOException {
    try (var stream = new ByteArrayInputStream(buf)) {
      putEntry(name, stream);
    }
  }

  public void putEntry(String name, String text) throws IOException {
    var streamBuilder = CharSequenceInputStream.builder()
        .setCharset(StandardCharsets.UTF_8)
        .setCharSequence(text);

    try (var stream = streamBuilder.get()) {
      putEntry(name, stream);
    }
  }

  public void saveReportEntry(String entryFileName, ContainerNode<?> jsonData) throws IOException {
    putEntry(entryFileName, JsonUtils.generate(jsonData));
  }

  public ContainerNode<?> loadReportEntry(String entryFileName) throws IOException {
    ReportEntity entity = getEntity(entryFileName);
    return JsonUtils.read(entity.getInputStream());
  }

  /**
   * Fetch multiple report entries in parallel for improved performance.
   *
   * @param names list of entry names to fetch
   * @return map of entry name to ReportEntry (null values if entry doesn't exist)
   * @throws IOException if there's an error reading any entry
   */
  public Map<String, ReportEntry> getEntries(final List<String> names) throws IOException {
    return loadEntriesInParallel(names, name -> {
      try {
        return getEntry(name);
      }
      catch (IOException e) {
        throw new CompletionException(e);
      }
    });
  }

  /**
   * Load and parse multiple JSON report entries in parallel for improved performance.
   *
   * @param names list of entry names to load
   * @return map of entry name to parsed ContainerNode (null values if entry doesn't exist)
   * @throws IOException if there's an error reading or parsing any entry
   */
  public Map<String, ContainerNode<?>> loadReportEntries(final List<String> names) throws IOException {
    return loadEntriesInParallel(names, name -> {
      try {
        return loadReportEntry(name);
      }
      catch (IOException e) {
        throw new CompletionException(e);
      }
    });
  }

  /**
   * Generic helper method to load multiple entries in parallel.
   *
   * @param names list of entry names to load
   * @param loader function that loads a single entry by name
   * @param <T> type of the entry being loaded
   * @return map of entry name to loaded entry (null values if entry doesn't exist)
   * @throws IOException if there's an error loading any entry
   */
  private <T> Map<String, T> loadEntriesInParallel(
      final List<String> names,
      final Function<String, T> loader) throws IOException
  {
    if (names.isEmpty()) {
      return Map.of();
    }

    // Load all entries in parallel using CompletableFuture
    Map<String, CompletableFuture<T>> futures = names.stream()
        .collect(Collectors.toMap(
            name -> name,
            name -> CompletableFuture.supplyAsync(
                new TenantAwareSupplier<>(() -> loader.apply(name)),
                reportEntityLoadingExecutors.get())));

    // Wait for all to complete and collect results
    Map<String, T> results = new HashMap<>();
    for (Map.Entry<String, CompletableFuture<T>> entry : futures.entrySet()) {
      try {
        results.put(entry.getKey(), entry.getValue().join());
      }
      catch (CompletionException e) {
        if (e.getCause() instanceof IOException ioException) {
          throw ioException;
        }
        throw e;
      }
    }

    return results;
  }

  public ReportEntry extractEntry(String name) throws IOException {
    ReportEntity entity = getEntity(name);
    if (entity.exists(MetadataSource.CACHED)) {
      try (var stream = entity.getInputStream()) {
        return new ReportEntry(name, entity.getTime(MetadataSource.CACHED), stream.readAllBytes());
      }
    }
    else {
      return getDeduplicatedLegacyReportEntry(name).orElse(null);
    }
  }

  public void embedApplicationPublicId() throws IOException {
    String filename = INDEX_HTML.getName();
    ReportEntry reportEntry = extractEntry(filename);
    String originalIndexHtmlContent = new String(reportEntry.buf, StandardCharsets.UTF_8);
    String augmentedIndexHtmlContent =
        originalIndexHtmlContent.replace("applicationId = ''", "applicationId = '" + application.getPublicId() + "'");
    if (!augmentedIndexHtmlContent.equals(originalIndexHtmlContent)) {
      var streamBuilder = CharSequenceInputStream.builder()
          .setCharset(StandardCharsets.UTF_8)
          .setCharSequence(augmentedIndexHtmlContent);

      try (var stream = streamBuilder.get()) {
        persistenceService.saveReportFile(application.getId(), scanId, filename, stream);
      }
    }
  }

  public void appendToReport(final ThirdPartyApplicationReportDTO dto) throws IOException {
    saveAdditionalReportFile(THIRD_PARTY_BOM_JSON.getName(), dto.billOfMaterials);
    saveAdditionalReportFile(THIRD_PARTY_SECURITY_JSON.getName(), dto.securityRows);
    saveAdditionalReportFile(THIRD_PARTY_LICENSE_JSON.getName(), dto.licenseRows);
  }

  public ReportType getType() throws IOException {
    boolean hasSecurityJson = getEntity(SECURITY_JSON.getName()).exists(MetadataSource.CACHED);
    boolean hasLicensesJson = getEntity(LICENSES_JSON.getName()).exists(MetadataSource.CACHED);

    if (!hasSecurityJson && !hasLicensesJson) {
      return ReportType.ERROR;
    }
    return ReportType.FULL;
  }

  /**
   * Gets the contents of the {@code template.properties} embedded in the report from the HDS or an empty map if none.
   */
  public Properties getTemplateProperties() throws IOException {
    ReportEntity entity = getEntity(TEMPLATE_PROPERTIES.getName());
    Properties props = new Properties();
    if (entity.exists(MetadataSource.CACHED)) {
      try (var stream = entity.getInputStream()) {
        props.load(stream);
      }
    }
    return props;
  }

  public String getLocation() {
    return persistenceService.getReportLocation(application.getId(), scanId);
  }

  public boolean exists() throws IOException {
    return persistenceService.reportExists(application.getId(), scanId);
  }

  public Application getApplication() {
    return application;
  }

  public String getScanId() {
    return scanId;
  }

  /**
   * Save a file with the report that will not get deleted by deleteCacheDir
   */
  public void saveAdditionalReportFile(final String filename, final List<?> data) throws IOException {
    byte[] bytes = JsonUtils.generate(JsonUtils.aaData(data));
    try (var stream = new ByteArrayInputStream(bytes)) {
      persistenceService.saveAdditionalReportFile(application.getId(), scanId, filename, stream);
    }
  }

  private void putEntry(String name, InputStream contents) throws IOException {
    persistenceService.saveReportFile(application.getId(), scanId, name, contents);
  }

  /**
   * Starting with release 1.168, we serve shared resources for legacy report from the jar
   * HDS does not include these files in the report.zip when IQ client is v1.168 or higher
   */
  private Optional<ReportEntry> getDeduplicatedLegacyReportEntry(final String name) throws IOException {
    String resource = "/com/sonatype/insight/brain/legacy.report/" + name;
    try (InputStream stream = getClass().getResourceAsStream(resource)) {
      if (stream != null) {
        return Optional.of(new ReportEntry(name, Instant.now().toEpochMilli(), stream.readAllBytes()));
      }
      else {
        return Optional.empty();
      }
    }
  }
}
