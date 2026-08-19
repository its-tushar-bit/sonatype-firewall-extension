/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { UIView } from '@uirouter/react';
import { useDispatch } from 'react-redux';
import { useRouterState } from 'MainRoot/react/RouterStateContext';
import { setReportParameters, loadReportIfNeeded } from 'MainRoot/applicationReport/applicationReportActions';
import { wrapApplicationReportRoot } from 'MainRoot/applicationReport/applicationReportNexusOneShell';

export default function ApplicationReportRoot() {
  const dispatch = useDispatch();
  const routerState = useRouterState();
  const params = routerState.params;

  // HACK: The deps array intentionally omits params that change on sub-route navigations
  // (componentHash, tabId, unknownjs, embeddable, policyViolationId). setReportParameters resets
  // the Redux state including selectedReport, which causes a race condition: ComponentDetails'
  // useEffect (child, fires first) starts an async chain that reads selectedReport, then this
  // effect (parent, fires second) synchronously wipes it, and the async chain crashes on the
  // now-null state.
  //
  // Before CLM-34416 (Angular removal), this component never re-rendered on sub-route
  // navigations because its parent (ReactRouterRoot) had no useSelector calls, so route changes
  // didn't propagate re-renders down the tree. The effect's deps were irrelevant because they
  // were never re-evaluated. After App.jsx's PageLayout was updated to use
  // useSelector(selectRouterState), the entire tree now re-renders on every route change,
  // which exposes the race condition. Restricting deps to [publicId, scanId] restores the prior
  // behavior where this effect only runs on initial mount and when the report itself changes.
  //
  // The proper fix would be to restructure how report parameters flow so that setReportParameters
  // doesn't need to wipe selectedReport, or to avoid the parent re-render propagation entirely
  // (e.g. by memoizing the subtree or removing the useSelector from PageLayout).
  useEffect(() => {
    if (params.publicId && params.scanId) {
      dispatch(
        setReportParameters(
          params.publicId,
          params.scanId,
          !!params.unknownjs,
          !!params.embeddable,
          params.policyViolationId,
          params.componentHash,
          params.tabId,
          true
        )
      );
      dispatch(loadReportIfNeeded());
    }
  }, [dispatch, params.publicId, params.scanId]);

  return wrapApplicationReportRoot(<UIView />);
}
