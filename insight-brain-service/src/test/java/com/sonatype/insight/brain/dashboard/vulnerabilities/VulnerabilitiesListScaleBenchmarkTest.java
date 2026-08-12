/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.vulnerabilities;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.Arrays;
import java.util.Locale;
import jakarta.inject.Inject;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.report.ReportTestUtils;
import com.sonatype.insight.brain.search.index.ItemType;
import com.sonatype.insight.brain.search.lucene.LuceneSearchIndexClient;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Times a full Vulnerabilities page load over a synthetic estate and reports the list and the
 * scope-facet rail separately, so their relative cost is visible rather than inferred.
 * <p>
 * A measurement harness, not a pass/fail gate: it asserts only that the estate it measured was
 * actually populated, never on elapsed time. The correctness gate is
 * {@link VulnerabilitiesListServiceTest}.
 * <p>
 * Documents come from the real report pipeline, and {@code componentsMetricReport} yields four
 * {@code SECURITY_VULNERABILITY} documents per application over the same four vulnerability ids.
 * Growing the estate therefore grows the documents each pass scans while the number of ranked rows
 * stays fixed, which is the shape that isolates scan cost. Estate size comes from
 * {@code -Dvulnerabilities.benchmark.apps=N}; the default is small so an accidental run is cheap.
 */
public class VulnerabilitiesListScaleBenchmarkTest
    extends AbstractComponentTest
{
  private static final Logger log = LoggerFactory.getLogger(VulnerabilitiesListScaleBenchmarkTest.class);

  private static final int WARMUP_RUNS = 3;

  private static final int MEASURED_RUNS = 7;

  private static final int PAGE_SIZE = 25;

  private static final int APPLICATIONS = Integer.getInteger("vulnerabilities.benchmark.apps", 25);

  @Inject
  private VulnerabilitiesListService vulnerabilitiesListService;

  @Inject
  private LuceneSearchIndexClient luceneSearchIndexClient;

  @Test
  public void reportsPageLoadTimingAtScale() throws Exception {
    long documents = buildSyntheticEstate();

    VulnerabilitiesListRequestDTO listOnlyRequest = pageRequest(false);
    VulnerabilitiesListRequestDTO listAndRailRequest = pageRequest(true);

    VulnerabilitiesListResponseDTO sample = vulnerabilitiesListService.listVulnerabilities(listAndRailRequest);

    double listOnlyMillis = medianMillis(listOnlyRequest);
    double listAndRailMillis = medianMillis(listAndRailRequest);

    log.info(
        "Vulnerabilities page load over {} applications / {} documents — "
            + "list only: {} ms, list + facet rail: {} ms, rail share: {} ms "
            + "(rows: {}, distinct total: {}, scope facet rail present: {})",
        APPLICATIONS,
        documents,
        millis(listOnlyMillis),
        millis(listAndRailMillis),
        millis(listAndRailMillis - listOnlyMillis),
        sample.vulnerabilities.size(),
        sample.total,
        sample.facets != null && sample.facets.applications != null);

    // Timings over an empty estate would be meaningless, so the populated estate is the one thing
    // this harness does gate on.
    assertThat(documents).isPositive();
    assertThat(sample.vulnerabilities).isNotEmpty();
  }

  private static VulnerabilitiesListRequestDTO pageRequest(final boolean includeFacets) {
    VulnerabilitiesListRequestDTO request = new VulnerabilitiesListRequestDTO();
    request.page = 0;
    request.pageSize = PAGE_SIZE;
    request.includeFacets = includeFacets;
    return request;
  }

  private double medianMillis(final VulnerabilitiesListRequestDTO request) {
    for (int i = 0; i < WARMUP_RUNS; i++) {
      vulnerabilitiesListService.listVulnerabilities(request);
    }
    long[] samples = new long[MEASURED_RUNS];
    for (int i = 0; i < MEASURED_RUNS; i++) {
      long start = System.nanoTime();
      vulnerabilitiesListService.listVulnerabilities(request);
      samples[i] = System.nanoTime() - start;
    }
    Arrays.sort(samples);
    return samples[samples.length / 2] / 1_000_000.0;
  }

  private static String millis(final double value) {
    return String.format(Locale.ROOT, "%.1f", value);
  }

  /**
   * Builds the estate through the real report pipeline and returns the indexed vulnerability
   * document count. The fixture is zipped once and copied per application, so estate construction
   * costs one file copy per application rather than one archive build.
   */
  private long buildSyntheticEstate() throws Exception {
    File reportFile = ReportTestUtils.zipReportDir("/IndexSearchingTest/componentsMetricReport", tempDir);
    Organization org = tempEntity.newOrganization();
    for (int i = 0; i < APPLICATIONS; i++) {
      Application app = tempEntity.newApplication(org.getId());
      PolicyEvaluation evaluation =
          tempEntity.newPolicyEvaluation(app.getId(), Stage.ID_BUILD, "benchmarkScan" + i);
      ReportTestUtils.createReportFile(
          evaluation.getOwnerId(), evaluation.getScanId(), reportFile, insightWork);
    }

    luceneSearchIndexClient.populateIndex();

    return luceneSearchIndexClient.count("itemType:" + ItemType.SECURITY_VULNERABILITY.name());
  }
}
