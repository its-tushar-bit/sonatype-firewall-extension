/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.scan.cli;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.clm.dto.model.policy.PolicyEvaluationResult;
import com.sonatype.insight.brain.client.PolicyAction;
import com.sonatype.insight.brain.client.RestClientFactory.RestClient;
import com.sonatype.insight.scan.model.ClientScanType;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AbstractPolicyEvaluatorTest
{
  private final AbstractPolicyEvaluator abstractPolicyEvaluator = new AbstractPolicyEvaluator(null, null)
  {
    @Override
    protected ClientScanType getClientScanType() {
      return null;
    }

    @Override
    protected void processResults(
        final AbstractParameters params,
        final ScanReceipt receipt,
        final PolicyEvaluationResult eval,
        final PolicyAction outcome,
        final RestClient restClient)
    {
      // noop
    }
  };

  @Test
  public void testGetModuleIndices() {
    File baseDirectory = new File(
        Objects.requireNonNull(getClass().getClassLoader().getResource("AbstractPolicyEvaluatorTest")).getFile());

    List<File> moduleIndices = abstractPolicyEvaluator.getModuleIndices(baseDirectory, null);

    assertThat(moduleIndices)
        .extracting(File::getPath)
        .extracting(path -> path.substring(path.indexOf("AbstractPolicyEvaluatorTest")).replaceAll("\\\\", "/"))
        .containsExactly(
            "AbstractPolicyEvaluatorTest/artifact/nested/nexus-iq/module.xml",
            "AbstractPolicyEvaluatorTest/artifact/nested/sonatype-clm/module.xml",
            "AbstractPolicyEvaluatorTest/artifact/nexus-iq/module.xml",
            "AbstractPolicyEvaluatorTest/artifact/sonatype-clm/module.xml");
  }

  @Test
  public void testGetModuleIndices_WithModuleAsTarget() {
    File module = new File(
        Objects.requireNonNull(getClass().getClassLoader()
            .getResource("AbstractPolicyEvaluatorTest/artifact/sonatype-clm/module.xml")).getFile());

    List<File> moduleIndices = abstractPolicyEvaluator.getModuleIndices(null, Collections.singletonList(module),null);

    assertThat(moduleIndices)
        .extracting(File::getPath)
        .extracting(path -> path.substring(path.indexOf("AbstractPolicyEvaluatorTest")).replaceAll("\\\\", "/"))
        .containsExactly("AbstractPolicyEvaluatorTest/artifact/sonatype-clm/module.xml");
  }

  @Test
  public void testGetModuleIndices_WithExcludes() {
    File baseDirectory = new File(
        Objects.requireNonNull(getClass().getClassLoader().getResource("AbstractPolicyEvaluatorTest")).getFile());

    List<File> moduleIndices = abstractPolicyEvaluator.getModuleIndices(baseDirectory,
        Arrays.asList("**/nested/**", "**/sonatype-clm/module.xml"));

    assertThat(moduleIndices)
        .extracting(File::getPath)
        .extracting(path -> path.substring(path.indexOf("AbstractPolicyEvaluatorTest")).replaceAll("\\\\", "/"))
        .containsExactly("AbstractPolicyEvaluatorTest/artifact/nexus-iq/module.xml");
  }

  @Test
  public void testGetModuleIndices_RelativePath() {
    File baseDirectory = new File(
        Objects.requireNonNull(getClass().getClassLoader().getResource("AbstractPolicyEvaluatorTest")).getFile());
    List<File> targets = Collections.singletonList(new File("./artifact/nested"));

    List<File> moduleIndices = abstractPolicyEvaluator.getModuleIndices(baseDirectory, targets, null);

    assertThat(moduleIndices)
        .extracting(File::getPath)
        .extracting(path -> path.substring(path.indexOf("AbstractPolicyEvaluatorTest")).replaceAll("\\\\", "/"))
        .containsExactly(
            "AbstractPolicyEvaluatorTest/./artifact/nested/nexus-iq/module.xml",
            "AbstractPolicyEvaluatorTest/./artifact/nested/sonatype-clm/module.xml");
  }

  @Test
  public void testGetModuleIndices_IgnoresFiles() {
    File target = new File(
        Objects.requireNonNull(
                getClass().getClassLoader().getResource("AbstractParametersTest/invalid-metadata.json"))
            .getFile());

    List<File> moduleIndices = abstractPolicyEvaluator.getModuleIndices(null, Collections.singletonList(target), null);

    assertThat(moduleIndices).isEmpty();
  }
}
