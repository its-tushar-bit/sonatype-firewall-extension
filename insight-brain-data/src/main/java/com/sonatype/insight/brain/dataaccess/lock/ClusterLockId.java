/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.lock;

/**
 * IDs for ClusterLocks, which consist of either one or two strings. The first/only string is a "class" - a category
 * of lock. Some lock classes require a second string while all other lock classes prohibit it. Where applicable, the
 * second string should be an identifier for the object being locked, unique among all objects of that class.
 *
 * Note that the nomenclature, "class" and "object ID", refers to the columns in postgres' `pg_locks` table where
 * the values derived from these fields will appear, in the postgres implementation of ClusterLock.
 */
public sealed interface ClusterLockId
{
  /*
   * Convenience accessors and creators for all types of ClusterLockIds
   */

  static ClusterLockId forSchemaMigration() {
    return SimpleId.SCHEMA_MIGRATION;
  }

  static ClusterLockId forSchemaMigrationInProgress() {
    return SimpleId.SCHEMA_MIGRATION_IN_PROGRESS;
  }

  static ClusterLockId forDataMigration() {
    return SimpleId.DATA_MIGRATION;
  }

  static ClusterLockId forNewInstancePopulation() {
    return SimpleId.NEW_INSTANCE_POPULATION;
  }

  static ClusterLockId forInactiveRepositoryViolationCleaner() {
    return SimpleId.INACTIVE_REPOSITORY_VIOLATION_CLEANER;
  }

  static ClusterLockId forPolicyViolations(String applicationId) {
    return new CompoundId(CompoundIdClass.POLICY_VIOLATIONS, applicationId);
  }

  static ClusterLockId forPolicyViolationAggregations(String applicationId) {
    return new CompoundId(CompoundIdClass.POLICY_VIOLATION_AGGREGATIONS, applicationId);
  }

  static ClusterLockId forRepositoryComponent(String repositoryId, String componentPathname) {
    if (repositoryId == null || componentPathname == null) {
      throw new NullPointerException("Repository ID and component pathname must not be null");
    }
    return new CompoundId(CompoundIdClass.REPOSITORY_COMPONENT, repositoryId + "-" + componentPathname);
  }

  /**
   * @Deprecated will be removed after table based lock mechanism is removed
   */
  static String prefixForRepositoryComponents(String repositoryId) {
    if (repositoryId == null) {
      throw new NullPointerException("Repository ID must not be null");
    }
    return new CompoundId(CompoundIdClass.REPOSITORY_COMPONENT, repositoryId + "-").getOldStyleLockId();
  }

  static ClusterLockId forRepositoryReevaluation(String repositoryId) {
    return new CompoundId(CompoundIdClass.REPOSITORY_REEVALUATION, repositoryId);
  }

  static ClusterLockId forPolicyEvaluation(String applicationId, String scanId) {
    if (applicationId == null || scanId == null) {
      throw new NullPointerException("Application ID and scan ID must not be null");
    }
    return new CompoundId(CompoundIdClass.POLICY_EVALUATION, applicationId + "-" + scanId);
  }

  /**
   * @Deprecated will be removed after table based lock mechanism is removed
   */
  static String prefixForPolicyEvaluations(String applicationId) {
    if (applicationId == null) {
      throw new NullPointerException("Application ID must not be null");
    }
    return new CompoundId(CompoundIdClass.POLICY_EVALUATION, applicationId + "-").getOldStyleLockId();
  }

  static ClusterLockId forAuditJsonFileStore(String ownerId) {
    return new CompoundId(CompoundIdClass.AUDIT_JSON_FILE_STORE, ownerId);
  }

  static ClusterLockId forPdfGeneration(String applicationId, String scanId) {
    if (applicationId == null || scanId == null) {
      throw new NullPointerException("Application ID and scan ID must not be null");
    }
    return new CompoundId(CompoundIdClass.PDF_GENERATION, applicationId + "-" + scanId);
  }

  /**
   * @Deprecated will be removed after table based lock mechanism is removed
   */
  static String prefixForPdfGeneration(String applicationId) {
    if (applicationId == null) {
      throw new NullPointerException("Application ID must not be null");
    }
    return new CompoundId(CompoundIdClass.PDF_GENERATION, applicationId + "-").getOldStyleLockId();
  }

  /**
   * @Deprecated will be removed after table based lock mechanism is removed
   */
  String getOldStyleLockId();

  /**
   * ClusterLockIds that consist only of a class and not a second string.
   */
  enum SimpleId
      implements ClusterLockId
  {
    /*
     * In the DB, the ordinal values of these enums are used, along with an unset high bit, as the high byte of the
     * classid (the tenant hashCode fills out the low bytes, see PostgresAdvisoryLockDAO).
     * Thus, locks corresponding to these classes will appear in the DB with decimal values in ranges as follows
     */
    // 0 - 16777215
    SCHEMA_MIGRATION("schema-migration"),
    // 16777216 - 3355443
    DATA_MIGRATION("data-migration"),
    // 33554432 - 50331647
    NEW_INSTANCE_POPULATION("new-instance-population"),
    // 50331648 - 67108863
    INACTIVE_REPOSITORY_VIOLATION_CLEANER("inactive-repository-violation-cleaner"),
    // 67108864 - 83886079
    SCHEMA_MIGRATION_IN_PROGRESS("schema-migration-in-progress");
    // MTIQ note: due to the use of ordinals in the values sent to the db, new values should always be added to
    // the end of the enum

    private final String lockId;

    SimpleId(String lockId) {
      this.lockId = lockId;
    }

    @Override
    public String toString() {
      return lockId;
    }

    @Override
    public String getOldStyleLockId() {
      return lockId;
    }
  }

  /**
   * Classes of lock IDs that require a second string.
   */
  enum CompoundIdClass
  {
    /*
     * In the DB, the ordinal values of these enums are used, along with a set high bit, as the high byte of the
     * classid (the tenant hashCode fills out the low bytes, see PostgresAdvisoryLockDAO).
     * Thus, locks corresponding to these classes will appear in the DB with decimal values in ranges as follows
     */
    // 2147483648 - 2164260863
    POLICY_VIOLATIONS("policy-violations"),
    // 2164260864 - 2181038079
    POLICY_VIOLATION_AGGREGATIONS("policy-violation-aggregations"),
    // 2181038080 - 2197815295
    REPOSITORY_COMPONENT("repository-component"),
    // 2197815296 - 2214592511
    REPOSITORY_REEVALUATION("repository-reevaluation"),
    // 2214592512 - 2231369727
    POLICY_EVALUATION("policy-evaluation"),
    // 2231369728 - 2248146943
    AUDIT_JSON_FILE_STORE("audit-json-file-store"),
    // 2248146944 - 2264924159
    PDF_GENERATION("pdf-generation");
    // MTIQ note: due to the use of ordinals in the values sent to the db, new values should always be added to
    // the end of the enum

    private final String classStr;

    CompoundIdClass(String classStr) {
      this.classStr = classStr;
    }

    @Override
    public String toString() {
      return classStr;
    }
  }

  /**
   * ClusterLockIds that consist of a class and a second string.
   */
  record CompoundId(CompoundIdClass lockClass, String lockObjId)
      implements ClusterLockId
  {
    public CompoundId {
      if (lockObjId == null || lockClass == null) {
        throw new NullPointerException("Lock class and object ID must not be null");
      }
    }

    @Override
    public String getOldStyleLockId() {
      return lockClass.toString() + "-" + lockObjId;
    }
  }
}
