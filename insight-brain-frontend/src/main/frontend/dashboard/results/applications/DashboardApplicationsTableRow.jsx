/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Fragment } from 'react';
import { NxOverflowTooltip, NxTableCell, NxTableRow } from '@sonatype/react-shared-components';

import * as PropTypes from 'prop-types';
import DashboardHeatMapCell, { heatMapColorStylerPropTypes } from '../DashboardHeatMapCell';
import DashboardApplicationsTableStageRiskRow, {
  applicationRiskPropTypes,
} from './DashboardApplicationsTableStageRiskRow';

export default function DashboardApplicationsTableRow(props) {
  const { application, colorStyler, tableRowIndex } = props,
    { totalApplicationRisk, stageRisks } = application,
    automationId = `app${tableRowIndex}_`,
    isLastStageRisk = (currentRiskIndex) => currentRiskIndex === stageRisks.length - 1;

  const mainRow = (
    <NxTableRow id={automationId} className="iq-dashboard-application-row">
      <NxTableCell>
        <NxOverflowTooltip>
          <div className="nx-truncate-ellipsis">{application.applicationName}</div>
        </NxOverflowTooltip>
      </NxTableCell>
      <DashboardHeatMapCell colorStyler={colorStyler}>{totalApplicationRisk.totalRisk}</DashboardHeatMapCell>
      <DashboardHeatMapCell colorStyler={colorStyler}>{totalApplicationRisk.criticalRisk}</DashboardHeatMapCell>
      <DashboardHeatMapCell colorStyler={colorStyler}>{totalApplicationRisk.severeRisk}</DashboardHeatMapCell>
      <DashboardHeatMapCell colorStyler={colorStyler}>{totalApplicationRisk.moderateRisk}</DashboardHeatMapCell>
      <DashboardHeatMapCell colorStyler={colorStyler}>{totalApplicationRisk.lowRisk}</DashboardHeatMapCell>
    </NxTableRow>
  );

  return (
    <Fragment>
      {mainRow}
      {stageRisks.map((stageRisk, index) => (
        <DashboardApplicationsTableStageRiskRow
          stageRisk={stageRisk}
          applicationId={application.applicationId}
          key={stageRisk.scanId}
          isLastStageRisk={isLastStageRisk(index)}
          appAutomationId={automationId}
        />
      ))}
    </Fragment>
  );
}

const stageRisksPropTypes = PropTypes.shape({
  stageTypeName: PropTypes.string.isRequired,
  scanId: PropTypes.string.isRequired,
  risk: applicationRiskPropTypes,
});

export const applicationPropTypes = PropTypes.shape({
  applicationId: PropTypes.string.isRequired,
  applicationName: PropTypes.string.isRequired,
  totalApplicationRisk: applicationRiskPropTypes,
  stageRisks: PropTypes.arrayOf(stageRisksPropTypes),
});

DashboardApplicationsTableRow.propTypes = {
  application: applicationPropTypes,
  colorStyler: heatMapColorStylerPropTypes,
  tableRowIndex: PropTypes.number.isRequired,
};
