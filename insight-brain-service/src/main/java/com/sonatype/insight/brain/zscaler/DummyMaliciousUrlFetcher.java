/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.zscaler;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import javax.inject.Named;

import static java.nio.charset.StandardCharsets.UTF_8;

@Named("dummy")
public class DummyMaliciousUrlFetcher
    implements ZScalerMaliciousUrlFetcher
{
  private static final String mavenUrls = """
      {
        "activeThreatUrls": [
          "repo.maven.apache.org/maven2/org/sonatype/maven-policy-demo/1.1.0/maven-policy-demo-1.1.0.jar",
          "repo1.maven.org/maven2/org/sonatype/maven-policy-demo/1.1.0/maven-policy-demo-1.1.0.jar",
          "repo.maven.apache.org/maven2/com/example/lib1/test.jar",
          "repo1.maven.org/maven2/com/example/lib2/test.jar"
        ]
      }
      """;

  private static final String npmUrls = """
      {
        "activeThreatUrls": [
          "registry.npmjs.org/example-package",
          "registry.npmjs.org/another-example"
        ]
      }
      """;

  private static final String pypiUrls = """
      {
        "activeThreatUrls": [
          "pypi.org/project/example-package",
          "pypi.org/project/another-example"
        ]
      }
      """;

  @Override
  public InputStream fetchMaliciousUrls(final ZScalerFormat format) {
    return switch (format) {
      case MAVEN -> new ByteArrayInputStream(mavenUrls.getBytes(UTF_8));
      case NPM -> new ByteArrayInputStream(npmUrls.getBytes(UTF_8));
      case PYPI -> new ByteArrayInputStream(pypiUrls.getBytes(UTF_8));
    };
  }
}
