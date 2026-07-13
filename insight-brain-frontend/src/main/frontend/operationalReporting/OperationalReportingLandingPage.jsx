/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import './_operationalReportingLandingPage.scss';
import {
  NxFontAwesomeIcon,
  NxH1,
  NxH2,
  NxLoadWrapper,
  NxP,
  NxPageMain,
  NxPageTitle,
  NxCard,
  NxTooltip,
} from '@sonatype/react-shared-components';
import { faInfoCircle } from '@fortawesome/free-solid-svg-icons';
import { faCalendar } from '@fortawesome/free-regular-svg-icons';
import { faLightbulbOn, faQuestionCircle } from '@fortawesome/pro-regular-svg-icons';
import { useDispatch, useSelector } from 'react-redux';

import React2ShellReportCard from 'MainRoot/enterpriseReporting/card/React2ShellReportCard';
import EnterpriseReportContactCard from 'MainRoot/enterpriseReporting/card/EnterpriseReportContactCard';
import { actions as productFeaturesActions } from 'MainRoot/productFeatures/productFeaturesSlice';
import { selectLoadingFeatures, selectLoadErrorFeatures } from 'MainRoot/productFeatures/productFeaturesSelectors';

export default function OperationalReportingLandingPage() {
  const dispatch = useDispatch();
  const loading = useSelector(selectLoadingFeatures);
  const loadError = useSelector(selectLoadErrorFeatures);

  const load = () => {
    dispatch(productFeaturesActions.fetchProductFeaturesIfNeeded());
  };

  useEffect(() => {
    load();
  }, []);

  return (
    <NxPageMain id="operational-reporting-landing-page">
      <NxPageTitle id="operational-reporting-landing-page-title">
        <NxH1 id="operational-reporting-landing-page-heading">Operational Reporting</NxH1>
        <NxPageTitle.Description id="operational-reporting-landing-page-description">
          <NxP>
            Operational Reporting provides immediate, real-time insight into your activities. These reports are
            generated directly from your local instance data. Use these reports to make timely decisions, track critical
            issues, and take immediate action.
          </NxP>
        </NxPageTitle.Description>
      </NxPageTitle>
      <NxH2>
        <span className="iq-enterprise-reporting__dashboard-grouping__title">Rapid Response Reports</span>
        <NxTooltip title="Rapid Response Reports provide quick insights into critical security vulnerabilities affecting your organization.">
          <NxFontAwesomeIcon icon={faInfoCircle} className="iq-enterprise-reporting__dashboard-grouping__icon" />
        </NxTooltip>
      </NxH2>
      <NxLoadWrapper loading={loading} retryHandler={load} error={loadError}>
        <NxCard.Container className="iq-enterprise-reporting-card__container">
          <React2ShellReportCard />
        </NxCard.Container>
      </NxLoadWrapper>

      <NxH2>Contact Us</NxH2>
      <NxCard.Container className="iq-enterprise-reporting-card__container iq-enterprise-reporting__contactus">
        <EnterpriseReportContactCard
          icon={faCalendar}
          title="Schedule a Discussion"
          description="Book a session with our team to talk about these insights."
          buttonText="Email Us"
          linkUrl="mailto:data-insights-pm@sonatype.com"
          external={false}
        />
        <EnterpriseReportContactCard
          icon={faLightbulbOn}
          title="Suggest an Improvement"
          description="Let us know how we can optimize what you see here."
          buttonText="Explore the Ideas Portal"
          linkUrl="http://links.sonatype.com/products/nxiq/feedback/data-insights-ideas"
          external={true}
        />
        <EnterpriseReportContactCard
          icon={faQuestionCircle}
          title="Receive Technical Support"
          description="Reach out to connect with our experts about issues."
          buttonText="Explore Support"
          linkUrl="http://links.sonatype.com/products/nexus/pro/support"
          external={true}
        />
      </NxCard.Container>
    </NxPageMain>
  );
}
