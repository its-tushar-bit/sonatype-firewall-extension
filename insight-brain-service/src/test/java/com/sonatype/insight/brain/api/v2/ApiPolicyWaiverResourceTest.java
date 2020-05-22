/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.Arrays;
import java.util.List;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiPolicyWaiverDTO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Test;

import static com.sonatype.insight.brain.api.v2.ApiPolicyWaiverResource.BY_POLICY_WAIVER_ID_PATH;
import static com.sonatype.insight.brain.model.repository.RepositoryContainer.REPOSITORY_CONTAINER_ID;
import static org.assertj.core.api.Assertions.assertThat;

public class ApiPolicyWaiverResourceTest
    extends AbstractResourceTest
{
  @Test
  public void testDeletePolicyWaiver() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(application);
    PolicyWaiver policyWaiver = tempEntity.newWaiver(policy.getId(), application.getId());

    HttpResponse response = restRequest()
        .path(BY_POLICY_WAIVER_ID_PATH)
        .parameter(OwnerType.APPLICATION, application.getId(), policyWaiver.getId())
        .delete();

    assertResponseStatus(204, response);
    assertThat(new PolicyWaiverDAO().getById(policyWaiver.getId())).isNull();
  }

  @Test
  public void testGetPolicyWaivers_Application() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    Policy policy = tempEntity.newPolicy(application);
    PolicyWaiver policyWaiver = tempEntity.newWaiver(policy.getId(), application.getId());

    HttpResponse response = restRequest().parameter(OwnerType.APPLICATION, application.getId()).get();

    assertResponseStatus(200, response);

    List<ApiPolicyWaiverDTO> policyWaiverDtoList = Arrays.asList(response.getBody(ApiPolicyWaiverDTO[].class));
    assertThat(policyWaiverDtoList).hasSize(1);

    ApiPolicyWaiverDTO apiPolicyWaiverDTO = policyWaiverDtoList.get(0);
    assertThat(apiPolicyWaiverDTO.policyWaiverId).isEqualTo(policyWaiver.getId());
    assertThat(apiPolicyWaiverDTO.comment).isEqualTo(policyWaiver.getComment());
    assertThat(apiPolicyWaiverDTO.createTime).isEqualTo(policyWaiver.getCreateTime());
    assertThat(apiPolicyWaiverDTO.hash).isEqualTo(policyWaiver.getHash());
    assertThat(apiPolicyWaiverDTO.policyId).isEqualTo(policyWaiver.getPolicyId());
    assertThat(apiPolicyWaiverDTO.scopeOwnerId).isEqualTo(application.getId());
    assertThat(apiPolicyWaiverDTO.scopeOwnerName).isEqualTo(application.getName());
    assertThat(apiPolicyWaiverDTO.scopeOwnerType).isEqualTo(OwnerType.APPLICATION.toString());
  }

  @Test
  public void testGetPolicyWaivers_Organization() throws Exception {
    Organization organization = tempEntity.newOrganization();
    Policy policy = tempEntity.newPolicy(organization);
    PolicyWaiver policyWaiver = tempEntity.newWaiver(policy.getId(), organization.getId());

    HttpResponse response = restRequest().parameter(OwnerType.ORGANIZATION, organization.getId()).get();

    assertResponseStatus(200, response);

    List<ApiPolicyWaiverDTO> policyWaiverDtoList = Arrays.asList(response.getBody(ApiPolicyWaiverDTO[].class));
    assertThat(policyWaiverDtoList).hasSize(1);

    ApiPolicyWaiverDTO apiPolicyWaiverDTO = policyWaiverDtoList.get(0);
    assertThat(apiPolicyWaiverDTO.policyWaiverId).isEqualTo(policyWaiver.getId());
    assertThat(apiPolicyWaiverDTO.comment).isEqualTo(policyWaiver.getComment());
    assertThat(apiPolicyWaiverDTO.createTime).isEqualTo(policyWaiver.getCreateTime());
    assertThat(apiPolicyWaiverDTO.hash).isEqualTo(policyWaiver.getHash());
    assertThat(apiPolicyWaiverDTO.policyId).isEqualTo(policyWaiver.getPolicyId());
    assertThat(apiPolicyWaiverDTO.scopeOwnerId).isEqualTo(organization.getId());
    assertThat(apiPolicyWaiverDTO.scopeOwnerName).isEqualTo(organization.getName());
    assertThat(apiPolicyWaiverDTO.scopeOwnerType).isEqualTo(OwnerType.ORGANIZATION.toString());
  }

  @Test
  public void testGetPolicyWaivers_Repository() throws Exception {
    Repository repository = tempEntity.newRepository();
    Policy policy = tempEntity.newPolicy();
    PolicyWaiver policyWaiver = tempEntity.newWaiver("hash", policy.getId(), repository.getId(), "comment");

    HttpResponse response = restRequest().parameter(OwnerType.REPOSITORY, repository.getId()).get();

    assertResponseStatus(200, response);

    List<ApiPolicyWaiverDTO> policyWaiverDtoList = Arrays.asList(response.getBody(ApiPolicyWaiverDTO[].class));
    assertThat(policyWaiverDtoList).hasSize(1);

    ApiPolicyWaiverDTO apiPolicyWaiverDTO = policyWaiverDtoList.get(0);
    assertThat(apiPolicyWaiverDTO.policyWaiverId).isEqualTo(policyWaiver.getId());
    assertThat(apiPolicyWaiverDTO.comment).isEqualTo(policyWaiver.getComment());
    assertThat(apiPolicyWaiverDTO.createTime).isEqualTo(policyWaiver.getCreateTime());
    assertThat(apiPolicyWaiverDTO.hash).isEqualTo(policyWaiver.getHash());
    assertThat(apiPolicyWaiverDTO.policyId).isEqualTo(policyWaiver.getPolicyId());
    assertThat(apiPolicyWaiverDTO.scopeOwnerId).isEqualTo(repository.getId());
    assertThat(apiPolicyWaiverDTO.scopeOwnerName).isEqualTo(repository.getName());
    assertThat(apiPolicyWaiverDTO.scopeOwnerType).isEqualTo(OwnerType.REPOSITORY.toString());
  }

  @Test
  public void testGetPolicyWaivers_RepositoryContainer() throws Exception {
    Policy policy = tempEntity.newPolicy();
    PolicyWaiver policyWaiver = tempEntity.newWaiver("hash", policy.getId(), REPOSITORY_CONTAINER_ID, "comment");

    HttpResponse response = restRequest().parameter(OwnerType.REPOSITORY_CONTAINER, REPOSITORY_CONTAINER_ID).get();

    assertResponseStatus(200, response);

    List<ApiPolicyWaiverDTO> policyWaiverDtoList = Arrays.asList(response.getBody(ApiPolicyWaiverDTO[].class));
    assertThat(policyWaiverDtoList).hasSize(1);

    ApiPolicyWaiverDTO apiPolicyWaiverDTO = policyWaiverDtoList.get(0);
    assertThat(apiPolicyWaiverDTO.policyWaiverId).isEqualTo(policyWaiver.getId());
    assertThat(apiPolicyWaiverDTO.comment).isEqualTo(policyWaiver.getComment());
    assertThat(apiPolicyWaiverDTO.createTime).isEqualTo(policyWaiver.getCreateTime());
    assertThat(apiPolicyWaiverDTO.hash).isEqualTo(policyWaiver.getHash());
    assertThat(apiPolicyWaiverDTO.policyId).isEqualTo(policyWaiver.getPolicyId());
    assertThat(apiPolicyWaiverDTO.scopeOwnerId).isEqualTo(REPOSITORY_CONTAINER_ID);
    assertThat(apiPolicyWaiverDTO.scopeOwnerName).isEqualTo("All Repositories");
    assertThat(apiPolicyWaiverDTO.scopeOwnerType).isEqualTo("all_repositories");
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.POLICY_WAIVER_PATH);
  }
}
