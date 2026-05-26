/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.export;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.sonatype.insight.SbomIdentityUtils;

import org.apache.commons.codec.digest.DigestUtils;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.Component.Scope;
import org.cyclonedx.model.Component.Type;
import org.cyclonedx.model.Hash;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.junit.runners.Suite;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Suite.class)
@Suite.SuiteClasses({
  AbstractCycloneDxExporterComponentRefTest.BasicTests.class,
  AbstractCycloneDxExporterComponentRefTest.GoldenValueTests.class
})
public class AbstractCycloneDxExporterComponentRefTest
{
  public static class BasicTests
  {
    @Test
    public void getComponentRef_withBomRef_returnsSha1OfBomRef() {
      Component component = new Component();
      component.setBomRef("pkg:maven/org.example/lib@1.0.0");
      component.setName("lib");
      component.setVersion("1.0.0");

      String result = SbomIdentityUtils.getComponentRef(component);

      assertThat(result).isEqualTo(DigestUtils.sha1Hex("pkg:maven/org.example/lib@1.0.0"));
    }

    @Test
    public void getComponentRef_withoutBomRef_computesIdentityHash() {
      Component component = new Component();
      component.setType(Type.LIBRARY);
      component.setGroup("org.example");
      component.setName("my-lib");
      component.setVersion("2.1.0");
      component.setPurl("pkg:maven/org.example/my-lib@2.1.0");

      String result = SbomIdentityUtils.getComponentRef(component);

      String expectedIdentity = "library:org.example:my-lib:2.1.0:pkg:maven/org.example/my-lib@2.1.0";
      assertThat(result).isEqualTo(DigestUtils.sha1Hex(expectedIdentity));
    }

    @Test
    public void getComponentRef_withAllFields_includesAllInHash() {
      Component component = new Component();
      component.setType(Type.LIBRARY);
      component.setMimeType("application/java-archive");
      component.setGroup("com.sonatype");
      component.setName("example");
      component.setVersion("3.0.0");
      component.setScope(Scope.REQUIRED);
      component.setHashes(List.of(new Hash(Hash.Algorithm.SHA_256, "abc123def456")));
      component.setCpe("cpe:2.3:a:sonatype:example:3.0.0");
      component.setPurl("pkg:maven/com.sonatype/example@3.0.0");

      String result = SbomIdentityUtils.getComponentRef(component);

      String expectedIdentity = String.join(":",
          "library",
          "application/java-archive",
          "com.sonatype",
          "example",
          "3.0.0",
          "REQUIRED",
          "abc123def456",
          "cpe:2.3:a:sonatype:example:3.0.0",
          "pkg:maven/com.sonatype/example@3.0.0");
      assertThat(result).isEqualTo(DigestUtils.sha1Hex(expectedIdentity));
    }

    @Test
    public void getComponentRef_nullComponent_returnsNull() {
      assertThat(SbomIdentityUtils.getComponentRef((Component) null)).isNull();
    }

    @Test
    public void getComponentRef_emptyComponent_returnsNull() {
      Component component = new Component();

      assertThat(SbomIdentityUtils.getComponentRef(component)).isNull();
    }

    @Test
    public void getComponentRef_skipsBlankFields() {
      Component component = new Component();
      component.setName("my-lib");
      component.setVersion("1.0");

      String result = SbomIdentityUtils.getComponentRef(component);

      String expectedIdentity = "my-lib:1.0";
      assertThat(result).isEqualTo(DigestUtils.sha1Hex(expectedIdentity));
    }
  }

  /**
   * Parameterized golden-value tests that validate getComponentRef produces stable hashes
   * matching the formula previously implemented in SbomIdentityUtils.getComponentRef().
   * These values were computed by the original SbomIdentityUtils implementation and serve as
   * regression guards: if any of these fail, existing customer SBOM records would silently
   * fail to match after upgrade.
   */
  @RunWith(Parameterized.class)
  public static class GoldenValueTests
  {
    private final String description;

    private final Component component;

    private final String expectedHash;

    public GoldenValueTests(String description, Component component, String expectedHash) {
      this.description = description;
      this.component = component;
      this.expectedHash = expectedHash;
    }

    @Parameters(name = "{0}")
    public static Collection<Object[]> data() {
      return Arrays.asList(new Object[][]{
        {"Maven component with bomRef (purl as bomRef)",
          buildComponent("pkg:maven/org.apache.commons/commons-lang3@3.12.0", Type.LIBRARY, null,
              "org.apache.commons", "commons-lang3", "3.12.0", null, null, null,
              "pkg:maven/org.apache.commons/commons-lang3@3.12.0"),
          DigestUtils.sha1Hex("pkg:maven/org.apache.commons/commons-lang3@3.12.0")},

        {"NPM component with bomRef (UUID-style)",
          buildComponent("a1b2c3d4-e5f6-7890-abcd-ef1234567890", Type.LIBRARY, null,
              null, "lodash", "4.17.21", null, null, null, "pkg:npm/lodash@4.17.21"),
          DigestUtils.sha1Hex("a1b2c3d4-e5f6-7890-abcd-ef1234567890")},

        {"Component without bomRef - name and version only",
          buildComponent(null, null, null, null, "my-internal-lib", "1.0.0-SNAPSHOT", null, null, null, null),
          DigestUtils.sha1Hex("my-internal-lib:1.0.0-SNAPSHOT")},

        {"Component without bomRef - full identity",
          buildComponent(null, Type.LIBRARY, "application/java-archive", "com.sonatype", "nexus-common",
              "3.45.0", Scope.REQUIRED, "cpe:2.3:a:sonatype:nexus-common:3.45.0:*:*:*:*:*:*:*",
              null, "pkg:maven/com.sonatype/nexus-common@3.45.0"),
          DigestUtils.sha1Hex("library:application/java-archive:com.sonatype:nexus-common:3.45.0:REQUIRED" +
              ":cpe:2.3:a:sonatype:nexus-common:3.45.0:*:*:*:*:*:*:*:pkg:maven/com.sonatype/nexus-common@3.45.0")},

        {"Component without bomRef - purl only",
          buildComponent(null, null, null, null, null, null, null, null, null,
              "pkg:pypi/requests@2.28.0"),
          DigestUtils.sha1Hex("pkg:pypi/requests@2.28.0")},

        {"Component without bomRef - with hash values",
          buildComponentWithHashes(null, Type.LIBRARY, null, null, "spring-core", "6.0.0", null, null, null,
              new Hash(Hash.Algorithm.SHA_256, "e3b0c44298fc1c149afbf4c8996fb924")),
          DigestUtils.sha1Hex("library:spring-core:6.0.0:e3b0c44298fc1c149afbf4c8996fb924")},

        {"Component with empty bomRef hashes empty string",
          buildComponent("", Type.LIBRARY, null, null, "guava", "31.1-jre", null, null, null,
              "pkg:maven/com.google.guava/guava@31.1-jre"),
          DigestUtils.sha1Hex("")},

        {"NuGet component with bomRef",
          buildComponent("pkg:nuget/Newtonsoft.Json@13.0.1", Type.LIBRARY, null,
              null, "Newtonsoft.Json", "13.0.1", null, null, null,
              "pkg:nuget/Newtonsoft.Json@13.0.1"),
          DigestUtils.sha1Hex("pkg:nuget/Newtonsoft.Json@13.0.1")},
      });
    }

    @Test
    public void getComponentRef_matchesGoldenValue() {
      String result = SbomIdentityUtils.getComponentRef(component);
      assertThat(result)
          .as("componentRef for: %s", description)
          .isEqualTo(expectedHash);
    }

    private static Component buildComponent(
        String bomRef,
        Type type,
        String mimeType,
        String group,
        String name,
        String version,
        Scope scope,
        String cpe,
        String unusedHash,
        String purl)
    {
      Component c = new Component();
      c.setBomRef(bomRef);
      c.setType(type);
      c.setMimeType(mimeType);
      c.setGroup(group);
      c.setName(name);
      c.setVersion(version);
      c.setScope(scope);
      c.setCpe(cpe);
      c.setPurl(purl);
      return c;
    }

    private static Component buildComponentWithHashes(
        String bomRef,
        Type type,
        String mimeType,
        String group,
        String name,
        String version,
        Scope scope,
        String cpe,
        String purl,
        Hash... hashes)
    {
      Component c = buildComponent(bomRef, type, mimeType, group, name, version, scope, cpe, null, purl);
      c.setHashes(Arrays.asList(hashes));
      return c;
    }
  }
}
