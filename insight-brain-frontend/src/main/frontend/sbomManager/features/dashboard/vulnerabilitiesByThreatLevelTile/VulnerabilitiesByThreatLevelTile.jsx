/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import * as PropTypes from 'prop-types';
import { useDispatch, useSelector } from 'react-redux';
import { all, compose, equals, keys, map, prop } from 'ramda';
import { NxH2, NxH3, NxTile, NxThreatIndicator, NxTextLink } from '@sonatype/react-shared-components';
import { ResponsivePie } from '@nivo/pie';

import { capitalize } from 'MainRoot/util/jsUtil';
import { formatNumberLocale } from 'MainRoot/util/formatUtils';
import LoadWrapper from 'MainRoot/react/LoadWrapper';

import { selectVulnerabilitiesByThreatLevelTile } from './vulnerabilitiesByThreatLevelTileSelectors';
import { actions } from './vulnerabilitiesByThreatLevelTileSlice';

import './VulnerabilitiesByThreatLevelTile.scss';
import { SORT_BY_FIELDS } from '../../sbomApplicationsPage/sbomApplicationsTable/sbomApplicationsTableSlice';
import { SORT_DIRECTION } from '../recentlyImportedSbomsTile/recentlyImportedSbomsTileSlice';
import { useRouterState } from 'MainRoot/react/RouterStateContext';

const NIVO_THREAT_COLORS_MAP = {
  critical: 'var(--nx-color-threat-critical)',
  high: 'var(--nx-color-threat-severe)',
  medium: 'var(--nx-color-threat-moderate)',
  low: 'var(--nx-color-threat-low)',
  none: 'var(--nx-color-threat-none)',
  unspecified: 'var(--nx-color-threat-unspecified)',
};

const NIVO_COMPLEMENT_COLOR_MAP = {
  complement: 'var(--nx-color-progress-background)',
};

const THREAT_LEVEL_MAP = {
  critical: 'critical',
  high: 'severe',
  medium: 'moderate',
  low: 'low',
};

const EMPTY_CHART_DATA = [
  {
    id: 'complement',
    label: 'complement',
    value: 100,
  },
];

const VulnerabilitiesByThreatLevelPieChart = ({ vulnerabilities }) => {
  const vulnerabilitiesChartData = map(
    (threat) => ({
      id: threat,
      label: threat,
      value: vulnerabilities[threat].unannotated,
    }),
    keys(vulnerabilities)
  );

  const chartData = compose(all(equals(0)), map(prop('value')))(vulnerabilitiesChartData)
    ? EMPTY_CHART_DATA
    : vulnerabilitiesChartData;

  const colorMap = {
    ...NIVO_THREAT_COLORS_MAP,
    ...NIVO_COMPLEMENT_COLOR_MAP,
  };

  return (
    <div className="sbom-manager-vulnerability-by-threat-level-pie-chart">
      <ResponsivePie
        data={chartData}
        enableArcLabels={false}
        enableArcLinkLabels={false}
        isInteractive={false}
        cornerRadius={4}
        borderWidth={0}
        innerRadius={0.6}
        padAngle={2}
        colors={map(({ label }) => colorMap[label], chartData)}
      />
    </div>
  );
};

VulnerabilitiesByThreatLevelPieChart.propTypes = {
  vulnerabilities: PropTypes.object.isRequired,
};

const VulnerabilitiesByThreatLevelTable = ({ vulnerabilities }) => {
  const tableRows = Object.entries(vulnerabilities).map(([threatLevel, value]) => (
    <tr key={threatLevel}>
      <td>
        <NxThreatIndicator threatLevelCategory={THREAT_LEVEL_MAP[threatLevel]} presentational />
        <span>{capitalize(threatLevel)}</span>
      </td>
      <td>{formatNumberLocale(value.unannotated)}</td>
      <td>{formatNumberLocale(value.annotated)}</td>
      <td>{formatNumberLocale(value.total)}</td>
    </tr>
  ));

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
  vulnerabilities: PropTypes.object.isRequired,
};

export default function VulnerabilitiesByThreatLevelTile() {
  const dispatch = useDispatch();
  const { loading, loadError, vulnerabilities, vulnerabilitiesTotal } = useSelector(
    selectVulnerabilitiesByThreatLevelTile
  );
  const uiRouterState = useRouterState();
  const load = () => dispatch(actions.loadVulnerabilitesByThreatLevel());

  useEffect(() => {
    load();
  }, []);

  const applicationsPageHref = uiRouterState.href('sbomManager.applications', {
    sortBy: SORT_BY_FIELDS.vulnerabilities,
    sortDirection: SORT_DIRECTION.DESC,
  });

  return (
    <NxTile id="vulnerabilities-by-threat-level-tile" className="sbom-manager-vulnerabilities-by-threat-level-tile">
      <NxTile.Header>
        <NxTile.HeaderTitle>
          <NxH2>Vulnerabilities by Threat Level</NxH2>
          <NxH3>(all time)</NxH3>
        </NxTile.HeaderTitle>
      </NxTile.Header>
      <NxTile.Content>
        <LoadWrapper retryHandler={() => load()} loading={loading} error={loadError}>
          <ul className="sbom-manager-vulnerabilities-by-threat-level-tile__list">
            <li
              className="sbom-manager-vulnerabilities-by-threat-level-tile__list__item"
              data-testid="vulnerabilities-by-threat-level-tile-total"
            >
              <span>Total:</span>
              <span>{formatNumberLocale(vulnerabilitiesTotal.totalVulnerabilities)}</span>
            </li>
            <li
              className="sbom-manager-vulnerabilities-by-threat-level-tile__list__item"
              data-testid="vulnerabilities-by-threat-level-tile-total-unannotated"
            >
              <span>Unannotated:</span>
              <span>{formatNumberLocale(vulnerabilitiesTotal.totalVulnerabilitiesUnannotated)}</span>
            </li>
            <li
              className="sbom-manager-vulnerabilities-by-threat-level-tile__list__item"
              data-testid="vulnerabilities-by-threat-level-tile-total-annotated"
            >
              <span>Annotated:</span>
              <span>{formatNumberLocale(vulnerabilitiesTotal.totalVulnerabilitiesAnnotated)}</span>
            </li>
          </ul>
          <VulnerabilitiesByThreatLevelPieChart vulnerabilities={vulnerabilities} />
          <VulnerabilitiesByThreatLevelTable vulnerabilities={vulnerabilities} />
          <div className="sbom-manager-vulnerabilities-by-threat-level-tile__action">
            <NxTextLink className="sbom-manager-vulnerabilities-by-threat-level-tile__link" href={applicationsPageHref}>
              View Applications by most vulnerabilities
            </NxTextLink>
          </div>
        </LoadWrapper>
      </NxTile.Content>
    </NxTile>
  );
}
