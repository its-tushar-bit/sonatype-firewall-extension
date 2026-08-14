/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import {
  NxH1,
  NxLoadWrapper,
  NxOverflowTooltip,
  NxPageMain,
  NxPageTitle,
  NxSmallThreatCounter,
  NxTable,
  NxTextLink,
  NxTile,
  NxTooltip,
} from '@sonatype/react-shared-components';
import { useDispatch, useSelector } from 'react-redux';
import { selectRouterCurrentParams, selectRouterSlice } from 'MainRoot/reduxUiRouter/routerSelectors';
import { actions } from 'MainRoot/applicationLatestEvaluations/applicationLatestEvaluationsSlice';
import { selectApplicationLatestEvaluationsSlice } from 'MainRoot/applicationLatestEvaluations/applicationLatestEvaluationsSelectors';
import moment from 'moment/moment';
import { useRouterState } from 'MainRoot/react/RouterStateContext';
import { STANDARD_DATE_TIME_FORMAT_NO_TZ } from 'MainRoot/util/dateUtils';
import { capitalize } from 'MainRoot/util/jsUtil';
import MenuBarBackButton from 'MainRoot/mainHeader/MenuBar/MenuBarBackButton';
import { getReleaseVersion } from 'MainRoot/util/versionUtil';

export default function ApplicationLatestEvaluationsPage() {
  const currentParams = useSelector(selectRouterCurrentParams);
  const { applicationPublicId, hrcId, stageId, scanId, componentDisplayName } = currentParams;
  const { loading, loadError, application, applicationReportHistory } = useSelector(
    selectApplicationLatestEvaluationsSlice
  );
  const { prevState, prevParams } = useSelector(selectRouterSlice);

  const dispatch = useDispatch();
  const uiRouterState = useRouterState();

  const isHrcMode = !!hrcId;

  const load = () => {
    dispatch(actions.load({ applicationPublicId, hrcId, stageId }));
  };

  const isFromApplicationReport = prevState?.name === 'applicationReport.policy';
  const isFromHrcReport = prevState?.name?.startsWith('hostedRepositoryComponentReport');

  let backHref;
  let backText;
  if (isHrcMode) {
    backHref = uiRouterState.href('hostedRepositoryComponentReport.policy', {
      hrcId,
      scanId: scanId || prevParams?.scanId,
      // Forward componentDisplayName so the HRC report renders the friendly component name
      // instead of the raw hrcId after this hop. The URL param is already declared on the
      // HRC route so it survives; the destination's getReportDisplayName reads it.
      componentDisplayName: componentDisplayName || prevParams?.componentDisplayName,
    });
    backText = 'Back to HRC Report';
  } else if (isFromApplicationReport || isFromHrcReport) {
    backHref = uiRouterState.href(prevState?.name || 'applicationReport.policy', prevParams);
    backText = isFromHrcReport ? 'Back to HRC Report' : 'Back to Application Report';
  } else {
    backHref = uiRouterState.href('violations');
    backText = 'All Reports';
  }

  // For HRC mode there's no `application` object; show the componentDisplayName or hrcId.
  const titleName = isHrcMode ? componentDisplayName || hrcId : application?.name;

  useEffect(load, []);

  const getScannerVersionToDisplay = (evaluation) => {
    if (!evaluation.scannerVersion) {
      return '—';
    }
    if (evaluation.scanTriggerInternal) {
      return getReleaseVersion(evaluation.scannerVersion);
    }
    return evaluation.scannerVersion.split('-')[0];
  };

  return (
    <NxPageMain id="application-latest-evaluations-page">
      <MenuBarBackButton href={backHref} text={backText} />
      <NxLoadWrapper loading={loading} error={loadError} retryHandler={load}>
        {(application || isHrcMode) && stageId && applicationReportHistory && (
          <>
            <NxPageTitle>
              <NxH1>{titleName} Latest Evaluations</NxH1>
              <NxPageTitle.Description>
                <span className="iq-application-latest-evaluations__stage-label">Stage:</span>{' '}
                {capitalize(stageId.toLowerCase())}
              </NxPageTitle.Description>
            </NxPageTitle>
            <NxTile>
              <NxTile.Content>
                <NxTable className="iq-application-latest-evaluations__table">
                  <NxTable.Head>
                    <NxTable.Row>
                      <NxTable.Cell>Evaluation Date</NxTable.Cell>
                      <NxTable.Cell className="iq-application-latest-evaluations__trigger-cell">Trigger</NxTable.Cell>
                      <NxTable.Cell>
                        <NxTooltip title="The integration version that triggered the evaluation.">
                          <span>Version</span>
                        </NxTooltip>
                      </NxTable.Cell>
                      <NxTable.Cell>Violations</NxTable.Cell>
                      <NxTable.Cell isNumeric>Components</NxTable.Cell>
                      <NxTable.Cell />
                    </NxTable.Row>
                  </NxTable.Head>
                  <NxTable.Body emptyMessage="No evaluations">
                    {applicationReportHistory.reports.map((evaluation) => (
                      <NxTable.Row key={evaluation.scanId}>
                        <NxTable.Cell>
                          {moment(evaluation.evaluationDate).format(STANDARD_DATE_TIME_FORMAT_NO_TZ)}
                        </NxTable.Cell>
                        <NxTable.Cell className="iq-application-latest-evaluations__trigger-cell">
                          <div className="iq-application-latest-evaluations__trigger-content">
                            <NxOverflowTooltip>
                              <span className="nx-truncate-ellipsis">{evaluation.scanTriggerTypeDisplayName}</span>
                            </NxOverflowTooltip>
                            {evaluation.isForMonitoring ? (
                              <span className="iq-application-latest-evaluations__continuous-monitoring">
                                {' (Continuous Monitoring)'}
                              </span>
                            ) : (
                              ''
                            )}
                          </div>
                        </NxTable.Cell>
                        <NxTable.Cell>{getScannerVersionToDisplay(evaluation)}</NxTable.Cell>
                        <NxTable.Cell>
                          <NxSmallThreatCounter
                            criticalCount={evaluation.policyEvaluationResult.criticalPolicyViolationCount}
                            severeCount={evaluation.policyEvaluationResult.severePolicyViolationCount}
                            moderateCount={evaluation.policyEvaluationResult.moderatePolicyViolationCount}
                          />
                        </NxTable.Cell>
                        <NxTable.Cell isNumeric>{evaluation.policyEvaluationResult.totalComponentCount}</NxTable.Cell>
                        <NxTable.Cell>
                          <NxTextLink
                            href={
                              isHrcMode
                                ? uiRouterState.href('hostedRepositoryComponentReport.policy', {
                                    hrcId,
                                    scanId: evaluation.scanId,
                                    // Forward componentDisplayName so the destination report renders
                                    // the friendly name instead of the raw hrcId.
                                    componentDisplayName,
                                  })
                                : uiRouterState.href('applicationReport.policy', {
                                    publicId: application.publicId,
                                    scanId: evaluation.scanId,
                                  })
                            }
                          >
                            View Report
                          </NxTextLink>
                        </NxTable.Cell>
                      </NxTable.Row>
                    ))}
                  </NxTable.Body>
                </NxTable>
              </NxTile.Content>
            </NxTile>
          </>
        )}
      </NxLoadWrapper>
    </NxPageMain>
  );
}
