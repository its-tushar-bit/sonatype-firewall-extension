/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Stream;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.persistence.Query;

import com.sonatype.insight.brain.api.v2.ApiFirewallMetricsService;
import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.dataaccess.configuration.ProductLicenseDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.policy.RepositoryPolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryComponentDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dataaccess.successmetrics.FirewallMetricsDAO;
import com.sonatype.insight.brain.model.configuration.ProductLicense;
import com.sonatype.insight.brain.model.policy.RepositoryPolicyViolation;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.successmetrics.FirewallMetrics;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.tenancy.TenantAwareFunction;
import com.sonatype.insight.brain.tenancy.TenantAwareSupplier;
import com.sonatype.insight.brain.utils.ExecutorThreadPools;
import com.sonatype.insight.brain.utils.ExecutorThreadPools.ThreadPools;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.license.model.SignedProductLicenseDetailsDTO;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.dataaccess.AbstractSqlDAO.createPaginationQuery;
import static com.sonatype.insight.brain.model.successmetrics.FirewallMetricsName.COMPONENTS_AUTO_RELEASED;
import static com.sonatype.insight.brain.model.successmetrics.FirewallMetricsName.COMPONENTS_QUARANTINED;
import static com.sonatype.insight.brain.model.successmetrics.FirewallMetricsName.NAMESPACE_ATTACKS_BLOCKED;
import static com.sonatype.insight.brain.model.successmetrics.FirewallMetricsName.SUPPLY_CHAIN_ATTACKS_BLOCKED;
import static com.sonatype.insight.brain.model.successmetrics.FirewallMetricsName.WAIVED_COMPONENTS;
import static com.sonatype.insight.brain.utils.DateConverter.toDate;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

@Named
public class FirewallMetricsMigrator
{
  private static final Logger log = LoggerFactory.getLogger(FirewallMetricsMigrator.class);

  // Visible for testing
  static final String MIGRATION_ID = "firewall-metrics-migrator";

  private final AtomicInteger processedRepositories = new AtomicInteger();

  private long lastProgressLog = System.currentTimeMillis();

  private final MigrationTrackerDAO migrationTrackerDAO;

  private final ProductLicenseDAO productLicenseDAO;

  private final ApiFirewallMetricsService apiFirewallMetricsService;

  private final RepositoryDAO repositoryDAO;

  private final RepositoryPolicyViolationDAO repositoryPolicyViolationDAO;

  private final RepositoryComponentDAO repositoryComponentDAO;

  private final PolicyWaiverDAO policyWaiverDAO;

  private final FirewallMetricsDAO firewallMetricsDAO;

  private Date processDate;

  private Date twelveMonthsAgo;

  private int totalRepositories;

  private int repositoryPolicyViolationsBatchSize = 50_000;

  @Inject
  public FirewallMetricsMigrator(
      MigrationTrackerDAO migrationTrackerDAO,
      ProductLicenseDAO productLicenseDAO,
      ApiFirewallMetricsService apiFirewallMetricsService,
      RepositoryDAO repositoryDAO,
      RepositoryPolicyViolationDAO repositoryPolicyViolationDAO,
      RepositoryComponentDAO repositoryComponentDAO,
      PolicyWaiverDAO policyWaiverDAO,
      FirewallMetricsDAO firewallMetricsDAO)
  {
    this.migrationTrackerDAO = migrationTrackerDAO;
    this.productLicenseDAO = productLicenseDAO;
    this.apiFirewallMetricsService = apiFirewallMetricsService;
    this.repositoryDAO = repositoryDAO;
    this.repositoryPolicyViolationDAO = repositoryPolicyViolationDAO;
    this.repositoryComponentDAO = repositoryComponentDAO;
    this.policyWaiverDAO = policyWaiverDAO;
    this.firewallMetricsDAO = firewallMetricsDAO;
  }

  public void migrate() {
    long start = System.currentTimeMillis();

    if (!checkProductLicenseFeatures()) {
      // Does not insert the migration track as a customer could get a product license for Next-Gen Firewall later
      return;
    }

    if (migrationTrackerDAO.isTrackerPresent(MIGRATION_ID)) {
      log.debug("Initial Firewall Metrics already calculated.");
      return;
    }

    List<Repository> repositories = repositoryDAO.getAll();
    totalRepositories = repositories.size();
    processDate = new Date();
    twelveMonthsAgo = DateUtils.addMonths(processDate, -12);

    log.info("Calculating Firewall Metrics from {} repositories.", totalRepositories);

    calculateNamespaceAttacksBlockedAndSupplyChainAttacksBlockedMetrics(repositories);
    calculateComponentsQuarantinedMetrics(repositories);
    calculateComponentsAutoReleasedMetrics(repositories);
    calculateWaivedComponentsMetrics(repositories);

    migrationTrackerDAO.insertTracker(MIGRATION_ID);

    log.info("Firewall Metrics calculated for {} repositories in {} ms.", totalRepositories,
        System.currentTimeMillis() - start);
  }

  private void calculateNamespaceAttacksBlockedAndSupplyChainAttacksBlockedMetrics(List<Repository> repositories) {
    long start = System.currentTimeMillis();
    log.info("Calculating namespace attacks blocked and supply chain attacks blocked Firewall Metrics");

    LocalDate earliestNamespaceAttacksBlockedMetric =
        firewallMetricsDAO.getEarliestMetricDateByName(NAMESPACE_ATTACKS_BLOCKED);
    LocalDate earliestSupplyChainAttacksBlockedMetric =
        firewallMetricsDAO.getEarliestMetricDateByName(SUPPLY_CHAIN_ATTACKS_BLOCKED);
    LocalDate earliestMetricDate =
        Stream.of(earliestNamespaceAttacksBlockedMetric, earliestSupplyChainAttacksBlockedMetric)
            .filter(Objects::nonNull)
            .min(LocalDate::compareTo)
            .orElse(null);

    List<RepositoryPolicyViolationsMetrics> allMetrics = CompletableFuture.supplyAsync(
        new TenantAwareSupplier<>(() -> repositories.parallelStream()
            .map(new TenantAwareFunction<Repository, RepositoryPolicyViolationsMetrics>(
                repository -> processRepositoryPolicyViolations(repository, earliestMetricDate)))
            .collect(toList())),
        ExecutorThreadPools.getInstance().getThreadPool(ThreadPools.GENERAL)).join();

    List<FirewallMetrics> allNamespaceAttacksBlockedMetrics = new ArrayList<>();
    List<FirewallMetrics> allSupplyChainAttacksBlockedMetrics = new ArrayList<>();

    for (RepositoryPolicyViolationsMetrics repositoryPolicyViolationsMetrics : allMetrics) {
      if (isNotEmpty(repositoryPolicyViolationsMetrics.namespaceAttacksBlockedMetrics)) {
        allNamespaceAttacksBlockedMetrics.addAll(repositoryPolicyViolationsMetrics.namespaceAttacksBlockedMetrics);
      }
      if (isNotEmpty(repositoryPolicyViolationsMetrics.supplyChainAttacksBlockedMetrics)) {
        allSupplyChainAttacksBlockedMetrics.addAll(repositoryPolicyViolationsMetrics.supplyChainAttacksBlockedMetrics);
      }
    }

    consolidateAndSaveFirewallMetrics(allNamespaceAttacksBlockedMetrics);
    consolidateAndSaveFirewallMetrics(allSupplyChainAttacksBlockedMetrics);

    log.info("Namespace attacks blocked and supply chain attacks blocked Firewall Metrics calculated in {} ms.",
        System.currentTimeMillis() - start);
  }

  @SuppressWarnings("unchecked")
  private RepositoryPolicyViolationsMetrics processRepositoryPolicyViolations(
      Repository repository,
      LocalDate fromDate)
  {
    Map<LocalDate, FirewallMetrics> namespaceAttacksBlockedMetrics = new HashMap<>();
    Map<LocalDate, FirewallMetrics> supplyChainAttacksBlockedMetrics = new HashMap<>();

    try (TransactionContext tx = repositoryPolicyViolationDAO.createTransactionContext()) {
      String sQuery = "SELECT entity" + //
          " FROM RepositoryPolicyViolation entity" + //
          " WHERE entity.repositoryId = ?1" + //
          (fromDate != null ? " AND entity.time < ?2" : "") + //
          " ORDER BY entity.id";

      int currentBatch = 0;
      List<RepositoryPolicyViolation> repositoryPolicyViolations = null;

      do {
        Query paginationQuery = createPaginationQuery(tx, sQuery, currentBatch * repositoryPolicyViolationsBatchSize,
            repositoryPolicyViolationsBatchSize);
        paginationQuery.setParameter(1, repository.getId());
        if (fromDate != null) {
          paginationQuery.setParameter(2, toDate(fromDate));
        }
        repositoryPolicyViolations = paginationQuery.getResultList();
        repositoryPolicyViolationDAO.loadConstraintFacts(repositoryPolicyViolations);

        for (RepositoryPolicyViolation repositoryPolicyViolation : repositoryPolicyViolations) {
          apiFirewallMetricsService.checkFirewallMetricsInRepositoryPolicyViolation(repositoryPolicyViolation,
              namespaceAttacksBlockedMetrics, supplyChainAttacksBlockedMetrics);

          logProgressIfNeeded(logger -> logger
              .info("Processed {} of {} repositories to calculate namespace attacks blocked and supply chain attacks"
                  + " blocked Firewall Metrics", processedRepositories.get(), totalRepositories));
        }

        currentBatch++;
      }
      while (isNotEmpty(repositoryPolicyViolations));
    }
    catch (Exception e) {
      log.error("Error processing policy violations for repository {}", repository.getId(), e);
    }

    processedRepositories.incrementAndGet();

    RepositoryPolicyViolationsMetrics metrics = new RepositoryPolicyViolationsMetrics();
    metrics.namespaceAttacksBlockedMetrics = namespaceAttacksBlockedMetrics.values();
    metrics.supplyChainAttacksBlockedMetrics = supplyChainAttacksBlockedMetrics.values();
    return metrics;
  }

  private void calculateComponentsQuarantinedMetrics(List<Repository> repositories) {
    if (firewallMetricsDAO.getEarliestMetricDateByName(COMPONENTS_QUARANTINED) != null) {
      log.info("Components quarantined Firewall Metrics already calculated");
      return;
    }

    long start = System.currentTimeMillis();
    log.info("Calculating components quarantined Firewall Metrics");

    processedRepositories.set(0);

    List<List<FirewallMetrics>> allMetrics = CompletableFuture.supplyAsync(
        new TenantAwareSupplier<>(() -> repositories.parallelStream()
            .map(new TenantAwareFunction<Repository, List<FirewallMetrics>>(repository -> {
              List<FirewallMetrics> repositoryMetrics = new ArrayList<>();

              Map<LocalDate, Long> results =
                  repositoryComponentDAO.getQuarantinedCountByRepositoryIdAndDate(repository.getId(), twelveMonthsAgo);

              for (Entry<LocalDate, Long> entry : results.entrySet()) {
                repositoryMetrics
                    .add(new FirewallMetrics(entry.getKey(), COMPONENTS_QUARANTINED, entry.getValue().intValue()));
              }

              processedRepositories.incrementAndGet();

              logProgressIfNeeded(logger -> logger.info(
                  "Processed {} of {} repositories to calculate components quarantined Firewall Metrics",
                  processedRepositories.get(), totalRepositories));

              return repositoryMetrics;
            }))
            .collect(toList())),
        ExecutorThreadPools.getInstance().getThreadPool(ThreadPools.GENERAL)).join();

    consolidateAndSaveFirewallMetrics(flat(allMetrics));

    log.info("Components quarantined Firewall Metrics calculated in {} ms.", System.currentTimeMillis() - start);
  }

  private void calculateComponentsAutoReleasedMetrics(List<Repository> repositories) {
    if (firewallMetricsDAO.getEarliestMetricDateByName(COMPONENTS_AUTO_RELEASED) != null) {
      log.info("Components auto-released Firewall Metrics already calculated");
      return;
    }

    long start = System.currentTimeMillis();
    log.info("Calculating components auto-released Firewall Metrics");

    processedRepositories.set(0);

    List<List<FirewallMetrics>> allMetrics = CompletableFuture.supplyAsync(new TenantAwareSupplier<>(() -> repositories
        .parallelStream()
        .map(new TenantAwareFunction<Repository, List<FirewallMetrics>>(repository -> {
          List<FirewallMetrics> repositoryMetrics = new ArrayList<>();

          Map<LocalDate, Long> results = repositoryComponentDAO
              .getAutoReleaseQuarantinedCountByRepositoryIdAndDate(repository.getId(), twelveMonthsAgo, false);

          for (Entry<LocalDate, Long> entry : results.entrySet()) {
            repositoryMetrics
                .add(new FirewallMetrics(entry.getKey(), COMPONENTS_AUTO_RELEASED, entry.getValue().intValue()));
          }

          processedRepositories.incrementAndGet();

          logProgressIfNeeded(logger -> logger.info(
              "Processed {} of {} repositories to calculate components auto-released Firewall Metrics",
              processedRepositories.get(), totalRepositories));

          return repositoryMetrics;
        }))
        .collect(toList())), ExecutorThreadPools.getInstance().getThreadPool(ThreadPools.GENERAL)).join();

    consolidateAndSaveFirewallMetrics(flat(allMetrics));

    log.info("Components auto-released Firewall Metrics calculated in {} ms.", System.currentTimeMillis() - start);
  }

  private void calculateWaivedComponentsMetrics(List<Repository> repositories) {
    if (firewallMetricsDAO.getEarliestMetricDateByName(WAIVED_COMPONENTS) != null) {
      log.info("Waived components Firewall Metrics already calculated");
      return;
    }

    long start = System.currentTimeMillis();
    log.info("Calculating waived components Firewall Metrics");

    processedRepositories.set(0);

    List<List<FirewallMetrics>> allMetrics = CompletableFuture.supplyAsync(new TenantAwareSupplier<>(() -> repositories
        .parallelStream()
        .map(new TenantAwareFunction<Repository, List<FirewallMetrics>>(repository -> {
          List<FirewallMetrics> repositoryMetrics = new ArrayList<>();

          Map<LocalDate, Long> results = policyWaiverDAO.getCountByOwnerIdAndDate(repository.getId(), twelveMonthsAgo);

          for (Entry<LocalDate, Long> entry : results.entrySet()) {
            repositoryMetrics.add(new FirewallMetrics(entry.getKey(), WAIVED_COMPONENTS, entry.getValue().intValue()));
          }

          processedRepositories.incrementAndGet();

          logProgressIfNeeded(
              logger -> logger.info("Processed {} of {} repositories to calculate waived components Firewall Metrics",
                  processedRepositories.get(), totalRepositories));

          return repositoryMetrics;
        }))
        .collect(toList())), ExecutorThreadPools.getInstance().getThreadPool(ThreadPools.GENERAL)).join();

    consolidateAndSaveFirewallMetrics(flat(allMetrics));

    log.info("Waived components Firewall Metrics calculated in {} ms.", System.currentTimeMillis() - start);
  }

  private boolean checkProductLicenseFeatures() {
    ProductLicense productLicense = productLicenseDAO.get();
    try {
      if (productLicense != null && StringUtils.isNotBlank(productLicense.getLicenseDetails())) {
        SignedProductLicenseDetailsDTO productLicenseDetailsDTO =
            JsonUtils.parse(productLicense.getLicenseDetails(), SignedProductLicenseDetailsDTO.class);

        if (productLicenseDetailsDTO != null && isNotEmpty(productLicenseDetailsDTO.features)) {
          Set<LicensedFeature> licensedFeatures = productLicenseDetailsDTO.features.stream()
              .map(feature -> EnumUtils.getEnum(LicensedFeature.class, feature))
              .filter(Objects::nonNull)
              .collect(toSet());
          apiFirewallMetricsService.checkLicensedFeatures(licensedFeatures);
          return true;
        }
      }
    }
    catch (IOException e) {
      log.error("Error parsing product license details: {}", productLicense.getLicenseDetails(), e);
    }
    catch (InvalidLicenseException e) {
      log.debug("Invalid license to calculate Firewall Metrics.");
    }
    return false;
  }

  private List<FirewallMetrics> flat(List<List<FirewallMetrics>> allMetrics) {
    return allMetrics.stream().flatMap(List::stream).collect(toList());
  }

  private void consolidateAndSaveFirewallMetrics(List<FirewallMetrics> allMetrics) {
    if (isNotEmpty(allMetrics)) {
      Collection<FirewallMetrics> metrics = allMetrics.stream()
          .collect(toMap(FirewallMetrics::getMetricsDate, identity(), (existingMetric, newMetric) -> {
            existingMetric.incrementMetricsValue(newMetric.getMetricsValue());
            return existingMetric;
          }))
          .values();

      log.info("Saving {} Firewall Metrics.", metrics.size());
      int insertBatchSize = 100;

      for (List<FirewallMetrics> partition : Lists.partition(new ArrayList<>(metrics), insertBatchSize)) {
        try (TransactionContext tx = firewallMetricsDAO.createTransactionContext()) {
          tx.begin();
          for (FirewallMetrics firewallMetrics : partition) {
            firewallMetrics.setMetricsLastUpdatedAt(processDate);
            firewallMetricsDAO.insert(tx, firewallMetrics);
          }
          tx.commit();
        }
      }
    }
  }

  private synchronized void logProgressIfNeeded(Consumer<Logger> consumer) {
    long now = System.currentTimeMillis();
    if (now - lastProgressLog >= 1000 * 30) {
      consumer.accept(log);
      lastProgressLog = now;
    }
  }

  private static class RepositoryPolicyViolationsMetrics
  {
    Collection<FirewallMetrics> namespaceAttacksBlockedMetrics;

    Collection<FirewallMetrics> supplyChainAttacksBlockedMetrics;
  }

  @VisibleForTesting
  void setRepositoryPolicyViolationsBatchSize(int repositoryPolicyViolationsBatchSize) {
    this.repositoryPolicyViolationsBatchSize = repositoryPolicyViolationsBatchSize;
  }
}
