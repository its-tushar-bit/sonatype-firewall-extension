/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.guide.api.dto;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the {@code /global/search} response deserializes each hit into its concrete
 * {@link com.sonatype.guide.api.dto.SearchResult} subtype via Jackson DEDUCTION (no discriminator
 * field). This defends the disjoint required-field fingerprints the record documents: a security
 * event hit must resolve to {@link GuideSecurityEventDocument}, not be silently dropped or coerced
 * into a component/vulnerability. Regression: security events vanished from the All tab and the
 * autosuggest dropdown when the subtype was unregistered.
 */
public class GuideGlobalSearchResponseDeserializationTest
{
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  public void deserializesMixedHitsIntoConcreteSubtypes() throws Exception {
    String json = """
        {
          "hits": [
            {
              "format": "maven",
              "name": "log4j-core",
              "version": "2.21.1"
            },
            {
              "vulnId": "CVE-2021-44228",
              "summary": "Log4Shell remote code execution"
            },
            {
              "eventId": "SEC-1",
              "title": "polyfill.io supply chain attack",
              "overview": "Malicious code served via the polyfill CDN.",
              "eventSeverityCategory": "CRITICAL",
              "eventThreatType": "MALWARE"
            }
          ],
          "total": 3,
          "offset": 0,
          "limit": 25,
          "aggregations": { "byType": { "components": 1, "vulnerabilities": 1, "security-events": 1 } }
        }
        """;

    GuideGlobalSearchResponse response = objectMapper.readValue(json, GuideGlobalSearchResponse.class);

    assertThat(response.hits()).hasSize(3);
    assertThat(response.hits().get(0)).isInstanceOf(GuideComponentDocument.class);
    assertThat(response.hits().get(1)).isInstanceOf(GuideVulnerabilityDocument.class);
    assertThat(response.hits().get(2)).isInstanceOf(GuideSecurityEventDocument.class);

    GuideSecurityEventDocument event = (GuideSecurityEventDocument) response.hits().get(2);
    assertThat(event.eventId()).isEqualTo("SEC-1");
    assertThat(event.title()).isEqualTo("polyfill.io supply chain attack");
    assertThat(event.eventThreatType()).isEqualTo("MALWARE");
  }

  @Test
  public void deserializesSecurityEventOnlyHits() throws Exception {
    String json = """
        {
          "hits": [
            {
              "eventId": "SEC-2",
              "title": "Known exploited RCE",
              "overview": "Actively exploited in the wild.",
              "eventSeverityCategory": "HIGH",
              "eventThreatType": "VULNERABILITY",
              "isKnownExploited": true
            }
          ],
          "total": 1,
          "offset": 0,
          "limit": 25,
          "aggregations": null
        }
        """;

    GuideGlobalSearchResponse response = objectMapper.readValue(json, GuideGlobalSearchResponse.class);

    assertThat(response.hits()).hasSize(1);
    assertThat(response.hits().get(0)).isInstanceOf(GuideSecurityEventDocument.class);
    assertThat(((GuideSecurityEventDocument) response.hits().get(0)).isKnownExploited()).isTrue();
  }
}
