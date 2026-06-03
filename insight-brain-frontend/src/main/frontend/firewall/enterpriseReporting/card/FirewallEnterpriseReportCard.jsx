/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import classNames from 'classnames';
import PropTypes from 'prop-types';
import {
  NxButton,
  NxH3,
  NxList,
  NxSmallTag,
  NxFontAwesomeIcon,
  NxTooltip,
  NxCard,
} from '@sonatype/react-shared-components';
import { fas } from '@fortawesome/free-solid-svg-icons';
import { fas as fasPro } from '@fortawesome/pro-solid-svg-icons';
import { useDispatch } from 'react-redux';

import { stateGo } from 'MainRoot/reduxUiRouter/routerActions';
import { smallTagColors } from 'MainRoot/enterpriseReporting/utils';
import './_firewallEnterpriseReportCard.scss';

export default function FirewallEnterpriseReportCard(props) {
  const { dashboard, iqVersion } = props;
  const isFirewallCategory = dashboard.category === 'firewall';

  const dispatch = useDispatch();

  const cardClassNames = classNames('iq-enterprise-reporting-card iq-enterprise-reporting-card--dashboard');

  const icon = fasPro[dashboard.previewImageIcon]
    ? fasPro[dashboard.previewImageIcon]
    : fas[dashboard.previewImageIcon];

  const cleanedIqVersion = iqVersion ? parseInt(iqVersion.split('.')[1]) : iqVersion;
  const isDashboardDisabled = (dashboard) => cleanedIqVersion < parseInt(dashboard.sinceIQVersion);
  const buttonDisabled = isDashboardDisabled(dashboard);

  const spotlightText = dashboard.spotlightText || 'NEW';

  const spotlightColor = smallTagColors.includes(dashboard.spotlightColor) ? dashboard.spotlightColor : 'teal';

  return (
    <div>
      <NxCard id={`fw-enterprise-reporting-dashboard-${dashboard.dashboardId}`} className={cardClassNames}>
        {dashboard.spotlight || dashboard.spotlightText ? (
          <NxSmallTag color={spotlightColor} className="iq-enterprise-reporting-card__spotlight">
            {spotlightText}
          </NxSmallTag>
        ) : null}
        <NxCard.Header className="iq-enterprise-reporting-card__header">
          <hgroup>
            <NxH3>{dashboard.title}</NxH3>
          </hgroup>
        </NxCard.Header>

        <NxCard.Content>
          <NxCard.CallOut className={classNames('fw-enterprise-report-card__icon', { firewall: isFirewallCategory })}>
            <NxFontAwesomeIcon icon={icon} />
          </NxCard.CallOut>
          <NxCard.Text>{dashboard.description}</NxCard.Text>
          <NxList bulleted className="iq-enterprise-reporting-card__features">
            {dashboard.features.map((f, idx) => (
              <NxList.Item key={idx}>
                <NxFontAwesomeIcon className={classNames({ firewall: isFirewallCategory })} icon={fas.faCheck} />
                <NxList.Text className="iq-enterprise-reporting-card__feature-item">{f}</NxList.Text>
              </NxList.Item>
            ))}
          </NxList>
        </NxCard.Content>
        <NxCard.Footer className="iq-enterprise-reporting-card__footer">
          <NxTooltip
            title={buttonDisabled ? `Upgrade to IQ version ${dashboard.sinceIQVersion} to access this insight` : null}
          >
            <span>
              <NxButton
                variant="tertiary"
                className={`iq-enterprise-reporting-card__button dashboard-id-btn-${dashboard.dashboardId}`}
                disabled={buttonDisabled}
                data-analytics-id={`fw-reporting-${dashboard.dashboardId}-view-cta`}
                onClick={() => {
                  dispatch(stateGo('firewall.enterpriseReportingDashboard', { id: dashboard.dashboardId }));
                }}
              >
                {dashboard.accessButtonText}
              </NxButton>
            </span>
          </NxTooltip>
        </NxCard.Footer>
      </NxCard>
    </div>
  );
}

FirewallEnterpriseReportCard.propTypes = {
  dashboard: PropTypes.shape({
    dashboardId: PropTypes.string,
    title: PropTypes.string,
    category: PropTypes.string,
    spotlight: PropTypes.bool,
    spotlightColor: PropTypes.string,
    spotlightText: PropTypes.string,
    previewImage: PropTypes.string,
    previewImageIcon: PropTypes.string,
    description: PropTypes.string,
    features: PropTypes.arrayOf(PropTypes.oneOfType([PropTypes.string, PropTypes.array])),
    accessButtonText: PropTypes.string,
    sinceIQVersion: PropTypes.string,
  }).isRequired,
  iqVersion: PropTypes.string.isRequired,
};
