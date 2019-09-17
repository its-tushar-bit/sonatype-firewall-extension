/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.sourcecontrol;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.nexus.scm.SourceControlProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class SourceControlDAOTest
    extends AbstractDbDAOTest
{
  private static final String VALID_URL = "https://example.com/organization/project";

  private final SourceControlDAO sourceControlDAO = new SourceControlDAO();

  private Application app;

  private Organization org;

  @Override
  @Before
  public void setup() {
    app = tempEntity.newApplicationWithParent();
    org = tempEntity.newOrganization();
  }

  @After
  public void cleanup() {
    sourceControlDAO.getAll().stream().forEach(sourceControlDAO::delete);
  }

  @Test
  public void testInsert_MissingOwnerId() {
    assertThatThrownBy(() -> {
      sourceControlDAO.insert(new SourceControl());
    }).isInstanceOf(BadRequestException.class).hasMessage("SourceControl owner id is required");
  }

  @Test
  public void testInsert_MissingTokenForOrganization() {
    SourceControl sourceControl = new SourceControl(org.getId(), null, null, null);
    assertThatThrownBy(() -> {
      sourceControlDAO.insert(sourceControl);
    }).isInstanceOf(BadRequestException.class).hasMessage(
        "SourceControl authentication token is required for organization");
  }

  @Test
  public void testInsert_MissingSourceControlProviderForOrganization() {
    SourceControl sourceControl = new SourceControl(org.getId(), null, "token", null);
    assertThatThrownBy(() -> {
      sourceControlDAO.insert(sourceControl);
    }).isInstanceOf(BadRequestException.class).hasMessageContaining(
        "SourceControl provider is required when a token is provided");
  }

  @Test
  public void testInsert_RepositoryUrlForOrganization() {
    SourceControl sourceControl = new SourceControl(org.getId(), VALID_URL, "token", SourceControlProvider.GITHUB);
    assertThatThrownBy(() ->
        sourceControlDAO.insert(sourceControl)
    ).isInstanceOf(BadRequestException.class).hasMessage(
        "SourceControl repositoryUrl is not allowed for organization");
  }

  @Test
  public void testInsert_MissingRepositoryUrlForApplication() {
    SourceControl sourceControl = new SourceControl(app.getId(), null, "token", SourceControlProvider.GITHUB);
    assertThatThrownBy(() ->
        sourceControlDAO.insert(sourceControl)
    ).isInstanceOf(BadRequestException.class).hasMessage(
        "SourceControl repositoryUrl is required for application");
  }

  @Test
  public void testInsert_InvalidUrl() {
    SourceControl sourceControl = new SourceControl(
        app.getId(), "https://not valid", "token", SourceControlProvider.GITHUB);
    assertThatThrownBy(() -> {
      sourceControlDAO.insert(sourceControl);
    }).isInstanceOf(BadRequestException.class).hasMessageContaining("repositoryUrl is invalid");
  }

  @Test
  public void testInsert_CannotValidateUrl() {
    SourceControl sourceControl = new SourceControl(app.getId(), "https://not valid", null, null);
    assertThatThrownBy(() -> {
      sourceControlDAO.insert(sourceControl);
    }).isInstanceOf(BadRequestException.class).hasMessageContaining("Cannot validate SourceControl repositoryUrl");
  }

  @Test
  public void testInsert_AppPublicIdDoesNotExist() {
    SourceControl sourceControl = new SourceControl("baz", VALID_URL, "bar", SourceControlProvider.GITHUB);
    assertThatThrownBy(() -> {
      sourceControlDAO.insert(sourceControl);
    }).isInstanceOf(BadRequestException.class)
        .hasMessageContaining("SourceControl ownerId 'baz' cannot be found");
  }

  @Test
  public void testInsert_DuplicateRepositoryUrlAllowed() {
    Application baz = tempEntity.newApplicationWithParent("baz");
    tempEntity.newSourceControl(baz.getId(), VALID_URL, "bar", SourceControlProvider.GITHUB);
    sourceControlDAO.insert(new SourceControl(app.getId(), VALID_URL, "bar", SourceControlProvider.GITHUB));
  }

  @Test
  public void testUpdate_MissingOwnerId() {
    SourceControl sourceControl =
        tempEntity.newSourceControl(app.getId(), VALID_URL, "bar", SourceControlProvider.GITHUB);
    sourceControl.setOwnerId(null);
    assertThatThrownBy(() -> {
      sourceControlDAO.update(sourceControl);
    }).isInstanceOf(BadRequestException.class).hasMessage("SourceControl owner id is required");
  }

  @Test
  public void testUpdate_MissingTokenForOrganization() {
    SourceControl sourceControl =
        tempEntity.newSourceControl(org.getId(), null, "bar", SourceControlProvider.GITHUB);
    sourceControl.setToken(null);
    assertThatThrownBy(() -> {
      sourceControlDAO.update(sourceControl);
    }).isInstanceOf(BadRequestException.class).hasMessage(
        "SourceControl authentication token is required for organization");
  }

  @Test
  public void testUpdate_MissingSourceControlProviderForOrganization() {
    SourceControl sourceControl =
        tempEntity.newSourceControl(org.getId(), null, "bar", SourceControlProvider.GITHUB);
    sourceControl.setProvider(null);
    assertThatThrownBy(() -> {
      sourceControlDAO.update(sourceControl);
    }).isInstanceOf(BadRequestException.class).hasMessage(
        "SourceControl provider is required when a token is provided");
  }

  @Test
  public void testUpdate_MissingRepositoryUrlForApplication() {
    SourceControl sourceControl = tempEntity.newSourceControl(
        app.getId(), VALID_URL, "bar", SourceControlProvider.GITHUB);
    sourceControl.setRepositoryUrl(null);
    assertThatThrownBy(() ->
        sourceControlDAO.update(sourceControl)
    ).isInstanceOf(BadRequestException.class).hasMessage("SourceControl repositoryUrl is required for application");
  }

  @Test
  public void testUpdate_RepositoryUrlForOrganization() {
    SourceControl sourceControl = tempEntity.newSourceControl(
        org.getId(), null, "bar", SourceControlProvider.GITHUB);
    sourceControl.setRepositoryUrl(VALID_URL);
    assertThatThrownBy(() ->
        sourceControlDAO.update(sourceControl)
    ).isInstanceOf(BadRequestException.class).hasMessage("SourceControl repositoryUrl is not allowed for organization");
  }

  @Test
  public void testUpdate_InvalidUrl() {
    SourceControl sourceControl =
        tempEntity.newSourceControl(app.getId(), VALID_URL, "bar", SourceControlProvider.GITHUB);
    sourceControl.setRepositoryUrl("https://not valid");
    assertThatThrownBy(() -> {
      sourceControlDAO.update(sourceControl);
    }).isInstanceOf(BadRequestException.class).hasMessageContaining("repositoryUrl is invalid");
  }

  @Test
  public void testUpdate_DuplicateRepositoryUrlAllowed() {
    Application baz = tempEntity.newApplicationWithParent();
    Application foo = tempEntity.newApplicationWithParent();
    tempEntity.newSourceControl(baz.getId(), VALID_URL, "bar", SourceControlProvider.GITHUB);
    SourceControl sourceControl =
        tempEntity.newSourceControl(foo.getId(), VALID_URL + ".1", "bar", SourceControlProvider.GITHUB);
    sourceControl.setRepositoryUrl(VALID_URL);
    sourceControlDAO.update(sourceControl);
  }

  @Test
  public void testUpdate_MissingSourceControlProvider() {
    SourceControl sourceControl =
        tempEntity.newSourceControl(app.getId(), VALID_URL, "bar", SourceControlProvider.GITHUB);
    sourceControl.setProvider(null);
    assertThatThrownBy(() -> {
      sourceControlDAO.update(sourceControl);
    }).isInstanceOf(BadRequestException.class).hasMessageContaining("SourceControl provider is required");
  }

  @Test
  public void testCRUD() {
    SourceControl sourceControl = new SourceControl(
        app.getId(), VALID_URL, "bar", SourceControlProvider.GITHUB);
    sourceControl.setBaseBranch("base/branch");
    sourceControl.setEnablePullRequests(true);
    sourceControl.setEnableStatusChecks(true);

    assertThat(sourceControl.getId()).isNull();
    sourceControlDAO.insert(sourceControl);
    assertThat(sourceControl.getId()).isNotNull();

    sourceControl = sourceControlDAO.getByIdNotNull(sourceControl.getId());
    assertThat(sourceControl.getOwnerId()).isEqualTo(app.getId());
    assertThat(sourceControl.getRepositoryUrl()).isEqualTo(VALID_URL);
    assertThat(sourceControl.getToken()).isEqualTo("bar");
    assertThat(sourceControl.getBaseBranch()).isEqualTo("base/branch");
    assertThat(sourceControl.getEnablePullRequests()).isTrue();
    assertThat(sourceControl.getEnableStatusChecks()).isTrue();

    sourceControl.setToken("baz");
    sourceControl.setBaseBranch("another");
    sourceControl.setEnablePullRequests(false);
    sourceControl.setEnableStatusChecks(false);
    sourceControlDAO.update(sourceControl);

    sourceControl = sourceControlDAO.getByIdNotNull(sourceControl.getId());
    assertThat(sourceControl.getToken()).isEqualTo("baz");
    assertThat(sourceControl.getBaseBranch()).isEqualTo("another");
    assertThat(sourceControl.getEnablePullRequests()).isFalse();
    assertThat(sourceControl.getEnableStatusChecks()).isFalse();

    sourceControlDAO.delete(sourceControl);
    assertThat(sourceControlDAO.getById(sourceControl.getId())).isNull();
  }

  @Test
  public void testCRUD_Organization() {
    SourceControl sourceControl = new SourceControl(
        org.getId(), null, "bar", SourceControlProvider.GITHUB);
    sourceControl.setBaseBranch("base/branch");
    sourceControl.setEnablePullRequests(true);
    sourceControl.setEnableStatusChecks(true);

    assertThat(sourceControl.getId()).isNull();
    sourceControlDAO.insert(sourceControl);
    assertThat(sourceControl.getId()).isNotNull();

    sourceControl = sourceControlDAO.getByIdNotNull(sourceControl.getId());
    assertThat(sourceControl.getOwnerId()).isEqualTo(org.getId());
    assertThat(sourceControl.getToken()).isEqualTo("bar");
    assertThat(sourceControl.getBaseBranch()).isEqualTo("base/branch");
    assertThat(sourceControl.getEnablePullRequests()).isTrue();
    assertThat(sourceControl.getEnableStatusChecks()).isTrue();

    sourceControl.setToken("baz");
    sourceControl.setBaseBranch("another");
    sourceControl.setEnablePullRequests(false);
    sourceControl.setEnableStatusChecks(false);
    sourceControlDAO.update(sourceControl);

    sourceControl = sourceControlDAO.getByIdNotNull(sourceControl.getId());
    assertThat(sourceControl.getToken()).isEqualTo("baz");
    assertThat(sourceControl.getBaseBranch()).isEqualTo("another");
    assertThat(sourceControl.getEnablePullRequests()).isFalse();
    assertThat(sourceControl.getEnableStatusChecks()).isFalse();

    sourceControlDAO.delete(sourceControl);
    assertThat(sourceControlDAO.getById(sourceControl.getId())).isNull();
  }

  @Test
  public void testPullRequestConfigsCanBeNull() {
    SourceControl sourceControl = new SourceControl(
        app.getId(), VALID_URL, "bar", SourceControlProvider.GITHUB);

    assertThat(sourceControl.getId()).isNull();
    assertThat(sourceControl.getBaseBranch()).isNull();
    assertThat(sourceControl.getEnablePullRequests()).isNull();
    assertThat(sourceControl.getEnableStatusChecks()).isNull();

    sourceControlDAO.insert(sourceControl);

    assertThat(sourceControl.getId()).isNotNull();
    assertThat(sourceControl.getBaseBranch()).isNull();
    assertThat(sourceControl.getEnablePullRequests()).isNull();
    assertThat(sourceControl.getEnableStatusChecks()).isNull();

    sourceControl = sourceControlDAO.getByIdNotNull(sourceControl.getId());
    assertThat(sourceControl.getBaseBranch()).isNull();
    assertThat(sourceControl.getEnablePullRequests()).isNull();
    assertThat(sourceControl.getEnableStatusChecks()).isNull();
  }

  @Test
  public void testGetAll() {
    assertThat(sourceControlDAO.getAll().isEmpty()).isTrue();
    Application app2 = tempEntity.newApplicationWithParent("bar");
    tempEntity.newSourceControl(app.getId(), VALID_URL, "token", SourceControlProvider.GITHUB);
    tempEntity.newSourceControl(app2.getId(), VALID_URL, "token", SourceControlProvider.GITHUB);

    List<SourceControl> scms = sourceControlDAO.getAll();
    assertThat(scms.size()).isEqualTo(2);
    Stream<String> appIds = scms.stream().map(SourceControl::getOwnerId);
    assertThat(appIds.collect(Collectors.toList()).containsAll(Arrays.asList(app.getId(), "bar")));
  }

  @Test
  public void testInsert_ProviderFromOrganization() {
    tempEntity.newSourceControl(app.getOrganizationId(), null, "token", SourceControlProvider.GITHUB);
    sourceControlDAO.insert(new SourceControl(app.getId(), VALID_URL, null, null));
  }

  @Test
  public void testInsert_ProviderFromRootOrganization() {
    tempEntity.newSourceControl(org.getParentOrganizationId(), null, "token", SourceControlProvider.GITHUB);
    sourceControlDAO.insert(new SourceControl(app.getId(), VALID_URL, null, null));
  }

  @Test
  public void testInsert_ProviderNotAvailable() {
    assertThatThrownBy(() -> sourceControlDAO.insert(new SourceControl(app.getId(), VALID_URL, null, null)))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("Cannot validate SourceControl repositoryUrl");
  }

  @Test
  public void testUpdate_ProviderFromOrganization() {
    tempEntity.newSourceControl(app.getOrganizationId(), null, "token", SourceControlProvider.GITHUB);
    SourceControl sourceControl = new SourceControl(app.getId(), VALID_URL, "TOKEN", SourceControlProvider.GITHUB);
    sourceControlDAO.insert(sourceControl);
    sourceControl.setProvider(null);
    sourceControl.setToken(null);
    sourceControlDAO.update(sourceControl);
  }

  @Test
  public void testUpdate_ProviderFromRootOrganization() {
    tempEntity.newSourceControl(org.getParentOrganizationId(), null, "token", SourceControlProvider.GITHUB);
    SourceControl sourceControl = new SourceControl(app.getId(), VALID_URL, "TOKEN", SourceControlProvider.GITHUB);
    sourceControlDAO.insert(sourceControl);
    sourceControl.setProvider(null);
    sourceControl.setToken(null);
    sourceControlDAO.update(sourceControl);
  }

  @Test
  public void testUpdate_ProviderNotAvailable() {
    SourceControl sourceControl = new SourceControl(app.getId(), VALID_URL, "TOKEN", SourceControlProvider.GITHUB);
    sourceControlDAO.insert(sourceControl);
    sourceControl.setProvider(null);
    sourceControl.setToken(null);
    assertThatThrownBy(() -> sourceControlDAO.update(sourceControl)).isInstanceOf(BadRequestException.class)
        .hasMessageContaining("Cannot validate SourceControl repositoryUrl");
  }
}
