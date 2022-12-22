/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration.crowd;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.JPA;
import com.sonatype.insight.brain.model.configuration.crowd.CrowdConfiguration;
import com.sonatype.insight.error.exception.BadRequestException;

import org.drools.core.util.StringUtils;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class CrowdConfigurationDAOTest
    extends AbstractDbDAOTest
{
  private final CrowdConfigurationDAO dao = new CrowdConfigurationDAO();

  @Test
  public void testCRUD() {
    // Create
    CrowdConfiguration crowdConfiguration =
        new CrowdConfiguration("serverUrl", "applicationName", "applicationPassword".toCharArray());
    dao.insert(crowdConfiguration);
    assertThat(crowdConfiguration.getId()).isNotNull();

    // Read
    CrowdConfiguration storedCrowdConfiguration = dao.getById(crowdConfiguration.getId());
    assertThat(storedCrowdConfiguration).usingRecursiveComparison().ignoringFields(JPA.IGNORE_FIELDS)
        .isEqualTo(crowdConfiguration);

    // Update
    crowdConfiguration.setServerUrl("newServerUrl");
    crowdConfiguration.setApplicationName("newApplicationName");
    crowdConfiguration.setApplicationPassword("newApplicationPassword".toCharArray());
    dao.update(crowdConfiguration);
    assertThat(dao.getById(crowdConfiguration.getId())).usingRecursiveComparison().ignoringFields(JPA.IGNORE_FIELDS)
        .isEqualTo(crowdConfiguration);

    // Delete
    dao.delete(crowdConfiguration);
    assertThat(dao.getById(crowdConfiguration.getId())).isNull();
  }

  @Test
  public void testInsert_MaxSizes() {
    CrowdConfiguration crowdConfiguration = new CrowdConfiguration(
        StringUtils.repeat("a", CrowdConfigurationDAO.MAX_SERVER_URL_SIZE),
        StringUtils.repeat("b", CrowdConfigurationDAO.MAX_APPLICATION_NAME_SIZE),
        StringUtils.repeat("c", CrowdConfigurationDAO.MAX_APPLICATION_PASSWORD_SIZE).toCharArray()
    );

    dao.insert(crowdConfiguration);

    assertThat(dao.get()).usingRecursiveComparison().ignoringFields(JPA.IGNORE_FIELDS).isEqualTo(crowdConfiguration);
  }

  @Test
  public void testUpdate_MaxSizes() {
    CrowdConfiguration crowdConfiguration = tempEntity.newCrowdConfiguration();
    crowdConfiguration.setServerUrl(StringUtils.repeat("a", CrowdConfigurationDAO.MAX_SERVER_URL_SIZE));
    crowdConfiguration.setApplicationName(StringUtils.repeat("b", CrowdConfigurationDAO.MAX_APPLICATION_NAME_SIZE));
    crowdConfiguration.setApplicationPassword(
        StringUtils.repeat("c", CrowdConfigurationDAO.MAX_APPLICATION_PASSWORD_SIZE).toCharArray());

    dao.update(crowdConfiguration);

    assertThat(dao.get()).usingRecursiveComparison().ignoringFields(JPA.IGNORE_FIELDS).isEqualTo(crowdConfiguration);
  }

  @Test
  public void testGet() {
    CrowdConfiguration crowdConfiguration = tempEntity.newCrowdConfiguration();

    assertThat(dao.get()).usingRecursiveComparison().ignoringFields(JPA.IGNORE_FIELDS).isEqualTo(crowdConfiguration);
  }

  @Test
  public void testSet_Insert() {
    CrowdConfiguration crowdConfiguration =
        new CrowdConfiguration("serverUrl", "applicationName", "applicationPassword".toCharArray());

    dao.set(crowdConfiguration);

    assertThat(dao.getById(crowdConfiguration.getId())).usingRecursiveComparison().ignoringFields(JPA.IGNORE_FIELDS)
        .isEqualTo(crowdConfiguration);
  }

  @Test
  public void testSet_Update() {
    CrowdConfiguration crowdConfiguration = tempEntity.newCrowdConfiguration();
    crowdConfiguration.setServerUrl(crowdConfiguration.getServerUrl() + "2");
    crowdConfiguration.setApplicationName(crowdConfiguration.getApplicationName() + "2");
    crowdConfiguration.setApplicationPassword(
        (String.valueOf(crowdConfiguration.getApplicationPassword()) + "2").toCharArray());

    dao.set(crowdConfiguration);

    assertThat(dao.getById(crowdConfiguration.getId())).usingRecursiveComparison().ignoringFields(JPA.IGNORE_FIELDS)
        .isEqualTo(crowdConfiguration);
  }

  @Test
  public void testDelete() {
    CrowdConfiguration crowdConfiguration = tempEntity.newCrowdConfiguration();

    dao.delete();

    assertThat(dao.getById(crowdConfiguration.getId())).isNull();
  }

  @Test
  public void testInsert_NullServerUrl() {
    CrowdConfiguration crowdConfiguration =
        new CrowdConfiguration(null, "applicationName", "applicationPassword".toCharArray());

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.insert(crowdConfiguration))
        .withMessageContaining("A Crowd server url is required.");
  }

  @Test
  public void testInsert_EmptyServerUrl() {
    CrowdConfiguration crowdConfiguration =
        new CrowdConfiguration("", "applicationName", "applicationPassword".toCharArray());

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.insert(crowdConfiguration))
        .withMessageContaining("A Crowd server url is required.");
  }

  @Test
  public void testInsert_BlankServerUrl() {
    CrowdConfiguration crowdConfiguration =
        new CrowdConfiguration(" ", "applicationName", "applicationPassword".toCharArray());

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.insert(crowdConfiguration))
        .withMessageContaining("A Crowd server url is required.");
  }

  @Test
  public void testInsert_ServerUrl_TooLong() {
    CrowdConfiguration crowdConfiguration =
        new CrowdConfiguration(StringUtils.repeat("a", CrowdConfigurationDAO.MAX_SERVER_URL_SIZE + 1),
            "applicationName", "applicationPassword".toCharArray());

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.insert(crowdConfiguration))
        .withMessageContaining("A Crowd server url cannot exceed 2048 characters.");
  }

  @Test
  public void testInsert_NullApplicationName() {
    CrowdConfiguration crowdConfiguration =
        new CrowdConfiguration("serverUrl", null, "applicationPassword".toCharArray());

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.insert(crowdConfiguration))
        .withMessageContaining("A Crowd application name is required.");
  }

  @Test
  public void testInsert_EmptyApplicationName() {
    CrowdConfiguration crowdConfiguration =
        new CrowdConfiguration("serverUrl", "", "applicationPassword".toCharArray());

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.insert(crowdConfiguration))
        .withMessageContaining("A Crowd application name is required.");
  }

  @Test
  public void testInsert_BlankApplicationName() {
    CrowdConfiguration crowdConfiguration =
        new CrowdConfiguration("serverUrl", " ", "applicationPassword".toCharArray());

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.insert(crowdConfiguration))
        .withMessageContaining("A Crowd application name is required.");
  }

  @Test
  public void testInsert_ApplicationName_TooLong() {
    CrowdConfiguration crowdConfiguration =
        new CrowdConfiguration("serverUrl",
            StringUtils.repeat("a", CrowdConfigurationDAO.MAX_APPLICATION_NAME_SIZE + 1),
            "applicationPassword".toCharArray());

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.insert(crowdConfiguration))
        .withMessageContaining("A Crowd application name cannot exceed 255 characters.");
  }

  @Test
  public void testInsert_NullApplicationPassword() {
    CrowdConfiguration crowdConfiguration =
        new CrowdConfiguration("serverUrl", "applicationName", null);

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.insert(crowdConfiguration))
        .withMessageContaining("A Crowd application password is required.");
  }

  @Test
  public void testInsert_EmptyApplicationPassword() {
    CrowdConfiguration crowdConfiguration =
        new CrowdConfiguration("serverUrl", "applicationName", "".toCharArray());

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.insert(crowdConfiguration))
        .withMessageContaining("A Crowd application password is required.");
  }

  @Test
  public void testInsert_BlankApplicationPassword() {
    CrowdConfiguration crowdConfiguration =
        new CrowdConfiguration("serverUrl", "applicationName", " ".toCharArray());

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.insert(crowdConfiguration))
        .withMessageContaining("A Crowd application password is required.");
  }

  @Test
  public void testInsert_ApplicationPassword_TooLong() {
    CrowdConfiguration crowdConfiguration = new CrowdConfiguration("serverUrl", "applicationName",
        StringUtils.repeat("a", CrowdConfigurationDAO.MAX_APPLICATION_PASSWORD_SIZE + 1).toCharArray());

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.insert(crowdConfiguration))
        .withMessageContaining("A Crowd application password cannot exceed 255 characters.");
  }

  @Test
  public void testUpdate_NullServerUrl() {
    CrowdConfiguration crowdConfiguration =
        new CrowdConfiguration(null, "applicationName", "applicationPassword".toCharArray());

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.update(crowdConfiguration))
        .withMessageContaining("A Crowd server url is required.");
  }

  @Test
  public void testUpdate_EmptyServerUrl() {
    CrowdConfiguration crowdConfiguration =
        new CrowdConfiguration("", "applicationName", "applicationPassword".toCharArray());

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.update(crowdConfiguration))
        .withMessageContaining("A Crowd server url is required.");
  }

  @Test
  public void testUpdate_BlankServerUrl() {
    CrowdConfiguration crowdConfiguration =
        new CrowdConfiguration(" ", "applicationName", "applicationPassword".toCharArray());

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.update(crowdConfiguration))
        .withMessageContaining("A Crowd server url is required.");
  }

  @Test
  public void testUpdate_ServerUrl_TooLong() {
    CrowdConfiguration crowdConfiguration =
        new CrowdConfiguration(StringUtils.repeat("a", CrowdConfigurationDAO.MAX_SERVER_URL_SIZE + 1),
            "applicationName", "applicationPassword".toCharArray());

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.update(crowdConfiguration))
        .withMessageContaining("A Crowd server url cannot exceed 2048 characters.");
  }

  @Test
  public void testUpdate_NullApplicationName() {
    CrowdConfiguration crowdConfiguration =
        new CrowdConfiguration("serverUrl", null, "applicationPassword".toCharArray());

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.update(crowdConfiguration))
        .withMessageContaining("A Crowd application name is required.");
  }

  @Test
  public void testUpdate_EmptyApplicationName() {
    CrowdConfiguration crowdConfiguration =
        new CrowdConfiguration("serverUrl", "", "applicationPassword".toCharArray());

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.update(crowdConfiguration))
        .withMessageContaining("A Crowd application name is required.");
  }

  @Test
  public void testUpdate_BlankApplicationName() {
    CrowdConfiguration crowdConfiguration =
        new CrowdConfiguration("serverUrl", " ", "applicationPassword".toCharArray());

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.update(crowdConfiguration))
        .withMessageContaining("A Crowd application name is required.");
  }

  @Test
  public void testUpdate_ApplicationName_TooLong() {
    CrowdConfiguration crowdConfiguration =
        new CrowdConfiguration("serverUrl",
            StringUtils.repeat("a", CrowdConfigurationDAO.MAX_APPLICATION_NAME_SIZE + 1),
            "applicationPassword".toCharArray());

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.update(crowdConfiguration))
        .withMessageContaining("A Crowd application name cannot exceed 255 characters.");
  }

  @Test
  public void testUpdate_NullApplicationPassword() {
    CrowdConfiguration crowdConfiguration =
        new CrowdConfiguration("serverUrl", "applicationName", null);

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.update(crowdConfiguration))
        .withMessageContaining("A Crowd application password is required.");
  }

  @Test
  public void testUpdate_EmptyApplicationPassword() {
    CrowdConfiguration crowdConfiguration =
        new CrowdConfiguration("serverUrl", "applicationName", "".toCharArray());

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.update(crowdConfiguration))
        .withMessageContaining("A Crowd application password is required.");
  }

  @Test
  public void testUpdate_BlankApplicationPassword() {
    CrowdConfiguration crowdConfiguration =
        new CrowdConfiguration("serverUrl", "applicationName", " ".toCharArray());

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.update(crowdConfiguration))
        .withMessageContaining("A Crowd application password is required.");
  }

  @Test
  public void testUpdate_ApplicationPassword_TooLong() {
    CrowdConfiguration crowdConfiguration = new CrowdConfiguration("serverUrl", "applicationName",
        StringUtils.repeat("a", CrowdConfigurationDAO.MAX_APPLICATION_PASSWORD_SIZE + 1).toCharArray());

    assertThatExceptionOfType(BadRequestException.class).isThrownBy(() -> dao.update(crowdConfiguration))
        .withMessageContaining("A Crowd application password cannot exceed 255 characters.");
  }
}
