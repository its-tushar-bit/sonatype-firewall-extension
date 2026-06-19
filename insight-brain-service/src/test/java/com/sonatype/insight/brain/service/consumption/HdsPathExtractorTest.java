/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.consumption;

import java.util.Collections;
import java.util.List;

import com.sonatype.insight.brain.model.consumption.ActivityType;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Enclosed.class)
public class HdsPathExtractorTest
{
  public static class ActivityTypeResolutionTests
  {
    @Test
    public void resolve_integrationPath_returnsComponentDetails() {
      HdsPathExtractor.PathMatch m =
          HdsPathExtractor.resolve("/api/v1/rest/component/details/integration/abc123def456");
      assertThat(m).isNotNull();
      assertThat(m.activityType()).isEqualTo(ActivityType.COMPONENT_DETAILS);
    }

    @Test
    public void resolve_evaluationPath_returnsComponentDetails() {
      HdsPathExtractor.PathMatch m = HdsPathExtractor.resolve("/api/v1/rest/component/details/evaluation/abc123def456");
      assertThat(m).isNotNull();
      assertThat(m.activityType()).isEqualTo(ActivityType.COMPONENT_DETAILS);
    }

    @Test
    public void resolve_componentDetailsLegacyPath_returnsComponentDetails() {
      HdsPathExtractor.PathMatch m = HdsPathExtractor.resolve("/api/v1/componentDetails");
      assertThat(m).isNotNull();
      assertThat(m.activityType()).isEqualTo(ActivityType.COMPONENT_DETAILS);
    }

    @Test
    public void resolve_versionScoringPath_returnsVersionRecommendation() {
      HdsPathExtractor.PathMatch m = HdsPathExtractor.resolve("/api/v1/rest/component/version-scoring/abc123def456");
      assertThat(m).isNotNull();
      assertThat(m.activityType()).isEqualTo(ActivityType.VERSION_RECOMMENDATION);
    }

    @Test
    public void resolve_dependenciesPath_returnsVersionRecommendation() {
      HdsPathExtractor.PathMatch m = HdsPathExtractor.resolve("/api/v1/rest/component/dependencies?hash=abc123def456");
      assertThat(m).isNotNull();
      assertThat(m.activityType()).isEqualTo(ActivityType.VERSION_RECOMMENDATION);
    }

    @Test
    public void resolve_signaturesVulnerabilityPath_returnsReachability() {
      HdsPathExtractor.PathMatch m =
          HdsPathExtractor.resolve("/api/v1/rest/component/signatures/vulnerability/abc123def456");
      assertThat(m).isNotNull();
      assertThat(m.activityType()).isEqualTo(ActivityType.REACHABILITY);
    }

    @Test
    public void resolve_vulnerabilityAffectedPath_returnsDeveloperPriorities() {
      HdsPathExtractor.PathMatch m = HdsPathExtractor.resolve("/api/v1/rest/vulnerability/affected");
      assertThat(m).isNotNull();
      assertThat(m.activityType()).isEqualTo(ActivityType.DEVELOPER_PRIORITIES);
    }

    @Test
    public void resolve_pathWithPrefix_resolvesCorrectly() {
      // Path with URI prefix before the key AND no hash segment after — verifies
      // substring matching works for embedded keys, AND that the regex extractor
      // returns null entityId when no segment follows the key.
      HdsPathExtractor.PathMatch m = HdsPathExtractor.resolve("/api/v1/rest/component/details/evaluation");
      assertThat(m).isNotNull();
      assertThat(m.activityType()).isEqualTo(ActivityType.COMPONENT_DETAILS);
      assertThat(m.entityId()).isNull();
    }

    @Test
    public void resolve_firewallPath_returnsNull() {
      assertThat(HdsPathExtractor.resolve("rest/component/details/firewall")).isNull();
    }
  }

  public static class EntityIdExtractionTests
  {
    @Test
    public void resolve_extractsComponentHashFromIntegrationPath() {
      HdsPathExtractor.PathMatch m = HdsPathExtractor.resolve("/rest/component/details/integration/abc123def456");
      assertThat(m).isNotNull();
      assertThat(m.activityType()).isEqualTo(ActivityType.COMPONENT_DETAILS);
      assertThat(m.entityId()).isEqualTo("abc123def456");
    }

    @Test
    public void resolve_extractsComponentHashFromEvaluationPath() {
      HdsPathExtractor.PathMatch m = HdsPathExtractor.resolve("/rest/component/details/evaluation/abc123def456");
      assertThat(m.activityType()).isEqualTo(ActivityType.COMPONENT_DETAILS);
      assertThat(m.entityId()).isEqualTo("abc123def456");
    }

    @Test
    public void resolve_extractsComponentHashFromVersionScoringPath() {
      HdsPathExtractor.PathMatch m = HdsPathExtractor.resolve("/rest/component/version-scoring/abc123def456");
      assertThat(m.activityType()).isEqualTo(ActivityType.VERSION_RECOMMENDATION);
      assertThat(m.entityId()).isEqualTo("abc123def456");
    }

    @Test
    public void resolve_dependenciesPathNoBody_returnsNullEntityId() {
      // ComponentRemediationService posts PURLs in the request body — there is no hash in the path or query.
      // Without a body the entityId falls through to null (no dedup, matches null-key contract).
      HdsPathExtractor.PathMatch m = HdsPathExtractor.resolve("/rest/component/dependencies", null);
      assertThat(m.activityType()).isEqualTo(ActivityType.VERSION_RECOMMENDATION);
      assertThat(m.entityId()).isNull();
    }

    @Test
    public void resolve_extractsComponentIdentifierHashFromComponentDetailsQueryString() {
      // The real IQ UI Component Details call carries both componentIdentifier= and hash=.
      // componentIdentifier= wins (iter3 priority) so both the entry-page call and the
      // no-hash sub-mount calls converge on the same entityId.
      HdsPathExtractor.PathMatch m = HdsPathExtractor.resolve(
          "/rest/ci/componentDetails/application/clm38070-fixture?componentIdentifier=foo&hash=abc123def456&scanId=s");
      assertThat(m).isNotNull();
      assertThat(m.activityType()).isEqualTo(ActivityType.COMPONENT_DETAILS);
      assertThat(m.entityId()).isNotNull().hasSize(16).matches("[0-9a-f]{16}");
    }

    @Test
    public void resolve_extractsComponentHashFromComponentDetailsAllVersionsQueryString() {
      HdsPathExtractor.PathMatch m = HdsPathExtractor.resolve(
          "/rest/ci/componentDetails/application/clm38070-fixture/allVersions?hash=abc123def456");
      assertThat(m).isNotNull();
      assertThat(m.activityType()).isEqualTo(ActivityType.COMPONENT_DETAILS);
      assertThat(m.entityId()).isEqualTo("abc123def456");
    }

    @Test
    public void resolve_extractsComponentHashFromReachabilityPath() {
      HdsPathExtractor.PathMatch m = HdsPathExtractor.resolve("/rest/component/signatures/vulnerability/abc123def456");
      assertThat(m.activityType()).isEqualTo(ActivityType.REACHABILITY);
      assertThat(m.entityId()).isEqualTo("abc123def456");
    }

    @Test
    public void resolve_returnsNullEntityIdForVulnerabilityAffected() {
      HdsPathExtractor.PathMatch m = HdsPathExtractor.resolve("/rest/vulnerability/affected");
      assertThat(m.activityType()).isEqualTo(ActivityType.DEVELOPER_PRIORITIES);
      assertThat(m.entityId()).isNull();
    }

    @Test
    public void resolve_returnsNullEntityIdForComponentDetailsWithoutHashQueryParam() {
      // Legacy path without the hash query parameter — entityId is legitimately null.
      HdsPathExtractor.PathMatch m = HdsPathExtractor.resolve("/api/v1/componentDetails");
      assertThat(m.activityType()).isEqualTo(ActivityType.COMPONENT_DETAILS);
      assertThat(m.entityId()).isNull();
    }

    @Test
    public void resolve_handlesPathWithQueryString() {
      HdsPathExtractor.PathMatch m = HdsPathExtractor.resolve("/rest/component/version-scoring/abc123def456?foo=bar");
      assertThat(m.entityId()).isEqualTo("abc123def456");
    }
  }

  public static class BoundaryTests
  {
    @Test
    public void resolve_returnsNullForUnmappedPath() {
      assertThat(HdsPathExtractor.resolve("/rest/something/else")).isNull();
    }

    @Test
    public void resolve_returnsNullForNullPath() {
      assertThat(HdsPathExtractor.resolve(null)).isNull();
    }

    @Test
    public void resolve_skipsComponentDetailsListPath() {
      // /componentDetails/list is the cross-version metadata fetch (Version Explorer
      // + Recommended Version + Auto-PR widgets). It is a sibling of the keyed
      // /componentDetails?hash=... call and is not a per-engagement signal.
      // Recording it would only produce NULL-keyed rows that cannot dedup.
      assertThat(HdsPathExtractor.resolve(
          "/rest/ci/componentDetails/list?componentIdentifier=foo&stableVersionsOnly=true")).isNull();
    }

    @Test
    public void resolve_skipsVersionScoringListPath() {
      // /version-scoring/list is the bulk version-scoring POST endpoint. The path
      // carries no per-component entity id — `hashAfter("rest/component/version-scoring")`
      // would extract the literal segment "list", collapsing every bulk batch in the
      // same (user, scan, session) into a single dedup'd row. Skip the recording
      // entirely so bulk batches do not undercount VR engagement.
      assertThat(HdsPathExtractor.resolve(
          "/rest/component/version-scoring/list?stageId=build")).isNull();
    }

    @Test
    public void resolve_doesNotSkipComponentDetailsBasePath() {
      // Sanity: the skip check must not eat the keyed /componentDetails call.
      // componentIdentifier= wins over hash= (iter3 priority).
      HdsPathExtractor.PathMatch m =
          HdsPathExtractor.resolve("/rest/ci/componentDetails?componentIdentifier=foo&hash=abc123");
      assertThat(m).isNotNull();
      assertThat(m.activityType()).isEqualTo(ActivityType.COMPONENT_DETAILS);
      assertThat(m.entityId()).isNotNull().hasSize(16).matches("[0-9a-f]{16}");
    }

    @Test
    public void pathMappings_haveNoSubstringCollisions() {
      java.util.Map<String, ActivityType> mappings = HdsPathExtractor.getPathMappings();
      for (String a : mappings.keySet()) {
        for (String b : mappings.keySet()) {
          if (!a.equals(b)) {
            assertThat(a)
                .as("Substring collision in PATH_MAPPINGS: key '%s' is contained in key '%s'."
                    + " Because resolve() iterates in insertion order, iteration order would silently"
                    + " pick one activity type over the other depending on map ordering. Either rename"
                    + " the keys or document the priority explicitly.", b, a)
                .doesNotContain(b);
          }
        }
      }
    }
  }

  public static class ComponentDetailsEntityIdTests
  {
    private static final String COMPONENT_IDENTIFIER_JSON =
        "{\"format\":\"maven\",\"coordinates\":{\"artifactId\":\"jackson-core\",\"classifier\":\"\",\"extension\":\"jar\",\"groupId\":\"com.fasterxml.jackson.core\",\"version\":\"2.18.6\"}}";

    private static final String COMPONENT_IDENTIFIER_ENCODED =
        java.net.URLEncoder.encode(COMPONENT_IDENTIFIER_JSON, java.nio.charset.StandardCharsets.UTF_8);

    @Test
    public void resolve_componentDetailsWithComponentIdentifierAndHash_prefersComponentIdentifier() {
      HdsPathExtractor.PathMatch m = HdsPathExtractor.resolve(
          "/rest/ci/componentDetails?hash=abc123&componentIdentifier=" + COMPONENT_IDENTIFIER_ENCODED);
      assertThat(m).isNotNull();
      assertThat(m.activityType()).isEqualTo(ActivityType.COMPONENT_DETAILS);
      assertThat(m.entityId()).isNotNull().hasSize(16).matches("[0-9a-f]{16}");
    }

    @Test
    public void resolve_componentDetailsWithoutHash_extractsComponentIdentifierHash() {
      HdsPathExtractor.PathMatch m = HdsPathExtractor.resolve(
          "/rest/ci/componentDetails?componentIdentifier=" + COMPONENT_IDENTIFIER_ENCODED);
      assertThat(m).isNotNull();
      assertThat(m.activityType()).isEqualTo(ActivityType.COMPONENT_DETAILS);
      assertThat(m.entityId()).isNotNull().hasSize(16).matches("[0-9a-f]{16}");
    }

    @Test
    public void resolve_componentDetailsWithoutHashOrComponentIdentifier_returnsNull() {
      HdsPathExtractor.PathMatch m = HdsPathExtractor.resolve("/rest/ci/componentDetails?scanId=foo");
      assertThat(m).isNotNull();
      assertThat(m.activityType()).isEqualTo(ActivityType.COMPONENT_DETAILS);
      assertThat(m.entityId()).isNull();
    }

    @Test
    public void resolve_componentDetailsSameComponentIdentifier_producesStableHash() {
      String path = "/rest/ci/componentDetails?componentIdentifier=" + COMPONENT_IDENTIFIER_ENCODED;
      String id1 = HdsPathExtractor.resolve(path).entityId();
      String id2 = HdsPathExtractor.resolve(path).entityId();
      assertThat(id1).isEqualTo(id2);
    }

    @Test
    public void resolve_componentDetailsDifferentComponentIdentifier_producesDifferentHashes() {
      String ciV1 = java.net.URLEncoder.encode(
          "{\"format\":\"maven\",\"coordinates\":{\"artifactId\":\"jackson-core\",\"classifier\":\"\",\"extension\":\"jar\",\"groupId\":\"com.fasterxml.jackson.core\",\"version\":\"2.18.5\"}}",
          java.nio.charset.StandardCharsets.UTF_8);
      String id1 = HdsPathExtractor.resolve(
          "/rest/ci/componentDetails?componentIdentifier=" + COMPONENT_IDENTIFIER_ENCODED).entityId();
      String id2 = HdsPathExtractor.resolve(
          "/rest/ci/componentDetails?componentIdentifier=" + ciV1).entityId();
      assertThat(id1).isNotEqualTo(id2);
    }

    @Test
    public void resolve_componentDetailsBothCallShapes_produceSameEntityId() {
      String pathWithHash = "/api/v1/rest/ci/componentDetails?componentIdentifier="
          + java.net.URLEncoder.encode(
              "{\"format\":\"maven\",\"coordinates\":{\"groupId\":\"foo\",\"artifactId\":\"bar\",\"version\":\"1.0\"}}",
              java.nio.charset.StandardCharsets.UTF_8)
          + "&hash=abc123def456";
      String pathWithoutHash = "/api/v1/rest/ci/componentDetails?componentIdentifier="
          + java.net.URLEncoder.encode(
              "{\"format\":\"maven\",\"coordinates\":{\"groupId\":\"foo\",\"artifactId\":\"bar\",\"version\":\"1.0\"}}",
              java.nio.charset.StandardCharsets.UTF_8);

      HdsPathExtractor.PathMatch withHash = HdsPathExtractor.resolve(pathWithHash);
      HdsPathExtractor.PathMatch withoutHash = HdsPathExtractor.resolve(pathWithoutHash);

      assertThat(withHash.entityId()).isEqualTo(withoutHash.entityId());
      assertThat(withHash.entityId()).hasSize(16);
    }

    @Test
    public void resolve_componentDetailsWithOnlyHash_fallsBackToHash() {
      HdsPathExtractor.PathMatch m = HdsPathExtractor.resolve(
          "/api/v1/rest/ci/componentDetails?hash=abc123def456");
      assertThat(m).isNotNull();
      assertThat(m.entityId()).isEqualTo("abc123def456");
    }
  }

  public static class DependenciesBodyExtractorTests
  {
    private static final String DEPS_PATH = "/api/v1/rest/component/dependencies";

    @Test
    public void resolve_dependenciesWithNullBody_returnsNullEntityId() {
      HdsPathExtractor.PathMatch m = HdsPathExtractor.resolve(DEPS_PATH, null);
      assertThat(m).isNotNull();
      assertThat(m.activityType()).isEqualTo(ActivityType.VERSION_RECOMMENDATION);
      assertThat(m.entityId()).isNull();
    }

    @Test
    public void resolve_dependenciesWithEmptyList_returnsNullEntityId() {
      HdsPathExtractor.PathMatch m = HdsPathExtractor.resolve(DEPS_PATH, Collections.emptyList());
      assertThat(m).isNotNull();
      assertThat(m.activityType()).isEqualTo(ActivityType.VERSION_RECOMMENDATION);
      assertThat(m.entityId()).isNull();
    }

    @Test
    public void resolve_dependenciesWithPurlBody_returns16HexEntityId() {
      PackageUrlIdentifier purl = new PackageUrlIdentifier("pkg:maven/com.example/foo@1.0");
      HdsPathExtractor.PathMatch m = HdsPathExtractor.resolve(DEPS_PATH, List.of(purl));
      assertThat(m).isNotNull();
      assertThat(m.activityType()).isEqualTo(ActivityType.VERSION_RECOMMENDATION);
      assertThat(m.entityId()).isNotNull().hasSize(16).matches("[0-9a-f]{16}");
    }

    @Test
    public void resolve_dependenciesWithSamePurlsDifferentOrder_returnsSameEntityId() {
      PackageUrlIdentifier p1 = new PackageUrlIdentifier("pkg:maven/com.example/aaa@1.0");
      PackageUrlIdentifier p2 = new PackageUrlIdentifier("pkg:maven/com.example/bbb@2.0");
      String id1 = HdsPathExtractor.resolve(DEPS_PATH, List.of(p1, p2)).entityId();
      String id2 = HdsPathExtractor.resolve(DEPS_PATH, List.of(p2, p1)).entityId();
      assertThat(id1).isEqualTo(id2);
    }

    @Test
    public void resolve_dependenciesWithDifferentPurlSets_returnsDifferentEntityIds() {
      PackageUrlIdentifier p1 = new PackageUrlIdentifier("pkg:maven/com.example/aaa@1.0");
      PackageUrlIdentifier p2 = new PackageUrlIdentifier("pkg:maven/com.example/bbb@2.0");
      String id1 = HdsPathExtractor.resolve(DEPS_PATH, List.of(p1)).entityId();
      String id2 = HdsPathExtractor.resolve(DEPS_PATH, List.of(p2)).entityId();
      assertThat(id1).isNotEqualTo(id2);
    }
  }
}
