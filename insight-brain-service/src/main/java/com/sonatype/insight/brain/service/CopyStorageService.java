/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Phaser;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor.AbortPolicy;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.api.v2.service.ApiConfigurationService;
import com.sonatype.insight.brain.api.v2.service.ConfigurationListener;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLock;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLockManager;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.report.LifecycleReport.ReportFile;
import com.sonatype.insight.brain.report.LifecycleReport.ReportFileLocationType;
import com.sonatype.insight.brain.report.LifecycleReportPersistenceService;
import com.sonatype.insight.brain.report.LifecycleReportPersistenceServiceProvider;
import com.sonatype.insight.brain.report.ReportEntity;
import com.sonatype.insight.brain.report.ReportPdfEntity;
import com.sonatype.insight.brain.sbom.datastore.SbomEntity;
import com.sonatype.insight.brain.sbom.datastore.SbomPersistenceService;
import com.sonatype.insight.brain.sbom.datastore.SbomPersistenceServiceProvider;
import com.sonatype.insight.brain.scan.datastore.ScanEntity;
import com.sonatype.insight.brain.scan.datastore.ScanPersistenceService;
import com.sonatype.insight.brain.scan.datastore.ScanPersistenceServiceProvider;
import com.sonatype.insight.brain.service.config.StorageConfig;
import com.sonatype.insight.brain.service.config.StorageConfig.DataStoreType;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.tenancy.TenantReference;
import com.sonatype.insight.brain.tenancy.TenantScheduledThreadPoolExecutor;
import com.sonatype.insight.brain.tenancy.TenantThreadPoolExecutor;
import com.sonatype.insight.error.exception.BadRequestException;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@Singleton
public class CopyStorageService
    implements ConfigurationListener
{
  private static final Logger log = LoggerFactory.getLogger(CopyStorageService.class);

  public static final String COPY_MARKER = "copyInProgress";

  private static final int DEFAULT_PAGE_SIZE = 10000;

  private static final Duration LOG_FREQUENCY = Duration.ofMinutes(1);

  private static final Set<DataStoreType> SUPPORTED = Set.of(DataStoreType.FILE, DataStoreType.S3);

  public static final int MAX_TENANT_THREAD_POOL_THREADS = 200;

  private final InsightConfig insightConfig;

  private final Provider<ScanPersistenceServiceProvider> scanPersistanceProvider;

  private final Provider<LifecycleReportPersistenceServiceProvider> reportPersistenceProvider;

  private final Provider<SbomPersistenceServiceProvider> sbomPersistenceProvider;

  private final ApplicationDAO applicationDAO;

  private final PolicyEvaluationDAO policyEvaluationDAO;

  private final ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO;

  private final ClusterLockManager clusterLockManager;

  private final ApiConfigurationService apiConfigurationService;

  private final TenantThreadPoolExecutor tenantThreadPoolExecutor;

  private volatile CopyStorageConfig copyStorageConfig;

  private final TenantReference<DynamicSemaphore> copyLimit;

  private final Set<Phaser> phasers;

  private final TenantReference<AtomicBoolean> active;

  @Inject
  public CopyStorageService(
      final InsightConfig insightConfig,
      final Provider<ScanPersistenceServiceProvider> scanPersistanceProvider,
      final Provider<LifecycleReportPersistenceServiceProvider> reportPersistenceProvider,
      final Provider<SbomPersistenceServiceProvider> sbomPersistenceProvider,
      final ApplicationDAO applicationDAO,
      final PolicyEvaluationDAO policyEvaluationDAO,
      final ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO,
      final ClusterLockManager clusterLockManager,
      final ApiConfigurationService apiConfigurationService,
      final ShutdownHandler shutdownHandler)
  {
    this.insightConfig = insightConfig;
    this.scanPersistanceProvider = scanPersistanceProvider;
    this.reportPersistenceProvider = reportPersistenceProvider;
    this.sbomPersistenceProvider = sbomPersistenceProvider;
    this.applicationDAO = applicationDAO;
    this.policyEvaluationDAO = policyEvaluationDAO;
    this.thirdPartySbomMetadataDAO = thirdPartySbomMetadataDAO;
    this.clusterLockManager = clusterLockManager;
    this.apiConfigurationService = apiConfigurationService;
    tenantThreadPoolExecutor = new TenantThreadPoolExecutor(
        MAX_TENANT_THREAD_POOL_THREADS,
        MAX_TENANT_THREAD_POOL_THREADS,
        5L,
        TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(),
        new ThreadFactoryBuilder().setNameFormat("CopyStorageService-%d").build(),
        new AbortPolicy(),
        "copy_storage_service",
        getClass().getSimpleName())
    {
      @Override
      public void shutdown() {
        super.shutdown();
        // Release a permit to ensure acquireUninterruptibly is not blocking
        // As soon as a waiting tenant gets this permit, it will
        // 1. Fail to submit since the executor is shutdown
        // 2. Release the extra permit
        // 3. Finish since the exception will propagate up
        // Also, the limit will now be off, but it doesn't matter because we're shutting down anyway
        copyLimit.get().release(1);
        // Force phaser termination to ensure arriveAndAwaitAdvance does not block
        phasers.forEach(Phaser::forceTermination);
      }
    };
    tenantThreadPoolExecutor.allowCoreThreadTimeOut(true);
    shutdownHandler.add(tenantThreadPoolExecutor);
    copyStorageConfig = (CopyStorageConfig) apiConfigurationService.getConfigurationNoAuthz(
        SystemConfigurationProperty.COPY_STORAGE_CONFIG);
    copyLimit = new TenantReference<>(() -> new DynamicSemaphore(copyStorageConfig.maxCopyThreads()));
    phasers = ConcurrentHashMap.newKeySet();
    active = new TenantReference<>(AtomicBoolean::new);
  }

  @Override
  public void configurationChanged(final Set<String> propertyNames) {
    if (propertyNames.contains(SystemConfigurationProperty.COPY_STORAGE_CONFIG)) {
      CopyStorageConfig oldCopyStorageConfig = copyStorageConfig;
      copyStorageConfig = (CopyStorageConfig) apiConfigurationService.getConfigurationNoAuthz(
          SystemConfigurationProperty.COPY_STORAGE_CONFIG);

      if (copyStorageConfig.maxCopyThreads() != oldCopyStorageConfig.maxCopyThreads()) {
        copyLimit.get().resize(copyStorageConfig.maxCopyThreads());
        log.debug("Updated 'copyLimit' to {}.", copyStorageConfig.maxCopyThreads());
      }
    }
  }

  private static class DynamicSemaphore
      extends Semaphore
  {
    private volatile int permits;

    public DynamicSemaphore(final int permits) {
      super(permits);
      this.permits = permits;
    }

    public DynamicSemaphore(final int permits, final boolean fair) {
      super(permits, fair);
      this.permits = permits;
    }

    public synchronized void resize(final int permits) {
      if (this.permits > permits) {
        reducePermits(this.permits - permits);
      }
      else if (this.permits < permits) {
        release(permits - this.permits);
      }
      this.permits = permits;
    }
  }

  static final class CopyStorageResult
  {
    private final Date start;

    private Date end;

    private final AtomicInteger appsProcessed = new AtomicInteger();

    private final AtomicInteger scansProcessed = new AtomicInteger();

    private final AtomicInteger reportsProcessed = new AtomicInteger();

    private final AtomicInteger sbomsProcessed = new AtomicInteger();

    private final AtomicInteger scansSkipped = new AtomicInteger();

    private final AtomicInteger reportsSkipped = new AtomicInteger();

    private final AtomicInteger sbomsSkipped = new AtomicInteger();

    private final AtomicInteger scansCopied = new AtomicInteger();

    private final AtomicInteger reportsCopied = new AtomicInteger();

    private final AtomicInteger sbomsCopied = new AtomicInteger();

    private final AtomicInteger scansFailed = new AtomicInteger();

    private final AtomicInteger reportsFailed = new AtomicInteger();

    private final AtomicInteger sbomsFailed = new AtomicInteger();

    public CopyStorageResult(final Date start) {
      this.start = start;
    }

    public Date getStart() {
      return start;
    }

    public Date getEnd() {
      return end;
    }

    public AtomicInteger getAppsProcessed() {
      return appsProcessed;
    }

    public AtomicInteger getScansProcessed() {
      return scansProcessed;
    }

    public AtomicInteger getReportsProcessed() {
      return reportsProcessed;
    }

    public AtomicInteger getSbomsProcessed() {
      return sbomsProcessed;
    }

    public AtomicInteger getScansSkipped() {
      return scansSkipped;
    }

    public AtomicInteger getReportsSkipped() {
      return reportsSkipped;
    }

    public AtomicInteger getSbomsSkipped() {
      return sbomsSkipped;
    }

    public AtomicInteger getScansCopied() {
      return scansCopied;
    }

    public AtomicInteger getReportsCopied() {
      return reportsCopied;
    }

    public AtomicInteger getSbomsCopied() {
      return sbomsCopied;
    }

    public AtomicInteger getScansFailed() {
      return scansFailed;
    }

    public AtomicInteger getReportsFailed() {
      return reportsFailed;
    }

    public AtomicInteger getSbomsFailed() {
      return sbomsFailed;
    }

    public void setEnd(final Date end) {
      this.end = end;
    }

    public Duration getDuration() {
      if (end == null) {
        return Duration.ofMillis(System.currentTimeMillis() - start.getTime());
      }
      return Duration.ofMillis(end.getTime() - start.getTime());
    }

    @Override
    public String toString() {
      return "CopyStorageResult{" +
          "start=" + start +
          ", end=" + end +
          ", appsProcessed=" + appsProcessed +
          ", scansProcessed=" + scansProcessed +
          ", reportsProcessed=" + reportsProcessed +
          ", sbomsProcessed=" + sbomsProcessed +
          ", scansSkipped=" + scansSkipped +
          ", reportsSkipped=" + reportsSkipped +
          ", sbomsSkipped=" + sbomsSkipped +
          ", scansCopied=" + scansCopied +
          ", reportsCopied=" + reportsCopied +
          ", sbomsCopied=" + sbomsCopied +
          ", scansFailed=" + scansFailed +
          ", reportsFailed=" + reportsFailed +
          ", sbomsFailed=" + sbomsFailed +
          '}';
    }
  }

  public void execute(final DataStoreType from, final DataStoreType to) {
    try {
      if (active.get().getAndSet(true)) {
        log.info("Request for copy of scans, reports, and SBOMs from '{}' to '{}' is already active.", from, to);
        return;
      }
      doExecute(from, to);
    }
    finally {
      active.get().set(false);
    }
  }

  // Visible for testing
  void doExecute(final DataStoreType from, final DataStoreType to) {
    checkSupported(from);
    checkSupported(to);
    checkPrimaryStorageIsTarget(to);
    checkFromAndToAreDifferent(from, to);

    // From persistence services
    ScanPersistenceService fromScan = scanPersistanceProvider.get().get(from);
    LifecycleReportPersistenceService fromReport = reportPersistenceProvider.get().get(from);
    SbomPersistenceService fromSbom = sbomPersistenceProvider.get().get(from);

    // To persistence services
    ScanPersistenceService toScan = scanPersistanceProvider.get().get(to);
    LifecycleReportPersistenceService toReport = reportPersistenceProvider.get().get(to);
    SbomPersistenceService toSbom = sbomPersistenceProvider.get().get(to);

    Iterator<Application> apps = createApplicationIterator();
    CopyStorageResult copyStorageResult = new CopyStorageResult(new Date());
    log.info("Starting copy of scans, reports, and SBOMs from '{}' to '{}': {}.", from, to, copyStorageResult);
    Phaser tenantPhaser = new Phaser(1);
    phasers.add(tenantPhaser);
    TenantScheduledThreadPoolExecutor logScheduler = new TenantScheduledThreadPoolExecutor(1,
        new ThreadFactoryBuilder().setNameFormat("CopyStorageServiceLogger-%d").build());
    try {
      logScheduler.scheduleAtFixedRate(
          () -> log.debug("Copy of scans, reports, and SBOMs from '{}' to '{}': {}.", from, to, copyStorageResult),
          LOG_FREQUENCY.toMillis(),
          LOG_FREQUENCY.toMillis(),
          TimeUnit.MILLISECONDS);
      String logStart = "Starting copy of {} from '{}' to '{}' for app '{}' with id '{}'.";
      String logEnd = "Finished copy of {} from '{}' to '{}' for app '{}' with id '{}'.";
      while (apps.hasNext()) {
        Application app = apps.next();
        Phaser appPhaser = createPhaser(tenantPhaser, 1, () -> copyStorageResult.getAppsProcessed().incrementAndGet());

        log.trace(logStart, "scans", from, to, app.getName(), app.getId());
        Phaser scanPhaser =
            createPhaser(appPhaser, 1, () -> log.trace(logEnd, "scans", from, to, app.getName(), app.getId()));
        copyScans(app, fromScan, toScan, copyStorageResult, scanPhaser);
        scanPhaser.arriveAndDeregister();

        log.trace(logStart, "reports", from, to, app.getName(), app.getId());
        Phaser reportPhaser =
            createPhaser(appPhaser, 1, () -> log.trace(logEnd, "reports", from, to, app.getName(), app.getId()));
        copyReports(app, fromReport, toReport, copyStorageResult, reportPhaser);
        reportPhaser.arriveAndDeregister();

        log.trace(logStart, "sboms", from, to, app.getName(), app.getId());
        Phaser sbomPhaser =
            createPhaser(appPhaser, 1, () -> log.trace(logEnd, "sboms", from, to, app.getName(), app.getId()));
        copySboms(app, fromSbom, toSbom, copyStorageResult, sbomPhaser);
        sbomPhaser.arriveAndDeregister();

        appPhaser.arriveAndDeregister();
      }
      tenantPhaser.arriveAndAwaitAdvance();
      copyStorageResult.setEnd(new Date());
      log.info("Finished copy of scans, reports, and SBOMs from '{}' to '{}': {}.", from, to, copyStorageResult);
    }
    catch (Exception e) {
      log.error("Unable to finish copy of scans, reports, and SBOMs from '{}' to '{}': {}.", from, to,
          copyStorageResult, e);
      throw e;
    }
    finally {
      phasers.remove(tenantPhaser);
      logScheduler.shutdownNow();
    }
  }

  private Phaser createPhaser(final Phaser parent, final int parties, final Runnable terminationAction) {
    parent.register();
    return new Phaser(parties)
    {
      @Override
      public int arriveAndDeregister() {
        int value = super.arriveAndDeregister();
        if (getRegisteredParties() == 0) {
          try {
            terminationAction.run();
          }
          finally {
            parent.arriveAndDeregister();
          }
        }
        return value;
      }
    };
  }

  public void checkSupported(final DataStoreType dataStoreType) {
    if (!SUPPORTED.contains(dataStoreType)) {
      throw new IllegalArgumentException("Storage '%s' is unsupported.".formatted(dataStoreType));
    }
  }

  public void checkPrimaryStorageIsTarget(final DataStoreType to) {
    StorageConfig storage = insightConfig.getStorage();
    DataStoreType primaryStorage = storage.getType();
    if (DataStoreType.HYBRID == primaryStorage) {
      primaryStorage = storage.getHybridConfig().getTypes().iterator().next();
    }
    if (primaryStorage != to) {
      throw new BadRequestException(("Primary storage type is '%s' but copy is targeting '%s'," +
          " scans, reports, and/or SBOMs written during copy may be missed.").formatted(primaryStorage, to));
    }
  }

  public void checkFromAndToAreDifferent(final DataStoreType from, final DataStoreType to) {
    if (from == to) {
      throw new BadRequestException("Not copying from '%s' to '%s', these should be different.".formatted(from, to));
    }
  }

  private void submit(
      final Runnable runnable,
      final Semaphore limit,
      final Phaser phaser)
  {
    try {
      limit.acquireUninterruptibly();
      phaser.register();
      tenantThreadPoolExecutor.submit(() -> {
        try {
          runnable.run();
        }
        finally {
          phaser.arriveAndDeregister();
          limit.release();
        }
      });
    }
    catch (Throwable t) {
      phaser.arriveAndDeregister();
      limit.release();
      throw t;
    }
  }

  private void copyScans(
      final Application app,
      final ScanPersistenceService from,
      final ScanPersistenceService to,
      final CopyStorageResult copyStorageResult,
      final Phaser phaser)
  {
    try (Stream<ScanEntity> scanEntityStream = from.allScanFilesFor(app.getId())) {
      scanEntityStream.forEach(sourceScan -> submit(() -> {
        copyScan(sourceScan, to, copyStorageResult);
        copyStorageResult.getScansProcessed().incrementAndGet();
      }, copyLimit.get(), phaser));
    }
  }

  private void copyScan(
      final ScanEntity sourceScan,
      final ScanPersistenceService to,
      final CopyStorageResult copyStorageResult)
  {
    String appId = sourceScan.getAppId();
    String scanId = sourceScan.getScanId();

    ScanEntity targetScan = to.getScan(appId, scanId);

    String fromLocation = sourceScan.getLocation();
    String toLocation = targetScan.getLocation();

    ScanEntity targetTempScan = null;

    try {
      if (!sourceScan.exists()) {
        copyStorageResult.getScansSkipped().incrementAndGet();
        log.trace("Skipping scan copying for app id '{}' scan id '{}' since it does not exist.", appId, scanId);
        return;
      }

      if (targetScan.exists()) {
        copyStorageResult.getScansSkipped().incrementAndGet();
        log.trace("Skipping scan copying for app id '{}' scan id '{}' since it is already done.", appId, scanId);
        return;
      }

      log.trace("Copying scan from '{}' to '{}'.", fromLocation, toLocation);
      targetTempScan = to.createTempScan(appId);
      try (InputStream inputStream = sourceScan.getInputStream();
          OutputStream outputStream = targetTempScan.getOutputStream())
      {
        inputStream.transferTo(outputStream);
      }
      to.moveTempScan(targetTempScan, appId, scanId);
      targetTempScan = null;
      copyStorageResult.getScansCopied().incrementAndGet();
      log.trace("Copied scan from '{}' to '{}'.", fromLocation, toLocation);
    }
    catch (Exception e) {
      try {
        if (targetTempScan != null && targetTempScan.exists()) {
          to.deleteScan(targetTempScan);
        }
      }
      catch (Exception ex) {
        e.addSuppressed(ex);
      }
      copyStorageResult.getScansFailed().incrementAndGet();
      log.error("Failed to copy scan from '{}' to '{}'.", fromLocation, toLocation, e);
    }
  }

  private void copyReports(
      final Application app,
      final LifecycleReportPersistenceService from,
      final LifecycleReportPersistenceService to,
      final CopyStorageResult copyStorageResult,
      final Phaser phaser)
  {
    Iterator<PolicyEvaluation> evals = createPolicyEvaluationIterator(app.getId());
    while (evals.hasNext()) {
      PolicyEvaluation eval = evals.next();
      submit(() -> {
        copyReport(app, from, to, eval, copyStorageResult);
        copyStorageResult.getReportsProcessed().incrementAndGet();
      }, copyLimit.get(), phaser);
    }
  }

  private void copyReport(
      final Application app,
      final LifecycleReportPersistenceService from,
      final LifecycleReportPersistenceService to,
      final PolicyEvaluation eval,
      final CopyStorageResult copyStorageResult)
  {
    String appId = eval.getOwnerId();
    String scanId = eval.getScanId();

    String fromLocation = from.getReportLocation(appId, scanId);
    String toLocation = to.getReportLocation(appId, scanId);

    try (ClusterLock clusterLock = clusterLockManager.createForPolicyEvaluation(app, scanId)) {
      if (!clusterLock.tryLock()) {
        copyStorageResult.getReportsSkipped().incrementAndGet();
        log.trace("Skipping report copying for app id '{}' scan id '{}' since it is in progress.", appId, scanId);
        return;
      }

      if (to.reportExists(appId, scanId)) {
        copyStorageResult.getReportsSkipped().incrementAndGet();
        log.trace("Skipping report copying for app id '{}' scan id '{}' since it is already done.", appId, scanId);
        return;
      }

      if (!from.reportExists(appId, scanId)) {
        copyStorageResult.getReportsSkipped().incrementAndGet();
        log.trace("Skipping report copying for app id '{}' scan id '{}' since it does not exist.", appId, scanId);
        return;
      }

      try (InputStream inputStream = new ByteArrayInputStream(new byte[]{0})) {
        to.saveAdditionalReportFile(appId, scanId, COPY_MARKER, inputStream);
      }
      ReportEntity copyMarker = to.getReportEntity(appId, scanId, COPY_MARKER);
      log.trace("Copying report from '{}' to '{}'.", fromLocation, toLocation);

      try (Stream<ReportEntity> originalReportEntities = from.getOriginalReportEntities(appId, scanId)) {
        to.saveOriginalReportEntities(appId, scanId, originalReportEntities);
      }
      try (Stream<ReportEntity> allEntities = from.getAllReportEntities(appId, scanId)) {
        allEntities.forEach(sourceReportEntity -> {
          try {
            ReportFile reportFile = ReportFile.fromName(sourceReportEntity.getName());
            if (reportFile == null || reportFile.getLocationTypes().contains(ReportFileLocationType.ADDITIONAL)) {
              try (InputStream inputStream = sourceReportEntity.getInputStream()) {
                to.saveAdditionalReportFile(appId, scanId, sourceReportEntity.getName(), inputStream);
              }
            }
            else {
              try (InputStream inputStream = sourceReportEntity.getInputStream()) {
                to.saveReportFile(appId, scanId, sourceReportEntity.getName(), inputStream);
              }
            }
          }
          catch (IOException e) {
            throw new UncheckedIOException(e);
          }
        });
      }

      ReportPdfEntity sourceReportPdfEntity = from.getPdfEntity(appId, scanId);
      ReportPdfEntity targetReportPdfEntity = to.getPdfEntity(appId, scanId);
      if (sourceReportPdfEntity.exists()) {
        try (InputStream inputStream = sourceReportPdfEntity.getInputStream();
            OutputStream outputStream = targetReportPdfEntity.getOutputStream())
        {
          inputStream.transferTo(outputStream);
        }
      }

      to.deleteReportEntity(copyMarker);
      copyStorageResult.getReportsCopied().incrementAndGet();
      log.trace("Copied report from '{}' to '{}'.", fromLocation, toLocation);
    }
    catch (Exception e) {
      copyStorageResult.getReportsFailed().incrementAndGet();
      log.error("Failed to copy report from '{}' to '{}'.", fromLocation, toLocation, e);
    }
  }

  private void copySboms(
      final Application app,
      final SbomPersistenceService from,
      final SbomPersistenceService to,
      final CopyStorageResult copyStorageResult,
      final Phaser phaser)
  {
    Iterator<ThirdPartySbomMetadata> sboms = createThirdPartySbomMetadataIterator(app.getId());
    while (sboms.hasNext()) {
      ThirdPartySbomMetadata sbomMetadata = sboms.next();
      submit(() -> {
        copySbom(from, to, sbomMetadata, copyStorageResult);
        copyStorageResult.getSbomsProcessed().incrementAndGet();
      }, copyLimit.get(), phaser);
    }
  }

  private void copySbom(
      final SbomPersistenceService from,
      final SbomPersistenceService to,
      final ThirdPartySbomMetadata sbomMetadata,
      final CopyStorageResult copyStorageResult)
  {
    String appId = sbomMetadata.getApplicationId();
    String fileName = sbomMetadata.getFilename();

    SbomEntity sourceSbom = from.getPermanentSbom(appId, fileName);
    SbomEntity targetSbom = to.getPermanentSbom(appId, fileName);

    String fromLocation = sourceSbom.getLocation();
    String toLocation = targetSbom.getLocation();

    SbomEntity targetTempSbom = null;

    try {
      if (!sourceSbom.exists()) {
        copyStorageResult.getSbomsSkipped().incrementAndGet();
        log.trace("Skipping sbom copying for app id '{}' file name '{}' since it does not exist.", appId,
            fileName);
        return;
      }

      if (targetSbom.exists()) {
        copyStorageResult.getSbomsSkipped().incrementAndGet();
        log.trace("Skipping sbom copying for app id '{}' file name '{}' since it is already done.", appId,
            fileName);
        return;
      }

      targetTempSbom = to.getTransientSbom(appId + "-" + fileName);
      log.trace("Copying sbom from '{}' to '{}'.", fromLocation, toLocation);
      try (
          InputStream inputStream = sourceSbom.getInputStream();
          OutputStream outputStream = targetTempSbom.getOutputStream())
      {
        inputStream.transferTo(outputStream);
      }
      to.moveSbomEntity(targetTempSbom, targetSbom);
      copyStorageResult.getSbomsCopied().incrementAndGet();
      log.trace("Copied sbom from '{}' to '{}'.", fromLocation, toLocation);
    }
    catch (Exception e) {
      copyStorageResult.getSbomsFailed().incrementAndGet();
      try {
        if (targetTempSbom != null && targetTempSbom.exists()) {
          to.deleteSbom(targetTempSbom);
        }
      }
      catch (Exception ex) {
        e.addSuppressed(ex);
      }
      log.error("Failed to copy sbom from '{}' to '{}'.", fromLocation, toLocation, e);
    }
  }

  private Iterator<Application> createApplicationIterator() {
    return new PageIterator<>(1, DEFAULT_PAGE_SIZE, applicationDAO::getAll);
  }

  private Iterator<PolicyEvaluation> createPolicyEvaluationIterator(final String applicationId) {
    return new PageIterator<>(1, DEFAULT_PAGE_SIZE,
        (page, pageSize) -> policyEvaluationDAO.getByOwnerId(applicationId, page, pageSize));
  }

  private Iterator<ThirdPartySbomMetadata> createThirdPartySbomMetadataIterator(final String applicationId) {
    return new PageIterator<>(1, DEFAULT_PAGE_SIZE,
        (page, pageSize) -> thirdPartySbomMetadataDAO.getActiveByApplicationId(applicationId, page, pageSize));
  }
}
