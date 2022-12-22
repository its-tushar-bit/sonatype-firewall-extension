/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.cloud.scan.cli;

import java.util.List;

import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.scan.cli.AbstractPolicyEvaluatorTest;
import com.sonatype.insight.scan.cli.AbstractPolicyEvaluatorTestRunner;
import com.sonatype.insight.scan.cli.ExitException;
import com.sonatype.insight.scan.cli.JUnitPolicyEvaluatorTestRunner;
import com.sonatype.insight.test.LogOutput;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static com.google.common.collect.ObjectArrays.concat;
import static com.sonatype.clm.dto.model.policy.Action.ID_WARN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class CloudPolicyEvaluatorTest
    extends AbstractPolicyEvaluatorTest
{
  private static final String TEST_APPLICATION_ID = "the-app-id";

  @Rule
  public LogOutput cloudLogOutput = new LogOutput(1, CloudPolicyEvaluatorTest.class);

  private CloudPolicyEvaluator evaluator;

  @Before
  public void before() {
    evaluator = getCLMServer().getInstance(CloudPolicyEvaluator.class);
  }

  @Override
  protected AbstractPolicyEvaluatorTestRunner withTestRunner(final List<String> params) {
    return new JUnitPolicyEvaluatorTestRunner(params, evaluator, logOutput);
  }

  @Test
  public void testRun_Validate_Waivers_Invalid_Json() {
    tempEntity.newApplicationWithParent(TEST_APPLICATION_ID);

    CloudParameters parameters = defaultValidParameters("--waivers", "bla");

    assertThatExceptionOfType(ExitException.class).isThrownBy(() -> evaluator.run(parameters));

    assertThat(cloudLogOutput).atErrorLevel().contains(
        "The waiver input is invalid JSON for waiving: Unrecognized token 'bla':" +
            " was expecting (JSON String, Number, Array, Object or token 'null'," +
            " 'true' or 'false')");
  }

  @Test
  public void testRun_BackwardCompatibility_NoViolations() throws Exception {
    tempEntity.newApplicationWithParent(TEST_APPLICATION_ID);
    evaluator.run(defaultValidParameters());

    assertLogSummary(newPolicyEvaluationResultForOneComponent());
  }

  @Test
  public void testRun_BackwardCompatibility_SomeViolations() throws Exception {
    Application app = tempEntity.newApplicationWithParent(TEST_APPLICATION_ID);
    createPolicy(app.getId(), "Policy Name", ID_WARN, 10);

    evaluator.run(defaultValidParameters());

    assertThat(logOutput).atInfoLevel().contains("Policy Action: Warning");
    PolicyEvaluationResult expectedPolicyEvaluationResult = newPolicyEvaluationResultForOneComponent();
    expectedPolicyEvaluationResult.setCriticalComponentCount(4);
    expectedPolicyEvaluationResult.setCriticalPolicyViolationCount(4);
    assertLogSummary(expectedPolicyEvaluationResult);
    assertThat(logOutput).atWarnLevel().contains("The IQ Server reports policy warning due to \nPolicy(Policy Name)");
  }

  private CloudParameters defaultValidParameters(final String... args) {
    String[] defaultParameters = {
        "-s", insightServerUrl,
        "-a", "admin:admin123",
        "-i", TEST_APPLICATION_ID,
        "--output-directory", tempDir.getRoot().getAbsolutePath(),
        "src/test/data/artifact.jar"
    };

    return new CloudParameters(concat(args, defaultParameters, String.class));
  }
}
