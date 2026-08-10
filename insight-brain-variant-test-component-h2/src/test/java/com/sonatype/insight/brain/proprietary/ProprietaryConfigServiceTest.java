/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.proprietary;

import java.util.Arrays;
import java.util.Collections;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.dataaccess.configuration.ProprietaryConfigDAO;
import com.sonatype.insight.brain.integration.Goal;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.configuration.ProprietaryConfig;
import com.sonatype.insight.brain.proprietary.ProprietaryConfigResource.FilePathRegex;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.error.exception.BadRequestException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.sonatype.insight.brain.Assert.assertProprietaryConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@ComponentH2Test
public class ProprietaryConfigServiceTest
    extends AbstractComponentH2Test
{
  @Inject
  private ProprietaryConfigDAO proprietaryConfigDAO;

  @Inject
  private ProprietaryConfigService proprietaryConfigService;

  private Application application;

  @BeforeEach
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

    assertThat(config.getRegexes()).containsExactly("application.regex", "organization.regex",
        "root.organization.regex");
    assertThat(config.getPackages()).containsExactly("application.package", "organization.package",
        "root.organization.package");
  }

  @Test
  public void testGetProprietaryConfig_GoalEvaluateComponent() {
    com.sonatype.clm.dto.model.ProprietaryConfig config = proprietaryConfigService
        .getProprietaryConfig(Goal.EVALUATE_COMPONENT, application.getPublicId());

    assertThat(config.getRegexes()).containsExactly("application.regex", "organization.regex",
        "root.organization.regex");
    assertThat(config.getPackages()).containsExactly("application.package", "organization.package",
        "root.organization.package");
  }

  @Test
  public void testGetProprietaryConfig_InvalidGoal() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(
            () -> proprietaryConfigService.getProprietaryConfig(Goal.SUMMARIZE_EVALUATION, application.getPublicId()))
        .withMessage("Proprietary Configuration requested for invalid goal: " + Goal.SUMMARIZE_EVALUATION);
  }

  @Test
  public void testGetProprietaryConfig_NoGoal() {
    com.sonatype.clm.dto.model.ProprietaryConfig config = proprietaryConfigService.getProprietaryConfig((Goal) null,
        application.getPublicId());

    assertThat(config.getRegexes()).containsExactly("root.organization.regex");
    assertThat(config.getPackages()).containsExactly("root.organization.package");
  }

  @Test
  public void testGetProprietaryConfig_NoAppId() {
    com.sonatype.clm.dto.model.ProprietaryConfig config = proprietaryConfigService
        .getProprietaryConfig(Goal.EVALUATE_APPLICATION, null);

    assertThat(config.getRegexes()).containsExactly("root.organization.regex");
    assertThat(config.getPackages()).containsExactly("root.organization.package");
  }

  @Test
  public void testGetProprietaryConfig_RootOrganization() {
    com.sonatype.clm.dto.model.ProprietaryConfig config = proprietaryConfigService
        .getProprietaryConfig(OwnerType.ORGANIZATION, Organization.ROOT_ORGANIZATION_ID);

    assertThat(config.getRegexes()).containsExactly("root.organization.regex");
    assertThat(config.getPackages()).containsExactly("root.organization.package");
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

    assertThat(proprietaryConfig.getRegexes()).containsExactly("application.regex", Pattern.quote("path1"),
        Pattern.quote("path2"),
        // This ensures we didn't add path1 twice
        Pattern.quote("path3"), "regex2");

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

    assertThat(proprietaryConfig.getRegexes()).containsExactly(Pattern.quote("path1"), Pattern.quote("path2"),
        // This ensures we didn't add path1 twice
        Pattern.quote("path3"), "regex");

    ProprietaryConfig persistedProprietaryConfig = proprietaryConfigDAO.getByOwnerId(org.getId());
    assertProprietaryConfig(proprietaryConfig, persistedProprietaryConfig);
  }

  @Test
  public void testCreateIsProprietary_NoProprietaryConfig() {
    Application application = tempEntity.newApplicationWithParent();
    assertThat(proprietaryConfigDAO.getByOwnerId(application.getId())).isNull();

    Predicate<String> isProprietary = proprietaryConfigService.createIsProprietary(application.getId());

    assertThat(isProprietary).rejects("any");
  }

  @Test
  public void testCreateIsProprietary_EmptyProprietaryConfig() {
    Application application = tempEntity.newApplicationWithParent();
    tempEntity.newProprietaryConfig(application.getId(), Collections.emptyList(), Collections.emptyList());

    Predicate<String> isProprietary = proprietaryConfigService.createIsProprietary(application.getId());

    assertThat(isProprietary).rejects("any");
  }

  @Test
  public void testCreateIsProprietary_WithPackages() {
    Application application = tempEntity.newApplicationWithParent();
    tempEntity.newProprietaryConfig(application.getId(), Arrays.asList("a1", "b1.b2"), Collections.emptyList());

    Predicate<String> isProprietary = proprietaryConfigService.createIsProprietary(application.getId());

    assertThat(isProprietary).accepts("a1");
    assertThat(isProprietary).rejects("a2");
    assertThat(isProprietary).accepts("a1.a2");
    assertThat(isProprietary).rejects("b1");
    assertThat(isProprietary).accepts("b1.b2");
    assertThat(isProprietary).rejects("b1.b3");
    assertThat(isProprietary).accepts("b1.b2.b3");
  }

  @Test
  public void testCreateIsProprietary_WithRegexes() {
    Application application = tempEntity.newApplicationWithParent();
    tempEntity.newProprietaryConfig(application.getId(), Collections.emptyList(), Arrays.asList("a1.*", ".*b1", "c1"));

    Predicate<String> isProprietary = proprietaryConfigService.createIsProprietary(application.getId());

    assertThat(isProprietary).accepts("a1");
    assertThat(isProprietary).accepts("a1.a2");
    assertThat(isProprietary).accepts("a1.a2.a3");
    assertThat(isProprietary).rejects("a2");
    assertThat(isProprietary).rejects("a2.a1");
    assertThat(isProprietary).rejects("a3.a2.a1");
    assertThat(isProprietary).accepts("b1");
    assertThat(isProprietary).rejects("b1.b2");
    assertThat(isProprietary).rejects("b1.b2.b3");
    assertThat(isProprietary).rejects("b2");
    assertThat(isProprietary).accepts("b2.b1");
    assertThat(isProprietary).accepts("b3.b2.b1");
    assertThat(isProprietary).accepts("c1");
    assertThat(isProprietary).rejects("c2");
    assertThat(isProprietary).rejects("c1.c2");
    assertThat(isProprietary).rejects("c2.c1");
  }

  @Test
  public void testCreateIsProprietary_WithPackagesAndRegexes() {
    Application application = tempEntity.newApplicationWithParent();
    tempEntity.newProprietaryConfig(application.getId(), Collections.singletonList("a1"),
        Collections.singletonList("b1"));

    Predicate<String> isProprietary = proprietaryConfigService.createIsProprietary(application.getId());

    assertThat(isProprietary).accepts("a1");
    assertThat(isProprietary).accepts("a1.a2");
    assertThat(isProprietary).accepts("a1.a2.a3");
    assertThat(isProprietary).rejects("a2");
    assertThat(isProprietary).rejects("a2.a1");
    assertThat(isProprietary).rejects("a3.a2.a1");
    assertThat(isProprietary).accepts("b1");
    assertThat(isProprietary).rejects("b1.b2");
    assertThat(isProprietary).rejects("b1.b2.b3");
    assertThat(isProprietary).rejects("b2");
    assertThat(isProprietary).rejects("b2.b1");
    assertThat(isProprietary).rejects("b3.b2.b1");
  }
}
