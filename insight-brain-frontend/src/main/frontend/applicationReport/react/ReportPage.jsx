/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, Fragment } from 'react';
import { NxLoadWrapper, NxStatefulSubmitMask } from '@sonatype/react-shared-components';
import ReportStatusBar from './ReportStatusBar';
import ReportContent from './ReportContent';
import ReportFilterPopover from './ReportFilterPopover';
import ReportTitle from './ReportTitle';
import MenuBarBackButton from 'MainRoot/mainHeader/MenuBar/MenuBarBackButton';
import { selectApplicationReportSlice } from 'MainRoot/applicationReport/applicationReportSelectors';
import { selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';
import * as applicationReportActions from '../applicationReportActions';
import { useDispatch, useSelector } from 'react-redux';
import { pick } from 'ramda';

export default function ReportPage() {
  const applicationReport = useSelector(selectApplicationReportSlice);
  const routerCurrentParams = useSelector(selectRouterCurrentParams);

  const { loadError, reevaluateMaskState } = pick(['loadError', 'reevaluateMaskState'], applicationReport);

  const { publicId, scanId, unknownjs, embeddable, policyViolationId } = routerCurrentParams;
  const loading =
    !applicationReport.loadError && (!!applicationReport.pendingLoads.size || !applicationReport.metadata);

  const dispatch = useDispatch();
  const loadReport = () => dispatch(applicationReportActions.loadReportIfNeeded());
  const setReportParameters = (appId, scanId, isUnknownJs, embeddable, policyViolationId, componentHash, tabId) =>
    dispatch(
      applicationReportActions.setReportParameters(
        appId,
        scanId,
        isUnknownJs,
        embeddable,
        policyViolationId,
        componentHash,
        tabId
      )
    );

  useEffect(() => {
    if (publicId && scanId) {
      setReportParameters(publicId, scanId, unknownjs, embeddable, policyViolationId);
      loadReport();
    }
  }, [publicId, scanId]);

  return (
    <Fragment>
      {reevaluateMaskState !== null && <NxStatefulSubmitMask success={reevaluateMaskState} message="Re-Evaluating" />}
      <main id="app-report" className="nx-page-main nx-viewport-sized iq-app-report">
        <MenuBarBackButton text="All Reports" stateName={'violations'} />
        <NxLoadWrapper loading={loading} error={loadError} retryHandler={loadReport}>
          <ReportFilterPopover />
          <ReportTitle />
          <ReportStatusBar />
          <ReportContent />
        </NxLoadWrapper>
      </main>
    </Fragment>
  );
}
