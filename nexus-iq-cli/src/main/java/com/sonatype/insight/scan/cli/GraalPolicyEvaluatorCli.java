/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.scan.cli;

import com.sonatype.insight.brain.client.RestClientFactory;
import com.sonatype.insight.scan.anon.Anonymizer;
import com.sonatype.insight.scan.client.ClientScanner;
import com.sonatype.insight.scan.config.ScanPropertiesLoader;
import com.sonatype.insight.scan.file.FileScanner;
import com.sonatype.insight.scan.hash.Digester;
import com.sonatype.insight.scan.hash.internal.DefaultDigester;
import com.sonatype.insight.scan.hash.internal.JavaDigester;
import com.sonatype.insight.scan.model.io.ScanWriterFactory;

/**
 * Graal has issues with Guice & Sisu. Until we figure that out, this is a custom instance of the Cli which manually
 * defines all the dependencies.
 */
public class GraalPolicyEvaluatorCli
    extends PolicyEvaluatorCli
{
  public static void main(String[] args) {
    Parameters params = new Parameters(args);

    Class<? extends PolicyEvaluator> policyEvaluatorClass = params
        .isExpandedCoverageMode() ? ExpandedCoveragePolicyEvaluator.class : DefaultPolicyEvaluator.class;

    new GraalPolicyEvaluatorCli().run(policyEvaluatorClass, params);
  }

  @Override
  protected <T extends PolicyEvaluator> T instantiate(Class<T> type, AbstractParameters params) {
    JavaDigester javaDigester = new JavaDigester();

    Digester digester = new DefaultDigester(javaDigester);
    Anonymizer anonymizer = new Anonymizer();

    ScanPropertiesLoader scanPropertiesLoader = new ScanPropertiesLoader();
    ClientScanner clientScanner = new ClientScanner();
    FileScanner fileScanner = new FileScanner(digester, anonymizer);
    ScanWriterFactory scanWriterFactory = new ScanWriterFactory();

    Scanner scanner = new Scanner(scanPropertiesLoader, clientScanner, fileScanner, scanWriterFactory);
    RestClientFactory restClientFactory = new RestClientFactory();

    return type.isAssignableFrom(ExpandedCoveragePolicyEvaluator.class)
        ? type.cast(new ExpandedCoveragePolicyEvaluator(scanner, restClientFactory, clientScanner, scanWriterFactory))
        : type.cast(new DefaultPolicyEvaluator(scanner, restClientFactory));
  }
}
