/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { useApplicationDetailShellContext } from './applicationDetailContext';
import {
  fetchApplicationRawReport,
  selectApplicationPolicyThreatsState,
  selectApplicationRawReportState,
  selectApplicationReportsState,
} from './applicationDetailSlice';
import {
  selectComponentCount,
  selectLatestReport,
  selectScanId,
  selectViolationCountByHash,
  selectViolationSummary,
} from './applicationDetailSelectors';
import { OverviewTab } from './OverviewTab';
import { PolicyFailuresTab } from './PolicyFailuresTab';
import { ComponentsTab } from './ComponentsTab';
import { AppWaiversTab } from './AppWaiversTab';

/** UI-Router child route: Overview tab (CLM-40901). */
export function ApplicationDetailOverviewRoute(): JSX.Element {
  const shell = useApplicationDetailShellContext();
  const reportsState = useSelector(selectApplicationReportsState);
  const policyState = useSelector(selectApplicationPolicyThreatsState);
  const summary = useSelector(selectViolationSummary);
  const componentCount = useSelector(selectComponentCount);
  const latestReport = useSelector(selectLatestReport);
  const scanId = useSelector(selectScanId);

  const overviewIsLoading =
    shell.appStatus === 'loading' ||
    reportsState.status === 'loading' ||
    policyState.status === 'loading';

  return (
    <OverviewTab
      appData={shell.appData}
      publicId={shell.publicId}
      overviewIsLoading={overviewIsLoading}
      policyStatus={policyState.status}
      reportsStatus={reportsState.status}
      scanId={scanId}
      latestReport={latestReport}
      reports={reportsState.data}
      totalViolations={summary.totalViolations}
      openViolations={summary.openViolations}
      waivedViolations={summary.waivedViolations}
      criticalCount={summary.criticalCount}
      severeCount={summary.severeCount}
      moderateCount={summary.moderateCount}
      componentCount={componentCount}
      maliciousCount={summary.maliciousCount}
      onRetryPolicy={shell.retryPolicy}
      onRetryReports={shell.retryReports}
    />
  );
}

/** UI-Router child route: Policy Failures / Violations tab. */
export function ApplicationDetailViolationsRoute(): JSX.Element {
  const shell = useApplicationDetailShellContext();
  const reportsState = useSelector(selectApplicationReportsState);
  const policyState = useSelector(selectApplicationPolicyThreatsState);
  const summary = useSelector(selectViolationSummary);
  const scanId = useSelector(selectScanId);

  return (
    <PolicyFailuresTab
      violations={summary.violations}
      loading={policyState.status === 'loading' || reportsState.status === 'loading'}
      errored={policyState.status === 'error'}
      onRetry={shell.retryPolicy}
      showNoScanYet={
        !scanId && policyState.status !== 'loading' && reportsState.status === 'ready'
      }
    />
  );
}

/** UI-Router child route: Components tab. */
export function ApplicationDetailComponentsRoute(): JSX.Element {
  const shell = useApplicationDetailShellContext();
  const dispatch = useDispatch();
  const reportsState = useSelector(selectApplicationReportsState);
  const rawState = useSelector(selectApplicationRawReportState);
  const violationCountByHash = useSelector(selectViolationCountByHash);
  const scanId = useSelector(selectScanId);
  // When reports are ready but no scan exists, skip the raw fetch and surface ComponentsTab's empty state.
  const noScanYet = !scanId && reportsState.status === 'ready';

  // Initial fetch when the tab mounts with a scanId; retries go through shell.retryRaw.
  // Guard on `idle` only — avoids re-downloading huge raw JSON on every tab revisit once cached.
  useEffect(() => {
    if (!scanId || rawState.status !== 'idle') return;
    void dispatch(fetchApplicationRawReport({ publicId: shell.publicId, scanId }));
  }, [dispatch, shell.publicId, scanId, rawState.status]);

  const effectiveStatus = noScanYet ? 'ready' : rawState.status;

  return (
    <ComponentsTab
      components={rawState.data?.components ?? []}
      status={effectiveStatus}
      publicId={shell.publicId}
      scanId={scanId}
      violationCountByHash={violationCountByHash}
      onRetry={shell.retryRaw}
    />
  );
}

/** UI-Router child route: Waivers tab. */
export function ApplicationDetailWaiversRoute(): JSX.Element {
  const shell = useApplicationDetailShellContext();
  return (
    <AppWaiversTab
      applicationInternalId={shell.applicationInternalId}
      publicId={shell.publicId}
    />
  );
}
