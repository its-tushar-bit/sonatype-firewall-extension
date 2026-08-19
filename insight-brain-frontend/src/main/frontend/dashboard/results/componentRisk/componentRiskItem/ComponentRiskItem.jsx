/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import {
  NxTile,
  NxBinaryDonutChart,
  NxTextLink,
  NxStatefulAccordion,
  NxAccordion,
} from '@sonatype/react-shared-components';
import { useRouterState } from 'MainRoot/react/RouterStateContext';
import { formatTimeAgo } from 'MainRoot/util/dateUtils';
import ComponentRiskItemTable from 'MainRoot/dashboard/results/componentRisk/componentRiskItem/ComponentRiskItemTable';
import { formatViolationRiskPercentage } from '../componentRiskUtils';

export default function ComponentRiskItem({ totalRisk, application, risk, stageDetails, policyViolations }) {
  const uiRouterState = useRouterState();
  const { name, publicId } = application;

  const getListDataContent = (time, scanId) => {
    const timeDiff = formatTimeAgo(time, true);
    return (
      <dd className="iq-component-risk-data-element-content">
        {timeDiff && (
          <NxTextLink newTab href={uiRouterState.href('applicationReport.policy', { publicId, scanId })}>
            {timeDiff}
          </NxTextLink>
        )}
      </dd>
    );
  };

  const listItemData = stageDetails.map(({ stageTypeName, time, scanId }) => (
    <div key={stageTypeName} className="iq-component-risk-data-element">
      <dt className="iq-component-risk-data-element-title">{stageTypeName.toUpperCase()}</dt>
      {getListDataContent(time, scanId)}
    </div>
  ));

  return (
    <NxTile className="iq-component-risk-item-content" aria-label={name + ' component risk'}>
      <div className="iq-component-risk-application-block">
        <h3 className="iq-component-risk-application-name nx-h3">{name}</h3>
        <dl className="iq-component-risk-data">
          <div className="iq-component-risk-data-element">
            <dt className="iq-component-risk-data-element-title">SHARE OF RISK</dt>
            <dd className="iq-component-risk-data-element-content">
              <NxBinaryDonutChart
                percent={totalRisk === 0 ? 0 : (risk / totalRisk) * 100}
                innerRadiusPercent={0}
                role="presentation"
              />
              {formatViolationRiskPercentage(risk, totalRisk)}
            </dd>
          </div>
          <div className="iq-component-risk-data-element">
            <dt className="iq-component-risk-data-element-title">RISK</dt>
            <dd className="iq-component-risk-data-element-content">{risk}</dd>
          </div>
          {listItemData}
        </dl>
      </div>
      <NxStatefulAccordion className="iq-component-risk-application-accordion" defaultOpen={false}>
        <NxAccordion.Header>
          <NxAccordion.Title className="iq-component-risk-application-accordion-header">
            Risk breakdown by policy
          </NxAccordion.Title>
        </NxAccordion.Header>
        <ComponentRiskItemTable policyViolations={policyViolations} publicId={publicId} totalRisk={totalRisk} />
      </NxStatefulAccordion>
    </NxTile>
  );
}

ComponentRiskItem.propTypes = {
  totalRisk: PropTypes.number,
  application: PropTypes.shape({
    name: PropTypes.string,
    publicId: PropTypes.string,
  }).isRequired,
  risk: PropTypes.number.isRequired,
  stageDetails: PropTypes.arrayOf(
    PropTypes.shape({
      time: PropTypes.number,
      scanId: PropTypes.string,
      stageTypeName: PropTypes.string,
    })
  ).isRequired,
  policyViolations: PropTypes.array.isRequired,
};
