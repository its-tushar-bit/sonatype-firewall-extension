/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.api.purl;

import com.sonatype.insight.brain.guide.api.dto.GuideAffectedComponentVersion;
import com.sonatype.insight.brain.guide.api.dto.GuideComponentDocument;
import com.sonatype.insight.brain.guide.api.error.GuideApiException;
import jakarta.ws.rs.core.Response;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class GuidePurlAssemblerTest
{
  @Test
  public void allCoordsPresent_buildsCanonicalPurl() {
    String purl = GuidePurlAssembler.buildPurl("maven", "org.apache.logging.log4j", "log4j-core", "2.14.0");
    assertThat(purl).isEqualTo("pkg:maven/org.apache.logging.log4j/log4j-core@2.14.0");
  }

  @Test
  public void blankNamespace_buildsPurlWithoutNamespace() {
    String purl = GuidePurlAssembler.buildPurl("npm", "", "lodash", "4.17.21");
    assertThat(purl).isEqualTo("pkg:npm/lodash@4.17.21");
  }

  @Test
  public void nullNamespace_buildsPurlWithoutNamespace() {
    String purl = GuidePurlAssembler.buildPurl("pypi", null, "requests", "2.28.0");
    assertThat(purl).isEqualTo("pkg:pypi/requests@2.28.0");
  }

  @Test
  public void scopedNpm_encodesAtAndSlashInName() {
    // Guard against the regression class fixed in branch commits bd9e000a491 / 0c8d372edf9
    // / 17f98ad2c0c. Naive string concatenation produces "pkg:npm/@types/node@25.9.2", which
    // the PackageURL parser misreads as (namespace=@types, name=node).
    String purl = GuidePurlAssembler.buildPurl("npm", null, "@types/node", "25.9.2");
    assertThat(purl).isEqualTo("pkg:npm/%40types%2Fnode@25.9.2");
  }

  @Test
  public void missingFormat_throws400() {
    assertThatThrownBy(() -> GuidePurlAssembler.buildPurl(null, "ns", "name", "1.0"))
        .isInstanceOf(GuideApiException.class)
        .hasMessageContaining("'format', 'name', 'version'")
        .extracting(e -> ((GuideApiException) e).getResponse().getStatus())
        .isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
  }

  @Test
  public void blankFormat_throws400() {
    assertThatThrownBy(() -> GuidePurlAssembler.buildPurl("  ", null, "name", "1.0"))
        .isInstanceOf(GuideApiException.class)
        .hasMessageContaining("'format', 'name', 'version'");
  }

  @Test
  public void missingName_throws400() {
    assertThatThrownBy(() -> GuidePurlAssembler.buildPurl("maven", "ns", null, "1.0"))
        .isInstanceOf(GuideApiException.class)
        .hasMessageContaining("'format', 'name', 'version'");
  }

  @Test
  public void missingVersion_throws400() {
    assertThatThrownBy(() -> GuidePurlAssembler.buildPurl("maven", "ns", "name", null))
        .isInstanceOf(GuideApiException.class)
        .hasMessageContaining("'format', 'name', 'version'");
  }

  @Test
  public void buildPurlForPolicyEval_maven_appendsTypeJar() {
    // The IQ canonical ComponentIdentifier requires extension (renamed "type" in PURL form)
    // for maven. Mirrors ComponentIdentifierHelper.parseMavenIdNotNull's default of "jar".
    String purl = GuidePurlAssembler.buildPurlForPolicyEval(
        "maven", "org.apache.logging.log4j", "log4j-core", "2.14.0");
    assertThat(purl).isEqualTo("pkg:maven/org.apache.logging.log4j/log4j-core@2.14.0?type=jar");
  }

  @Test
  public void buildPurlForPolicyEval_capitalizedMaven_stillAppendsTypeJar() {
    // ecosystem from the search-server is not guaranteed lowercase, so the maven default check is
    // case-insensitive (matching SaaS). A capitalized format still gets type=jar, and PackageURL
    // canonicalizes the type to lowercase so the key matches on both build and lookup sides.
    String purl = GuidePurlAssembler.buildPurlForPolicyEval(
        "Maven", "org.apache.logging.log4j", "log4j-core", "2.14.0");
    assertThat(purl).isEqualTo("pkg:maven/org.apache.logging.log4j/log4j-core@2.14.0?type=jar");
  }

  @Test
  public void buildPurlForPolicyEval_npm_unchanged() {
    // npm only requires packageId + version; no defaults needed.
    String purl = GuidePurlAssembler.buildPurlForPolicyEval("npm", null, "lodash", "4.17.21");
    assertThat(purl).isEqualTo("pkg:npm/lodash@4.17.21");
  }

  @Test
  public void buildPurlForPolicyEval_pypi_appendsSdistExtension() {
    // PyPI: HDS keys component intelligence (vulnerabilities, malware advisories, license threat
    // groups) to the source distribution, whose ComponentIdentifier "extension" coordinate is
    // "tar.gz". A bare pkg:pypi/name@version yields extension="" which HDS resolves to NO facts, so
    // the component would falsely evaluate as policy-compliant. Default the "extension" qualifier to
    // the sdist so policy eval resolves the same component the scanner does (verified against HDS:
    // have/pyyaml/jinja2 return their advisories only at extension=tar.gz).
    String purl = GuidePurlAssembler.buildPurlForPolicyEval("pypi", null, "requests", "2.28.0");
    assertThat(purl).isEqualTo("pkg:pypi/requests@2.28.0?extension=tar.gz");
  }

  @Test
  public void buildPurlForPolicyEval_capitalizedPypi_stillAppendsSdistExtension() {
    // ecosystem from the search-server is not guaranteed lowercase, so the pypi default check is
    // case-insensitive (mirroring the maven default); PackageURL canonicalizes the format to lowercase.
    String purl = GuidePurlAssembler.buildPurlForPolicyEval("PyPI", null, "requests", "2.28.0");
    assertThat(purl).isEqualTo("pkg:pypi/requests@2.28.0?extension=tar.gz");
  }

  @Test
  public void buildPurlForPolicyEval_missingFormat_throws400() {
    // Same coordinate-completeness contract as buildPurl.
    assertThatThrownBy(() -> GuidePurlAssembler.buildPurlForPolicyEval(null, "ns", "name", "1.0"))
        .isInstanceOf(GuideApiException.class)
        .hasMessageContaining("'format', 'name', 'version'");
  }

  @Test
  public void buildPurlForPolicyEvalOrNull_invalidCoordinates_returnsNull() {
    // The null-tolerant variant returns null rather than throwing, for response-enrichment callers
    // that skip a malformed row instead of failing the whole response. A blank format passes the
    // top-level null guard but fails PURL assembly, exercising the caught-GuideApiException path.
    assertThat(GuidePurlAssembler.buildPurlForPolicyEvalOrNull("  ", null, "name", "1.0")).isNull();
  }

  @Test
  public void purlFor_componentDocument_buildsCanonicalPurl() {
    GuideComponentDocument doc = new GuideComponentDocument(
        "npm", null, null, "@types/node", "25.9.2", null,
        null, null, null, null, null, null, null, null, null);

    String purl = GuidePurlAssembler.purlFor(doc);

    assertThat(purl).isEqualTo("pkg:npm/%40types%2Fnode@25.9.2");
  }

  @Test
  public void purlFor_mavenComponent_addsTypeJarQualifier() {
    // Maven needs the "extension" (PURL "type") qualifier to satisfy
    // ComponentIdentifier.ensureComplete() in the downstream policy-eval chain. Search
    // results don't carry it, so buildPurlForPolicyEval defaults to "jar".
    GuideComponentDocument doc = new GuideComponentDocument(
        "maven", null, "org.apache.logging.log4j", "log4j-core", "2.14.0", null,
        null, null, null, null, null, null, null, null, null);

    String purl = GuidePurlAssembler.purlFor(doc);

    assertThat(purl).isEqualTo("pkg:maven/org.apache.logging.log4j/log4j-core@2.14.0?type=jar");
  }

  @Test
  public void purlFor_pypiComponent_addsSdistExtensionQualifier() {
    // PyPI search hits omit the artifact extension; without the sdist default the policy-eval PURL
    // resolves to NO facts in HDS and the component falsely reads as compliant. See
    // buildPurlForPolicyEval_pypi_appendsSdistExtension.
    GuideComponentDocument doc = new GuideComponentDocument(
        "pypi", null, null, "requests", "2.28.0", null,
        null, null, null, null, null, null, null, null, null);

    String purl = GuidePurlAssembler.purlFor(doc);

    assertThat(purl).isEqualTo("pkg:pypi/requests@2.28.0?extension=tar.gz");
  }

  @Test
  public void purlFor_affectedComponentVersion_usesEcosystemAndPackageName() {
    GuideAffectedComponentVersion v = new GuideAffectedComponentVersion(
        "npm", null, "@types/node", "25.9.2", "@types/node", null);

    String purl = GuidePurlAssembler.purlFor(v);

    assertThat(purl).isEqualTo("pkg:npm/%40types%2Fnode@25.9.2");
  }

  @Test
  public void purlFor_componentDocument_missingFormat_returnsNull() {
    GuideComponentDocument doc = new GuideComponentDocument(
        null, null, null, "log4j-core", "2.14.0", null,
        null, null, null, null, null, null, null, null, null);

    String purl = GuidePurlAssembler.purlFor(doc);

    assertThat(purl).isNull();
  }
}
