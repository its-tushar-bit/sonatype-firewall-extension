/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dashboard.violations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;

import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.search.ConversionHelper;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.search.session.IndexReadSessionFactory;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ViolationsListService} sort semantics and page-scoped first-seen enrich
 * (CLM-42254 / CLM-43210).
 */
@RunWith(MockitoJUnitRunner.class)
public class ViolationsListServiceTest
{
  @Mock
  private SearchIndexClient searchIndexClient;

  @Mock
  private ViolationsListIndexQueryBuilder indexQueryBuilder;

  @Mock
  private ViolationsListRequestValidator requestValidator;

  @Mock
  private ViolationsListFacetsBuilder facetsBuilder;

  @Mock
  private IndexReadSessionFactory sessionFactory;

  @Mock
  private ConversionHelper conversionHelper;

  @Mock
  private PolicyViolationDAO policyViolationDAO;

  @Test
  public void descendingSort_ordersHighestThreatFirst_withNullsLast() {
    List<ViolationRowDTO> rows = rowsWithThreat(3, null, 10, 8);

    rows.sort(ViolationsListService.comparator("-policyThreatLevel"));

    assertThat(rows).extracting(row -> row.threatLevel).containsExactly(10, 8, 3, null);
  }

  @Test
  public void ascendingSort_ordersLowestThreatFirst_withNullsLast() {
    List<ViolationRowDTO> rows = rowsWithThreat(3, null, 10, 8);

    rows.sort(ViolationsListService.comparator("policyThreatLevel"));

    assertThat(rows).extracting(row -> row.threatLevel).containsExactly(3, 8, 10, null);
  }

  @Test
  public void enrichFirstOccurredTimes_setsEpochMillisFromOpenTime() {
    ViolationRowDTO withTime = row("pv-1");
    ViolationRowDTO missingInDb = row("pv-2");
    ViolationRowDTO nullOpenTime = row("pv-3");

    PolicyViolation open = new PolicyViolation();
    open.setId("pv-1");
    open.setOpenTime(new Date(1_700_000_000_000L));
    PolicyViolation noOpen = new PolicyViolation();
    noOpen.setId("pv-3");
    noOpen.setOpenTime(null);

    when(policyViolationDAO.getByIds(Set.of("pv-1", "pv-2", "pv-3"))).thenReturn(List.of(open, noOpen));

    service().enrichFirstOccurredTimes(List.of(withTime, missingInDb, nullOpenTime));

    assertThat(withTime.firstOccurredTime).isEqualTo(1_700_000_000_000L);
    assertThat(missingInDb.firstOccurredTime).isNull();
    assertThat(nullOpenTime.firstOccurredTime).isNull();
  }

  @Test
  public void enrichFirstOccurredTimes_skipsDaoWhenPageEmpty() {
    service().enrichFirstOccurredTimes(List.of());
    verify(policyViolationDAO, never()).getByIds(anySet());
  }

  private ViolationsListService service() {
    return new ViolationsListService(
        searchIndexClient,
        indexQueryBuilder,
        requestValidator,
        facetsBuilder,
        sessionFactory,
        conversionHelper,
        policyViolationDAO);
  }

  private static ViolationRowDTO row(final String policyViolationId) {
    ViolationRowDTO row = new ViolationRowDTO();
    row.policyViolationId = policyViolationId;
    return row;
  }

  private static List<ViolationRowDTO> rowsWithThreat(final Integer... threatLevels) {
    List<ViolationRowDTO> rows = new ArrayList<>();
    Arrays.stream(threatLevels).forEach(threat -> {
      ViolationRowDTO row = new ViolationRowDTO();
      row.threatLevel = threat;
      rows.add(row);
    });
    return rows;
  }
}
