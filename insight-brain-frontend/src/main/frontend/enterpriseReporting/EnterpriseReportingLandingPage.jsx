/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useState } from 'react';
import {
  NxFontAwesomeIcon,
  NxH1,
  NxH2,
  NxLoadWrapper,
  NxP,
  NxPageMain,
  NxPageTitle,
  NxTextLink,
  NxTooltip,
  NxCard,
  NxStatefulInfoAlert,
  NxWarningAlert,
} from '@sonatype/react-shared-components';
import { faInfoCircle } from '@fortawesome/free-solid-svg-icons';
import { faCalendar } from '@fortawesome/free-regular-svg-icons';
import { faLightbulbOn, faQuestionCircle } from '@fortawesome/pro-regular-svg-icons';

import classnames from 'classnames';
import { useDispatch, useSelector } from 'react-redux';

import { useRouterState } from 'MainRoot/react/RouterStateContext';
import EnterpriseReportCard from 'MainRoot/enterpriseReporting/card/EnterpriseReportCard';
import EnterpriseReportContactCard from 'MainRoot/enterpriseReporting/card/EnterpriseReportContactCard';
import EnterpriseReportingSupportInfo from 'MainRoot/enterpriseReporting/supportInfo/EnterpiseReportingSupportInfo';
import { selectEnterpriseReportingSupportInfo } from 'MainRoot/enterpriseReporting/supportInfo/enterpriseReportingSupportInfoSelectors';
import {
  selectEnterpriseReportingLandingPage,
  selectEnterpriseDashboards,
  selectDataInsightsDashboards,
} from 'MainRoot/enterpriseReporting/enterpriseReportingLandingPageSelectors';
import { actions } from 'MainRoot/enterpriseReporting/enterpriseReportingLandingPageSlice';
import { actions as dashboardActions } from 'MainRoot/enterpriseReporting/dashboard/enterpriseReportingDashboardSlice';
import {
  selectEnterpriseReportingLicenseError,
  selectLoadingFeatures,
} from 'MainRoot/productFeatures/productFeaturesSelectors';

export default function EnterpriseReportingLandingPage() {
  const dispatch = useDispatch();
  const routerState = useRouterState();
  const { iqVersion, loading, loadError } = useSelector(selectEnterpriseReportingLandingPage);
  const enterpriseDashboards = useSelector(selectEnterpriseDashboards);
  const dataInsightsDashboards = useSelector(selectDataInsightsDashboards);
  const { telemetryStatus, loading: loadingTelemetry, loadError: loadTelemetryError } = useSelector(
    selectEnterpriseReportingSupportInfo
  );
  const loadingFeatures = useSelector(selectLoadingFeatures);
  const isLoading = loading || loadingFeatures;
  const licenseError = useSelector(selectEnterpriseReportingLicenseError);
  const error = licenseError || loadError;

  // React2Shell banner dismissal state
  const [showReact2ShellBanner, setShowReact2ShellBanner] = useState(() => {
    return localStorage.getItem('react2shell-banner-dismissed') !== 'true';
  });

  const handleDismissReact2ShellBanner = () => {
    localStorage.setItem('react2shell-banner-dismissed', 'true');
    setShowReact2ShellBanner(false);
  };

  const load = () => {
    dispatch(dashboardActions.reset());
    dispatch(actions.load());
  };

  useEffect(() => {
    load();
  }, []);

  const enterpriseTooltipText = `Enterprise Reports are product features offering a holistic view of OSS usage, risks, and policy
    compliance using Sonatype solutions.`;

  const dataInsightsTooltipText = `Data Insights reveal specific/singular open-source trends and test data like EOL, AI/ML use, scoring
    and tech diversity.`;

  const statusIndicatorText = telemetryStatus.advancedReportingEnabled ? 'On' : 'Off';
  const statusIndicatorClassNames = classnames('nx-status-indicator', {
    'nx-status-indicator--positive': telemetryStatus.advancedReportingEnabled,
  });

  const boldFeatureText = (feature) =>
    feature
      .split(/(\*[^*]+\*)/g)
      .map((part, i) =>
        part.startsWith('*') && part.endsWith('*') ? <strong key={i}>{part.slice(1, -1)}</strong> : part
      );

  return (
    <NxPageMain id="enterprise-reporting-landing-page">
      <NxPageTitle id="enterprise-reporting-landing-page-title">
        <div className="iq-enterprise-reporting__header">
          <NxH1 id="enterprise-reporting-landing-page-heading" className="iq-enterprise-reporting__header__title">
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
        <NxPageTitle.Description
          id="enterprise-reporting-landing-page-description"
          className="iq-enterprise-reporting__page-description"
        >
          <NxP>
            If you have disabled Advanced Reporting, application names will be obfuscated. To see application names,{' '}
            <NxTextLink external href={'https://links.sonatype.com/products/nxiq/doc/data-insights-advanced-reporting'}>
              enable Advanced Reporting
            </NxTextLink>
            .
          </NxP>
          <NxP>
            Application names will also be obfuscated if you are using an older version of Lifecycle.{' '}
            <NxTextLink external href={'https://links.sonatype.com/products/clm/download'}>
              Update Lifecycle
            </NxTextLink>{' '}
            to resolve this issue.
          </NxP>
        </NxPageTitle.Description>
      </NxPageTitle>
      {showReact2ShellBanner && (
        <NxWarningAlert onClose={handleDismissReact2ShellBanner} className="iq-react2shell-banner">
          <strong>React2Shell:</strong> A severe flaw in React Server Components could allow attackers to run arbitrary
          code. Check your applications now to understand your exposure and remediate quickly.{' '}
          <NxTextLink href={routerState.href('react2ShellReport')}>React2Shell Impact Report</NxTextLink>
        </NxWarningAlert>
      )}
      <NxStatefulInfoAlert>
        Dashboards and Insights may appear incomplete and/or nonfunctional if there is insufficient data.
      </NxStatefulInfoAlert>
      <NxH2>
        <span className="iq-enterprise-reporting__dashboard-grouping__title">Enterprise Dashboards</span>
        <NxTooltip title={enterpriseTooltipText}>
          <NxFontAwesomeIcon icon={faInfoCircle} className="iq-enterprise-reporting__dashboard-grouping__icon" />
        </NxTooltip>
      </NxH2>
      <NxLoadWrapper loading={isLoading} retryHandler={load} error={error}>
        <NxCard.Container
          className="iq-enterprise-reporting-card__container"
          id="enterprise-reporting-dashboards--enterprise"
        >
          {enterpriseDashboards.map((dashboard, idx) => (
            <EnterpriseReportCard
              dashboard={
                dashboard.groupedDashboards
                  ? { ...dashboard, features: dashboard.features.map((f) => boldFeatureText(f)) }
                  : dashboard
              }
              key={idx}
              iqVersion={iqVersion}
            />
          ))}
        </NxCard.Container>
      </NxLoadWrapper>

      <NxH2>
        <span className="iq-enterprise-reporting__dashboard-grouping__title">Data Insights</span>
        <NxTooltip title={dataInsightsTooltipText}>
          <NxFontAwesomeIcon icon={faInfoCircle} className="iq-enterprise-reporting__dashboard-grouping__icon" />
        </NxTooltip>
      </NxH2>
      <NxLoadWrapper loading={isLoading} retryHandler={load} error={error}>
        <NxCard.Container
          className="iq-enterprise-reporting-card__container"
          id="enterprise-reporting-dashboards--insights"
        >
          {dataInsightsDashboards.map((dashboard, idx) => (
            <EnterpriseReportCard
              dashboard={
                dashboard.groupedDashboards
                  ? { ...dashboard, features: dashboard.features.map((f) => boldFeatureText(f)) }
                  : dashboard
              }
              key={idx}
              iqVersion={iqVersion}
            />
          ))}
        </NxCard.Container>
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
