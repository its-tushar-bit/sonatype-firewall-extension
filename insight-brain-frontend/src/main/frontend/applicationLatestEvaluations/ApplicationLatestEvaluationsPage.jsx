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

export default function ApplicationLatestEvaluationsPage() {
  const { applicationPublicId, stageId } = useSelector(selectRouterCurrentParams);
  const { loading, loadError, application, applicationReportHistory } = useSelector(
    selectApplicationLatestEvaluationsSlice
  );
  const { prevState, prevParams } = useSelector(selectRouterSlice);

  const dispatch = useDispatch();
  const uiRouterState = useRouterState();

  const load = () => {
    dispatch(actions.load({ applicationPublicId, stageId }));
  };

  const backHref =
    prevState?.name === 'applicationReport.policy'
      ? uiRouterState.href('applicationReport.policy', prevParams)
      : uiRouterState.href('violations');

  useEffect(load, []);

  return (
    <NxPageMain id="application-latest-evaluations-page">
      <MenuBarBackButton
        href={backHref}
        text={prevState?.name === 'applicationReport.policy' ? 'Back to Application Report' : 'All Reports'}
      />
      <NxLoadWrapper loading={loading} error={loadError} retryHandler={load}>
        {application && stageId && applicationReportHistory && (
          <>
            <NxPageTitle>
              <NxH1>{application.name} Latest Evaluations</NxH1>
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
                            href={uiRouterState.href('applicationReport.policy', {
                              publicId: application.publicId,
                              scanId: evaluation.scanId,
                            })}
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
