/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import com.sonatype.insight.brain.policy.evaluator.PolicyAlertNotifier.MailPolicyAlertCounts;

import org.junit.Assert;
import org.junit.Test;

public class PolicyAlertNotifierTest
{
  @Test
  public void testNotificationEmailSubject() throws Exception {
    Assert.assertEquals("Policy Alert: 1 critical violation out of 15",
        PolicyAlertNotifier.createPolicyMailSubject(new MailPolicyAlertCounts(1, 2, 3, 4, 5)));
    Assert.assertEquals("Policy Alert: 2 severe violations out of 14",
        PolicyAlertNotifier.createPolicyMailSubject(new MailPolicyAlertCounts(0, 2, 3, 4, 5)));
    Assert.assertEquals("Policy Alert: 3 moderate violations out of 12",
        PolicyAlertNotifier.createPolicyMailSubject(new MailPolicyAlertCounts(0, 0, 3, 4, 5)));
    Assert.assertEquals("Policy Alert: 9 neutral violations out of 9",
        PolicyAlertNotifier.createPolicyMailSubject(new MailPolicyAlertCounts(0, 0, 0, 4, 5)));
    Assert.assertEquals("Policy Alert: 5 neutral violations out of 5",
        PolicyAlertNotifier.createPolicyMailSubject(new MailPolicyAlertCounts(0, 0, 0, 0, 5)));
  }
}
