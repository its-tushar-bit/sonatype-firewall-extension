/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import {
  NxPageTitle,
  NxH1,
  NxPageMain,
  NxLoadWrapper,
  NxStatefulInfoAlert,
  NxStatefulWarningAlert,
  NxH2,
  NxCard,
  NxTextLink,
} from '@sonatype/react-shared-components';
import { faCalendar } from '@fortawesome/free-regular-svg-icons';
import { faLightbulbOn, faQuestionCircle } from '@fortawesome/pro-regular-svg-icons';
import { selectEnterpriseReportingSupportInfo } from 'MainRoot/enterpriseReporting/supportInfo/enterpriseReportingSupportInfoSelectors';
import {
  selectDashboards,
  selectLoading,
  selectLoadError,
  selectIqVersion,
} from './firewallEnterpriseReportingSelectors';
import { actions } from './firewallEnterpriseReportingSlice';
import EnterpriseReportContactCard from 'MainRoot/enterpriseReporting/card/EnterpriseReportContactCard';
import FirewallEnterpriseReportCard from './card/FirewallEnterpriseReportCard';
import EnterpriseReportingSupportInfo from 'MainRoot/enterpriseReporting/supportInfo/EnterpiseReportingSupportInfo';
import classnames from 'classnames';

export default function EnterpriseReportingPage() {
  const dispatch = useDispatch();

  /** @type {Array} */
  const dashboards = useSelector(selectDashboards) || [];
  /** @type {boolean} */
  const loading = useSelector(selectLoading) || false;
  const loadError = useSelector(selectLoadError);

  // Load all data on component mount
  useEffect(() => {
    dispatch(actions.loadDashboards());
  }, [dispatch]);

  const retryLoad = () => {
    dispatch(actions.loadDashboards());
  };

  const { telemetryStatus, loading: loadingTelemetry, loadError: loadTelemetryError } = useSelector(
    selectEnterpriseReportingSupportInfo
  );

  const iqVersion = useSelector(selectIqVersion);
  const statusIndicatorText = telemetryStatus?.advancedReportingEnabled ? 'On' : 'Off';
  const statusIndicatorClassNames = classnames('nx-status-indicator', {
    'nx-status-indicator--positive': telemetryStatus?.advancedReportingEnabled,
  });

  return (
    <NxPageMain id="fw-enterprise-reporting-page">
      <NxPageTitle id="fw-enterprise-reporting-page-title">
        <div className="iq-fw-enterprise-reporting__header">
          <NxH1 id="fw-enterprise-reporting-landing-page-heading" className="iq-fw-enterprise-reporting__header__title">
            Enterprise Reporting
          </NxH1>
          {!loadTelemetryError && !loadingTelemetry && (
            <div className="iq-enterprise-reporting__advanced-reporting">
              <span className={statusIndicatorClassNames} role="status">
                Advanced Reporting: {statusIndicatorText}
              </span>
              <NxTextLink
                external
                href={'https://links.sonatype.com/products/nxiq/doc/data-insights-advanced-reporting'}
              >
                What&apos;s this?
              </NxTextLink>
            </div>
          )}
        </div>
      </NxPageTitle>

      <div className="iq-fw-enterprise-reporting__alerts-container">
        <NxStatefulWarningAlert>
          <strong>React2Shell:</strong> A severe flaw in the React Server Components could allow attackers to run
          arbitrary code. Check your Repository instances now to understand your exposure and remediate quickly.
        </NxStatefulWarningAlert>
        <NxStatefulInfoAlert>
          Dashboards and Insights may appear incomplete and/or nonfunctional if there is insufficient data.
        </NxStatefulInfoAlert>
      </div>

      <NxH2>Data Insights</NxH2>
      <NxLoadWrapper loading={loading} retryHandler={retryLoad} error={loadError}>
        {dashboards.length > 0 && (
          <NxCard.Container
            className="iq-enterprise-reporting-card__container"
            id="fw-enterprise-reporting-dash-insights-container"
          >
            {dashboards.map((dashboard) => (
              <FirewallEnterpriseReportCard dashboard={dashboard} key={dashboard.dashboardId} iqVersion={iqVersion} />
            ))}
          </NxCard.Container>
        )}
      </NxLoadWrapper>
      <NxH2 className="iq-enterprise-reporting__header--contact">Contact Us</NxH2>
      <NxCard.Container className="iq-enterprise-reporting-card__container iq-enterprise-reporting__contactus">
        <EnterpriseReportContactCard
          icon={faCalendar}
          title={'Schedule a Discussion'}
          description={'Book a session with our team to talk about these insights.'}
          buttonText={'Email Us'}
          linkUrl={'mailto:data-insights-pm@sonatype.com'}
          external={false}
        />
        <EnterpriseReportContactCard
          icon={faLightbulbOn}
          title={'Suggest an Improvement'}
          description={'Let us know how we can optimize what you see here.'}
          buttonText={'Explore the Ideas Portal'}
          linkUrl={'http://links.sonatype.com/products/nxiq/feedback/data-insights-ideas'}
          external={true}
        />
        <EnterpriseReportContactCard
          icon={faQuestionCircle}
          title={'Receive Technical Support'}
          description={'Reach out to connect with our experts about issues.'}
          buttonText={'Explore Support'}
          linkUrl={'http://links.sonatype.com/products/nexus/pro/support'}
          external={true}
        />
      </NxCard.Container>
      <EnterpriseReportingSupportInfo />
    </NxPageMain>
  );
}
