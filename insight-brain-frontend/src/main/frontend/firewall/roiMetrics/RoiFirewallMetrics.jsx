/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import * as PropTypes from 'prop-types';
import { faInfoCircle } from '@fortawesome/pro-solid-svg-icons';
import {
  NxH2,
  NxTextLink,
  NxTile,
  NxGrid,
  NxFontAwesomeIcon,
  NxTooltip,
  NxLoadWrapper,
} from '@sonatype/react-shared-components';
import { selectRoiFirewallMetrics } from './roiFirewallMetricsSelectors';
import { actions } from './roiFirewallMetricsSlice';
import './_roiFirewallMetrics.scss';

const RoiFirewallMetricsContent = ({ id, title, tooltipTitle, value }) => (
  <div className="roi-firewall-metrics-content" id={`roi-firewall-metrics-content__${id}`}>
    <header className="roi-firewall-metrics-content__header">
      <h2 className="roi-firewall-metrics-content__title" data-testId={`roi-firewall-metrics-content__title__${id}`}>
        <span>{title}</span>
        <NxTooltip data-testId={`roi-firewall-metrics-content__tool-tip-title__${id}`} title={tooltipTitle}>
          <NxFontAwesomeIcon className="roi-firewall-metrics-content__icon" icon={faInfoCircle} />
        </NxTooltip>
      </h2>
    </header>
    <div className="roi-firewall-metrics-content__value" data-testId={`roi-firewall-metrics-content__value__${id}`}>
      <span>${value.toLocaleString('en-US')}</span>
    </div>
  </div>
);

RoiFirewallMetricsContent.propTypes = {
  id: PropTypes.string.isRequired,
  title: PropTypes.string.isRequired,
  tooltipTitle: PropTypes.string.isRequired,
  value: PropTypes.number.isRequired,
};

export default function RoiFirewallMetrics() {
  const dispatch = useDispatch();
  const loadMetrics = () => dispatch(actions.loadMetrics());
  const {
    loading,
    error,
    hasConfigureSystemPermission,
    total,
    supplyChainAttacksBlocked,
    namespaceAttacksBlocked,
    safeVersionsSelected,
  } = useSelector(selectRoiFirewallMetrics);

  const roiDescriptionText = hasConfigureSystemPermission
    ? 'The metrics below highlights the Return on Investment (ROI) of your organization’s partnership with Sonatype. ' +
      'Configure the values for each category based on your industry to provide accurate results. '
    : 'The metrics below highlights the Return on Investment (ROI) of your organization’s partnership with Sonatype.';

  useEffect(() => {
    loadMetrics();
  }, []);

  return (
    <div id="roi-firewall-metrics" data-testId="roi-firewall-metrics">
      <NxLoadWrapper loading={loading} error={error} retryHandler={loadMetrics}>
        <header>
          <NxH2 className="roi-firewall-metrics__title" data-testId="roi-firewall-metrics-title">
            Return on Investment (ROI)
          </NxH2>
          <span className="roi-firewall-metrics__description" data-testId="roi-firewall-metrics-description">
            {roiDescriptionText}
            {hasConfigureSystemPermission && <NxTextLink href="#">Configure ROI values</NxTextLink>}
          </span>
        </header>
        <div className="roi-firewall-metrics__total" data-testId="roi-firewall-metrics-total">
          Total USD Saved
          <span>${total.toLocaleString('en-US')}</span>
        </div>
        <NxTile id="roi-firewall-metrics__tile" className="nx-grid roi-firewall-metrics__tile">
          <NxGrid.Row>
            <NxGrid.Column className="nx-grid-col--33 roi-firewall-metrics-column">
              <RoiFirewallMetricsContent
                id="malware-attacks-prevented"
                title="Malware attacks prevented"
                tooltipTitle="Determined based on the number of Malware attacks prevented and the ROI value configured per attack."
                value={supplyChainAttacksBlocked}
              />
            </NxGrid.Column>
            <NxGrid.Column className="nx-grid-col--33 roi-firewall-metrics-column">
              <RoiFirewallMetricsContent
                id="namespace-attacks-prevented"
                title="Namespace attacks prevented"
                tooltipTitle="Determined based on the number of namespace attacks protected and the ROI value configured per attack."
                value={namespaceAttacksBlocked}
              />
            </NxGrid.Column>
            <NxGrid.Column className="nx-grid-col--33 roi-firewall-metrics-column">
              <RoiFirewallMetricsContent
                id="safe-components-auto-selected"
                title="Safe Components Auto-selected"
                tooltipTitle="Determined based on the number of safe components auto-selected and the ROI value configured per attack."
                value={safeVersionsSelected}
              />
            </NxGrid.Column>
          </NxGrid.Row>
        </NxTile>
      </NxLoadWrapper>
    </div>
  );
}
