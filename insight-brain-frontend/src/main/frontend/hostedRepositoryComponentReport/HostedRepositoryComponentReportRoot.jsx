/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { UIView } from '@uirouter/react';
import { useDispatch, useSelector } from 'react-redux';
import { useRouterState } from 'MainRoot/react/RouterStateContext';
import {
  setReportParameters,
  loadReportIfNeeded,
  setHostedRepoContext,
} from 'MainRoot/applicationReport/applicationReportActions';
import { selectRouterPrevParams } from 'MainRoot/reduxUiRouter/routerSelectors';
import { wrapHostedRepositoryComponentReportRoot } from './hostedRepositoryComponentReportNexusOneShell';

// sessionStorage bridge for the HRC parent-repo context (repositoryManagerId,
// repositoryId, repositoryPublicId). Redux state is in-memory and prevParams is empty
// after a full page refresh, so without this the back button falls through to the
// generic "Back to Hosted Repos" label on refresh (see CLM-44275 back-button work).
// Keyed by hrcId so different HRC tabs don't leak context into each other. Kept in
// sessionStorage (not localStorage) so it dies with the tab — no long-lived stale data.
const HRC_CONTEXT_SESSION_STORAGE_KEY = 'iq.hrcReport.hostedRepoContext';

function readHostedRepoContextFromSession(hrcId) {
  if (!hrcId || typeof window === 'undefined') return null;
  try {
    const raw = window.sessionStorage.getItem(`${HRC_CONTEXT_SESSION_STORAGE_KEY}:${hrcId}`);
    if (!raw) return null;
    const parsed = JSON.parse(raw);
    if (parsed && parsed.repositoryManagerId && parsed.repositoryId) return parsed;
    return null;
  } catch {
    return null;
  }
}

function writeHostedRepoContextToSession(hrcId, ctx) {
  if (!hrcId || !ctx || typeof window === 'undefined') return;
  try {
    window.sessionStorage.setItem(`${HRC_CONTEXT_SESSION_STORAGE_KEY}:${hrcId}`, JSON.stringify(ctx));
  } catch {
    // sessionStorage can throw in private modes / when quota is exceeded — the back
    // button just falls back to the generic label, so swallow silently.
  }
}

/**
 * Root component for Hosted Repository Component (HRC) Lifecycle Report.
 * <p>
 * This component is the entry point for the HRC report UI, parallel to ApplicationReportRoot.
 * Key differences from application reports:
 * - Uses hrcId (UUID) instead of publicId
 * - Blocks waiver creation (out of scope for v1)
 * - Blocks re-evaluate button (deferred to Epic 2)
 * - Calls HRC-specific REST endpoints
 */
export default function HostedRepositoryComponentReportRoot() {
  const dispatch = useDispatch();
  const routerState = useRouterState();
  const params = routerState.params;
  const prevParams = useSelector(selectRouterPrevParams);

  // Similar HACK comment as ApplicationReportRoot: deps array intentionally omits
  // params that change on sub-route navigations to avoid race conditions.
  // Only re-run when the report itself changes (hrcId or scanId).
  useEffect(() => {
    if (params.hrcId && params.scanId) {
      dispatch(
        setReportParameters(
          params.hrcId, // 1: ownerId (HRC UUID)
          params.scanId, // 2
          false, // 3: isUnknownJs — N/A for HRC
          !!params.embeddable, // 4: embeddable
          params.policyViolationId, // 5
          params.componentHash, // 6
          params.tabId, // 7
          true, // 8: isNotFiltered — treat like a fresh navigation
          false // 9: isApplication — HRC, not application
        )
      );
      // Resolve the parent-repository context (repositoryManagerId, repositoryId,
      // repositoryPublicId) that the back button reads. Two sources, in priority order:
      //   1. prevParams — populated on a fresh navigation from the components list.
      //   2. sessionStorage — populated on a prior visit; survives page refresh where both
      //      Redux and prevParams are empty. Deep-links from a fresh tab still fall through
      //      to the generic "Back to Hosted Repos" label since neither source has data.
      // This effect only re-runs when hrcId/scanId change, so subsequent sub-route
      // navigations (component-details drill-in) don't clobber it — that's the drill-and-
      // back cycle where prevParams gets replaced. Later reads come from
      // selectHostedRepoContext, not from prevParams or sessionStorage.
      const fromPrev =
        prevParams?.repositoryManagerId && prevParams?.repositoryId
          ? {
              repositoryManagerId: prevParams.repositoryManagerId,
              repositoryId: prevParams.repositoryId,
              repositoryPublicId: prevParams.repositoryPublicId,
            }
          : null;
      const resolvedContext = fromPrev ?? readHostedRepoContextFromSession(params.hrcId);
      if (resolvedContext) {
        dispatch(setHostedRepoContext(resolvedContext));
        writeHostedRepoContextToSession(params.hrcId, resolvedContext);
      }
      dispatch(loadReportIfNeeded());
    }
  }, [dispatch, params.hrcId, params.scanId]);

  return wrapHostedRepositoryComponentReportRoot(<UIView />);
}
