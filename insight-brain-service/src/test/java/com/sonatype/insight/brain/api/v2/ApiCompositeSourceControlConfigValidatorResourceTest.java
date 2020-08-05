/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.git.ConfigurationValidationResult;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.service.AbstractResourceTest;
import com.sonatype.nexus.scm.SourceControlProvider;

import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.api.PublicApiPaths.COMPOSITE_SOURCE_CONTROL_CONFIG_VALIDATOR_PATH_V2;
import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static org.assertj.core.api.Assertions.assertThat;

public class ApiCompositeSourceControlConfigValidatorResourceTest
    extends AbstractResourceTest
{
  static final String VALID_URL = "https://example.com/organization/project";

  private Application app;

  private PasswordHandler pwHandler;

  @Before
  public void setup() throws Exception {
    pwHandler = getCLMServer().getInstance(PasswordHandler.class);
    app = tempEntity.newApplicationWithParent();
    tempEntity
        .newSourceControl(ROOT_ORGANIZATION_ID, null, null, null, SourceControlProvider.GITHUB, null, null,
            "BASE_BRANCH", null);
    tempEntity.newSourceControl(app.getId(), VALID_URL, null, encrypt("TOKEN"), null, null, true, null, null);
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(COMPOSITE_SOURCE_CONTROL_CONFIG_VALIDATOR_PATH_V2).auth();
  }

  @Test
  public void testValidateSourceControlConfig_ValidApplication() throws Exception {
    final HttpResponse response = restRequest()
        .parameter(app.getId())
        .get();
    assertResponseStatus(200, response);
    final ConfigurationValidationResult result = response.getBody(ConfigurationValidationResult.class);

    assertThat(result).isNotNull();
    assertThat(result.getConfigurationComplete().isValid()).isTrue();
  }

  @Test
  public void testValidateSourceControlConfig_Incomplete() throws Exception {
    final HttpResponse response = restRequest()
        .parameter("1234")
        .get();
    assertResponseStatus(200, response);
    final ConfigurationValidationResult result = response.getBody(ConfigurationValidationResult.class);

    assertThat(result).isNotNull();
    assertThat(result.getConfigurationComplete().isValid()).isFalse();
    assertThat(result.getConfigurationComplete().getMessage()).isEqualTo("Some required values are missing or unsaved");
  }

  @Test
  public void testValidateSourceControlConfig_UnexpectedException() throws Exception {
    Application appWithBrokenToken = tempEntity.newApplicationWithParent();
    // do not encrypt the password
    tempEntity
        .newSourceControl(appWithBrokenToken.getId(), VALID_URL, null, "UNENCRYPTED", null, null, true, null, null);

    // retrieving the GitRepositoryInfo will throw an exception
    final HttpResponse response = restRequest()
        .parameter(appWithBrokenToken.getId())
        .get();
    assertResponseStatus(500, response);
  }

  private String encrypt(String password) {
    return new String(pwHandler.encryptPassword(password.toCharArray()));
  }
}
