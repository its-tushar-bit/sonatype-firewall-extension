/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import * as PropTypes from 'prop-types';
import { omit, sum } from 'ramda';
import { ResponsivePie } from '@nivo/pie';
import { NxH2, NxH3, NxP, NxProgressBar, NxTile } from '@sonatype/react-shared-components';
import classNames from 'classnames';
import MetadataAccordion from 'MainRoot/sbomManager/features/billOfMaterials/metadataAccordion/MetadataAccordion';

import { capitalize } from 'MainRoot/util/jsUtil';
import { formatNumberLocale } from 'MainRoot/util/formatUtils';

import './SummaryTile.scss';

const NIVO_COMPONENT_SUMMARY_COLOR_MAP = {
  direct: 'var(--nx-swatch-blue-40)',
  transitive: 'var(--nx-swatch-purple-40)',
  unspecified: 'var(--nx-swatch-teal-80)',
};

const NIVO_VULNERABILITIES_SUMMARY_COLOR_MAP = {
  critical: 'var(--nx-color-threat-critical)',
  high: 'var(--nx-color-threat-severe)',
  medium: 'var(--nx-color-threat-moderate)',
  low: 'var(--nx-color-threat-low)',
};

const NIVO_ANNOTATED_VULNERABILITIES_PERCENTAGE_COLOR_MAP = {
  percentage: 'var(--nx-swatch-teal-40)',
};

const NIVO_COMPLEMENT_COLOR_MAP = {
  complement: 'var(--nx-color-progress-background)',
};

const NIVO_EMPTY_CHART_DATA = [{ id: 'complement', label: 'complement', value: 100 }];

const PieChart = ({ total, data, colorMap }) => {
  const totalValue =
    typeof total === 'number' ? (
      <NxH3 className="sbom-manager-bill-of-materials-summary-pie-chart__total" data-testid="pie-chart-total">
        {formatNumberLocale(total)}
      </NxH3>
    ) : null;

  const rawChartData = Object.values(data).length === 1 ? { complement: 100 - Object.values(data)[0], ...data } : data;

  const isEmptyChart = Object.values(data).every((value) => value === 0 || value === null);
  const chartData = isEmptyChart
    ? NIVO_EMPTY_CHART_DATA
    : Object.keys(rawChartData).map((field) => ({ id: field, label: field, value: rawChartData[field] }));

  const combinedColorMap = {
    ...colorMap,
    ...NIVO_COMPLEMENT_COLOR_MAP,
  };

  return (
    <div className="sbom-manager-bill-of-materials-summary-pie-chart">
      <ResponsivePie
        data={chartData}
        enableArcLabels={false}
        enableArcLinkLabels={false}
        isInteractive={false}
        cornerRadius={4}
        borderWidth={0}
        innerRadius={0.7}
        padAngle={2}
        colors={chartData.map(({ label }) => combinedColorMap[label])}
      />
      {totalValue}
    </div>
  );
};

PieChart.propTypes = {
  total: PropTypes.number,
  data: PropTypes.object.isRequired,
  colorMap: PropTypes.object.isRequired,
};

const SummaryChartAndProgress = ({ id, title, data, colorMap }) => {
  const total = data.total;
  const dataFields = omit(['total'], data);

  const progressBars = Object.keys(dataFields).map((field) => {
    const value = data[field];
    const percentage = !value || !total ? 0 : (value / total) * 100;

    const progressClasses = classNames(
      'sbom-manager-summary-chart-and-progress__progress-bar',
      `sbom-manager-summary-chart-and-progress__progress-bar--${field}`
    );

    const labelText = `${formatNumberLocale(value)} ${capitalize(field)}`;

    return (
      <div className="sbom-manager-summary-chart-and-progress__progress-bar-wrapper" key={field}>
        <span className="sbom-manager-summary-chart-and-progress__label">{labelText}</span>
        <NxProgressBar label={labelText} className={progressClasses} value={percentage} variant="inline" />
      </div>
    );
  });

  return (
    <div id={id} className="sbom-manager-summary-chart-and-progress">
      <NxH3>{title}</NxH3>
      <section>
        <div className="sbom-manager-summary-chart-and-progress__chart-container">
          <PieChart total={total} data={dataFields} colorMap={colorMap} />
        </div>
        <div className="sbom-manager-summary-chart-and-progress__progress-bars">{progressBars}</div>
      </section>
    </div>
  );
};

SummaryChartAndProgress.propTypes = {
  id: PropTypes.string.isRequired,
  title: PropTypes.string.isRequired,
  data: PropTypes.object.isRequired,
  colorMap: PropTypes.object.isRequired,
};

export default function BillOfMaterialSummaryTile(props) {
  const { annotatedVulnerabilitesPercentage, componentSummary, vulnerabilitiesSummary } = props;

  const componentSummaryData = {
    ...componentSummary,
    total: sum(Object.values(componentSummary)),
  };

  const vulnerabilitiesSummaryData = {
    ...vulnerabilitiesSummary,
    total: sum(Object.values(vulnerabilitiesSummary)),
  };

  const annotatedVulnerabilitiesData = {
    percentage: annotatedVulnerabilitesPercentage,
  };

  const annotatedVulnerabilitiesText =
    typeof annotatedVulnerabilitesPercentage === 'number' ? (
      <>
        <strong>{annotatedVulnerabilitesPercentage}%</strong> of vulnerabilities annotated with exploitability{' '}
        information
      </>
    ) : (
      'No vulnerabilities to annotate'
    );

  return (
    <NxTile id="bill-of-materials-summary-tile" className="sbom-manager-bill-of-materials-summary-tile">
      <NxTile.Header>
        <NxTile.HeaderTitle>
          <NxH2>Bill of Material</NxH2>
        </NxTile.HeaderTitle>
      </NxTile.Header>
      <NxTile.Content>
        <div className="sbom-manager-bill-of-materials-summary-tile__summaries">
          <SummaryChartAndProgress
            id="bill-of-materials-summary-tile-chart-and-progress-component-summary"
            title="Component Summary"
            data={componentSummaryData}
            colorMap={NIVO_COMPONENT_SUMMARY_COLOR_MAP}
          />
          <div className="sbom-manager-bill-of-materials-summary-tile__summaries__divider"></div>
          <SummaryChartAndProgress
            id="bill-of-materials-summary-tile-chart-and-progress-vulnerability-summary"
            title="Vulnerabilities Summary"
            data={vulnerabilitiesSummaryData}
            colorMap={NIVO_VULNERABILITIES_SUMMARY_COLOR_MAP}
          />
          <div className="sbom-manager-bill-of-materials-summary-tile__annotated-vulnerabilities-summary">
            <PieChart
              data={annotatedVulnerabilitiesData}
              colorMap={NIVO_ANNOTATED_VULNERABILITIES_PERCENTAGE_COLOR_MAP}
            />
            <NxP
              className="sbom-manager-bill-of-materials-summary-tile__annotated-vulnerabilities-summary__description"
              data-testid="annotated-vulnerabilities-summary-description"
            >
              {annotatedVulnerabilitiesText}
            </NxP>
          </div>
        </div>
      </NxTile.Content>
      <MetadataAccordion />
    </NxTile>
  );
}

BillOfMaterialSummaryTile.propTypes = {
  annotatedVulnerabilitesPercentage: PropTypes.number,
  componentSummary: PropTypes.object.isRequired,
  vulnerabilitiesSummary: PropTypes.object.isRequired,
};
