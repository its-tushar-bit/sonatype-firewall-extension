/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.export;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyVulnerabilityExploitabilityExchange;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Manual-run benchmarks to demonstrate the performance impact of SBOM PDF generation optimizations.
 * Run with: mvn verify -pl insight-brain-service
 * -Dit.test="SbomPdfPerformanceBenchmarkTest#benchmarkVexBatchQuery"
 * See CLM-39736
 */
@Disabled("Manual benchmarks - run individually to measure performance")
@ComponentH2Test
public class SbomPdfPerformanceBenchmarkTest
    extends AbstractPdfExporterH2Test
{
  private static final Logger log = LoggerFactory.getLogger(SbomPdfPerformanceBenchmarkTest.class);

  private static final int COMPONENT_COUNT = 50;

  private static final int VULNS_PER_COMPONENT = 20;

  @Override
  @BeforeEach
  public void init() throws SbomExportException {
    app = tempEntity.newApplicationWithParent(APP_ID);
    thirdPartyFile = tempEntity.newThirdPartyFile(THIRD_PARTY_FILE);
  }

  @Test
  public void benchmarkVexBatchQuery() {
    List<ThirdPartyFileCoordinate> components = new ArrayList<>();
    List<ThirdPartyCoordinateSecurity> allVulns = new ArrayList<>();

    for (int i = 0; i < COMPONENT_COUNT; i++) {
      String hash = String.format("hash%016d", i);
      String purl = "pkg:maven/group" + i + "/artifact" + i + "@1.0?type=jar";
      ThirdPartyFileCoordinate fc = setupFileCoordinateEntity(
          "artifact" + i, "1.0", hash, purl, purl);
      components.add(fc);

      for (int j = 0; j < VULNS_PER_COMPONENT; j++) {
        String refId = "CVE-" + i + "-" + j;
        ThirdPartyCoordinateSecurity cs = tempEntity.newThirdPartyCoordinateSecurity(
            fc, refId, null, "link=" + refId, 7.5d, null, "NVD", "CVSS:3.1", "HIGH", "502", "CVSSV3",
            null, null, "Sonatype");
        allVulns.add(cs);
        tempEntity.newThirdPartyVulnerabilityExploitabilityExchange(
            cs, refId, "exploitable", "code_not_reachable", "response", "details");
      }
    }

    int totalVulns = COMPONENT_COUNT * VULNS_PER_COMPONENT;
    log.info("Setup complete: {} components, {} vulnerabilities, {} VEX records",
        COMPONENT_COUNT, totalVulns, totalVulns);

    // Benchmark: N individual queries (the old approach)
    long startIndividual = System.nanoTime();
    for (ThirdPartyCoordinateSecurity vuln : allVulns) {
      thirdPartyVulnerabilityExploitabilityExchangeDAO.getByCoordinateSecurityIdAndRefId(
          vuln.getId(), vuln.getRefId());
    }
    long individualMs = (System.nanoTime() - startIndividual) / 1_000_000;

    // Benchmark: 1 batch query + in-memory filter (the new approach)
    List<String> allIds = allVulns.stream()
        .map(ThirdPartyCoordinateSecurity::getId)
        .toList();

    long startBatch = System.nanoTime();
    List<ThirdPartyVulnerabilityExploitabilityExchange> batchResults =
        thirdPartyVulnerabilityExploitabilityExchangeDAO.getListByCoordinateSecurityIds(allIds);
    var vexMap = batchResults.stream()
        .collect(Collectors.groupingBy(ThirdPartyVulnerabilityExploitabilityExchange::getCoordinateSecurityId));
    for (ThirdPartyCoordinateSecurity vuln : allVulns) {
      List<ThirdPartyVulnerabilityExploitabilityExchange> candidates =
          vexMap.getOrDefault(vuln.getId(), List.of());
      candidates.stream()
          .filter(v -> v.getRefId().equals(vuln.getRefId()))
          .findFirst()
          .orElse(null);
    }
    long batchMs = (System.nanoTime() - startBatch) / 1_000_000;

    double speedup = (double) individualMs / Math.max(batchMs, 1);
    log.info("VEX query benchmark ({} vulnerabilities):", totalVulns);
    log.info("  Individual queries: {} ms", individualMs);
    log.info("  Batch query + filter: {} ms", batchMs);
    log.info("  Speedup: {}x", String.format("%.1f", speedup));

    assertThat(batchResults).hasSize(totalVulns);
    assertThat(vexMap.values().stream().mapToInt(List::size).sum()).isEqualTo(totalVulns);
  }
}
