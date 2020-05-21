/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.sourcecontrol;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.nexus.scm.SourceControlProvider;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlTestUtils.getTestUrlForProvider;
import static com.sonatype.nexus.scm.SourceControlProvider.GITHUB;
import static com.sonatype.nexus.scm.SourceControlProvider.GITLAB;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class SourceControlDAOUsernameTest
    extends AbstractDbDAOTest
{
  // This is the list of providers that we *KNOW* do not allow username
  private static final List<SourceControlProvider> USERNAME_NOT_ALLOWED_PROVIDERS = ImmutableList.of(GITHUB, GITLAB);

  private final SourceControlDAO sourceControlDAO = new SourceControlDAO();

  private final SourceControlProvider sourceControlProvider;

  private Application app;

  private Organization org;

  public SourceControlDAOUsernameTest(final SourceControlProvider sourceControlProvider) {
    this.sourceControlProvider = sourceControlProvider;
  }

  @Parameterized.Parameters(name = "provider={0}")
  public static Collection<Object[]> data() {
    // dynamically compute the test provider set
    return Arrays.stream(SourceControlProvider.values())
        .map(v -> new Object[]{v})
        .collect(Collectors.toList());
  }

  @Override
  @Before
  public void setup() {
    app = tempEntity.newApplicationWithParent();
    org = tempEntity.newOrganization();
  }

  @Test
  public void testUsernameNotAllowed() {
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
