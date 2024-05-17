/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import {
  NxH1,
  NxH2,
  NxH3,
  NxLoadWrapper,
  NxP,
  NxPageMain,
  NxPageTitle,
  NxTextLink,
  NxTile,
} from '@sonatype/react-shared-components';

import EnterpriseReportCard from 'MainRoot/enterpriseReporting/card/EnterpriseReportCard';
import { useDispatch, useSelector } from 'react-redux';
import {
  selectDashboardsData,
  selectError,
  selectLoading,
} from 'MainRoot/enterpriseReporting/enterpriseReportingLandingPageSelectors';
import { actions } from 'MainRoot/enterpriseReporting/enterpriseReportingLandingPageSlice';
import { actions as dashboardActions } from 'MainRoot/enterpriseReporting/dashboard/enterpriseReportingDashboardSlice';
import {
  selectDataInsightsLicenseError,
  selectLoadingFeatures,
} from 'MainRoot/productFeatures/productFeaturesSelectors';

export default function EnterpriseReportingLandingPage() {
  const dispatch = useDispatch();
  const dashboardsData = useSelector(selectDashboardsData);
  const loading = useSelector(selectLoading);
  const loadingFeatures = useSelector(selectLoadingFeatures);
  const isLoading = loading || loadingFeatures;
  const apiError = useSelector(selectError);
  const licenseError = useSelector(selectDataInsightsLicenseError);
  const error = licenseError || apiError;
  const load = () => {
    dispatch(dashboardActions.reset());
    dispatch(actions.load());
  };

  useEffect(() => {
    load();
  }, []);

  return (
    <NxPageMain id="enterprise-reporting-landing-page">
      <NxPageTitle id="enterprise-reporting-landing-page-title">
        <NxPageTitle.Headings>
          <NxH1 id="enterprise-reporting-landing-page-heading">Data Insights</NxH1>
        </NxPageTitle.Headings>
        <NxPageTitle.Description id="enterprise-reporting-landing-page-description">
          <NxP>
            The following visualizations have been generated to help you better understand your product usage, start
            conversations about new product features, and reveal what is happening within your organizations. Click the
            buttons below to view or use the links listed to share your feedback with our team.
          </NxP>
        </NxPageTitle.Description>
      </NxPageTitle>

      <NxLoadWrapper loading={isLoading} retryHandler={load} error={error}>
        <div className="iq-enterprise-reporting__dashboards-container" id="enterprise-reporting-dashboards-container">
          {dashboardsData
            ? dashboardsData.map((dashboard, idx) => <EnterpriseReportCard dashboard={dashboard} key={idx} />)
            : ''}

          <div className="iq-enterprise-reporting__contactus">
            <NxTile>
              <NxTile.Header>
                <NxTile.HeaderTitle>
                  <NxH2>Contact Us</NxH2>
                </NxTile.HeaderTitle>
              </NxTile.Header>
              <NxTile.Content>
                <NxTile.Subsection>
                  <NxTile.SubsectionHeader>
                    <NxH3>Schedule a Discussion</NxH3>
                  </NxTile.SubsectionHeader>
                  <NxP>
                    Book a session with our team to talk about these insights.
                    <br />
                    <NxTextLink
                      href="mailto:data-insights-pm@sonatype.com"
                      className="enterprise-reporting-contact-us-schedule-session"
                    >
                      data-insights-pm@sonatype.com
                    </NxTextLink>
                  </NxP>
                </NxTile.Subsection>

                <NxTile.Subsection>
                  <NxTile.SubsectionHeader>
                    <NxH3>Suggest an Improvement</NxH3>
                  </NxTile.SubsectionHeader>
                  <NxP>
                    Send an email about how we can better what you see here.
                    <br />
                    <NxTextLink
                      href="http://links.sonatype.com/products/nxiq/feedback/data-insights-ideas"
                      external
                      className="enterprise-reporting-contact-us-suggest-improvement"
                    >
                      Sonatype Ideas Portal - Data Insights
                    </NxTextLink>
                  </NxP>
                </NxTile.Subsection>

                <NxTile.Subsection>
                  <NxTile.SubsectionHeader>
                    <NxH3>Receive Technical Support</NxH3>
                  </NxTile.SubsectionHeader>
                  <NxP>
                    Use the email to connect with our experts about issues.
                    <br />
                    <NxTextLink
                      href="http://links.sonatype.com/products/nexus/pro/support"
                      external
                      className="enterprise-reporting-contact-us-tech-support"
                    >
                      support.sonatype.com
                    </NxTextLink>
                  </NxP>
                </NxTile.Subsection>
              </NxTile.Content>
            </NxTile>
          </div>
        </div>
      </NxLoadWrapper>
    </NxPageMain>
  );
}
