/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.zscaler;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class DummyMaliciousUrlFetcherTest
{
  private DummyMaliciousUrlFetcher underTest;

  @Before
  public void setUp() {
    underTest = new DummyMaliciousUrlFetcher();
  }

  @Test
  public void testFetchMaliciousUrls_maven2() throws Exception {
    InputStream result = underTest.fetchMaliciousUrls(ZScalerFormat.MAVEN2);
    String expected = String.join("\n",
        "repo.maven.apache.org/maven2/org/sonatype/maven-policy-demo/1.1.0/maven-policy-demo-1.1.0.jar",
        "repo1.maven.org/maven2/org/sonatype/maven-policy-demo/1.1.0/maven-policy-demo-1.1.0.jar",
        "repo.maven.apache.org/maven2/com/example/lib1/test.jar",
        "repo1.maven.org/maven2/com/example/lib2/test.jar"
    );
    assertEquals(expected, readInputStream(result));
  }

  @Test
  public void testFetchMaliciousUrls_npm() throws Exception {
    InputStream result = underTest.fetchMaliciousUrls(ZScalerFormat.NPM);
    String expected = String.join("\n",
        "registry.npmjs.org/example-package",
        "registry.npmjs.org/another-example"
    );
    assertEquals(expected, readInputStream(result));
  }

  @Test
  public void testFetchMaliciousUrls_pypi() throws Exception {
    InputStream result = underTest.fetchMaliciousUrls(ZScalerFormat.PYPI);
    String expected = String.join("\n",
        "pypi.org/project/example-package",
        "pypi.org/project/another-example"
    );
    assertEquals(expected, readInputStream(result));
  }

  private String readInputStream(InputStream inputStream) throws Exception {
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
      StringBuilder result = new StringBuilder();
      String line;
      while ((line = reader.readLine()) != null) {
        result.append(line).append("\n");
      }
      return result.toString().trim();
    }
  }
}
