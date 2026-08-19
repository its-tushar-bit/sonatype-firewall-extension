/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import * as PropTypes from 'prop-types';
import { omit, sum } from 'ramda';

import { NxH2, NxH3, NxProgressBar, NxTile } from '@sonatype/react-shared-components';
import classNames from 'classnames';
import MetadataAccordion from 'MainRoot/sbomManager/features/billOfMaterials/metadataAccordion/MetadataAccordion';

import PieChart from './summaryTilePieChart/summaryTilePieChart';
import SummaryTileReleaseStatus from './summaryTileReleaseStatus/SummaryTileReleaseStatus';

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
  severe: 'var(--nx-color-threat-severe)',
  medium: 'var(--nx-color-threat-moderate)',
  moderate: 'var(--nx-color-threat-moderate)',
  low: 'var(--nx-color-threat-low)',
};

const SummaryChartAndProgress = ({ id, title, data, colorMap, isSbomPoliciesSupported }) => {
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

  const classes = classNames('sbom-manager-summary-chart-and-progress', {
    'sbom-manager-summary-chart-and-progress--with-policies': isSbomPoliciesSupported,
  });

  return (
    <div id={id} className={classes}>
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
  isSbomPoliciesSupported: PropTypes.bool.isRequired,
};

export default function BillOfMaterialSummaryTile(props) {
  const {
    releaseStatusPercentage,
    componentSummary,
    vulnerabilitiesSummary,
    policyViolationSummary,
    isSbomPoliciesSupported,
  } = props;

  const componentSummaryData = {
    ...componentSummary,
    total: sum(Object.values(componentSummary)),
  };

  const vulnerabilitiesSummaryData = {
    ...vulnerabilitiesSummary,
    total: sum(Object.values(vulnerabilitiesSummary)),
  };

  const policyViolationsSummaryData = {
    ...policyViolationSummary,
    total: sum(Object.values(policyViolationSummary)),
  };

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
            isSbomPoliciesSupported={isSbomPoliciesSupported}
          />
          <div className="sbom-manager-bill-of-materials-summary-tile__summaries__divider"></div>
          <SummaryChartAndProgress
            id="bill-of-materials-summary-tile-chart-and-progress-vulnerability-summary"
            title="Vulnerabilities Summary"
            data={vulnerabilitiesSummaryData}
            colorMap={NIVO_VULNERABILITIES_SUMMARY_COLOR_MAP}
            isSbomPoliciesSupported={isSbomPoliciesSupported}
          />
          <div className="sbom-manager-bill-of-materials-summary-tile__summaries__divider"></div>
          <SummaryTileReleaseStatus percentage={releaseStatusPercentage} />
          {isSbomPoliciesSupported && (
            <>
              <div className="sbom-manager-bill-of-materials-summary-tile__summaries__divider"></div>
              <SummaryChartAndProgress
                id="bill-of-materials-summary-tile-chart-and-progress-policy-violation-summary"
                title="Policy Violation Summary"
                data={policyViolationsSummaryData}
                colorMap={NIVO_VULNERABILITIES_SUMMARY_COLOR_MAP}
                isSbomPoliciesSupported={isSbomPoliciesSupported}
              />
            </>
          )}
        </div>
      </NxTile.Content>
      <MetadataAccordion />
    </NxTile>
  );
}

BillOfMaterialSummaryTile.propTypes = {
  releaseStatusPercentage: PropTypes.number,
  componentSummary: PropTypes.object.isRequired,
  vulnerabilitiesSummary: PropTypes.object.isRequired,
  policyViolationSummary: PropTypes.object.isRequired,
  isSbomPoliciesSupported: PropTypes.bool.isRequired,
};
