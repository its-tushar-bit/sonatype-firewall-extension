/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.vulnerabilities;

import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Files;
import java.util.Locale;
import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.report.ReportTestUtils;
import com.sonatype.insight.brain.search.index.ItemType;
import com.sonatype.insight.brain.search.lucene.LuceneSearchIndexClient;
import com.sonatype.insight.brain.service.Zipper;

import org.junit.jupiter.api.Test;

/**
 * Proves the list ranks over the whole estate rather than over a leading sample of documents
 * (CLM-44692): the worst vulnerability in the estate reaches page one even when its documents sort
 * last and sit far beyond any page the list reads.
 * <p>
 * Sizing is what gives the test its teeth. Ranking used to be derived from the first
 * {@code 5,000} documents a query collected, so the estate is built past that bound and the one
 * CVSS 10.0 vulnerability is indexed last, where a leading-sample ranking cannot reach it. Estate
 * size comes from vulnerability count rather than application count — one scan report holds as many
 * vulnerabilities as it is written with, and each becomes an indexed document — so the estate costs
 * a single report rather than the thousands of applications the same document count would need.
 * <p>
 * Slow by nature rather than by neglect: the estate has to clear the sample bound for the assertion
 * to mean anything, and indexing that many documents is most of the runtime.
 */
@ComponentH2Test
public class VulnerabilitiesListEstateRankingTest
    extends AbstractComponentH2Test
{
  /**
   * Components carrying the burying vulnerabilities. Every component is affected by every burying
   * vulnerability, so the two counts multiply out to
   * {@code BURYING_COMPONENTS * DISTINCT_BURYING_VULNERABILITIES} documents indexed ahead of the
   * buried one — past the {@code 5,000} leading-sample bound that ranking used to inherit.
   * <p>
   * The split between the two is what keeps the test cheap. Indexing cost tracks distinct
   * vulnerabilities rather than components, and the bound is a document count, so the documents are
   * made of many components sharing few vulnerabilities rather than the reverse. That is also the
   * realistic shape: a widely-used component set, and one rare critical finding among them.
   */
  private static final int BURYING_COMPONENTS = 150;

  private static final int DISTINCT_BURYING_VULNERABILITIES = 40;

  private static final String BURIED_VULNERABILITY_ID = "CVE-9999-0001";

  /** Every burying vulnerability scores below this, so rank one is unambiguous. */
  private static final float BURIED_SCORE = 10.0f;

  @Inject
  private VulnerabilitiesListService vulnerabilitiesListService;

  @Inject
  private LuceneSearchIndexClient luceneSearchIndexClient;

  @Test
  public void worstVulnerabilityInTheEstateRanksFirstEvenWhenItsDocumentsSortLast() throws Exception {
    long documents = buildEstateWithWorstVulnerabilityLast();

    // orderBy is left unset so the list sorts the way it does by default: worst first.
    VulnerabilitiesListRequestDTO request = new VulnerabilitiesListRequestDTO();
    request.page = 0;
    request.pageSize = 25;

    VulnerabilitiesListResponseDTO response = vulnerabilitiesListService.listVulnerabilities(request);

    // An estate that did not clear the sample bound would make the assertions below vacuous.
    assertThat(documents).isGreaterThan(BURYING_COMPONENTS * DISTINCT_BURYING_VULNERABILITIES);
    assertThat(response.vulnerabilities).isNotEmpty();
    assertThat(response.vulnerabilities.get(0).vulnerabilityId).isEqualTo(BURIED_VULNERABILITY_ID);
    assertThat(response.vulnerabilities.get(0).cvssScore).isEqualTo(BURIED_SCORE);
    // The ranked row is a real row, not a bare id: hydration has to reach it too.
    assertThat(response.vulnerabilities.get(0).applicationCount).isEqualTo(1);
  }

  /**
   * Writes one scan report holding the lower-scored vulnerabilities followed by the single CVSS 10.0
   * one, indexes it, and returns the indexed vulnerability document count. Report order carries
   * through to index order, so writing the worst one last is what puts it beyond the reach of a
   * leading sample.
   */
  private long buildEstateWithWorstVulnerabilityLast() throws Exception {
    File reportDir = tempDir.newFolder("estateRankingReport");
    Files.writeString(reportDir.toPath().resolve("dependencies.json"), "{}");
    Files.writeString(reportDir.toPath().resolve("licenses.json"), "{\"aaData\":[]}");

    StringBuilder components = new StringBuilder("{\"aaData\":[");
    StringBuilder vulnerabilities = new StringBuilder("{\"aaData\":[");
    for (int component = 0; component <= BURYING_COMPONENTS; component++) {
      if (component > 0) {
        components.append(',');
      }
      components.append(component(component));
    }
    for (int component = 0; component < BURYING_COMPONENTS; component++) {
      for (int cve = 0; cve < DISTINCT_BURYING_VULNERABILITIES; cve++) {
        if (component > 0 || cve > 0) {
          vulnerabilities.append(',');
        }
        // Scores stay under the buried one and vary so the ranking has real work to order.
        vulnerabilities.append(vulnerability(component, "CVE-9000-" + cve, 1.0 + (cve % 80) / 10.0));
      }
    }
    // Last component, last entry: the worst vulnerability in the estate is also the last indexed.
    vulnerabilities.append(',').append(vulnerability(BURYING_COMPONENTS, BURIED_VULNERABILITY_ID, BURIED_SCORE));

    Files.writeString(reportDir.toPath().resolve("bom.json"), components.append("]}").toString());
    Files.writeString(reportDir.toPath().resolve("security.json"), vulnerabilities.append("]}").toString());

    File reportZip = new File(tempDir.getRoot(), "estateRankingReport.zip");
    Zipper.zip(reportDir, reportZip);

    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());
    PolicyEvaluation evaluation = tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "estateRankingScan");
    ReportTestUtils.createReportFile(evaluation.getOwnerId(), evaluation.getScanId(), reportZip, insightWork);

    luceneSearchIndexClient.populateIndex();

    return luceneSearchIndexClient.count("itemType:" + ItemType.SECURITY_VULNERABILITY.name());
  }

  /**
   * Report ingestion reads the nullable keys rather than defaulting them, so a component carries the
   * same key set as the checked-in report fixtures even where the values are null.
   */
  private static String component(final int index) {
    String artifactId = "artifact" + index;
    return """
        {%s,"filenames":["%s.jar"],"pathnames":["some/dir/%s.jar"],\
        "displayName":{"parts":[{"value":"%s"}]},"matchState":"exact","scanError":false,"proprietary":false,\
        "relativePopularity":null,"createTime":1364313072251,"lastModifiedTime":1481029585000,\
        "lastModifiedEntryTime":null,"website":null,"identificationSource":"Sonatype",\
        "componentCategories":[{"componentCategoryId":113,"path":"Other"}],"hygieneRating":null}\
        """.formatted(identifier(index), artifactId, artifactId, artifactId);
  }

  /** One vulnerability affecting the component at {@code componentIndex}, keyed by the same hash. */
  private static String vulnerability(
      final int componentIndex,
      final String vulnerabilityId,
      final double score)
  {
    return """
        {%s,"reference":"%s","source":"cve","score":%s,"cwe":"200","status":"Open",\
        "matchState":"exact","proprietary":false}\
        """.formatted(identifier(componentIndex), vulnerabilityId, score);
  }

  /** Coordinates and hash, repeated verbatim by the component and by every vulnerability on it. */
  private static String identifier(final int componentIndex) {
    return """
        "componentIdentifier":{"format":"maven","coordinates":\
        {"artifactId":"artifact%d","classifier":"","extension":"jar","groupId":"test","version":"1.0"}},\
        "hash":"%s"\
        """.formatted(componentIndex, String.format(Locale.ROOT, "hash%016d", componentIndex));
  }
}
