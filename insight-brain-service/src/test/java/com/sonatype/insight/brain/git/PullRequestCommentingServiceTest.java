/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.git;

import java.security.NoSuchAlgorithmException;
import java.util.Optional;

import javax.inject.Provider;

import com.sonatype.insight.brain.dataaccess.sourcecontrol.SourceControlPullRequestCommentDAO;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControlPullRequestComment;
import com.sonatype.insight.brain.policy.PolicyEvaluationDiffService;
import com.sonatype.insight.brain.policy.evaluator.PolicyViolationDiff;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class PullRequestCommentingServiceTest
    extends VerifiableLoggingTestBase
{
  @Mock
  private PullRequestCommentCreator mockPullRequestCommentCreator;

  @Mock
  private PolicyEvaluationDiffService mockPolicyEvaluationDiffService;

  public PullRequestCommentingServiceTest() {
    super(PullRequestCommentingService.class);
  }

  @Before
  @Override
  public void setup() {
    MockitoAnnotations.openMocks(this);
    super.setup();
  }

  @Test
  public void testDoCreateOrUpdatePullRequestComment_noPolicyEvalDiff() {
    // given: no policy eval diff for the given PR
    PullRequestCommentingService pullRequestCommentingService = new TestablePullRequestCommentingService()
        .withNoPolicyEvaluationDiff()
        .build();

    PullRequestPolicyEvaluationsDTO pullRequestPolicyEvaluationsDTO = new PullRequestPolicyEvaluationsDTO()
        .setApplicationId("app1")
        .setPullRequestNumber(123);

    // when:
    pullRequestCommentingService.doCreateOrUpdatePullRequestComment(pullRequestPolicyEvaluationsDTO);

    // then: expecting no attempt to create a comment
    verify(mockPullRequestCommentCreator, never()).createPullRequestComment(any(), any(), any(), any());
    verify(mockPullRequestCommentCreator, never()).updatePullRequestComment(any(), any(), any(), any(), any());
    verify(mockPolicyEvaluationDiffService).createPolicyViolationDiffByComponents(any(), any(),
        eq(PullRequestCommentingService.MINIMUM_THREAT_LEVEL));
    assertThatLogMessagesEqual(
        info("Unable to get the policy evaluation diff for application 'app1' pull request '123'.")
    );
  }

  @Test
  public void testDoCreateOrUpdatePullRequestComment_noClearedOrAppearedViolations() {
    // given: policy violation diff with no cleared or appeared violations
    PullRequestCommentingService pullRequestCommentingService = new TestablePullRequestCommentingService()
        .withMeaninglessPolicyViolationDiff()
        .build();

    PullRequestPolicyEvaluationsDTO pullRequestPolicyEvaluationsDTO = new PullRequestPolicyEvaluationsDTO()
        .setApplicationId("app1")
        .setPullRequestNumber(123);

    // when:
    pullRequestCommentingService.doCreateOrUpdatePullRequestComment(pullRequestPolicyEvaluationsDTO);

    // then: expecting no attempt to create a comment
    verify(mockPullRequestCommentCreator, never()).createPullRequestComment(any(), any(), any(), any());
    verify(mockPullRequestCommentCreator, never()).updatePullRequestComment(any(), any(), any(), any(), any());
    verify(mockPolicyEvaluationDiffService).createPolicyViolationDiffByComponents(any(), any(),
        eq(PullRequestCommentingService.MINIMUM_THREAT_LEVEL));
    assertThatLogMessagesEqual(
        info("No added or cleared violations in policy evaluation diff, and no previous PR comments for application" +
            " 'app1' pull request '123'.")
    );
  }

  @Test
  public void testDoCreateOrUpdatePullRequestComment_withAppearedViolation() {
    // given: policy violation diff with an appeared violation
    PullRequestCommentingService pullRequestCommentingService = new TestablePullRequestCommentingService()
        .withAppearedViolation()
        .build();

    PullRequestPolicyEvaluationsDTO pullRequestPolicyEvaluationsDTO = new PullRequestPolicyEvaluationsDTO()
        .setApplicationId("app1")
        .setPullRequestNumber(123);

    // when:
    pullRequestCommentingService.doCreateOrUpdatePullRequestComment(pullRequestPolicyEvaluationsDTO);

    // then: we should be creating a comment
    verify(mockPullRequestCommentCreator, times(1))
        .createPullRequestComment(eq(pullRequestPolicyEvaluationsDTO), any(), any(), any());
    verify(mockPullRequestCommentCreator, never()).updatePullRequestComment(any(), any(), any(), any(), any());
    verify(mockPolicyEvaluationDiffService).createPolicyViolationDiffByComponents(any(), any(),
        eq(PullRequestCommentingService.MINIMUM_THREAT_LEVEL));

    assertNoErrorsInLogs();
    assertNoWarningsInLogs();
  }

  @Test
  public void testDoCreateOrUpdatePullRequestComment_withClearedViolation() {
    // given: policy violation diff with a cleared violation
    PullRequestCommentingService pullRequestCommentingService = new TestablePullRequestCommentingService()
        .withClearedViolation()
        .build();

    PullRequestPolicyEvaluationsDTO pullRequestPolicyEvaluationsDTO = new PullRequestPolicyEvaluationsDTO()
        .setApplicationId("app1")
        .setPullRequestNumber(123);

    // when:
    pullRequestCommentingService.doCreateOrUpdatePullRequestComment(pullRequestPolicyEvaluationsDTO);

    // then: we should be creating a comment
    verify(mockPullRequestCommentCreator, times(1))
        .createPullRequestComment(eq(pullRequestPolicyEvaluationsDTO), any(), any(), any());
    verify(mockPullRequestCommentCreator, never()).updatePullRequestComment(any(), any(), any(), any(), any());
    verify(mockPolicyEvaluationDiffService).createPolicyViolationDiffByComponents(any(), any(),
        eq(PullRequestCommentingService.MINIMUM_THREAT_LEVEL));

    assertNoErrorsInLogs();
    assertNoWarningsInLogs();
  }

  @Test
  public void testDoCreateOrUpdatePullRequestComment_withAppearedAndClearedViolations() {
    // given: policy violation diff with both cleared and appeared violations
    PullRequestCommentingService pullRequestCommentingService = new TestablePullRequestCommentingService()
        .withAppearedViolation()
        .withClearedViolation()
        .build();

    PullRequestPolicyEvaluationsDTO pullRequestPolicyEvaluationsDTO = new PullRequestPolicyEvaluationsDTO()
        .setApplicationId("app1")
        .setPullRequestNumber(123);

    // when:
    pullRequestCommentingService.doCreateOrUpdatePullRequestComment(pullRequestPolicyEvaluationsDTO);

    // then: we should be creating a comment
    verify(mockPullRequestCommentCreator, times(1))
        .createPullRequestComment(eq(pullRequestPolicyEvaluationsDTO), any(), any(), any());
    verify(mockPullRequestCommentCreator, never()).updatePullRequestComment(any(), any(), any(), any(), any());
    verify(mockPolicyEvaluationDiffService).createPolicyViolationDiffByComponents(any(), any(),
        eq(PullRequestCommentingService.MINIMUM_THREAT_LEVEL));

    assertNoErrorsInLogs();
    assertNoWarningsInLogs();
  }

  @Test
  public void testDoCreateOrUpdatePullRequestComment_existingCommentNoChanges() {
    // given: existing PR comment and new request with no content changes
    PullRequestCommentingService pullRequestCommentingService = new TestablePullRequestCommentingService()
        .withAppearedViolation()
        .withClearedViolation()
        .withExistingPullRequestComment()
        .withSameContentHash()
        .build();

    PullRequestPolicyEvaluationsDTO pullRequestPolicyEvaluationsDTO = new PullRequestPolicyEvaluationsDTO()
        .setApplicationId("app1")
        .setPullRequestNumber(123);

    // when:
    pullRequestCommentingService.doCreateOrUpdatePullRequestComment(pullRequestPolicyEvaluationsDTO);

    // then: we should be not be creating or updating a comment
    verify(mockPullRequestCommentCreator, never()).createPullRequestComment(any(), any(), any(), any());
    verify(mockPullRequestCommentCreator, never()).updatePullRequestComment(any(), any(), any(), any(), any());
    verify(mockPolicyEvaluationDiffService).createPolicyViolationDiffByComponents(any(), any(),
        eq(PullRequestCommentingService.MINIMUM_THREAT_LEVEL));

    assertThatLogMessagesEqual(
        info("Policy evaluations have not changed for application 'app1' pull request '123'.")
    );
  }

  @Test
  public void testDoCreateOrUpdatePullRequestComment_updateExistingComment() {
    // given: existing PR comment and new request with some content changes
    PullRequestCommentingService pullRequestCommentingService = new TestablePullRequestCommentingService()
        .withAppearedViolation()
        .withClearedViolation()
        .withExistingPullRequestComment()
        .withDifferentContentHash()
        .build();

    PullRequestPolicyEvaluationsDTO pullRequestPolicyEvaluationsDTO = new PullRequestPolicyEvaluationsDTO()
        .setApplicationId("app1")
        .setPullRequestNumber(123);

    // when:
    pullRequestCommentingService.doCreateOrUpdatePullRequestComment(pullRequestPolicyEvaluationsDTO);

    // then: we should be updating a comment
    verify(mockPullRequestCommentCreator, never()).createPullRequestComment(any(), any(), any(), any());
    verify(mockPullRequestCommentCreator, times(1))
        .updatePullRequestComment(eq(pullRequestPolicyEvaluationsDTO), any(), any(), any(), any());
    verify(mockPolicyEvaluationDiffService).createPolicyViolationDiffByComponents(any(), any(),
        eq(PullRequestCommentingService.MINIMUM_THREAT_LEVEL));

    assertNoErrorsInLogs();
    assertNoWarningsInLogs();
  }

  @Test
  public void testDoCreateOrUpdatePullRequestComment_propagateException() {
    // given: existing PR comment and new request with some content changes
    PullRequestCommentingService pullRequestCommentingService = new TestablePullRequestCommentingService()
        .withAppearedViolation()
        .withDifferentContentHash()
        .withExceptionOnCommentCreation()
        .build();

    PullRequestPolicyEvaluationsDTO pullRequestPolicyEvaluationsDTO = new PullRequestPolicyEvaluationsDTO()
        .setApplicationId("app1")
        .setPullRequestNumber(123);

    // expect:
    assertThatExceptionOfType(SourceControlException.class).isThrownBy(() ->
        pullRequestCommentingService.doCreateOrUpdatePullRequestComment(pullRequestPolicyEvaluationsDTO)
    ).withMessage("test");
    verify(mockPolicyEvaluationDiffService).createPolicyViolationDiffByComponents(any(), any(),
        eq(PullRequestCommentingService.MINIMUM_THREAT_LEVEL));

    assertNoErrorsInLogs();
    assertNoWarningsInLogs();
  }

  private class TestablePullRequestCommentingService
  {
    @Mock
    private Provider<PullRequestCommentingHashBuilder> mockHashBuilderProvider;

    @Mock
    private PullRequestCommentingHashBuilder mockHashBuilder;

    @Mock
    private PullRequestCommentingRemediationService mockRemediationService;

    @Mock
    private SourceControlPullRequestCommentDAO mockPullRequestCommentDAO;

    private SourceControlPullRequestComment existingPullRequestComment;

    private boolean omitPolicyViolationDiff;

    private final Optional<PolicyViolationDiff<PolicyViolation>> policyViolationDiff =
        Optional.of(new PolicyViolationDiff<>());

    TestablePullRequestCommentingService withAppearedViolation() {
      policyViolationDiff.get().addAppeared(new PolicyViolation());
      return this;
    }

    TestablePullRequestCommentingService withClearedViolation() {
      policyViolationDiff.get().addCleared(new PolicyViolation());
      return this;
    }

    TestablePullRequestCommentingService withExistingPullRequestComment() {
      existingPullRequestComment = new SourceControlPullRequestComment()
          .setContentHash("content-hash-2");
      return this;
    }

    TestablePullRequestCommentingService withMeaninglessPolicyViolationDiff() {
      // no-op - this is the default behavior; method exists for test clarity
      return this;
    }

    TestablePullRequestCommentingService withNoPolicyEvaluationDiff() {
      omitPolicyViolationDiff = true;
      return this;
    }

    TestablePullRequestCommentingService withDifferentContentHash() {
      if (null != existingPullRequestComment) {
        existingPullRequestComment.setContentHash("content-hash-2");
      }
      return this;
    }

    TestablePullRequestCommentingService withSameContentHash() {
      if (null != existingPullRequestComment) {
        existingPullRequestComment.setContentHash("content-hash-1");
      }
      return this;
    }

    TestablePullRequestCommentingService withExceptionOnCommentCreation() {
      doThrow(new SourceControlException("test")).when(mockPullRequestCommentCreator)
          .createPullRequestComment(any(), any(), any(), any());
      return this;
    }

    PullRequestCommentingService build() {
      MockitoAnnotations.openMocks(this);

      setupContentHash();
      setupExistingPullRequestComment();
      setupPolicyViolationDiff();

      return new PullRequestCommentingService(
          mockPullRequestCommentCreator,
          mockHashBuilderProvider,
          mockPolicyEvaluationDiffService,
          mockPullRequestCommentDAO,
          mockRemediationService
      );
    }

    private void setupContentHash() {
      doReturn(mockHashBuilder).when(mockHashBuilderProvider).get();
      doReturn(mockHashBuilder).when(mockHashBuilder).withPolicyViolationDiff(any());
      doReturn(mockHashBuilder).when(mockHashBuilder).withRemediationVersionMap(any());
      try {
        doReturn("content-hash-1").when(mockHashBuilder).generateHash();
      }
      catch (NoSuchAlgorithmException e) {
        fail(e.getMessage());
      }
    }

    private void setupExistingPullRequestComment() {
      doReturn(existingPullRequestComment).when(mockPullRequestCommentDAO)
          .getByApplicationIdAndPullRequestIdWithoutComponent(any(), anyInt());
    }

    private void setupPolicyViolationDiff() {
      if (!omitPolicyViolationDiff) {
        doReturn(policyViolationDiff).when(mockPolicyEvaluationDiffService)
            .createPolicyViolationDiffByComponents(any(), any(), eq(PullRequestCommentingService.MINIMUM_THREAT_LEVEL));
      }
    }
  }
}
