/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.slo;

import java.util.Date;
import java.util.List;
import java.util.Locale;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.policy.SloFeedSortKey;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.policy.InvalidStageException;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;

import org.apache.commons.lang3.StringUtils;

@Named
public class SloViolationFeedService
{
  static final int MAX_PAGE_SIZE = 1000;

  private final ApplicationDAO applicationDAO;

  private final PolicyEvaluationDAO policyEvaluationDAO;

  private final PolicyViolationDAO policyViolationDAO;

  private final SloViolationEnricher enricher;

  @Inject
  public SloViolationFeedService(
      final ApplicationDAO applicationDAO,
      final PolicyEvaluationDAO policyEvaluationDAO,
      final PolicyViolationDAO policyViolationDAO,
      final SloViolationEnricher enricher)
  {
    this.applicationDAO = applicationDAO;
    this.policyEvaluationDAO = policyEvaluationDAO;
    this.policyViolationDAO = policyViolationDAO;
    this.enricher = enricher;
  }

  /**
   * Returns the SLO violation feed for an application/stage across all states (open, fixed, waived). Pagination is
   * cursor-based ({@code afterViolationId}); see {@link SloViolationPageResult} for the frozen-cursor rationale and the
   * client dedupe-by-{@code violationId} contract.
   * <p>
   * Note on enrichment provenance: {@code dependencyType} and {@code recommendedRemediationVersion} are derived
   * from the <em>latest</em> policy evaluation's scan report for the stage, not from the scan in which each
   * violation was originally opened.
   * </p>
   * <p>
   * Note on {@code total} staleness: the count and the slice are read in <em>separate</em> transactions, so under
   * a concurrent scan the reported {@code total} may be slightly stale relative to the returned rows.
   * </p>
   *
   * @see SloViolationPageResult
   */
  @Authorize(permission = Permission.READ)
  public SloViolationFeedResults getSloViolations(
      @AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) final String applicationPublicId,
      final String stageIdParam,
      final Date updatedSince,
      final String afterViolationId,
      final int pageSize)
  {
    validatePageSize(pageSize);

    // Normalize blank/whitespace cursor ids to null so a stray "afterViolationId=" query param is treated as "first
    // page" rather than reaching the DAO as an empty-string cursor that silently returns zero rows.
    final String cursorId = StringUtils.trimToNull(afterViolationId);

    final String stageId = resolveAndValidateStage(stageIdParam);

    final Application application = applicationDAO.getByPublicIdNotNull(applicationPublicId);

    validateCursor(updatedSince, cursorId);

    final PolicyEvaluation evaluation =
        policyEvaluationDAO.getLastByOwnerIdAndStageId(application.getId(), stageId);
    if (evaluation == null) {
      throw new NotFoundException(
          "No policy evaluation found for application " + applicationPublicId + " at stage " + stageId + ".");
    }
    final String scanId = evaluation.getScanId();

    final long total = policyViolationDAO.countByOwnerIdAndStage(application.getId(), stageId, updatedSince);

    final List<PolicyViolation> violations = policyViolationDAO.getByOwnerIdAndStageAfterCursor(
        application.getId(), stageId, updatedSince, cursorId, pageSize);

    final List<SloViolation> mapped = enricher.enrich(application, stageId, scanId, violations);

    // A full page implies there may be more rows; freeze the continuation point at the last returned row's
    // (sort key, id). The cursor field names mirror the request params so the caller round-trips them verbatim.
    final PolicyViolation lastRow =
        violations.size() == pageSize ? violations.get(violations.size() - 1) : null;
    final SloViolationFeedCursor nextPageCursor = lastRow == null
        ? null
        : new SloViolationFeedCursor(SloFeedSortKey.of(lastRow).getTime(), lastRow.getId());

    return new SloViolationFeedResults(
        stageId,
        scanId,
        new SloViolationPageResult(total, mapped, nextPageCursor));
  }

  private void validatePageSize(final int pageSize) {
    if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
      throw new BadRequestException(
          "Invalid page size: " + pageSize + ". Page size must be between 1 and " + MAX_PAGE_SIZE + ".");
    }
  }

  private void validateCursor(final Date updatedSince, final String cursorId) {
    // afterViolationId is only the tiebreaker within the frozen (updatedSince, afterViolationId) keyset, so a
    // continuation needs the time component. The cursor row is deliberately NOT looked up (see the frozen-cursor
    // rationale on SloViolationPageResult): the query is already scoped to this application/stage, so a stale or
    // foreign afterViolationId can only page this application's rows — it cannot leak another application's data nor
    // act
    // as an existence oracle. A first page or a plain updatedSince delta poll (cursorId == null) is always valid.
    if (cursorId != null && updatedSince == null) {
      throw new BadRequestException(
          "Invalid cursor: afterViolationId requires updatedSince (pass back the prior page's nextPageCursor fields).");
    }
  }

  private String resolveAndValidateStage(final String stageIdParam) {
    if (StringUtils.isBlank(stageIdParam)) {
      return Stage.ID_RELEASE;
    }
    final String normalized = stageIdParam.toLowerCase(Locale.ROOT);
    if (!Stage.isValidStageTypeId(normalized)) {
      throw new InvalidStageException(stageIdParam);
    }
    return normalized;
  }
}
