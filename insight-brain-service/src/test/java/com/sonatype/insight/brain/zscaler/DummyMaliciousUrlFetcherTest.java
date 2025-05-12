/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.zscaler;

import java.io.InputStream;
import java.util.List;

import com.sonatype.insight.brain.zscaler.ApiZScalerService.ActiveUrls;

import com.fasterxml.jackson.databind.ObjectMapper;
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
    InputStream result = underTest.fetchMaliciousUrls(ZScalerFormat.MAVEN);
    List<String> expected = List.of(
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
    List<String> expected = List.of(
        "registry.npmjs.org/example-package",
        "registry.npmjs.org/another-example"
    );
    assertEquals(expected, readInputStream(result));
  }

  @Test
  public void testFetchMaliciousUrls_pypi() throws Exception {
    InputStream result = underTest.fetchMaliciousUrls(ZScalerFormat.PYPI);
    List<String> expected = List.of(
        "pypi.org/project/example-package",
        "pypi.org/project/another-example"
    );
    assertEquals(expected, readInputStream(result));
  }

  private List<String> readInputStream(InputStream inputStream) throws Exception {
    return new ObjectMapper().readValue(inputStream, ActiveUrls.class).getActiveThreatUrls();
  }
}
