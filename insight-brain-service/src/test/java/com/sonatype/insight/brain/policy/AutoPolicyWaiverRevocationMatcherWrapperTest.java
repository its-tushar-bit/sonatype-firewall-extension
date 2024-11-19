/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy;

import java.util.TreeMap;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.policy.ComponentFact;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiver;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiverRevocation;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiverRevocation.ComponentMatcherStrategyForRevocation;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import static com.sonatype.clm.dto.model.component.ComponentIdentifier.FORMAT_MAVEN;
import static com.sonatype.clm.dto.model.component.ComponentIdentifier.FORMAT_PYPI;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class AutoPolicyWaiverRevocationMatcherWrapperTest
    extends AbstractComponentTest
{
  @Test
  public void testMatcherWrapper_MatchesComponent_null_EXACT_COMPONENT() {
    Application app = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());
    AutoPolicyWaiverRevocation revocation = tempEntity.newAutoPolicyWaiverRevocation(app.getId(), waiver.getId());
    revocation.setComponentMatchStrategy(ComponentMatcherStrategyForRevocation.EXACT_COMPONENT);
    AutoPolicyWaiverRevocationMatcherWrapper wrapper = new AutoPolicyWaiverRevocationMatcherWrapper(revocation);

    assertThatThrownBy(() -> wrapper.matchesComponent(null))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("componentFact is required but got null instead");
  }

  @Test
  public void testMatcherWrapper_MatchesComponent_null_ALL_VERSIONS() {
    Application app = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());
    AutoPolicyWaiverRevocation revocation = tempEntity.newAutoPolicyWaiverRevocation(app.getId(), waiver.getId());
    revocation.setComponentMatchStrategy(ComponentMatcherStrategyForRevocation.ALL_VERSIONS);
    AutoPolicyWaiverRevocationMatcherWrapper wrapper = new AutoPolicyWaiverRevocationMatcherWrapper(revocation);

    assertThatThrownBy(() -> wrapper.matchesComponent(null))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("componentFact is required but got null instead");
  }

  @Test
  public void testMatcherWrapper_MatchesComponent_EXACT_COMPONENT() {
    Application app = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());
    AutoPolicyWaiverRevocation revocation = tempEntity.newAutoPolicyWaiverRevocation(app.getId(), waiver.getId());
    revocation.setAssociatedPackageUrl("pkg:maven/group/artifact@2.0?classifier=c1&type=jar");
    revocation.setComponentMatchStrategy(ComponentMatcherStrategyForRevocation.EXACT_COMPONENT);
    AutoPolicyWaiverRevocationMatcherWrapper wrapper = new AutoPolicyWaiverRevocationMatcherWrapper(revocation);

    ComponentFact componentFact = new ComponentFact(revocation.getComponentIdentifier(), revocation.getHash());

    assertThat(wrapper.matchesComponent(componentFact)).isTrue();
  }

  @Test
  public void testMatcherWrapper_MatchesComponent_EXACT_COMPONENT_nullHash() {
    Application app = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());
    AutoPolicyWaiverRevocation revocation = tempEntity.newAutoPolicyWaiverRevocation(app.getId(), waiver.getId());
    revocation.setAssociatedPackageUrl("pkg:maven/group/artifact@2.0?classifier=c1&type=jar");
    revocation.setComponentMatchStrategy(ComponentMatcherStrategyForRevocation.EXACT_COMPONENT);
    AutoPolicyWaiverRevocationMatcherWrapper wrapper = new AutoPolicyWaiverRevocationMatcherWrapper(revocation);

    ComponentFact componentFact = new ComponentFact(revocation.getComponentIdentifier(), null);

    assertThat(wrapper.matchesComponent(componentFact)).isTrue();
  }

  @Test
  public void testMatcherWrapper_MatchesComponent_EXACT_COMPONENT_nullHash_missingRequiredCoordinates() {
    Application app = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());
    AutoPolicyWaiverRevocation revocation = tempEntity.newAutoPolicyWaiverRevocation(app.getId(), waiver.getId());
    revocation.setAssociatedPackageUrl("pkg:pypi/name?extension=e&qualifier=q");
    revocation.setComponentMatchStrategy(ComponentMatcherStrategyForRevocation.EXACT_COMPONENT);
    AutoPolicyWaiverRevocationMatcherWrapper wrapper = new AutoPolicyWaiverRevocationMatcherWrapper(revocation);

    ComponentIdentifier componentIdentifier = new ComponentIdentifier(FORMAT_PYPI, new TreeMap<String, String>()
    {{
        this.put("name", "name");
        this.put("extension", "e");
        this.put("qualifier", "q");
      }});

    ComponentFact componentFact = new ComponentFact(componentIdentifier, null);
    assertThat(wrapper.matchesComponent(componentFact)).isTrue();
  }

  @Test
  public void testMatcherWrapper_MatchesComponent_ALL_VERSIONS() {
    Application app = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());
    AutoPolicyWaiverRevocation revocation = tempEntity.newAutoPolicyWaiverRevocation(app.getId(), waiver.getId());
    revocation.setAssociatedPackageUrl("pkg:maven/group/artifact@2.0?type=jar");
    revocation.setComponentMatchStrategy(ComponentMatcherStrategyForRevocation.ALL_VERSIONS);
    AutoPolicyWaiverRevocationMatcherWrapper wrapper = new AutoPolicyWaiverRevocationMatcherWrapper(revocation);

    ComponentIdentifier componentIdentifier =
        ComponentIdentifier.createMavenCoordinates("group", "artifact", "otherVersion", "", "jar");
    ComponentFact componentFact = new ComponentFact(componentIdentifier, "otherHash");

    assertThat(wrapper.matchesComponent(componentFact)).isTrue();
  }

  @Test
  public void testMatcherWrapper_MatchesComponent_ALL_VERSIONS_UnknownComponent() {
    Application app = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());
    AutoPolicyWaiverRevocation revocation = tempEntity.newAutoPolicyWaiverRevocation(app.getId(), waiver.getId());
    revocation.setAssociatedPackageUrl("pkg:maven/group/artifact@2.0?type=jar");
    revocation.setComponentMatchStrategy(ComponentMatcherStrategyForRevocation.ALL_VERSIONS);
    AutoPolicyWaiverRevocationMatcherWrapper wrapper = new AutoPolicyWaiverRevocationMatcherWrapper(revocation);

    ComponentFact componentFact = new ComponentFact(null, "otherHash");
    assertThat(wrapper.matchesComponent(componentFact)).isFalse();
  }

  @Test
  public void testMatcherWrapper_MatchesComponent_ALL_VERSIONS_missingRequiredCoordinates() {
    Application app = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());
    AutoPolicyWaiverRevocation revocation = tempEntity.newAutoPolicyWaiverRevocation(app.getId(), waiver.getId());
    revocation.setAssociatedPackageUrl("pkg:maven/group/artifact@2.0?type=jar");
    revocation.setComponentMatchStrategy(ComponentMatcherStrategyForRevocation.ALL_VERSIONS);
    AutoPolicyWaiverRevocationMatcherWrapper wrapper = new AutoPolicyWaiverRevocationMatcherWrapper(revocation);

    ComponentIdentifier componentIdentifier = new ComponentIdentifier(FORMAT_MAVEN, new TreeMap<String, String>()
    {{
        this.put("artifactId", "artifact");
        this.put("groupId", "group");
        this.put("version", "1.0");
        this.put("classifier", "");
      }});
    ComponentFact componentFact = new ComponentFact(componentIdentifier, "otherHash");
    assertThatNoException().isThrownBy(() -> wrapper.matchesComponent(componentFact));
  }

  @Test
  public void testMatcherWrapper_MatchesComponent_ALL_VERSIONS_caseMissMatch() {
    Application app = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());
    AutoPolicyWaiverRevocation revocation = tempEntity.newAutoPolicyWaiverRevocation(app.getId(), waiver.getId());
    revocation.setAssociatedPackageUrl("pkg:pypi/py-component@1.0?extension=whl&qualifier=py3-none-any");
    revocation.setComponentMatchStrategy(ComponentMatcherStrategyForRevocation.ALL_VERSIONS);
    AutoPolicyWaiverRevocationMatcherWrapper wrapper = new AutoPolicyWaiverRevocationMatcherWrapper(revocation);

    ComponentIdentifier componentIdentifier =
        ComponentIdentifier.createPypiCoordinates("Py-component", "otherVersion", "py3-none-any", "whl");
    ComponentFact componentFact = new ComponentFact(componentIdentifier, "otherHash");

    assertThat(wrapper.matchesComponent(componentFact)).isTrue();
  }

  @Test
  public void testMatcherWrapper_MatchesComponent_ALL_VERSIONS_PythonPackageWithDot() {
    Application app = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());
    AutoPolicyWaiverRevocation revocation = tempEntity.newAutoPolicyWaiverRevocation(app.getId(), waiver.getId());
    revocation.setAssociatedPackageUrl("pkg:pypi/ruamel.yaml@0.17.35?extension=whl&qualifier=py3-none-any");
    revocation.setComponentMatchStrategy(ComponentMatcherStrategyForRevocation.ALL_VERSIONS);
    AutoPolicyWaiverRevocationMatcherWrapper wrapper = new AutoPolicyWaiverRevocationMatcherWrapper(revocation);

    ComponentIdentifier componentIdentifier =
        ComponentIdentifier.createPypiCoordinates("ruamel.yaml", "otherVersion", "py3-none-any", "whl");
    ComponentFact componentFact = new ComponentFact(componentIdentifier, "otherHash");

    assertThat(wrapper.matchesComponent(componentFact)).isTrue();
  }

  @Test
  public void testMatcherWrapper_CompareWhenMissingRequiredCoordinates() {
    ComponentIdentifier componentIdentifierSame =
        new ComponentIdentifier(FORMAT_MAVEN, new TreeMap<String, String>()
        {{
            this.put("artifactId", "artifact");
            this.put("groupId", "group");
            this.put("version", "*");
          }});

    ComponentIdentifier componentIdentifierOther =
        new ComponentIdentifier(FORMAT_MAVEN, new TreeMap<String, String>()
        {{
            this.put("artifactId", "otherArtifact");
            this.put("groupId", "group");
            this.put("version", "*");
          }});

    String associatedPackagedUrlAllVersions = "pkg:maven/group/artifact@*?type=jar&classifier=";

    Application app = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());
    AutoPolicyWaiverRevocation revocation = tempEntity.newAutoPolicyWaiverRevocation(app.getId(), waiver.getId());
    revocation.setAssociatedPackageUrl(associatedPackagedUrlAllVersions);
    revocation.setComponentMatchStrategy(ComponentMatcherStrategyForRevocation.ALL_VERSIONS);
    AutoPolicyWaiverRevocationMatcherWrapper wrapper = new AutoPolicyWaiverRevocationMatcherWrapper(revocation);

    Assertions.assertThat(wrapper
        .compareWhenMissingRequiredCoordinates(revocation
            .getComponentIdentifier(), componentIdentifierSame)).isTrue();
    Assertions.assertThat(wrapper
        .compareWhenMissingRequiredCoordinates(revocation
            .getComponentIdentifier(), componentIdentifierOther)).isFalse();
  }

  @Test
  public void testMatcherWrapper_matchesComponent_EXACT_COMPONENT() {
    Application app = tempEntity.newApplicationWithParent();
    AutoPolicyWaiver waiver = tempEntity.newAutoPolicyWaiver(app.getId());
    AutoPolicyWaiverRevocation revocation = tempEntity.newAutoPolicyWaiverRevocation(app.getId(), waiver.getId());
    revocation.setAssociatedPackageUrl("pkg:maven/group/artifact@*?type=jar&classifier=");
    revocation.setComponentMatchStrategy(ComponentMatcherStrategyForRevocation.EXACT_COMPONENT);
    AutoPolicyWaiverRevocationMatcherWrapper wrapper = new AutoPolicyWaiverRevocationMatcherWrapper(revocation);

    ComponentFact componentFact = new ComponentFact(null, "fakeHash");
    assertThat(wrapper.matchesComponent(componentFact)).isTrue();
  }
}
