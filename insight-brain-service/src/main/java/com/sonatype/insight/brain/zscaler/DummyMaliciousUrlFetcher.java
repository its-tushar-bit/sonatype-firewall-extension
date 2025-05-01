/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.zscaler;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.inject.Named;

import static java.nio.charset.StandardCharsets.UTF_8;

@Named("dummy")
public class DummyMaliciousUrlFetcher
    implements ZScalerMaliciousUrlFetcher
{
  @Override
  public InputStream fetchMaliciousUrls(final ZScalerFormat format) {
    Map<String, List<String>> formatUrls = new HashMap<>();
    formatUrls.put("maven2", Arrays.asList(
        "repo.maven.apache.org/maven2/org/sonatype/maven-policy-demo/1.1.0/maven-policy-demo-1.1.0.jar",
        "repo1.maven.org/maven2/org/sonatype/maven-policy-demo/1.1.0/maven-policy-demo-1.1.0.jar",
        "repo.maven.apache.org/maven2/com/example/lib1/test.jar",
        "repo1.maven.org/maven2/com/example/lib2/test.jar"
    ));
    formatUrls.put("npm", Arrays.asList(
        "registry.npmjs.org/example-package",
        "registry.npmjs.org/another-example"
    ));
    formatUrls.put("pypi", Arrays.asList(
        "pypi.org/project/example-package",
        "pypi.org/project/another-example"
    ));

    String urls = String.join("\n", formatUrls.get(format.toString().toLowerCase(Locale.ROOT)));
    return new ByteArrayInputStream(urls.getBytes(UTF_8));
  }
}
