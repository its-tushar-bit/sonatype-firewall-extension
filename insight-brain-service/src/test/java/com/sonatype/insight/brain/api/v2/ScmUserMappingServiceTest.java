/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;
import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

import com.sonatype.insight.brain.api.v2.dto.scmusermatching.FromMappingEnum;
import com.sonatype.insight.brain.api.v2.dto.scmusermatching.SCMUserMappingsDTO;
import com.sonatype.insight.brain.api.v2.dto.scmusermatching.SCMUserMappingsResponseDTO;
import com.sonatype.insight.brain.api.v2.dto.scmusermatching.ToMappingEnum;
import com.sonatype.insight.brain.api.v2.dto.scmusermatching.UserMapping;
import com.sonatype.insight.brain.dataaccess.sourcecontrol.ScmUserMappingsDAO;
import com.sonatype.insight.brain.git.ScmUserMappingService;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.sourcecontrol.ScmUserMappings;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.nexus.scm.SourceControlProvider;
import org.junit.Before;
import org.junit.Test;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static com.sonatype.insight.brain.utils.ScmUserMappingsHelper.getRandomMappings;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Category(SlowTest.class)
public class ScmUserMappingServiceTest
    extends AbstractComponentTest
{
  private Organization org;

  @Inject
  private ScmUserMappingService scmUserMappingService;

  @Inject
  private ScmUserMappingsDAO scmUserMappingsDAO;

  @Before
  public void setup() {
    org = tempEntity.newOrganization();
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, null, SourceControlProvider.GITHUB);
  }

  @Test
  public void testAddUserMappingByOrg_NoExistingMapping_WithRoleId()
      throws Exception
  {
    ScmUserMappings existingUserMappings = scmUserMappingsDAO.getByOrganizationId(org.getId());
    assertThat(existingUserMappings).isNull();
    List<UserMapping> userMappings =
        Arrays.asList(new UserMapping(FromMappingEnum.GITLOG_EMAIL, ToMappingEnum.IQ_EMAIL));
    SCMUserMappingsDTO scmUserMappingsDTO = new SCMUserMappingsDTO("developer", userMappings);

    scmUserMappingService.addOrUpdateUserMappingByOrg(org.getId(), scmUserMappingsDTO);

    existingUserMappings = scmUserMappingsDAO.getByOrganizationId(org.getId());
    List<Entry<String, String>>
        userMappingsAsEntries = SCMUserMappingsDTO.userMappingsAsEntries(scmUserMappingsDTO.mappings());
    assertThat(existingUserMappings).isNotNull();
    assertThat(existingUserMappings.getMappings()).containsExactlyElementsOf(userMappingsAsEntries);
    assertThat(existingUserMappings.getRoleId()).isEqualTo(Role.DEVELOPER_ROLE_ID);
  }

  @Test
  public void testAddUserMappingByOrg_NoExistingMapping_WithoutRoleId()
      throws Exception
  {
    ScmUserMappings existingUserMappings = scmUserMappingsDAO.getByOrganizationId(org.getId());
    assertThat(existingUserMappings).isNull();
    List<UserMapping> userMappings =
        Arrays.asList(new UserMapping(FromMappingEnum.GITLOG_EMAIL, ToMappingEnum.IQ_EMAIL));
    SCMUserMappingsDTO scmUserMappingsDTO = new SCMUserMappingsDTO(null, userMappings);

    scmUserMappingService.addOrUpdateUserMappingByOrg(org.getId(), scmUserMappingsDTO);

    existingUserMappings = scmUserMappingsDAO.getByOrganizationId(org.getId());
    List<Entry<String, String>>
        userMappingsAsEntries = SCMUserMappingsDTO.userMappingsAsEntries(scmUserMappingsDTO.mappings());
    assertThat(existingUserMappings).isNotNull();
    assertThat(existingUserMappings.getMappings()).containsExactlyElementsOf(userMappingsAsEntries);
    assertThat(existingUserMappings.getRoleId()).isEqualTo(Role.DEVELOPER_ROLE_ID);
  }

  @Test
  public void testAddUserMappingByOrg_ExistingMapping_WithRoleId()
      throws Exception
  {
    List<UserMapping> userMappings =
        Arrays.asList(new UserMapping(FromMappingEnum.GITLOG_EMAIL, ToMappingEnum.IQ_EMAIL));
    SCMUserMappingsDTO scmUserMappingsDTO = new SCMUserMappingsDTO(null, userMappings);
    List<Entry<String, String>>
        userMappingsAsEntries = SCMUserMappingsDTO.userMappingsAsEntries(scmUserMappingsDTO.mappings());
    tempEntity.createScmUserMappings(org.getId(), userMappingsAsEntries);
    ScmUserMappings existingUserMappings = scmUserMappingsDAO.getByOrganizationId(org.getId());
    userMappingsAsEntries = SCMUserMappingsDTO.userMappingsAsEntries(scmUserMappingsDTO.mappings());

    assertThat(existingUserMappings).isNotNull();
    assertThat(existingUserMappings.getMappings()).containsExactlyElementsOf(userMappingsAsEntries);
    assertThat(existingUserMappings.getRoleId()).isNull();

    List<UserMapping> newUserMappings =
        Arrays.asList(new UserMapping(FromMappingEnum.GITLOG_FULLNAME, ToMappingEnum.IQ_FULLNAME));
    SCMUserMappingsDTO newScmUserMappingsDTO = new SCMUserMappingsDTO("developer", newUserMappings);

    scmUserMappingService.addOrUpdateUserMappingByOrg(org.getId(), newScmUserMappingsDTO);

    existingUserMappings = scmUserMappingsDAO.getByOrganizationId(org.getId());

    userMappingsAsEntries = SCMUserMappingsDTO.userMappingsAsEntries(newScmUserMappingsDTO.mappings());
    assertThat(existingUserMappings).isNotNull();
    assertThat(existingUserMappings.getMappings()).containsExactlyElementsOf(userMappingsAsEntries);
    assertThat(existingUserMappings.getRoleId()).isEqualTo(Role.DEVELOPER_ROLE_ID);
  }

  @Test
  public void testAddUserMappingByOrg_ExistingMapping_WithoutRoleId()
      throws Exception
  {
    List<UserMapping> userMappings =
        Arrays.asList(new UserMapping(FromMappingEnum.GITLOG_EMAIL, ToMappingEnum.IQ_EMAIL));
    SCMUserMappingsDTO scmUserMappingsDTO = new SCMUserMappingsDTO("developer", userMappings);
    List<Entry<String, String>>
        userMappingsAsEntries = SCMUserMappingsDTO.userMappingsAsEntries(scmUserMappingsDTO.mappings());
    tempEntity.createScmUserMappings("developer", org.getId(), userMappingsAsEntries);
    ScmUserMappings existingUserMappings = scmUserMappingsDAO.getByOrganizationId(org.getId());
    userMappingsAsEntries = SCMUserMappingsDTO.userMappingsAsEntries(scmUserMappingsDTO.mappings());

    assertThat(existingUserMappings).isNotNull();
    assertThat(existingUserMappings.getMappings()).containsExactlyElementsOf(userMappingsAsEntries);
    assertThat(existingUserMappings.getRoleId()).isEqualTo("developer");

    List<UserMapping> newUserMappings =
        Arrays.asList(new UserMapping(FromMappingEnum.GITLOG_FULLNAME, ToMappingEnum.IQ_FULLNAME));
    SCMUserMappingsDTO newScmUserMappingsDTO = new SCMUserMappingsDTO(null, newUserMappings);

    scmUserMappingService.addOrUpdateUserMappingByOrg(org.getId(), newScmUserMappingsDTO);

    existingUserMappings = scmUserMappingsDAO.getByOrganizationId(org.getId());

    userMappingsAsEntries = SCMUserMappingsDTO.userMappingsAsEntries(newScmUserMappingsDTO.mappings());
    assertThat(existingUserMappings).isNotNull();
    assertThat(existingUserMappings.getMappings()).containsExactlyElementsOf(userMappingsAsEntries);
    assertThat(existingUserMappings.getRoleId()).isEqualTo(Role.DEVELOPER_ROLE_ID);
  }

  @Test
  public void testDeleteMappingByOrg() {
    List<UserMapping> userMappings =
        Arrays.asList(new UserMapping(FromMappingEnum.GITLOG_EMAIL, ToMappingEnum.IQ_EMAIL));
    SCMUserMappingsDTO scmUserMappingsDTO = new SCMUserMappingsDTO(null, userMappings);
    List<Entry<String, String>>
        userMappingsAsEntries = SCMUserMappingsDTO.userMappingsAsEntries(scmUserMappingsDTO.mappings());
    tempEntity.createScmUserMappings(org.getId(), userMappingsAsEntries);

    ScmUserMappings existingUserMappings = scmUserMappingsDAO.getByOrganizationId(org.getId());

    assertThat(existingUserMappings).isNotNull();

    scmUserMappingService.deleteUserMappingByOrg(org.getId());

    existingUserMappings = scmUserMappingsDAO.getByOrganizationId(org.getId());
    assertThat(existingUserMappings).isNull();
  }

  @Test
  public void testGetUserMappingsByOwner_GetMappingsForSameOrg() {
    ScmUserMappings existingScmUserMappings = tempEntity.createScmUserMappings(Role.OWNER_ROLE_ID, org.getId(),
        getRandomMappings());

    SCMUserMappingsResponseDTO scmUserMappingsResponseDTO =
        scmUserMappingService.getUserMappingsByOwner(OwnerType.ORGANIZATION, org.getId());

    List<UserMapping> existingMappings = existingScmUserMappings.getMappings().stream().map(UserMapping::new).toList();

    assertThat(scmUserMappingsResponseDTO.ownerInternalId()).isEqualTo(org.getId());
    assertThat(scmUserMappingsResponseDTO.inherited()).isFalse();
    assertThat(scmUserMappingsResponseDTO.userMapping().role()).isEqualTo("owner");
    assertThat(scmUserMappingsResponseDTO.userMapping().mappings()).isEqualTo(existingMappings);
  }

  @Test
  public void testGetUserMappingsByOwner_GetMappingsForChildApp() {
    ScmUserMappings existingScmUserMappings = tempEntity.createScmUserMappings(Role.OWNER_ROLE_ID, org.getId(),
        getRandomMappings());

    Application app = tempEntity.newApplication(org.getId());

    SCMUserMappingsResponseDTO scmUserMappingsResponseDTO =
        scmUserMappingService.getUserMappingsByOwner(OwnerType.APPLICATION, app.getId());

    List<UserMapping> existingMappings = existingScmUserMappings.getMappings().stream().map(UserMapping::new).toList();

    assertThat(scmUserMappingsResponseDTO.ownerInternalId()).isEqualTo(org.getId());
    assertThat(scmUserMappingsResponseDTO.inherited()).isTrue();
    assertThat(scmUserMappingsResponseDTO.userMapping().role()).isEqualTo("owner");
    assertThat(scmUserMappingsResponseDTO.userMapping().mappings()).isEqualTo(existingMappings);
  }

  @Test
  public void testGetUserMappingsByOwner_GetMappingsForVeryDeepHierarchyChildApp() {
    Organization org1 = tempEntity.newOrganization();
    Organization org2 = tempEntity.newOrganization(org1);
    Organization org3 = tempEntity.newOrganization(org2);
    Organization org4 = tempEntity.newOrganization(org3);
    Organization org5 = tempEntity.newOrganization(org4);
    Organization org6 = tempEntity.newOrganization(org5);
    Organization org7 = tempEntity.newOrganization(org6);
    Organization org8 = tempEntity.newOrganization(org7);

    ScmUserMappings existingScmUserMappings = tempEntity.createScmUserMappings(Role.DEVELOPER_ROLE_ID, org1.getId(),
        getRandomMappings());

    Application app = tempEntity.newApplication(org8.getId());

    SCMUserMappingsResponseDTO scmUserMappingsResponseDTO =
        scmUserMappingService.getUserMappingsByOwner(OwnerType.APPLICATION, app.getId());

    List<UserMapping> existingMappings = existingScmUserMappings.getMappings().stream().map(UserMapping::new).toList();

    assertThat(scmUserMappingsResponseDTO.ownerInternalId()).isEqualTo(org1.getId());
    assertThat(scmUserMappingsResponseDTO.inherited()).isTrue();
    assertThat(scmUserMappingsResponseDTO.userMapping().role()).isEqualTo("developer");
    assertThat(scmUserMappingsResponseDTO.userMapping().mappings()).isEqualTo(existingMappings);
  }

  @Test
  public void testGetUserMappingsByOwner_GetMappingsForVeryDeepHierarchyChildOrg() {
    Organization org1 = tempEntity.newOrganization();
    Organization org2 = tempEntity.newOrganization(org1);
    Organization org3 = tempEntity.newOrganization(org2);
    Organization org4 = tempEntity.newOrganization(org3);
    Organization org5 = tempEntity.newOrganization(org4);
    Organization org6 = tempEntity.newOrganization(org5);
    Organization org7 = tempEntity.newOrganization(org6);
    Organization org8 = tempEntity.newOrganization(org7);

    ScmUserMappings existingScmUserMappings = tempEntity.createScmUserMappings(Role.DEVELOPER_ROLE_ID, org1.getId(),
        getRandomMappings());

    SCMUserMappingsResponseDTO scmUserMappingsResponseDTO =
        scmUserMappingService.getUserMappingsByOwner(OwnerType.ORGANIZATION, org8.getId());

    List<UserMapping> existingMappings = existingScmUserMappings.getMappings().stream().map(UserMapping::new).toList();

    assertThat(scmUserMappingsResponseDTO.ownerInternalId()).isEqualTo(org1.getId());
    assertThat(scmUserMappingsResponseDTO.inherited()).isTrue();
    assertThat(scmUserMappingsResponseDTO.userMapping().role()).isEqualTo("developer");
    assertThat(scmUserMappingsResponseDTO.userMapping().mappings()).isEqualTo(existingMappings);
  }

  @Test
  public void testGetUserMappingsByOwner_GetFirstMappingsForVeryDeepHierarchyChildApp() {
    Organization org1 = tempEntity.newOrganization();
    Organization org2 = tempEntity.newOrganization(org1);
    Organization org3 = tempEntity.newOrganization(org2);
    Organization org4 = tempEntity.newOrganization(org3);
    Organization org5 = tempEntity.newOrganization(org4);
    Organization org6 = tempEntity.newOrganization(org5);
    Organization org7 = tempEntity.newOrganization(org6);
    Organization org8 = tempEntity.newOrganization(org7);

    tempEntity.createScmUserMappings(Role.OWNER_ROLE_ID, org2.getId(), getRandomMappings());

    ScmUserMappings existingScmUserMappings = tempEntity.createScmUserMappings(Role.DEVELOPER_ROLE_ID, org5.getId(),
        getRandomMappings());

    Application app = tempEntity.newApplication(org8.getId());

    SCMUserMappingsResponseDTO scmUserMappingsResponseDTO =
        scmUserMappingService.getUserMappingsByOwner(OwnerType.APPLICATION, app.getId());

    List<UserMapping> existingMappings = existingScmUserMappings.getMappings().stream().map(UserMapping::new).toList();

    assertThat(scmUserMappingsResponseDTO.ownerInternalId()).isEqualTo(org5.getId());
    assertThat(scmUserMappingsResponseDTO.inherited()).isTrue();
    assertThat(scmUserMappingsResponseDTO.userMapping().role()).isEqualTo("developer");
    assertThat(scmUserMappingsResponseDTO.userMapping().mappings()).isEqualTo(existingMappings);
  }

  @Test
  public void testGetUserMappingsByOwner_GetFirstMappingsForVeryDeepHierarchyChildAppOnDifferentBranch() {
    Organization org1 = tempEntity.newOrganization();
    Organization org2 = tempEntity.newOrganization(org1);
    Organization org3 = tempEntity.newOrganization(org2);
    Organization org4 = tempEntity.newOrganization(org3);
    Organization org5 = tempEntity.newOrganization(org4);

    Organization branch1Org1 = tempEntity.newOrganization(org5);
    tempEntity.newOrganization(branch1Org1);

    Organization branch2Org1 = tempEntity.newOrganization(org3);
    Organization branch2Org2 = tempEntity.newOrganization(branch2Org1);

    tempEntity.createScmUserMappings(Role.OWNER_ROLE_ID, org2.getId(), getRandomMappings());
    tempEntity.createScmUserMappings(Role.APPLICATION_EVALUATOR_ROLE_ID, branch1Org1.getId(), getRandomMappings());

    ScmUserMappings existingScmUserMappings = tempEntity.createScmUserMappings(Role.DEVELOPER_ROLE_ID,
        branch2Org1.getId(), getRandomMappings());

    Application app = tempEntity.newApplication(branch2Org2.getId());

    SCMUserMappingsResponseDTO scmUserMappingsResponseDTO =
        scmUserMappingService.getUserMappingsByOwner(OwnerType.APPLICATION, app.getId());

    List<UserMapping> existingMappings = existingScmUserMappings.getMappings().stream().map(UserMapping::new).toList();

    assertThat(scmUserMappingsResponseDTO.ownerInternalId()).isEqualTo(branch2Org1.getId());
    assertThat(scmUserMappingsResponseDTO.inherited()).isTrue();
    assertThat(scmUserMappingsResponseDTO.userMapping().role()).isEqualTo("developer");
    assertThat(scmUserMappingsResponseDTO.userMapping().mappings()).isEqualTo(existingMappings);
  }

  @Test
  public void testGetUserMappingsByOwner_ReturnNullWhenNoMappingsInHierarchy() {
    Application app = tempEntity.newApplication(org.getId());

    SCMUserMappingsResponseDTO scmUserMappingsResponseDTO =
        scmUserMappingService.getUserMappingsByOwner(OwnerType.APPLICATION, app.getId());

    assertThat(scmUserMappingsResponseDTO).isNull();
  }

  @Test
  public void testGetUserMappingsByOwner_ThrowBadRequestExceptionWhenUseRepo() {
    Repository repository = tempEntity.newRepository();

    assertThatThrownBy(() -> scmUserMappingService.getUserMappingsByOwner(OwnerType.REPOSITORY, repository.getId()))
        .isInstanceOf(BadRequestException.class).hasMessage("OwnerType not supported: "
            + OwnerType.REPOSITORY);
  }
}
