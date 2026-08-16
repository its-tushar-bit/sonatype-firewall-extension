/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.sourcecontrol;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.nexus.scm.SourceControlProvider;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlTestUtils.getTestUrlForProvider;
import static com.sonatype.nexus.scm.SourceControlProvider.GITHUB;
import static com.sonatype.nexus.scm.SourceControlProvider.GITLAB;
import static org.assertj.core.api.Assertions.assertThat;

public class SourceControlDAOUsernameTest
    extends AbstractDbDAOTest
{
  // This is the list of providers that we *KNOW* do not allow username
  private static final List<SourceControlProvider> USERNAME_NOT_ALLOWED_PROVIDERS = ImmutableList.of(GITHUB, GITLAB);

  private SourceControlDAO sourceControlDAO;

  private Application app;

  private Organization org;

  static Stream<SourceControlProvider> data() {
    // dynamically compute the test provider set
    return Arrays.stream(SourceControlProvider.values());
  }

  @BeforeEach
  @Override
  public void setup() {
    super.setup();
    sourceControlDAO = daoFactory.createSourceControlDAO();
    app = tempEntity.newApplicationWithParent();
    org = tempEntity.newOrganization();
  }

  @ParameterizedTest(name = "provider={0}")
  @MethodSource("data")
  public void testUsernameNotAllowed(final SourceControlProvider sourceControlProvider) {
    createRootOrgWithProvider(sourceControlProvider);
    SourceControl sourceControl = tempEntity
        .newSourceControl(app.getId(), getTestUrlForProvider(sourceControlProvider), null, null);

    // try to set the username
    sourceControl.setUsername("username");

    try {
      sourceControlDAO.update(sourceControl);

      // if we are here, verify we are allowed to provide a username
      assertThat(USERNAME_NOT_ALLOWED_PROVIDERS).doesNotContain(sourceControlProvider);
    }
    catch (BadRequestException e) {
      // if we are here, verify we are not allowed to provide a username
      assertThat(USERNAME_NOT_ALLOWED_PROVIDERS).contains(sourceControlProvider);
      assertThat(e.getMessage())
          .contains(String.format("SourceControl provider '%s' does not allow username", sourceControlProvider));
    }
  }

  private SourceControl createRootOrgWithProvider(final SourceControlProvider sourceControlProvider) {
    return tempEntity.newSourceControl(org.getParentOrganizationId(), null, null, sourceControlProvider);
  }
}
