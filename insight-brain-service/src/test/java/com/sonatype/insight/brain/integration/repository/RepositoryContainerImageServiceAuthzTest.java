/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration.repository;

import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationPollingResult;
import com.sonatype.clm.dto.model.repository.RepositoryType;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PersistedPolicyEvaluationPollingResultDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.PersistedPolicyEvaluationPollingResult;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.cyclonedx.Version;
import org.cyclonedx.exception.GeneratorException;
import org.cyclonedx.generators.BomGeneratorFactory;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.Component.Type;
import org.cyclonedx.model.Metadata;
import org.cyclonedx.model.Property;
import org.junit.Test;

import static com.sonatype.clm.dto.model.repository.container.image.FirewallContainerImageUtils.SONATYPE_NEXUS_REPOSITORY_BASE_URL_PROPERTY_NAME;

public class RepositoryContainerImageServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  private RepositoryContainerImageService service;

  @Inject
  private PersistedPolicyEvaluationPollingResultDAO persistedPolicyEvaluationPollingResultDAO;

  @Inject
  private OrganizationDAO organizationDAO;

  @Test(expected = UnauthenticatedException.class)
  public void testIsContainerImageQuarantined_unauthenticated() {
    service.isContainerImageQuarantined(repositoryManager.getInstanceId(), repository.getName(), "test-image");
  }

  @Test(expected = UnauthorizedException.class)
  public void testIsContainerImageQuarantined_unauthorized() {
    login();
    service.isContainerImageQuarantined(repositoryManager.getInstanceId(), repository.getName(), "test-image");
  }

  @Test(expected = NotFoundException.class)
  public void testIsContainerImageQuarantined_authorized() {
    repository = tempEntity.newRepository(repositoryManager, "docker-repo", RepositoryType.proxy, "docker");
    grantEvaluateComponentPermission(repository.getId());

    service.isContainerImageQuarantined(repositoryManager.getInstanceId(), repository.getName(), "test-image");
  }

  @Test(expected = UnauthenticatedException.class)
  public void testEvaluateContainerImage_unauthenticated() {
    service.evaluateContainerImage(repositoryManager.getInstanceId(), repository.getName(), buildTestBom(), null);
  }

  @Test(expected = UnauthorizedException.class)
  public void testEvaluateContainerImage_unauthorized() {
    login();
    service.evaluateContainerImage(repositoryManager.getInstanceId(), repository.getName(), buildTestBom(), null);
  }

  @Test
  public void testEvaluateContainerImage_authorized() {
    repository = tempEntity.newRepository(repositoryManager, "docker-repo", RepositoryType.proxy, "docker");
    grantEvaluateComponentPermission(repository.getId());

    service.evaluateContainerImage(repositoryManager.getInstanceId(), repository.getName(), buildTestBom(), null);
  }

  @Test(expected = UnauthenticatedException.class)
  public void testPollContainerImageEvaluationResult_unauthenticated() {
    Application application = tempEntity.newApplicationWithParent();
    insertPersistedPolicyEvaluationPollingResult("statusId", application.getId());
    service.pollContainerImageEvaluationResult(application.getPublicId(), "statusId");
  }

  @Test(expected = UnauthorizedException.class)
  public void testPollContainerImageEvaluationResult_unauthorized() {
    login();
    Application application = tempEntity.newApplicationWithParent();
    insertPersistedPolicyEvaluationPollingResult("statusId", application.getId());
    service.pollContainerImageEvaluationResult(application.getPublicId(), "statusId");
  }

  @Test
  public void testPollContainerImageEvaluationResult_authorized() {
    repository = tempEntity.newRepository(repositoryManager, "docker-repo", RepositoryType.proxy, "docker");

    Organization organization = tempEntity.newOrganization();
    organization.setRelatedRepositoryId(repository.getId());
    organizationDAO.update(organization);

    Application application = tempEntity.newApplicationWithParent(organization);
    insertPersistedPolicyEvaluationPollingResult("status-id", application.getId());

    grantEvaluateComponentPermission(application.getId());

    service.pollContainerImageEvaluationResult(application.getPublicId(), "status-id");
  }

  private String buildTestBom() {
    Bom bom = new Bom();
    Metadata metadata = new Metadata();
    bom.setMetadata(metadata);

    Component containerImageComponent = new Component();
    containerImageComponent.setType(Type.CONTAINER);
    containerImageComponent.setPurl(PackageUrlIdentifier.toPackageUrl(
        ComponentIdentifier.createContainerCoordinates("test-namespace", "test-image", "1.0.0")));
    metadata.setComponent(containerImageComponent);

    Property baseUrlProperty = new Property();
    baseUrlProperty.setName(SONATYPE_NEXUS_REPOSITORY_BASE_URL_PROPERTY_NAME);
    baseUrlProperty.setValue("https://test.repository-manager.com");
    metadata.addProperty(baseUrlProperty);

    try {
      return BomGeneratorFactory.createJson(Version.VERSION_16, bom).toJsonString();
    }
    catch (GeneratorException e) {
      throw new RuntimeException(e);
    }
  }

  private void insertPersistedPolicyEvaluationPollingResult(String statusId, String appId) {
    PolicyEvaluationPollingResult policyEvaluationPollingResult = new PolicyEvaluationPollingResult();
    policyEvaluationPollingResult.setReason("reason");
    PersistedPolicyEvaluationPollingResult expected =
        new PersistedPolicyEvaluationPollingResult(appId, statusId, policyEvaluationPollingResult);
    persistedPolicyEvaluationPollingResultDAO.insert(expected);
  }
}
