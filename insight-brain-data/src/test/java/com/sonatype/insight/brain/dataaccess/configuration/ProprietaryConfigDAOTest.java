/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.InvalidProprietaryConfigRegexException;
import com.sonatype.insight.brain.model.configuration.ProprietaryConfig;
import com.sonatype.insight.error.exception.BadRequestException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ProprietaryConfigDAOTest
    extends AbstractDbDAOTest
{
  private ProprietaryConfigDAO dao;

  @BeforeEach
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createProprietaryConfigDAO();
  }

  @Test
  public void testCRUD() {
    // Create
    List<String> packages = Collections.singletonList("foo");
    List<String> regexes = Collections.singletonList("bar");
    ProprietaryConfig config = new ProprietaryConfig(application.getId(), packages, regexes);
    dao.insert(config);
    assertThat(config.getId()).isNotNull();

    // Read
    config = dao.getById(config.getId());
    assertProprietaryConfig(application.getId(), packages, regexes, config);

    // Update
    packages = Collections.singletonList("foo.updated");
    config.setPackages(packages);
    dao.update(config);

    // Read
    config = dao.getById(config.getId());
    assertProprietaryConfig(application.getId(), packages, regexes, config);

    // Delete
    dao.delete(config);

    config = dao.getById(config.getId());
    assertThat(config).isNull();
  }

  @Test
  public void testInsert_Duplicate() {
    tempEntity.newProprietaryConfig(application.getId());

    ProprietaryConfig config1 = new ProprietaryConfig(application.getId(), null /* packages */, null /* regexes */);
    assertThatThrownBy(() -> dao.insert(config1)).isInstanceOf(BadRequestException.class)
        .hasMessage("A proprietary config already exists for owner id " + application.getId());
  }

  @Test
  public void testUpdate_Duplicate() {
    tempEntity.newProprietaryConfig(application.getId());
    ProprietaryConfig config1 = tempEntity.newProprietaryConfig(organization.getId());

    config1.setOwnerId(application.getId());
    assertThatThrownBy(() -> dao.update(config1)).isInstanceOf(BadRequestException.class)
        .hasMessage("A proprietary config already exists for owner id " + application.getId());
  }

  private void assertProprietaryConfig(
      String applicationId,
      List<String> packages,
      List<String> regexes,
      ProprietaryConfig config)
  {
    assertThat(config.getOwnerId()).isEqualTo(applicationId);
    assertThat(config.getPackages()).isEqualTo(packages);
    assertThat(config.getRegexes()).isEqualTo(regexes);
  }

  @Test
  public void testInsert_InvalidRegex() {
    for (String regex : ProprietaryConfigDAO.REGEX_BLACK_LIST) {
      List<String> regexes = Collections.singletonList(regex);
      ProprietaryConfig config = new ProprietaryConfig(application.getId(), null /* packages */, regexes);
      assertThatThrownBy(() -> dao.insert(config)).isInstanceOf(InvalidProprietaryConfigRegexException.class)
          .hasMessage("This regex is specifically disallowed: " + regex);
    }
  }

  @Test
  public void testInsert_InvalidRegexStar() {
    List<String> regexes = Collections.singletonList("*");
    ProprietaryConfig config = new ProprietaryConfig(application.getId(), null /* packages */, regexes);
    assertThrows(InvalidProprietaryConfigRegexException.class, () -> dao.insert(config));
  }

  @Test
  public void testInsert_InvalidRegexNull() {
    List<String> regexes = new ArrayList<>();
    regexes.add(null);
    ProprietaryConfig config = new ProprietaryConfig(application.getId(), null /* packages */, regexes);
    assertThrows(InvalidProprietaryConfigRegexException.class, () -> dao.insert(config));
  }

  @Test
  public void testUpdate_InvalidRegex() {
    ProprietaryConfig config = tempEntity.newProprietaryConfig(application.getId());

    for (String regex : ProprietaryConfigDAO.REGEX_BLACK_LIST) {
      List<String> regexes = Collections.singletonList(regex);
      config.setRegexes(regexes);
      assertThatThrownBy(() -> dao.update(config)).isInstanceOf(InvalidProprietaryConfigRegexException.class)
          .hasMessage("This regex is specifically disallowed: " + regex);
    }
  }

  @Test
  public void testUpdate_InvalidRegexStar() {
    ProprietaryConfig config = tempEntity.newProprietaryConfig(application.getId());

    List<String> regexes = Collections.singletonList("*");
    config.setRegexes(regexes);
    assertThrows(InvalidProprietaryConfigRegexException.class, () -> dao.update(config));
  }

  @Test
  public void testUpdate_InvalidRegexNull() {
    ProprietaryConfig config = tempEntity.newProprietaryConfig(application.getId());

    List<String> regexes = new ArrayList<>();
    regexes.add(null);
    config.setRegexes(regexes);
    assertThrows(InvalidProprietaryConfigRegexException.class, () -> dao.update(config));
  }

  @Test
  public void testGetByOwnerIdWithHierarchy_returnsConfigsAcrossAncestors() {
    ProprietaryConfig orgConfig = tempEntity.newProprietaryConfig(organization.getId());
    ProprietaryConfig appConfig = tempEntity.newProprietaryConfig(application.getId());

    List<ProprietaryConfig> result = dao.getByOwnerIdWithHierarchy(application.getId());

    assertThat(result).extracting(ProprietaryConfig::getId)
        .containsExactly(appConfig.getId(), orgConfig.getId());
  }

  @Test
  public void testGetByOwnerIdWithHierarchy_emptyWhenNoConfigs() {
    List<ProprietaryConfig> result = dao.getByOwnerIdWithHierarchy(application.getId());

    assertThat(result).isEmpty();
  }
}
