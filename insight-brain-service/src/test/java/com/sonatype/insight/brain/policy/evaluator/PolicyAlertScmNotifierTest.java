/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.io.IOException;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.git.PullRequestFeatureCheck;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.Mockito.when;

public class PolicyAlertScmNotifierTest
    extends AbstractComponentTest
{
  private static final Stage DEFAULT_STAGE = new Stage(Stage.ID_BUILD);

  private static final String PUBLIC_ID = "abc123";

  private static final String NAME = "reponame";

  private static final String ORGANIZATION_ID = "sonatype";

  @Mock
  private PullRequestFeatureCheck pullRequestFeatureCheck;

  private PolicyAlertScmNotifier scmNotifier;

  @Before
  public void setup() {
    scmNotifier = new PolicyAlertScmNotifier(pullRequestFeatureCheck);
  }

  @Test
  public void testShouldRunPullRequestFeature() throws IOException {
    Application application = new Application(PUBLIC_ID, NAME, ORGANIZATION_ID);
    when(pullRequestFeatureCheck.isPullRequestFeatureSupported(application)).thenReturn(false);
    scmNotifier.sendNotifications(application, "", DEFAULT_STAGE, Lists.emptyList(), 0, "");

    // TODO assert no invocations on anything once we have something
  }
}
