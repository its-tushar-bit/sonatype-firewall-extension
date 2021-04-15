/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useContext } from 'react';

import { NxFontAwesomeIcon, NxTableCell, NxTableRow } from '@sonatype/react-shared-components';
import * as PropTypes from 'prop-types';
import { faExternalLink } from '@fortawesome/pro-regular-svg-icons';
import RouterStateContext from '../../../react/RouterStateContext';
import classNames from 'classnames';

export default function DashboardApplicationsTableStageRiskRow(props) {
  const uiRouterState = useContext(RouterStateContext);

  const { applicationId, stageRisk, isLastStageRisk, appAutomationId } = props,
    stageRowAutomationId = `${appAutomationId}stage${stageRisk.scanId}`,
    reportRef = uiRouterState.href('applicationReport.policy', {
      publicId: applicationId,
      scanId: stageRisk.scanId,
    });

  const rowClassNames = classNames('iq-dashboard-application-risk-row', {
    'iq-dashboard-application-risk-row--separator-border': isLastStageRisk,
  });

  return (
    <NxTableRow id={stageRowAutomationId} className={rowClassNames} key={stageRisk.scanId}>
      <NxTableCell>
        <a href={reportRef} target="_blank" rel="noopener noreferrer">
          <NxFontAwesomeIcon icon={faExternalLink} />
          {stageRisk.stageTypeName}
        </a>
      </NxTableCell>
      <NxTableCell className="nx-cell--num">{stageRisk.risk.totalRisk}</NxTableCell>
      <NxTableCell className="nx-cell--num">{stageRisk.risk.criticalRisk}</NxTableCell>
      <NxTableCell className="nx-cell--num">{stageRisk.risk.severeRisk}</NxTableCell>
      <NxTableCell className="nx-cell--num">{stageRisk.risk.moderateRisk}</NxTableCell>
      <NxTableCell className="nx-cell--num">{stageRisk.risk.lowRisk}</NxTableCell>
    </NxTableRow>
  );
}

export const applicationRiskPropTypes = PropTypes.shape({
  totalRisk: PropTypes.number.isRequired,
  criticalRisk: PropTypes.number.isRequired,
  severeRisk: PropTypes.number.isRequired,
  moderateRisk: PropTypes.number.isRequired,
  lowRisk: PropTypes.number.isRequired,
});

DashboardApplicationsTableStageRiskRow.propTypes = {
  applicationId: PropTypes.string.isRequired,
  stageRisk: PropTypes.shape({
    stageTypeName: PropTypes.string.isRequired,
    scanId: PropTypes.string.isRequired,
    risk: applicationRiskPropTypes,
  }),
  isLastStageRisk: PropTypes.bool,
  appAutomationId: PropTypes.string.isRequired,
};
