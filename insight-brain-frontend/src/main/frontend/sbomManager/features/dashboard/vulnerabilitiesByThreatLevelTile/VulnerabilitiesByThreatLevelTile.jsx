/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import { reduce, keys, values } from 'ramda';
import { NxH2, NxH3, NxTile, NxThreatIndicator, NxTextLink } from '@sonatype/react-shared-components';
import { ResponsivePie } from '@nivo/pie';

import LoadWrapper from 'MainRoot/react/LoadWrapper';

import './VulnerabilitiesByThreatLevelTile.scss';

const NIVO_THREAT_COLORS_MAP = {
  critical: 'var(--nx-color-threat-critical)',
  severe: 'var(--nx-color-threat-severe)',
  moderate: 'var(--nx-color-threat-moderate)',
  low: 'var(--nx-color-threat-low)',
  none: 'var(--nx-color-threat-none)',
  unspecified: 'var(--nx-color-threat-unspecified)',
};

const VulnerabilitiesByThreatLevelPieChart = ({ data }) => (
  <div className="sbom-manager-vulnerability-by-threat-level-pie-chart">
    <ResponsivePie
      data={data}
      enableArcLabels={false}
      enableArcLinkLabels={false}
      isInteractive={false}
      cornerRadius={4}
      borderWidth={0}
      innerRadius={0.6}
      padAngle={2}
      colors={data.map(({ label }) => NIVO_THREAT_COLORS_MAP[label])}
    />
  </div>
);

VulnerabilitiesByThreatLevelPieChart.propTypes = {
  data: PropTypes.array.isRequired,
};

const VulnerabilitiesByThreatLevelTable = ({ data }) => {
  const threatLevels = keys(data);

  const tableRows = threatLevels.map((threatLevel) => {
    return (
      <tr key={threatLevel}>
        <td>
          <NxThreatIndicator threatLevelCategory={threatLevel} presentational />
          <span>{threatLevel[0].toUpperCase() + threatLevel.slice(1)}</span>
        </td>
        <td>{data[threatLevel].unannotated.toLocaleString('en-US')}</td>
        <td>{data[threatLevel].annotated.toLocaleString('en-US')}</td>
        <td>{data[threatLevel].total.toLocaleString('en-US')}</td>
      </tr>
    );
  });

  return (
    <table className="sbom-manager-vulnerabilities-by-threat-level-table">
      <thead>
        <tr>
          <th>Threat Level</th>
          <th>Unannotated</th>
          <th>Annotated</th>
          <th>Total</th>
        </tr>
      </thead>
      <tbody>{tableRows}</tbody>
    </table>
  );
};

VulnerabilitiesByThreatLevelTable.propTypes = {
  data: PropTypes.object.isRequired,
};

export default function VulnerabilitiesByThreatLevelTile() {
  const doLoad = () => {};

  const data = {
    critical: {
      unannotated: 811,
      annotated: 423,
      total: 1234,
    },
    severe: {
      unannotated: 1100,
      annotated: 1100,
      total: 2200,
    },
    moderate: {
      unannotated: 734,
      annotated: 3500,
      total: 4234,
    },
    low: {
      unannotated: 760,
      annotated: 1100,
      total: 1860,
    },
  };

  const totalValues = reduce(
    (acc, value) => {
      acc.unannotated = acc.unannotated + value.unannotated;
      acc.annotated = acc.annotated + value.annotated;
      acc.total = acc.total + value.total;
      return acc;
    },
    { unannotated: 0, annotated: 0, total: 0 },
    values(data)
  );

  const chartData = Object.keys(data).map((threat) => ({ id: threat, label: threat, value: data[threat].unannotated }));

  return (
    <NxTile id="vulnerabilities-by-threat-level-tile" className="sbom-manager-vulnerabilities-by-threat-level-tile">
      <NxTile.Header>
        <NxTile.HeaderTitle>
          <NxH2>Vulnerabilities by Threat Level</NxH2>
          <NxH3>(all time)</NxH3>
        </NxTile.HeaderTitle>
      </NxTile.Header>
      <NxTile.Content>
        <LoadWrapper retryHandler={doLoad} error={null}>
          <ul className="sbom-manager-vulnerabilities-by-threat-level-list">
            <li className="sbom-manager-vulnerabilities-by-threat-level-list__item">
              <span>Total:</span>
              <span>{totalValues.total.toLocaleString('en-US')}</span>
            </li>
            <li className="sbom-manager-vulnerabilities-by-threat-level-list__item">
              <span>Unannotated:</span>
              <span>{totalValues.unannotated.toLocaleString('en-US')}</span>
            </li>
            <li className="sbom-manager-vulnerabilities-by-threat-level-list__item">
              <span>Annotated:</span>
              <span>{totalValues.annotated.toLocaleString('en-US')}</span>
            </li>
          </ul>
          <VulnerabilitiesByThreatLevelPieChart data={chartData} />
          <VulnerabilitiesByThreatLevelTable data={data} />
          <div className="sbom-manager-vulnerabilities-by-threat-level-tile__action">
            <NxTextLink href="#">View Applications by most vulnerabilities</NxTextLink>
          </div>
        </LoadWrapper>
      </NxTile.Content>
    </NxTile>
  );
}
