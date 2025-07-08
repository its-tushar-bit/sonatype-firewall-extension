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
import './_enterpriseReportCard.scss';

export default function EnterpriseReportCard(props) {
  const { dashboard: dashboard, iqVersion } = props;
  const isEnterprise = dashboard.category === 'enterprise';

  const dispatch = useDispatch();

  const retiringRegex = /retiring/i;
  const cardClassNames = classNames('iq-enterprise-reporting-card iq-enterprise-reporting-card--dashboard', {
    retiring: retiringRegex.test(dashboard.spotlightText),
  });

  const iconContainerClassName = classNames('iq-enterprise-reporting-card__icon', {
    enterprise: isEnterprise,
  });
  const iconPositioningAdjustment = ['success-metrics', 'ai-consumption', 'rolling-recap', 'dependency-scorecard'];
  const iconClassName = iconPositioningAdjustment.includes(dashboard.dashboardId) && 'custom-positioning';
  const icon = fas[dashboard.previewImageIcon] ? fas[dashboard.previewImageIcon] : fasPro[dashboard.previewImageIcon];

  const cleanedIqVersion = parseInt(iqVersion.split('.')[1]);
  const disabled = dashboard.sinceIQVersion && cleanedIqVersion < parseInt(dashboard.sinceIQVersion);

  const spotlightText = dashboard.spotlightText ? dashboard.spotlightText : 'NEW';
  const smallTagColors = ['blue', 'green', 'indigo', 'orange', 'pink', 'purple', 'red', 'teal', 'turquoise'];

  const spotlightColor =
    dashboard.spotlightColor && smallTagColors.includes(dashboard.spotlightColor)
      ? dashboard.spotlightColor
      : isEnterprise
      ? 'teal'
      : 'purple';

  return (
    <div>
      <NxCard
        id={`enterprise-reporting-dashboard-${dashboard.dashboardId}`}
        className={cardClassNames}
        role="enterprise-reporting-dashboard-card"
      >
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
          <NxCard.CallOut className={iconContainerClassName}>
            <NxFontAwesomeIcon icon={icon} className={iconClassName} />
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
          <NxTooltip title={disabled && `Upgrade to IQ version ${dashboard.sinceIQVersion} to access this insight`}>
            <span>
              <NxButton
                variant="tertiary"
                className={`iq-enterprise-reporting-card__button dashboard-id-btn-${dashboard.dashboardId}`}
                disabled={disabled}
                onClick={() => dispatch(stateGo('enterpriseReportingDashboard', { id: dashboard.dashboardId }))}
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

EnterpriseReportCard.propTypes = {
  dashboard: PropTypes.shape({
    dashboardId: PropTypes.string.isRequired,
    title: PropTypes.string.isRequired,
    category: PropTypes.string,
    spotlight: PropTypes.bool,
    spotlightColor: PropTypes.string,
    spotlightText: PropTypes.string,
    previewImage: PropTypes.string,
    previewImageIcon: PropTypes.string,
    description: PropTypes.string,
    features: PropTypes.arrayOf(PropTypes.string),
    accessButtonText: PropTypes.string,
    sinceIQVersion: PropTypes.string,
  }).isRequired,
  iqVersion: PropTypes.string.isRequired,
};
