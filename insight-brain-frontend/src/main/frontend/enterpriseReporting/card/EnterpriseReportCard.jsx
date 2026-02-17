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
  NxStatefulSegmentedButton,
} from '@sonatype/react-shared-components';
import { tail } from 'ramda';
import { fas } from '@fortawesome/free-solid-svg-icons';
import { fas as fasPro } from '@fortawesome/pro-solid-svg-icons';
import { useDispatch } from 'react-redux';

import { stateGo } from 'MainRoot/reduxUiRouter/routerActions';
import {
  smallTagColors,
  getUpgradeVersion,
  isElementDisabled,
  isRetiringDashboard,
  getFormattedRetirementDate,
} from '../utils';
import './_enterpriseReportCard.scss';

export default function EnterpriseReportCard(props) {
  const { dashboard, iqVersion } = props;
  const isEnterprise = dashboard.category === 'enterprise';
  const isGroupCard = !!dashboard.groupedDashboards;

  const dispatch = useDispatch();

  const retiringRegex = /retiring/i;
  const isRetiring =
    isRetiringDashboard(dashboard.dashboardId) &&
    dashboard.spotlightText &&
    retiringRegex.test(dashboard.spotlightText);

  const cardClassNames = classNames('iq-enterprise-reporting-card iq-enterprise-reporting-card--dashboard', {
    retiring: isRetiring,
  });

  const icon = fas[dashboard.previewImageIcon] ? fas[dashboard.previewImageIcon] : fasPro[dashboard.previewImageIcon];

  const cleanedIqVersion = parseInt(iqVersion.split('.')[1]);
  const isDashboardDisabled = (dashboard) => cleanedIqVersion < parseInt(dashboard.sinceIQVersion);
  const buttonDisabled = isElementDisabled(dashboard, isDashboardDisabled);

  const spotlightText = dashboard.spotlightText || 'NEW';

  const spotlightColor = smallTagColors.includes(dashboard.spotlightColor)
    ? dashboard.spotlightColor
    : isEnterprise
    ? 'teal'
    : 'purple';

  const onButtonClick = (dashId) => {
    dispatch(stateGo('enterpriseReportingDashboardGroup', { id: dashId, groupId: dashboard.groupId }));
  };

  return (
    <div>
      <NxCard
        id={`enterprise-reporting-dashboard-${dashboard.dashboardId || dashboard.groupId}`}
        className={cardClassNames}
        role="enterprise-reporting-dashboard-card"
      >
        {isRetiring && (
          <div
            className="iq-enterprise-reporting-card__retirement-date"
            aria-label={`Retirement date: ${getFormattedRetirementDate()}`}
          >
            <NxFontAwesomeIcon icon={fas.faCalendar} />
            <span>{getFormattedRetirementDate()}</span>
          </div>
        )}
        {dashboard.spotlight || dashboard.spotlightText ? (
          <NxSmallTag color={spotlightColor} className="iq-enterprise-reporting-card__spotlight">
            {spotlightText}
          </NxSmallTag>
        ) : (
          ''
        )}
        <NxCard.Header className="iq-enterprise-reporting-card__header">
          <hgroup>
            <NxH3>{dashboard.title}</NxH3>
          </hgroup>
        </NxCard.Header>

        <NxCard.Content>
          <NxCard.CallOut className={classNames('iq-enterprise-reporting-card__icon', { enterprise: isEnterprise })}>
            <NxFontAwesomeIcon icon={icon} />
          </NxCard.CallOut>
          <NxCard.Text>{dashboard.description}</NxCard.Text>
          <NxList bulleted className="iq-enterprise-reporting-card__features">
            {dashboard.features.map((f, idx) => (
              <NxList.Item key={idx}>
                <NxFontAwesomeIcon className={isEnterprise && 'enterprise'} icon={fas.faCheck} />
                <NxList.Text className="iq-enterprise-reporting-card__feature-item">{f}</NxList.Text>
              </NxList.Item>
            ))}
          </NxList>
        </NxCard.Content>
        <NxCard.Footer className="iq-enterprise-reporting-card__footer">
          <NxTooltip
            title={buttonDisabled && `Upgrade to IQ version ${getUpgradeVersion(dashboard)} to access this insight`}
          >
            <span>
              {isGroupCard ? (
                <NxStatefulSegmentedButton
                  buttonContent={dashboard.groupedDashboards[0]?.accessButtonText}
                  variant="tertiary"
                  onClick={() => onButtonClick(dashboard.groupedDashboards[0]?.dashboardId)}
                  disabled={buttonDisabled}
                  className={`iq-enterprise-reporting-card__button dashboard-id-btn-${dashboard.groupedDashboards[0].dashboardId}`}
                >
                  {tail(dashboard.groupedDashboards).map((dash) => (
                    <button
                      key={dash.dashboardId}
                      className={`nx-dropdown-button dashboard-id-btn-${dash.dashboardId}`}
                      onClick={() => onButtonClick(dash.dashboardId)}
                    >
                      {dash.accessButtonText}
                    </button>
                  ))}
                </NxStatefulSegmentedButton>
              ) : (
                <NxButton
                  variant="tertiary"
                  className={`iq-enterprise-reporting-card__button dashboard-id-btn-${dashboard.dashboardId}`}
                  disabled={buttonDisabled}
                  onClick={() => {
                    // Special route for HeroDevs EOL
                    if (dashboard.dashboardId === 'herodevs_eol') {
                      dispatch(stateGo('heroDevsEol'));
                    } else {
                      dispatch(stateGo('enterpriseReportingDashboard', { id: dashboard.dashboardId }));
                    }
                  }}
                  data-analytics-id={
                    dashboard.dashboardId === 'herodevs_eol' ? 'lc-reporting-herodevs-view-cta' : undefined
                  }
                >
                  {dashboard.accessButtonText}
                </NxButton>
              )}
            </span>
          </NxTooltip>
        </NxCard.Footer>
      </NxCard>
    </div>
  );
}

EnterpriseReportCard.propTypes = {
  dashboard: PropTypes.shape({
    dashboardId: PropTypes.string,
    title: PropTypes.string,
    category: PropTypes.string,
    groupId: PropTypes.string,
    spotlight: PropTypes.bool,
    spotlightColor: PropTypes.string,
    spotlightText: PropTypes.string,
    previewImage: PropTypes.string,
    previewImageIcon: PropTypes.string,
    description: PropTypes.string,
    features: PropTypes.arrayOf(PropTypes.oneOfType([PropTypes.string, PropTypes.array])),
    accessButtonText: PropTypes.string,
    sinceIQVersion: PropTypes.string,
    groupedDashboards: PropTypes.arrayOf(PropTypes.object),
  }).isRequired,
  iqVersion: PropTypes.string.isRequired,
};
