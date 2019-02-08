/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.InvalidProprietaryConfigRegexException;
import com.sonatype.insight.brain.model.configuration.ProprietaryConfig;
import com.sonatype.insight.error.exception.BadRequestException;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ProprietaryConfigDAOTest
    extends AbstractDbDAOTest
{
  private ProprietaryConfigDAO dao = new ProprietaryConfigDAO();

  @Test
  public void testCRUD() throws Exception {
    // Create
    List<String> packages = Collections.singletonList("foo");
    List<String> regexes = Collections.singletonList("bar");
    ProprietaryConfig config = new ProprietaryConfig(applicationId, packages, regexes);
    dao.insert(config);
    assertThat(config.getId()).isNotNull();

    // Read
    config = dao.getById(config.getId());
    assertProprietaryConfig(applicationId, packages, regexes, config);

    // Update
    packages = Collections.singletonList("foo.updated");
    config.setPackages(packages);
    dao.update(config);

    // Read
    config = dao.getById(config.getId());
    assertProprietaryConfig(applicationId, packages, regexes, config);

    // Delete
    dao.delete(config);

    config = dao.getById(config.getId());
    assertThat(config).isNull();
  }

  @Test
  public void testInsert_Duplicate() {
    tempEntity.newProprietaryConfig(applicationId);

    ProprietaryConfig config1 = new ProprietaryConfig(applicationId, null /* packages */, null /* regexes */);
    assertThatThrownBy(() -> {
      dao.insert(config1);
    }).isInstanceOf(BadRequestException.class)
        .hasMessage("A proprietary config already exists for owner id " + applicationId);
  }

  @Test
  public void testUpdate_Duplicate() {
    tempEntity.newProprietaryConfig(applicationId);
    ProprietaryConfig config1 = tempEntity.newProprietaryConfig(organization.getId());

    config1.setOwnerId(applicationId);
    assertThatThrownBy(() -> {
      dao.update(config1);
    }).isInstanceOf(BadRequestException.class)
        .hasMessage("A proprietary config already exists for owner id " + applicationId);
  }

  private void assertProprietaryConfig(String applicationId,
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
      List<String> regexes = Arrays.asList(regex);
      ProprietaryConfig config = new ProprietaryConfig(applicationId, null /* packages */, regexes);
      assertThatThrownBy(() -> {
        dao.insert(config);
      }).isInstanceOf(InvalidProprietaryConfigRegexException.class)
          .hasMessage("This regex is specifically disallowed: " + regex);
    }
  }

  @Test(expected = InvalidProprietaryConfigRegexException.class)
  public void testInsert_InvalidRegexStar() {
    List<String> regexes = Arrays.asList("*");
    ProprietaryConfig config = new ProprietaryConfig(applicationId, null /* packages */, regexes);
    dao.insert(config);
  }

  @Test(expected = InvalidProprietaryConfigRegexException.class)
  public void testInsert_InvalidRegexNull() {
    List<String> regexes = new ArrayList<>();
    regexes.add(null);
    ProprietaryConfig config = new ProprietaryConfig(applicationId, null /* packages */, regexes);
    dao.insert(config);
  }

  @Test
  public void testUpdate_InvalidRegex() {
    ProprietaryConfig config = tempEntity.newProprietaryConfig(applicationId);

    for (String regex : ProprietaryConfigDAO.REGEX_BLACK_LIST) {
      List<String> regexes = Arrays.asList(regex);
      config.setRegexes(regexes);
      assertThatThrownBy(() -> {
        dao.update(config);
      }).isInstanceOf(InvalidProprietaryConfigRegexException.class)
          .hasMessage("This regex is specifically disallowed: " + regex);
    }
  }

  @Test(expected = InvalidProprietaryConfigRegexException.class)
  public void testUpdate_InvalidRegexStar() {
    ProprietaryConfig config = tempEntity.newProprietaryConfig(applicationId);

    List<String> regexes = Arrays.asList("*");
    config.setRegexes(regexes);
    dao.update(config);
  }

  @Test(expected = InvalidProprietaryConfigRegexException.class)
  public void testUpdate_InvalidRegexNull() {
    ProprietaryConfig config = tempEntity.newProprietaryConfig(applicationId);

    List<String> regexes = new ArrayList<>();
    regexes.add(null);
    config.setRegexes(regexes);
    dao.update(config);
  }
}
