/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration.repository;

import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.component.ProprietaryComponentNames;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataList;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList;
import com.sonatype.clm.dto.model.component.UnquarantinedComponentList;
import com.sonatype.clm.dto.model.policy.RepositoryPolicyEvaluationSummary;
import com.sonatype.clm.dto.model.repository.ConfigureRepositoriesRequest;
import com.sonatype.clm.dto.model.repository.QuarantinedComponentReport;
import com.sonatype.clm.dto.model.repository.RepositoryDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRepositoryDTO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryManagerDAO;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.hash.Hashing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wrapper designed for {@link ArtifactoryRepositoryService} to handle the data migration between the plugin versions
 * 1.x and 2.x. INT-2403 has full details, but for reference the 1.x version of the plugin incorrectly used the
 * 'repository manager instance id' value. In order to permanently work around and resolve this issue, this wrapper
 * class was introduced to sit between {@link ArtifactoryRepositoryResource} and {@link ArtifactoryRepositoryService}
 * and correctly resolve the value to use for the 'repository manager instance id'.
 *
 * See the {@link #getRepositoryManagerInstanceId(String, String)} for details on the migration.
 *
 * Note: This class intentionally does not implement any interface to follow a decorator pattern or anything similar.
 * The intention is to force any future work to go through this wrapper to make it clear that the repository manager
 * instance ID <B>MUST BE</B> processed first before it can be passed onto the real {@link ArtifactoryRepositoryService}
 */
@Named
public class ArtifactoryRepositoryServiceWrapper
{
  private static final Logger log = LoggerFactory.getLogger(ArtifactoryRepositoryServiceWrapper.class);

  private final ArtifactoryRepositoryService repositoryService;

  private final RepositoryManagerDAO repositoryManagerDAO;

  private final RepositoryDAO repositoryDAO;

  @Inject
  public ArtifactoryRepositoryServiceWrapper(
      final ArtifactoryRepositoryService repositoryService,
      final RepositoryDAO repositoryDAO,
      final RepositoryManagerDAO repositoryManagerDAO)
  {
    this.repositoryService = repositoryService;
    this.repositoryManagerDAO = repositoryManagerDAO;
    this.repositoryDAO = repositoryDAO;
  }

  /**
   * Artifactory Plugin 1.x generated the repository manager ID as the SHA256 of the repository ID, limited to 50 chars.
   * This method copies that legacy behaviour here so we can calculate if a migration is needed.
   */
  @VisibleForTesting
  static String getLegacyRepositoryInstanceId(final String repositoryId) {
    return Hashing.sha256().hashString(repositoryId, StandardCharsets.UTF_8).toString().substring(0, 50);
  }

  ApiRepositoryDTO setAuditEnabled(
      final String repositoryManagerInstanceId,
      final String repositoryPublicId,
      final boolean enabled,
      final String clientUserAgent)
  {
    return repositoryService.setAuditEnabled(
        getRepositoryManagerInstanceId(repositoryManagerInstanceId, repositoryPublicId),
        repositoryPublicId, enabled, clientUserAgent);
  }

  RepositoryPolicyEvaluationSummary getPolicyEvaluationSummary(
      final String repositoryManagerInstanceId,
      final String repositoryPublicId,
      final String clientUserAgent)
  {
    return repositoryService.getPolicyEvaluationSummary(
        getRepositoryManagerInstanceId(repositoryManagerInstanceId, repositoryPublicId), repositoryPublicId,
        clientUserAgent);
  }

  String getRepositoryResultsUrl(
      final String repositoryManagerInstanceId,
      final String repositoryPublicId,
      final String clientUserAgent)
  {
    return repositoryService.getRepositoryResultsUrl(
        getRepositoryManagerInstanceId(repositoryManagerInstanceId, repositoryPublicId), repositoryPublicId,
        clientUserAgent);
  }

  RepositoryComponentEvaluationDataList evaluateComponents(
      final String repositoryManagerInstanceId,
      final String repositoryPublicId,
      final RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList,
      final boolean withQuarantine,
      final String clientUserAgent)
  {
    return repositoryService
        .evaluateComponents(getRepositoryManagerInstanceId(repositoryManagerInstanceId, repositoryPublicId),
            repositoryPublicId,
            componentEvaluationDataRequestList,
            withQuarantine, clientUserAgent);
  }

  RepositoryComponentEvaluationDataList evaluateComponentMetadata(
      String repositoryManagerInstanceId,
      String repositoryPublicId,
      RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList,
      String clientUserAgent)
  {
    return repositoryService.evaluateComponentMetadata(repositoryManagerInstanceId, repositoryPublicId,
        componentEvaluationDataRequestList, clientUserAgent);
  }

  void setQuarantine(
      final String repositoryManagerInstanceId,
      final String repositoryPublicId,
      final boolean enabled,
      final String clientUserAgent)
  {
    repositoryService.setQuarantine(getRepositoryManagerInstanceId(repositoryManagerInstanceId, repositoryPublicId),
        repositoryPublicId, enabled, clientUserAgent);
  }

  void removeComponent(
      final String repositoryManagerInstanceId,
      final String repositoryPublicId,
      final String pathname,
      final String clientUserAgent)
  {
    repositoryService.removeComponent(getRepositoryManagerInstanceId(repositoryManagerInstanceId, repositoryPublicId),
        repositoryPublicId, pathname, clientUserAgent);
  }

  UnquarantinedComponentList getUnquarantinedComponents(
      final String repositoryManagerInstanceId,
      final String repositoryPublicId,
      final long sinceUtcTimestamp,
      final String clientUserAgent)
  {
    return repositoryService
        .getUnquarantinedComponents(getRepositoryManagerInstanceId(repositoryManagerInstanceId, repositoryPublicId),
            repositoryPublicId, sinceUtcTimestamp, clientUserAgent);
  }

  void addProprietaryComponentNames(
      final String repositoryManagerInstanceId,
      final String repositoryPublicId,
      final ProprietaryComponentNames proprietaryComponentNames)
  {
    repositoryService.addProprietaryComponentNames(
        getRepositoryManagerInstanceId(repositoryManagerInstanceId, repositoryPublicId), repositoryPublicId,
        proprietaryComponentNames);
  }

  /**
   * Return the correct 'repository manager id' for the situation. Resolves INT-2403 where there is a data issue in
   * plugin version 1.x which incorrectly used a hash of the repository name as the 'repository manager id'. This
   * method will return the correct value based on any migration that is required.
   * <ul>
   *   <li>If plugin version 2.x <B>AND</B> configured repository manager ID, then perform migration <U>if necessary</U>
   *   </li>
   *   <li>If plugin version 2.x and <B>NO CONFIGURED</B> repository manager ID, then return old value</li>
   *   <li>If plugin version 1.x then return old value</li>
   * </ul>
   *
   * @param repositoryManagerInstanceId The current repository manager instance ID (old format or new format)
   * @param repositoryPublicId          The repository instance attached to the repository manager you are working with
   * @return The correct, and possibly migrated, repository manager instance ID
   */
  @VisibleForTesting
  String getRepositoryManagerInstanceId(final String repositoryManagerInstanceId, final String repositoryPublicId) {
    // Has this ID been migrated yet? The old/wrong way the Artifactory plugin did it is just by computing the repo
    // manager ID as the sha256 of the repositoryId (limited to 50 chars). Calculate this value here for comparison.
    String hashRepositoryId = getLegacyRepositoryInstanceId(repositoryPublicId);

    // Is the value being sent from Artifactory still the old/wrong/hash version? If so just return it to use as we
    // can't migrate it on this side until we start getting the new version from the plugin.
    if (repositoryManagerInstanceId.equals(hashRepositoryId)) {
      return repositoryManagerInstanceId;
    }

    // If we are here then the repositoryManagerInstanceId *IS NOT* the sha256 of the repositoryId which, at minimum,
    // means the plugin is version 2.x *AND* correctly sending the configured repository manager ID.

    // Now we need to determine if we need a migration.
    // First try to load it out the *repository* by its public ID and the new the new RM value.
    Repository repositoryByNewId = repositoryDAO
        .getByRepositoryManagerInstanceIdAndPublicId(repositoryManagerInstanceId, repositoryPublicId);

    // Got it by the new value! No migration needed for this repository record. Once a system is migrated the code
    // will never get past here.
    if (repositoryByNewId != null) {
      return repositoryManagerInstanceId;
    }

    // Check if we can load it out by the old hashed repository ID
    Repository repositoryByHashId = repositoryDAO
        .getByRepositoryManagerInstanceIdAndPublicId(hashRepositoryId, repositoryPublicId);
    if (repositoryByHashId == null) {
      // Didn't find it, must just be a brand new record
      return repositoryManagerInstanceId;
    }

    // Else, we need to do a migration
    return doMigration(repositoryManagerInstanceId, hashRepositoryId, repositoryPublicId);
  }

  /**
   * Perform a migration of the repository manager ID. There are two possible situations:
   * <or>
   * <li>First time ever for a repository manager. The new record will be inserted into the database, and the
   * `repository` record will be updated to the new record. Then the old `repository_manager` record will be
   * deleted.</li>
   * <li>For every subsequent repository on the same repository manager, we only need to update the reference to the
   * existing repository manager, and delete the old `repository_manager` record.
   * </or>
   *
   * Before migration:
   * <pre>
   * ┌───────────────────────┬─────────────┐
   * │ repository_manager_id │ instance_id │
   * ├───────────────────────┼─────────────┤
   * │ 1                     │ abc         │
   * │ 2                     │ def         │
   * │ 3                     │ ghi         │
   * │ 4                     │ jkl         │
   * └───────────────────────┴─────────────┘
   * ┌───────────────┬───────────────────────┬───────────┬─────┐
   * │ repository_id │ repository_manager_id │ public_id │ ... │
   * ├───────────────┼───────────────────────┼───────────┼─────┤
   * │ 6             │ 1                     │ mvn-one   │ ... │
   * │ 7             │ 2                     │ mvn-two   │ ... │
   * │ 8             │ 3                     │ mvn-three │ ... │
   * │ 9             │ 4                     │ mvn-four  │ ... │
   * └───────────────┴───────────────────────┴───────────┴─────┘
   *
   * After migration:
   * ┌───────────────────────┬─────────────────┐
   * │ repository_manager_id │ instance_id     │
   * ├───────────────────────┼─────────────────┤
   * │ 10                    │ artifactory-one │
   * │ 11                    │ artifactory-two │
   * └───────────────────────┴─────────────────┘
   * ┌───────────────┬───────────────────────┬───────────┬─────┐
   * │ repository_id │ repository_manager_id │ public_id │ ... │
   * ├───────────────┼───────────────────────┼───────────┼─────┤
   * │ 6             │ 10                    │ mvn-one   │ ... │
   * │ 7             │ 10                    │ mvn-two   │ ... │
   * │ 8             │ 11                    │ mvn-three │ ... │
   * │ 9             │ 11                    │ mvn-four  │ ... │
   * └───────────────┴───────────────────────┴───────────┴─────┘
   * </pre>
   *
   * When `mvn-one` is requested we insert record 10 and update mvn-one to point to it, then delete record 1.
   * When `mvn-two` is requested we update mvn-two to point to record 10, then delete record 2.
   *
   * @param newRepositoryManagerInstanceId The new and correct repository manager ID
   * @param hashRepositoryId               The old and incorrect hash used for the repository manager ID
   * @param repositoryPublicId             The repository ID that needs migration. The legacy/broken approach had a 1-1
   *                                       mapping between a repository manager and repository. The migration will fix
   *                                       this relationship, and we need the repository public ID to verify.
   * @return the newly migrated repository manager instance ID
   */
  private String doMigration(
      final String newRepositoryManagerInstanceId,
      final String hashRepositoryId,
      final String repositoryPublicId)
  {
    log.info(
        "Performing Artifactory Repository Manager ID migration. Updating '{}' to '{}' for repository '{}'",
        hashRepositoryId, newRepositoryManagerInstanceId, repositoryPublicId);

    // Load the repository record by the old hash RM id
    Repository repositoryByHashId = repositoryDAO
        .getByRepositoryManagerInstanceIdAndPublicId(hashRepositoryId, repositoryPublicId);

    // Determine if the new repository manager record has already been inserted or not
    RepositoryManager repositoryManager = repositoryManagerDAO.getByInstanceId(newRepositoryManagerInstanceId);

    if (repositoryManager == null) {
      // Doesn't exist yet, we need to insert
      repositoryManager = new RepositoryManager(newRepositoryManagerInstanceId);
      repositoryManagerDAO.insert(repositoryManager);
      log.debug("Inserted new repository manager '{}'", newRepositoryManagerInstanceId);
    }

    // At this point, for both cases, we have a repository manager record and need to update the repository record to
    // point to it, then delete the old repository manager record
    repositoryByHashId.setRepositoryManagerId(repositoryManager.getId());
    repositoryDAO.update(repositoryByHashId);

    // delete old and now-orphaned repository manager record
    RepositoryManager oldRepositoryManager = repositoryManagerDAO.getByInstanceId(hashRepositoryId);
    repositoryManagerDAO.delete(oldRepositoryManager);

    // Return the new repository manager instance ID from the updated hash record
    return repositoryManager.getInstanceId();
  }

  QuarantinedComponentReport getQuarantinedComponentReportUrl(
      String repositoryManagerInstanceId,
      String repositoryPublicId,
      String pathname,
      String clientUserAgent)
  {
    return repositoryService.getQuarantinedComponentReportUrl(
        getRepositoryManagerInstanceId(repositoryManagerInstanceId, repositoryPublicId), repositoryPublicId,
        pathname, clientUserAgent);
  }

  void configureRepositories(
      String repositoryManagerInstanceId,
      ConfigureRepositoriesRequest configureRepositoriesRequest,
      String clientUserAgent)
  {
    repositoryService.configureRepositories(repositoryManagerInstanceId, configureRepositoriesRequest, clientUserAgent);
  }

  void removeRepository(String repositoryManagerInstanceId, String repositoryPublicId) {
    repositoryService.removeRepository(repositoryManagerInstanceId, repositoryPublicId);
  }

  List<RepositoryDTO> getConfiguredRepositories(
      String repositoryManagerInstanceId,
      Long sinceUtcTimestamp,
      String clientUserAgent)
  {
    return repositoryService.getConfiguredRepositories(repositoryManagerInstanceId, sinceUtcTimestamp, clientUserAgent);
  }
}
