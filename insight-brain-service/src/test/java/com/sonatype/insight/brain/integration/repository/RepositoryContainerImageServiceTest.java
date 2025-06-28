/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import javax.inject.Inject;

import org.junit.Test;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.clm.dto.model.repository.RepositoryType;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

public class RepositoryContainerImageServiceTest
    extends AbstractComponentTest
{
  @Inject
  private RepositoryDAO repositoryDAO;

  @Inject
  private PolicyViolationDAO policyViolationDAO;

  @Inject
  private RepositoryContainerImageService service;

  @Test
  public void testIsContainerImageQuarantined_repositoryManagerNotExists() {
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> service.isContainerImageQuarantined("fake-repo-manager", "fake-repo", "fake-image"));
  }

  @Test
  public void testIsContainerImageQuarantined_repositoryNotExists() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();

    assertThatExceptionOfType(NotFoundException.class).isThrownBy(
        () -> service.isContainerImageQuarantined(repositoryManager.getInstanceId(), "fake-repo", "fake-image"));
  }

  @Test
  public void testIsContainerImageQuarantined_repositoryNotProxy() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, "docker-repo", RepositoryType.hosted, "docker");

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> service
        .isContainerImageQuarantined(repositoryManager.getInstanceId(), repository.getName(), "fake-image"))
        .withMessage("The repository must be of type proxy and format docker");
  }

  @Test
  public void testIsContainerImageQuarantined_repositoryNotDocker() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, "docker-repo", RepositoryType.proxy, "maven2");

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> service
        .isContainerImageQuarantined(repositoryManager.getInstanceId(), repository.getName(), "fake-image"))
        .withMessage("The repository must be of type proxy and format docker");
  }

  @Test
  public void testIsContainerImageQuarantined_containerImageNotExists() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, "docker-repo", RepositoryType.proxy, "docker");

    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> service
        .isContainerImageQuarantined(repositoryManager.getInstanceId(), repository.getName(), "fake-image"))
        .withMessage("No container image was found with public ID fake-image");
  }

  @Test
  public void testIsContainerImageQuarantined_containerImageNotFromSameRepository() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, "docker-repo", RepositoryType.proxy, "docker");
    Application application = tempEntity.newApplicationWithParent();

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> service.isContainerImageQuarantined(repositoryManager.getInstanceId(), repository.getName(),
            application.getPublicId()))
        .withMessage("No container image was found with public ID " + application.getPublicId());
  }

  @Test
  public void testIsContainerImageQuarantined_notInQuarantineNoPolicyViolations() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, "docker-repo", RepositoryType.proxy, "docker");
    Application application = tempEntity.newApplicationWithParent();
    repository.setRelatedOrganizationId(application.getOrganizationId());
    repositoryDAO.update(repository);

    boolean result = service.isContainerImageQuarantined(repositoryManager.getInstanceId(), repository.getName(),
        application.getPublicId());

    assertThat(result).isFalse();
  }

  @Test
  public void testIsContainerImageQuarantined_notInQuarantineNoPolicyViolationsFailingAtProxy() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, "docker-repo", RepositoryType.proxy, "docker");
    Application application = tempEntity.newApplicationWithParent();
    repository.setRelatedOrganizationId(application.getOrganizationId());
    repositoryDAO.update(repository);

    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), Stage.ID_PROXY, "scanId");
    Policy policy = tempEntity.newPolicy(application.getOrganizationId());
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    policyViolation.setActionTypeId(Action.ID_WARN);
    policyViolationDAO.update(policyViolation);

    boolean result = service.isContainerImageQuarantined(repositoryManager.getInstanceId(), repository.getName(),
        application.getPublicId());

    assertThat(result).isFalse();
  }

  @Test
  public void testIsContainerImageQuarantined_inQuarantine() {
    RepositoryManager repositoryManager = tempEntity.newRepositoryManager();
    Repository repository = tempEntity.newRepository(repositoryManager, "docker-repo", RepositoryType.proxy, "docker");
    Application application = tempEntity.newApplicationWithParent();
    repository.setRelatedOrganizationId(application.getOrganizationId());
    repositoryDAO.update(repository);

    PolicyEvaluation policyEvaluation = tempEntity.newPolicyEvaluation(application.getId(), Stage.ID_PROXY, "scanId");
    Policy policy = tempEntity.newPolicy(application.getOrganizationId());
    PolicyViolation policyViolation = tempEntity.newPolicyViolation(policyEvaluation, policy);
    policyViolation.setActionTypeId(Action.ID_FAIL);
    policyViolationDAO.update(policyViolation);

    boolean result = service.isContainerImageQuarantined(repositoryManager.getInstanceId(), repository.getName(),
        application.getPublicId());

    assertThat(result).isTrue();
  }
}
