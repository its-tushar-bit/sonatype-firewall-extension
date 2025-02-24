/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import PropTypes from 'prop-types';
import { NxButton, NxH2, NxList, NxP, NxTag, NxTile, selectableColors } from '@sonatype/react-shared-components';
import { useDispatch } from 'react-redux';
import { stateGo } from 'MainRoot/reduxUiRouter/routerActions';
import { getEnterpriseReportingIconUrl } from 'MainRoot/util/CLMLocation';

export default function EnterpriseReportCard(props) {
  const { dashboard: dashboard } = props;

  const dispatch = useDispatch();

  const color = selectableColors.includes(dashboard.spotlightColor) ? dashboard.spotlightColor : selectableColors[4];
  const spotlightText = dashboard.spotlightText ? dashboard.spotlightText : 'NEW';
  const btnClassName = 'iq-enterprise-reporting__dashboard__btn dashboard-id-btn-' + dashboard.dashboardId;

  return (
    <div className="iq-enterprise-reporting__dashboard" role="enterprise-reporting-dashboard-card">
      <NxTile className="iq-enterprise-reporting__dashboard__tile">
        <NxTile.Header className="iq-enterprise-reporting__dashboard__header">
          <NxTile.HeaderTitle className="iq-enterprise-reporting__dashboard__header-title">
            <NxH2>{dashboard.title}</NxH2>
            {dashboard.spotlight || dashboard.spotlightText ? (
              <NxTag color={color} className="iq-enterprise-reporting__dashboard__spotlight">
                {spotlightText}
              </NxTag>
            ) : (
              ''
            )}
          </NxTile.HeaderTitle>
        </NxTile.Header>

        <NxTile.Content>
          <div className="iq-enterprise-reporting__dashboard-content">
            <div className="iq-enterprise-reporting__dashboard-icon">
              <img src={getEnterpriseReportingIconUrl(dashboard.previewImage)} alt="" />
            </div>
            <div className="iq-enterprise-reporting__dashboard-data">
              <NxP>{dashboard.description}</NxP>
              <NxList bulleted className="iq-enterprise-reporting__dashboard-data__features">
                {dashboard.features.map((f, idx) => (
                  <NxList.Item key={idx}>
                    <NxList.Text className="iq-enterprise-reporting__dashboard-data__feature">{f}</NxList.Text>
                  </NxList.Item>
                ))}
              </NxList>
              <NxButton
                variant="tertiary"
                className={btnClassName}
                onClick={() => dispatch(stateGo('enterpriseReportingDashboard', { id: dashboard.dashboardId }))}
              >
                {dashboard.accessButtonText}
              </NxButton>
            </div>
          </div>
        </NxTile.Content>
      </NxTile>
    </div>
  );
}

EnterpriseReportCard.propTypes = {
  dashboard: PropTypes.shape({
    dashboardId: PropTypes.string.isRequired,
    title: PropTypes.string.isRequired,
    spotlight: PropTypes.bool,
    spotlightColor: PropTypes.string,
    spotlightText: PropTypes.string,
    previewImage: PropTypes.string,
    description: PropTypes.string,
    features: PropTypes.arrayOf(PropTypes.string),
    accessButtonText: PropTypes.string,
  }).isRequired,
};
