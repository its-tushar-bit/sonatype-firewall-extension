/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration.repository;

import java.util.Collections;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.RepositoryComponentEvaluationDataRequestList;
import com.sonatype.clm.dto.model.repository.ConfigureRepositoriesRequest;
import com.sonatype.clm.dto.model.repository.RepositoryDTO;
import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.audit.AuditDTO;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import org.cyclonedx.Version;
import org.cyclonedx.generators.BomGeneratorFactory;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.Component.Type;
import org.cyclonedx.model.Metadata;
import org.cyclonedx.model.Property;
import org.junit.Test;

import static com.sonatype.clm.dto.model.repository.container.image.FirewallContainerImageUtils.SONATYPE_NEXUS_REPOSITORY_BASE_URL_PROPERTY_NAME;
import org.junit.experimental.categories.Category;

public class RepositoryResourceAuditTest
    extends AbstractRepositoryResourceAuditTest
{
  @Override
  protected String getResourcePath() {
    return RepositoryResource.RESOURCE_PATH;
  }

  @Test
  public void testEvaluateComponentsAdhoc_Unauthorized() throws Exception {
    Repository repository = tempEntity.newRepository(REPOSITORY_MANAGER_INSTANCE_ID, REPOSITORY_PUBLIC_ID);
    evaluateAdhocRequest(new RepositoryComponentEvaluationDataRequestList()).with(unauthorizedUser()).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.EVALUATE_AD_HOC, "unauthorized");
    assertRepositoryData(auditDTO, repository);
  }

  @Test
  public void testEvaluateComponentsAdhoc_OneComponent() throws Exception {
    testEvaluateComponentsAdhoc(1);
  }

  @Test
  public void testEvaluateComponentsAdhoc_TwoComponents() throws Exception {
    testEvaluateComponentsAdhoc(2);
  }

  @Test
  public void testEvaluateComponentsAdhoc_NoComponents() throws Exception {
    testEvaluateComponentsAdhoc(0);
  }

  private HttpRequest evaluateAdhocRequest(
      RepositoryComponentEvaluationDataRequestList componentEvaluationDataRequestList)
  {
    return restRequest()
        .path(getResourcePath())
        .path(RepositoryResource.EVALUATE_COMPONENTS_ADHOC_PATH)
        .parameter(REPOSITORY_MANAGER_INSTANCE_ID, REPOSITORY_PUBLIC_ID)
        .body(componentEvaluationDataRequestList);
  }

  private void testEvaluateComponentsAdhoc(int count) throws Exception {
    Repository repository = tempEntity.newRepository(REPOSITORY_MANAGER_INSTANCE_ID, REPOSITORY_PUBLIC_ID);
    RepositoryComponentEvaluationDataRequestList repoComponentEvalList = repoComponentEvalList(count);
    repoComponentEvalList.cause = RepositoryComponentEvaluationDataRequestList.ADHOC;

    evaluateAdhocRequest(repoComponentEvalList).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.EVALUATE_AD_HOC, null);
    assertRepositoryData(auditDTO, repository);
    assertRepositoryEvaluationData(auditDTO, count, RepositoryComponentEvaluationDataRequestList.ADHOC);
  }

  @Test
  public void testRemoveProprietaryComponentNames() throws Exception {
    restRequest().path(getResourcePath(), AbstractRepositoryResource.PROPRIETARY_NAMES_PATH)
        .parameter(REPOSITORY_MANAGER_INSTANCE_ID, REPOSITORY_PUBLIC_ID).delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.REMOVE_PROPRIETARY_COMPONENT_NAMES, null);
    assertCustomData(auditDTO, "repositoryManagerInstanceId", REPOSITORY_MANAGER_INSTANCE_ID);
    assertCustomData(auditDTO, "repositoryPublicId", REPOSITORY_PUBLIC_ID);
  }

  @Test
  public void testRemoveProprietaryComponentNames_Unauthorized() throws Exception {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Repository repo = tempEntity.newHostedRepository(repoManager, "testPublicId", "npm", true);
    restRequest().path(getResourcePath(), AbstractRepositoryResource.PROPRIETARY_NAMES_PATH)
        .parameter(repoManager.getInstanceId(), repo.getPublicId()).with(unauthorizedUser()).delete();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.REMOVE_PROPRIETARY_COMPONENT_NAMES, "unauthorized");
    assertCustomData(auditDTO, "repositoryManagerInstanceId", repoManager.getInstanceId());
    assertCustomData(auditDTO, "repositoryPublicId", repo.getPublicId());
  }

  @Test
  public void testEvaluateContainerImage() throws Exception {
    Repository repository = tempEntity.newRepository(REPOSITORY_MANAGER_INSTANCE_ID, REPOSITORY_PUBLIC_ID);
    String bomJson = buildTestBom();

    mockReport("SCAN-ID", "/" + getClass().getSimpleName() + "/report");

    evaluateContainerImageRequest(bomJson).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.EVALUATE_REPOSITORY, null);
    assertRepositoryData(auditDTO, repository);
  }

  @Test
  public void testEvaluateContainerImage_Unauthorized() throws Exception {
    Repository repository = tempEntity.newRepository(REPOSITORY_MANAGER_INSTANCE_ID, REPOSITORY_PUBLIC_ID);
    String bomJson = buildTestBom();

    evaluateContainerImageRequest(bomJson).with(unauthorizedUser()).post();

    AuditDTO auditDTO = assertAuditLog(AuditEvent.EVALUATE_REPOSITORY, "unauthorized");
    assertRepositoryData(auditDTO, repository);
  }

  private HttpRequest evaluateContainerImageRequest(String bomJson) {
    return restRequest()
        .path(getResourcePath())
        .path(RepositoryResource.EVALUATE_CONTAINER_IMAGE_PATH)
        .parameter(REPOSITORY_MANAGER_INSTANCE_ID, REPOSITORY_PUBLIC_ID)
        .body(bomJson);
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
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected ConfigureRepositoriesRequest createConfigureRepositoriesRequest(RepositoryDTO repositoryDTO) {
    return new ConfigureRepositoriesRequest("Nexus", "3.60.0", Collections.singletonList(repositoryDTO));
  }
}
