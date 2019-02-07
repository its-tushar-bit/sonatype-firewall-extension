/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import java.util.Arrays;
import java.util.Collections;

import javax.inject.Inject;

import com.sonatype.insight.brain.model.policy.notifications.PolicyNotification;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.repository.RepositoryPolicyAlertNotificationTask.ProcessAlertRunnable;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import com.google.inject.Binder;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

public class RepositoryPolicyAlertNotificationTaskTest
    extends AbstractComponentTest
{
  @Inject
  RepositoryPolicyAlertNotificationTask notifier;

  @Inject
  PendingRepositoryPolicyNotifications notificationQueue;

  @Mock
  RepositoryPolicyAlertEmailer emailer;

  @Override
  public void configure(Binder binder) {
    binder.bind(RepositoryPolicyAlertEmailer.class).toInstance(emailer);
    super.configure(binder);
  }

  @Test
  public void testProcessNotificationRunnable() throws Exception {
    Repository repo1 = tempEntity.newRepository();
    Repository repo2 = tempEntity.newRepository();

    PolicyNotification policyNotification1 = new PolicyNotification();
    PolicyNotification policyNotification2 = new PolicyNotification();
    PolicyNotification policyNotification3 = new PolicyNotification();

    notificationQueue.add(repo1.getId(), policyNotification1);
    notificationQueue.add(repo2.getId(), policyNotification2);
    notificationQueue.add(repo2.getId(), policyNotification3);

    ProcessAlertRunnable runnable = new ProcessAlertRunnable(notificationQueue, emailer);

    runnable.run();

    verify(emailer)
        .sendNotifications(argThat(new RepositoryEq(repo1)), eq(Collections.singletonList(policyNotification1)));
    verify(emailer).sendNotifications(argThat(new RepositoryEq(repo2)),
        eq(Arrays.asList(policyNotification2, policyNotification3)));

    assertThat(notificationQueue.remove()).isEmpty();
  }

  //This is required as Repository doesn't implement equals/hashCode
  private static class RepositoryEq
      implements ArgumentMatcher<Repository>
  {
    private final Repository repository;

    RepositoryEq(Repository repository) {
      this.repository = repository;
    }

    @Override
    public boolean matches(Repository other) {
      if (repository == null) {
        return other == null;
      }

      if (other == null) {
        return false;
      }

      return repository.getId().equals(other.getId());
    }
  }
}
