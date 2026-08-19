/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import { formatTimeAgo } from 'MainRoot/util/dateUtils';
import { formatViolationRiskPercentage } from '../componentRiskUtils';
import { NxTable, NxTextLink, NxThreatIndicator, NxBinaryDonutChart } from '@sonatype/react-shared-components';
import { useRouterState } from 'MainRoot/react/RouterStateContext';

export default function ComponentRiskItemTable({ policyViolations, publicId, totalRisk }) {
  const uiRouterState = useRouterState();

  const renderStages = (stageDetails) => {
    const stages = new Map(stageDetails.map((stageDetail) => [stageDetail.stageTypeId, stageDetail]));

    return (
      <>
        {policyThreatStageCell(stages.get('source'))}
        {policyThreatStageCell(stages.get('build'))}
        {policyThreatStageCell(stages.get('stage-release'))}
        {policyThreatStageCell(stages.get('release'))}
        {policyThreatStageCell(stages.get('operate'))}
      </>
    );
  };

  const policyThreatStageCell = (stageDetail) => {
    if (!stageDetail) return <NxTable.Cell className="iq-component-risk-cell stage"></NxTable.Cell>;

    const { time, scanId } = stageDetail;
    const timeDiff = formatTimeAgo(time, true);
    return (
      <NxTable.Cell className="iq-component-risk-cell stage">
        {timeDiff && (
          <NxTextLink newTab href={uiRouterState.href('applicationReport.policy', { publicId, scanId })}>
            {timeDiff}
          </NxTextLink>
        )}
      </NxTable.Cell>
    );
  };

  const policyThreatRows = () => {
    if (policyViolations.length === 0) {
      return [];
    }
    return policyViolations.map(({ threatLevel, policyName, stageDetails }, index) => (
      <NxTable.Row key={index}>
        <NxTable.Cell className="iq-component-risk-cell">
          <NxThreatIndicator policyThreatLevel={threatLevel} />
          <span className="nx-threat-number">{threatLevel}</span>
        </NxTable.Cell>
        <NxTable.Cell className="iq-component-risk-cell">{policyName}</NxTable.Cell>
        <NxTable.Cell className="iq-component-risk-cell percentage">
          <NxBinaryDonutChart
            className="iq-component-risk-table-donut-chart"
            percent={totalRisk === 0 ? 0 : (threatLevel / totalRisk) * 100}
            innerRadiusPercent={0}
            role="presentation"
          />
          {formatViolationRiskPercentage(threatLevel, totalRisk)}
        </NxTable.Cell>
        <NxTable.Cell className="iq-component-risk-cell">{threatLevel}</NxTable.Cell>
        {renderStages(stageDetails)}
      </NxTable.Row>
    ));
  };

  return (
    <NxTable className="iq-component-risk-item-table">
      <NxTable.Head>
        <NxTable.Row>
          <NxTable.Cell>Threat</NxTable.Cell>
          <NxTable.Cell>Threat Policy</NxTable.Cell>
          <NxTable.Cell>Share of Risk</NxTable.Cell>
          <NxTable.Cell>Risk</NxTable.Cell>
          <NxTable.Cell>Source</NxTable.Cell>
          <NxTable.Cell>Build</NxTable.Cell>
          <NxTable.Cell>Stage</NxTable.Cell>
          <NxTable.Cell>Release</NxTable.Cell>
          <NxTable.Cell>Operate</NxTable.Cell>
        </NxTable.Row>
      </NxTable.Head>
      <NxTable.Body emptyMessage="No Results">{policyThreatRows()}</NxTable.Body>
    </NxTable>
  );
}

ComponentRiskItemTable.propTypes = {
  publicId: PropTypes.string.isRequired,
  totalRisk: PropTypes.number.isRequired,
  policyViolations: PropTypes.arrayOf(
    PropTypes.shape({
      policyId: PropTypes.string,
      policyName: PropTypes.string,
      stageDetails: PropTypes.arrayOf(
        PropTypes.shape({
          time: PropTypes.number,
          scanId: PropTypes.string,
          stageTypeName: PropTypes.string,
        })
      ),
      threatLevel: PropTypes.number,
      time: PropTypes.number,
    })
  ).isRequired,
};
