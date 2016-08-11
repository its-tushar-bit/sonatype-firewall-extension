/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.proprietary;

import java.util.Arrays;
import java.util.Collections;
import java.util.regex.Pattern;

import javax.inject.Inject;

import com.sonatype.insight.brain.configuration.ProprietaryConfig;
import com.sonatype.insight.brain.dataaccess.configuration.ProprietaryConfigDAO;
import com.sonatype.insight.brain.integration.Goal;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.proprietary.ProprietaryConfigResource.FilePathRegex;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadRequestException;

import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.Assert.assertProprietaryConfig;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;

public class ProprietaryConfigServiceTest
    extends AbstractComponentTest
{
  @Inject
  private ProprietaryConfigDAO proprietaryConfigDAO;

  @Inject
  private ProprietaryConfigService proprietaryConfigService;

  private Application application;

  @Before
  public void before() {
    application = tempEntity.newApplicationWithParent("ProprietaryComponentMatchers");

    tempEntity.newProprietaryConfig(Organization.ROOT_ORGANIZATION_ID,
        Collections.singletonList("root.organization.package"), Collections.singletonList("root.organization.regex"));

    tempEntity.newProprietaryConfig(application.getId(), Collections.singletonList("application.package"),
        Collections.singletonList("application.regex"));

    tempEntity.newProprietaryConfig(application.getParentOwnerId(), Collections.singletonList("organization.package"),
        Collections.singletonList("organization.regex"));
  }

  @Test
  public void testGetProprietaryConfig_GoalEvaluateApplication() {
    com.sonatype.clm.dto.model.ProprietaryConfig config = proprietaryConfigService
        .getProprietaryConfig(Goal.EVALUATE_APPLICATION, application.getPublicId());

    assertThat(config.getRegexes(), contains("application.regex", "organization.regex", "root.organization.regex"));
    assertThat(config.getPackages(),
        contains("application.package", "organization.package", "root.organization.package"));
  }

  @Test
  public void testGetProprietaryConfig_GoalEvaluateComponent() {
    com.sonatype.clm.dto.model.ProprietaryConfig config = proprietaryConfigService
        .getProprietaryConfig(Goal.EVALUATE_COMPONENT, application.getPublicId());

    assertThat(config.getRegexes(), contains("application.regex", "organization.regex", "root.organization.regex"));
    assertThat(config.getPackages(),
        contains("application.package", "organization.package", "root.organization.package"));
  }

  @Test
  public void testGetProprietaryConfig_InvalidGoal() throws Exception {
    try {
      proprietaryConfigService.getProprietaryConfig(Goal.SUMMARIZE_EVALUATION, application.getPublicId());
      fail("Expected exception was not thrown");
    }
    catch (BadRequestException e) {
      assertThat(e.getMessage(),
          is("Proprietary Configuration requested for invalid goal: " + Goal.SUMMARIZE_EVALUATION));
    }
  }

  @Test
  public void testGetProprietaryConfig_NoGoal() throws Exception {
    com.sonatype.clm.dto.model.ProprietaryConfig config = proprietaryConfigService.getProprietaryConfig((Goal) null,
        application.getPublicId());

    assertThat(config.getRegexes(), contains("root.organization.regex"));
    assertThat(config.getPackages(), contains("root.organization.package"));
  }

  @Test
  public void testGetProprietaryConfig_NoAppId() throws Exception {
    com.sonatype.clm.dto.model.ProprietaryConfig config = proprietaryConfigService
        .getProprietaryConfig(Goal.EVALUATE_APPLICATION, null);

    assertThat(config.getRegexes(), contains("root.organization.regex"));
    assertThat(config.getPackages(), contains("root.organization.package"));
  }

  @Test
  public void testGetProprietaryConfig_RootOrganization() {
    com.sonatype.clm.dto.model.ProprietaryConfig config = proprietaryConfigService
        .getProprietaryConfig(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID);

    assertThat(config.getRegexes(), contains("root.organization.regex"));
    assertThat(config.getPackages(), contains("root.organization.package"));
  }

  @Test
  public void testUpsertProprietaryConfig_Success_Insert() {
    Application app = tempEntity.newApplicationWithParent("appPublicId");
    ProprietaryConfig proprietaryConfig = new ProprietaryConfig(app.getId(), Arrays.asList("package1", "package2"),
        Arrays.asList("regex1", "regex2"));

    proprietaryConfigService.upsertProprietaryConfig(app.getType(), app.getPublicId(), proprietaryConfig);

    ProprietaryConfig persistedProprietaryConfig = proprietaryConfigDAO.getByOwnerId(app.getId());

    assertProprietaryConfig(proprietaryConfig, persistedProprietaryConfig);
  }

  @Test
  public void testUpsertProprietaryConfig_Success_Update() {
    ProprietaryConfig proprietaryConfig = proprietaryConfigDAO.getByOwnerId(application.getId());

    // Add Package and Regex
    proprietaryConfig.setPackages(Arrays.asList("package1", "package2"));
    proprietaryConfig.setRegexes(Arrays.asList("regex1", "regex2"));

    proprietaryConfigService
        .upsertProprietaryConfig(application.getType(), application.getPublicId(), proprietaryConfig);

    ProprietaryConfig persistedProprietaryConfig = proprietaryConfigDAO.getByOwnerId(application.getId());

    assertProprietaryConfig(proprietaryConfig, persistedProprietaryConfig);
  }

  @Test
  public void testAddFilePathRegexToProprietaryConfig_Success_WhenProprietaryConfigExist() {
    // Add File Path regex
    FilePathRegex filePathRegex = new FilePathRegex();
    filePathRegex.paths = Arrays.asList("path1", "path2", "path1", "path3");
    filePathRegex.regex = "regex2";

    ProprietaryConfig proprietaryConfig = proprietaryConfigService
        .addFilePathRegexToProprietaryConfig(application.getType(), application.getPublicId(), filePathRegex);

    assertThat(proprietaryConfig.getRegexes(), hasSize(5));
    assertThat(proprietaryConfig.getRegexes().get(0), is("application.regex"));
    assertThat(proprietaryConfig.getRegexes().get(1), is(Pattern.quote("path1")));
    assertThat(proprietaryConfig.getRegexes().get(2), is(Pattern.quote("path2")));

    // This ensures we didn't add path1 twice
    assertThat(proprietaryConfig.getRegexes().get(3), is(Pattern.quote("path3")));
    assertThat(proprietaryConfig.getRegexes().get(4), is("regex2"));

    ProprietaryConfig persistedProprietaryConfig = proprietaryConfigDAO.getByOwnerId(application.getId());
    assertProprietaryConfig(proprietaryConfig, persistedProprietaryConfig);
  }

  @Test
  public void testAddFilePathRegexToProprietaryConfig_Success_WhenProprietaryConfigDoesNotExist() {
    Organization org = tempEntity.newOrganization("Org123");

    // Add File Path regex
    FilePathRegex filePathRegex = new FilePathRegex();
    filePathRegex.paths = Arrays.asList("path1", "path2", "path1", "path3");
    filePathRegex.regex = "regex";

    ProprietaryConfig proprietaryConfig = proprietaryConfigService
        .addFilePathRegexToProprietaryConfig(org.getType(), org.getId(), filePathRegex);

    assertThat(proprietaryConfig.getRegexes(), hasSize(4));
    assertThat(proprietaryConfig.getRegexes().get(0), is(Pattern.quote("path1")));
    assertThat(proprietaryConfig.getRegexes().get(1), is(Pattern.quote("path2")));

    // This ensures we didn't add path1 twice
    assertThat(proprietaryConfig.getRegexes().get(2), is(Pattern.quote("path3")));
    assertThat(proprietaryConfig.getRegexes().get(3), is("regex"));

    ProprietaryConfig persistedProprietaryConfig = proprietaryConfigDAO.getByOwnerId(org.getId());
    assertProprietaryConfig(proprietaryConfig, persistedProprietaryConfig);
  }
}
