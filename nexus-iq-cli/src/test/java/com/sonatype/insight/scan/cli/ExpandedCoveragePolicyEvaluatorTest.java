/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.scan.cli;

import java.io.File;
import java.util.List;
import java.util.Objects;

import javax.inject.Inject;

import com.sonatype.clm.dto.model.ProprietaryConfig;
import com.sonatype.insight.scan.model.Scan;
import com.sonatype.insight.scan.model.io.ScanReader;
import com.sonatype.insight.test.InjectedTest;
import com.sonatype.insight.test.LogOutput;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.owasp.dependencycheck.dependency.Dependency;
import org.owasp.dependencycheck.utils.Settings.KEYS;

import static com.sonatype.insight.scan.cli.ExpandedCoveragePolicyEvaluator.EXPANDED_COVERAGE_SCAN_DISCLAIMER;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

public class ExpandedCoveragePolicyEvaluatorTest
    extends InjectedTest
{
  @Rule
  public TemporaryFolder tempDir = new TemporaryFolder();

  @Rule
  public LogOutput logOutput = new LogOutput();

  @Inject
  private ExpandedCoveragePolicyEvaluator evaluator;

  @Inject
  private ScanReader scanReader;

  @Test
  public void testScan_Java() throws Exception {
    List<Dependency> dependencies = testScan("java");

    logOutput.assertInfo("Found 3 items.");
    assertThat(dependencies, hasSize(3));
    assertThat(dependencies, hasItems(dependencyWithName("uber-1.0-SNAPSHOT.jar"),
        dependencyWithName("uber-1.0-SNAPSHOT.jar/META-INF/maven/com.example/uber/pom.xml"),
        dependencyWithName("uber-1.0-SNAPSHOT.jar/META-INF/maven/org.apache.commons/commons-lang3/pom.xml")));
  }

  @Test
  public void testScan_CMake() throws Exception {
    List<Dependency> dependencies = testScan("cmake");

    logOutput.assertInfo("Found 1 items.");
    assertThat(dependencies, hasSize(1));
    assertThat(dependencies, hasItem(dependencyWithName("cmake/CMakeLists.txt")));
  }

  @Test
  public void testScan_Directory() throws Exception {
    List<Dependency> dependencies = testScan("");

    logOutput.assertInfo("Found 4 items.");
    assertThat(dependencies, hasSize(4));
    assertThat(dependencies, hasItem(dependencyWithName("cmake/CMakeLists.txt")));
    assertThat(dependencies, hasItem(dependencyWithName("uber-1.0-SNAPSHOT.jar")));
    assertThat(dependencies,
        hasItem(dependencyWithName("uber-1.0-SNAPSHOT.jar/META-INF/maven/com.example/uber/pom.xml")));
    assertThat(dependencies,
        hasItem(dependencyWithName("uber-1.0-SNAPSHOT.jar/META-INF/maven/org.apache.commons/commons-lang3/pom.xml")));
  }

  private Matcher<Dependency> dependencyWithName(final String name) {
    return new TypeSafeDiagnosingMatcher<Dependency>()
    {
      @Override
      protected boolean matchesSafely(final Dependency item, final Description mismatchDescription) {
        if (!Objects.equals(item.getDisplayFileName().replace('\\', '/'), name)) {
          mismatchDescription.appendText("has displayFilename ").appendValue(item.getDisplayFileName());
          return false;
        }
        return true;
      }

      @Override
      public void describeTo(final Description description) {
        description.appendText("Dependency with displayFilename ").appendValue(name);
      }
    };
  }

  private List<Dependency> testScan(String scanTarget) throws Exception {
    Parameters params = new Parameters("-o", tempDir.newFolder().getAbsolutePath(),
        getClass().getResource("/" + getClass().getSimpleName() + "/" + scanTarget).getFile());

    File scanFile = evaluator.scan(params, new ProprietaryConfig());

    // Assert that the disclaimer banner is logged
    logOutput.assertInfo(EXPANDED_COVERAGE_SCAN_DISCLAIMER);

    // Assert applied client settings are logged in debug move
    logOutput.assertDebug("Setting: " + KEYS.ANALYZER_EXPERIMENTAL_ENABLED + "='true'");

    Scan scan = scanReader.read(scanFile);

    assertThat(scan.getExpandedCoverage(), is(notNullValue()));
    assertThat(scan.getExpandedCoverage().getVersion().matches("[0-9]+\\.[0-9]+.*"), is(true));

    String dependenciesJson = scan.getExpandedCoverage().getDependenciesJson();
    assertThat(dependenciesJson, is(notNullValue()));
    // Assert that Jackson is configured correctly to leave out derived properties
    assertThat(dependenciesJson, not(containsString("\"actualFile\"")));
    assertThat(dependenciesJson, not(containsString("\"fileNameForJavaScript\"")));
    assertThat(dependenciesJson, not(containsString("\"displayFileName\"")));
    assertThat(dependenciesJson, not(containsString("\"evidence\"")));
    assertThat(dependenciesJson, not(containsString("\"evidenceForDisplay\"")));
    assertThat(dependenciesJson, not(containsString("\"evidenceUsed\"")));

    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.setVisibilityChecker(objectMapper.getDeserializationConfig().getDefaultVisibilityChecker()
        .withFieldVisibility(JsonAutoDetect.Visibility.ANY).withGetterVisibility(JsonAutoDetect.Visibility.NONE));
    return objectMapper.readValue(dependenciesJson, new TypeReference<List<Dependency>>() { });
  }
}
