/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sourcecontrol;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.nexus.scm.SourceControlProvider;

import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.model.Organization.ROOT_ORGANIZATION_ID;
import static org.assertj.core.api.Assertions.assertThat;

public class SourceControlDataServiceTest
    extends AbstractComponentTest
{
  private static final String TOKEN = "test_token";

  @Inject
  private SourceControlDataService sourceControlDataService;

  @Inject
  private PasswordHandler passwordHandler;

  private Application app;

  @Before
  public void setup() {
    Organization org = tempEntity.newOrganization();
    tempEntity.newSourceControl(ROOT_ORGANIZATION_ID, null, null, SourceControlProvider.GITHUB);
    app = tempEntity.newApplication(org.getId());
  }

  @Test
  public void testGetCompositeSourceControlByOwnerDecrypted_WithToken() {
    tempEntity.newSourceControl(app.getId(), "https://github.com/org/repo",
        passwordHandler.encryptPassword(TOKEN), null);

    SourceControl result = sourceControlDataService.getCompositeSourceControlByOwnerDecrypted(app.getId());

    assertThat(result).isNotNull();
    assertThat(result.getOwnerId()).isEqualTo(app.getId());
    assertThat(result.getToken()).isEqualTo(TOKEN);
  }

  @Test
  public void testGetCompositeSourceControlByOwnerDecrypted_WithoutToken() {
    tempEntity.newSourceControl(app.getId(), "https://github.com/org/repo", null, null);

    SourceControl result = sourceControlDataService.getCompositeSourceControlByOwnerDecrypted(app.getId());

    assertThat(result).isNotNull();
    assertThat(result.getOwnerId()).isEqualTo(app.getId());
    assertThat(result.getToken()).isNull();
  }

  @Test
  public void testGetCompositeSourceControlByOwnerDecrypted_WithEmptyToken() {
    tempEntity.newSourceControl(app.getId(), "https://github.com/org/repo", "", null);

    SourceControl result = sourceControlDataService.getCompositeSourceControlByOwnerDecrypted(app.getId());

    assertThat(result).isNotNull();
    assertThat(result.getOwnerId()).isEqualTo(app.getId());
    assertThat(result.getToken()).isEmpty();
  }

  @Test
  public void testGetCompositeSourceControlByApplicationId() {
    tempEntity.newSourceControl(app.getId(), "https://github.com/org/repo",
        passwordHandler.encryptPassword(TOKEN), null);

    SourceControl result = sourceControlDataService.getCompositeSourceControlByApplicationId(app.getId());

    assertThat(result).isNotNull();
    assertThat(result.getOwnerId()).isEqualTo(app.getId());
  }

  @Test
  public void testFillWithDecryptedToken() {
    SourceControl sourceControl = new SourceControl.Builder()
        .setOwnerId(app.getId())
        .setRepositoryUrl("https://github.com/org/repo")
        .setToken(passwordHandler.encryptPassword(TOKEN))
        .build();

    sourceControlDataService.fillWithDecryptedToken(sourceControl);

    assertThat(sourceControl.getToken()).isEqualTo(TOKEN);
  }

  @Test
  public void testDecryptToken_Success() {
    String result = sourceControlDataService.decryptToken(passwordHandler.encryptPassword(TOKEN));

    assertThat(result).isEqualTo(TOKEN);
  }

  @Test
  public void testDecryptToken_EmptyInput() {
    String result = sourceControlDataService.decryptToken("");

    assertThat(result).isEmpty();
  }
}
