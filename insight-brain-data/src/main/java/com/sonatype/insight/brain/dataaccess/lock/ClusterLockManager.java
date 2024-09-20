/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.lock;

import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.dataaccess.TransactionContext;

/**
 * Entry for the cluster lock mechanism. Provides accessors for easy use of the locking system.
 */
public interface ClusterLockManager
{
  String POLICY_VIOLATIONS_LOCK_PREFIX = "policy-violations-";

  String POLICY_VIOLATION_AGGREGATIONS_LOCK_PREFIX = "policy-violation-aggregations-";

  String REPOSITORY_COMPONENT_LOCK_PREFIX = "repository-component-";

  String REPOSITORY_REEVALUATION_LOCK_PREFIX = "repository-reevaluation-";

  String POLICY_EVALUATION_LOCK_PREFIX = "policy-evaluation-";

  String AUDIT_JSON_FILE_STORE_LOCK_PREFIX = "audit-json-file-store-";

  String SCHEMA_MIGRATION = "schema-migration";

  String SCHEMA_MIGRATION_IN_PROGRESS = "schema-migration-in-progress";

  String DATA_MIGRATION = "data-migration";

  String ASYNC_DB_MIGRATION = "async-db-migration";

  String NEW_INSTANCE_POPULATION = "new-instance-population";

  String PDF_GENERATION_LOCK_PREFIX = "pdf-generation-";

  String INACTIVE_REPOSITORY_VIOLATION_CLEANER = "inactive-repository-violation-cleaner";

  String FILENAME_LOCK_PREFIX = "filename-";

  boolean lockExists(String lockId);

  static String getLockIdForPolicyViolations(Application application) {
    return POLICY_VIOLATIONS_LOCK_PREFIX + application.getId();
  }

  static String getLockIdForPolicyViolationAggregations(String applicationId) {
    return POLICY_VIOLATION_AGGREGATIONS_LOCK_PREFIX + applicationId;
  }

  static String getLockIdForRepositoryComponent(String repositoryId, String componentPathname) {
    return REPOSITORY_COMPONENT_LOCK_PREFIX + repositoryId + "-" + componentPathname;
  }

  static String getLockIdForRepositoryReevaluation(Repository repository) {
    return REPOSITORY_REEVALUATION_LOCK_PREFIX + repository.getId();
  }

  static String getLockIdForPolicyEvaluation(Application application, String scanId) {
    return POLICY_EVALUATION_LOCK_PREFIX + application.getId() + "-" + scanId;
  }

  static String getLockIdForAuditJsonFileStore(String ownerId) {
    return AUDIT_JSON_FILE_STORE_LOCK_PREFIX + ownerId;
  }

  static String getLockIdForSchemaMigration() {
    return SCHEMA_MIGRATION;
  }

  static String getLockIdForSchemaMigrationInProgress() {
    return SCHEMA_MIGRATION_IN_PROGRESS;
  }

  static String getLockIdForDataMigration() {
    return DATA_MIGRATION;
  }

  static String getLockIdForAsyncDbMigration(String jobName) {
    return ASYNC_DB_MIGRATION + "-" + jobName;
  }

  static String getLockIdForNewInstancePopulation() {
    return NEW_INSTANCE_POPULATION;
  }

  static String getLockIdForPdfGeneration(Application application, String scanId) {
    return PDF_GENERATION_LOCK_PREFIX + application.getId() + "-" + scanId;
  }

  static String getLockIdForInactiveRepositoryViolationCleaner() {
    return INACTIVE_REPOSITORY_VIOLATION_CLEANER;
  }

  static String getLockIdForFilename(String filename) {
    return FILENAME_LOCK_PREFIX + filename;
  }

  ClusterLock createForPolicyViolations(Application application);

  void deleteForPolicyViolations(TransactionContext tx, Application application);

  ClusterLock createForPolicyViolationAggregations(String applicationId);

  void deleteForPolicyViolationAggregations(TransactionContext tx, String applicationId);

  ClusterLock createForRepositoryComponent(String repositoryId, String componentPathname);

  void deleteForRepositoryComponent(TransactionContext tx, String repositoryId, String componentPathname);

  void deleteForRepository(TransactionContext tx, String repositoryId);

  ClusterLock createForRepositoryReevaluation(Repository repository);

  void deleteForRepositoryReevaluation(TransactionContext tx, Repository repository);

  ClusterLock createForPolicyEvaluation(Application application, String scanId);

  void deleteForPolicyEvaluation(TransactionContext tx, Application application, String scanId);

  void deleteForPolicyEvaluations(TransactionContext tx, Application application);

  ClusterLock createForAuditJsonFileStore(String ownerId);

  void deleteForAuditJsonFileStore(TransactionContext tx, String ownerId);

  ClusterLock createForSchemaMigration();

  void deleteForSchemaMigration();

  ClusterLock createForSchemaMigrationInProgress();

  void deleteForSchemaMigrationInProgress();

  ClusterLock createForDataMigration();

  void deleteForDataMigration();

  ClusterLock createForAsyncDbMigration(String jobName);

  void deleteForAsyncDbMigration(String jobName);

  ClusterLock createForNewInstancePopulation();

  void deleteForNewInstancePopulation();

  ClusterLock createForPdfGeneration(Application application, String scanId);

  void deleteForPdfGeneration(TransactionContext tx, Application application);

  ClusterLock createForInactiveRepositoryViolationCleaner();

  void deleteForInactiveRepositoryViolationCleaner();

  ClusterLock createForFilename(String filename);

  void deleteForFilename(String filename);
}
