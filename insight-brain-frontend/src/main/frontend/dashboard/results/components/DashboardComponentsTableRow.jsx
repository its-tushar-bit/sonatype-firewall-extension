/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { NxOverflowTooltip, NxTableCell, NxTableRow } from '@sonatype/react-shared-components';

import * as PropTypes from 'prop-types';
import DashboardHeatMapCell, { heatMapColorStylerPropTypes } from '../DashboardHeatMapCell';

export default function DashboardComponentsTableRow(props) {
  const { component, stateGo, colorStyler } = props;

  const goToComponentDetails = () => {
    stateGo('dashboard.component', {
      hash: component.hash,
    });
  };

  return (
    <NxTableRow className="iq-dashboard-component-row" onClick={goToComponentDetails} isClickable>
      <NxTableCell>
        <NxOverflowTooltip>
          <div className="nx-truncate-ellipsis">{component.derivedComponentName}</div>
        </NxOverflowTooltip>
      </NxTableCell>
      <NxTableCell className={'nx-cell--num'}>{component.affectedApplications}</NxTableCell>
      <DashboardHeatMapCell colorStyler={colorStyler}>{component.score}</DashboardHeatMapCell>
      <DashboardHeatMapCell colorStyler={colorStyler}>{component.scoreCritical}</DashboardHeatMapCell>
      <DashboardHeatMapCell colorStyler={colorStyler}>{component.scoreSevere}</DashboardHeatMapCell>
      <DashboardHeatMapCell colorStyler={colorStyler}>{component.scoreModerate}</DashboardHeatMapCell>
      <DashboardHeatMapCell colorStyler={colorStyler}>{component.scoreLow}</DashboardHeatMapCell>
      <DashboardHeatMapCell threatScore={component.scoreLow} colorStyler={colorStyler} chevron></DashboardHeatMapCell>
    </NxTableRow>
  );
}

export const componentPropTypes = PropTypes.shape({
  hash: PropTypes.string.isRequired,
  derivedComponentName: PropTypes.string.isRequired,
  affectedApplications: PropTypes.number.isRequired,
  score: PropTypes.number.isRequired,
  scoreCritical: PropTypes.number.isRequired,
  scoreSevere: PropTypes.number.isRequired,
  scoreModerate: PropTypes.number.isRequired,
  scoreLow: PropTypes.number.isRequired,
});

DashboardComponentsTableRow.propTypes = {
  component: componentPropTypes,
  stateGo: PropTypes.func.isRequired,
  colorStyler: heatMapColorStylerPropTypes,
};
