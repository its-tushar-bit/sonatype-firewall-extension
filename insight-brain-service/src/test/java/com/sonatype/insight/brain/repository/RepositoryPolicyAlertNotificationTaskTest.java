/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.repository;

import java.util.Arrays;
import java.util.Collections;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.policy.PolicyAlert;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.repository.RepositoryPolicyAlertNotificationTask.ProcessAlertRunnable;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import com.google.inject.Binder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
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
    super.configure(binder);
    binder.bind(RepositoryPolicyAlertEmailer.class).toInstance(emailer);
  }

  @Test
  public void testProcessAlertRunnable() throws Exception {
    Repository repo1 = tempEntity.newRepository();
    Repository repo2 = tempEntity.newRepository();

    PolicyAlert alert1 = new PolicyAlert();
    PolicyAlert alert2 = new PolicyAlert();
    PolicyAlert alert3 = new PolicyAlert();

    notificationQueue.add(repo1.getId(), alert1);
    notificationQueue.add(repo2.getId(), alert2);
    notificationQueue.add(repo2.getId(), alert3);

    ProcessAlertRunnable runnable = new ProcessAlertRunnable(notificationQueue, emailer);

    runnable.run();

    verify(emailer).sendNotifications(argThat(new RepositoryEq(repo1)), eq(Collections.singletonList(alert1)));
    verify(emailer).sendNotifications(argThat(new RepositoryEq(repo2)), eq(Arrays.asList(alert2, alert3)));

    assertThat(notificationQueue.remove()).isEmpty();
  }

  //This is required as Repository doesn't implement equals/hashCode
  private static class RepositoryEq
      extends ArgumentMatcher<Repository>
  {
    private final Repository repository;

    RepositoryEq(Repository repository) {
      this.repository = repository;
    }

    public boolean matches(Object obj) {
      Repository other = (Repository) obj;
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
