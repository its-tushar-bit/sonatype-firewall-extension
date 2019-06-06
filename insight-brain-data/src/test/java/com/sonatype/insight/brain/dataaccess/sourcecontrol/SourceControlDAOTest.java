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
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.error.exception.BadRequestException;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class SourceControlDAOTest
    extends AbstractDbDAOTest
{
  private static final String VALID_URL = "https://example.com";

  private final SourceControlDAO sourceControlDAO = new SourceControlDAO();

  private Application app;

  @Override
  @Before
  public void setup() {
    app = tempEntity.newApplicationWithParent();
  }

  @Test
  public void testInsert_MissingAppId() {
    assertThatThrownBy(() -> {
      sourceControlDAO.insert(new SourceControl());
    }).isInstanceOf(BadRequestException.class).hasMessage("SourceControl application id is required");
  }

  @Test
  public void testInsert_MissingToken() {
    SourceControl sourceControl = new SourceControl();
    sourceControl.setApplicationId(app.getId());
    assertThatThrownBy(() -> {
      sourceControlDAO.insert(sourceControl);
    }).isInstanceOf(BadRequestException.class).hasMessage("SourceControl authentication token is required");
  }

  @Test
  public void testInsert_NonHttpsUrl() {
    SourceControl sourceControl = new SourceControl(app.getId(), VALID_URL.replaceFirst("https", "http"), "token");
    assertThatThrownBy(() -> {
      sourceControlDAO.insert(sourceControl);
    }).isInstanceOf(BadRequestException.class).hasMessage("SourceControl URL must start with https://");
  }

  @Test
  public void testInsert_InvalidUrl() {
    assertThatThrownBy(() -> {
      sourceControlDAO.insert(new SourceControl(app.getId(), "https://not valid", "bar"));
    }).isInstanceOf(BadRequestException.class).hasMessageContaining("URL is invalid");
  }

  @Test
  public void testInsert_AppPublicIdDoesNotExist() {
    assertThatThrownBy(() -> {
      sourceControlDAO.insert(new SourceControl("baz", VALID_URL, "bar"));
    }).isInstanceOf(BadRequestException.class)
        .hasMessageContaining("SourceControl applicationId 'baz' cannot be found");
  }

  @Test
  public void testInsert_DuplicateApplicationId() {
    tempEntity.newSourceControl(app.getId(), VALID_URL, "bar");
    assertThatThrownBy(() -> {
      sourceControlDAO.insert(new SourceControl(app.getId(), VALID_URL + ".1", "bar"));
    }).isInstanceOf(BadRequestException.class)
        .hasMessageContaining("SourceControl already configured for application with id: '" + app.getId() + "'");
  }

  @Test
  public void testInsert_DuplicateRepositoryUrlAllowed() {
    Application baz = tempEntity.newApplicationWithParent("baz");
    tempEntity.newSourceControl(baz.getId(), VALID_URL, "bar");
    sourceControlDAO.insert(new SourceControl(app.getId(), VALID_URL, "bar"));
  }

  @Test
  public void testUpdate_MissingAppId() {
    SourceControl sourceControl = tempEntity.newSourceControl(app.getId(), VALID_URL, "bar");
    sourceControl.setApplicationId(null);
    assertThatThrownBy(() -> {
      sourceControlDAO.update(sourceControl);
    }).isInstanceOf(BadRequestException.class).hasMessage("SourceControl application id is required");
  }

  @Test
  public void testUpdate_MissingToken() {
    SourceControl sourceControl = tempEntity.newSourceControl(app.getId(), VALID_URL, "bar");
    sourceControl.setToken(null);
    assertThatThrownBy(() -> {
      sourceControlDAO.update(sourceControl);
    }).isInstanceOf(BadRequestException.class).hasMessage("SourceControl authentication token is required");
  }

  @Test
  public void testUpdate_NonHttpsUrl() {
    SourceControl sourceControl = tempEntity.newSourceControl(app.getId(), VALID_URL, "token");
    sourceControl.setRepositoryUrl(VALID_URL.replaceFirst("https", "http"));
    assertThatThrownBy(() -> {
      sourceControlDAO.update(sourceControl);
    }).isInstanceOf(BadRequestException.class).hasMessage("SourceControl URL must start with https://");
  }

  @Test
  public void testUpdate_InvalidUrl() {
    SourceControl sourceControl = tempEntity.newSourceControl(app.getId(), VALID_URL, "bar");
    sourceControl.setRepositoryUrl("https://not valid");
    assertThatThrownBy(() -> {
      sourceControlDAO.update(sourceControl);
    }).isInstanceOf(BadRequestException.class).hasMessageContaining("URL is invalid");
  }

  @Test
  public void testUpdate_DuplicateRepositoryUrlAllowed() {
    Application baz = tempEntity.newApplicationWithParent();
    Application foo = tempEntity.newApplicationWithParent();
    tempEntity.newSourceControl(baz.getId(), VALID_URL, "bar");
    SourceControl sourceControl = tempEntity.newSourceControl(foo.getId(), VALID_URL + ".1", "bar");
    sourceControl.setRepositoryUrl(VALID_URL);
    sourceControlDAO.update(sourceControl);
  }

  @Test
  public void testCRUD() {
    SourceControl sourceControl = new SourceControl(app.getId(), VALID_URL, "bar");
    assertThat(sourceControl.getId()).isNull();
    sourceControlDAO.insert(sourceControl);
    assertThat(sourceControl.getId()).isNotNull();

    sourceControl = sourceControlDAO.getByIdNotNull(sourceControl.getId());
    assertThat(sourceControl.getApplicationId()).isEqualTo(app.getId());
    assertThat(sourceControl.getRepositoryUrl()).isEqualTo(VALID_URL);
    assertThat(sourceControl.getToken()).isEqualTo("bar");

    sourceControl.setToken("baz");
    sourceControlDAO.update(sourceControl);

    sourceControl = sourceControlDAO.getByIdNotNull(sourceControl.getId());
    assertThat(sourceControl.getToken()).isEqualTo("baz");

    sourceControlDAO.delete(sourceControl);
    assertThat(sourceControlDAO.getById(sourceControl.getId())).isNull();
  }

  @Test
  public void testGetAll() {
    assertThat(sourceControlDAO.getAll().isEmpty()).isTrue();
    Application app2 = tempEntity.newApplicationWithParent("bar");
    tempEntity.newSourceControl(app.getId(), VALID_URL, "token");
    tempEntity.newSourceControl(app2.getId(), VALID_URL, "token");

    List<SourceControl> scms = sourceControlDAO.getAll();
    assertThat(scms.size()).isEqualTo(2);
    Stream<String> appIds = scms.stream().map(SourceControl::getApplicationId);
    assertThat(appIds.collect(Collectors.toList()).containsAll(Arrays.asList(app.getId(), "bar")));
  }
}
