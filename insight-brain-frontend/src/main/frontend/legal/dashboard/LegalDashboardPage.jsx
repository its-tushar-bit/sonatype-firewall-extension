/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { NxButton, NxStatefulTabs, NxTab, NxTabList, NxTabPanel } from '@sonatype/react-shared-components';
import LegalDashboardApplicationsTab from './LegalDashboardApplicationsTab';
import LegalDashboardComponentsTab from './LegalDashboardComponentsTab';
import * as PropTypes from 'prop-types';
import LegalDashboardFilterContainer from './LegalDashboardFilterContainer';
import LoadWrapper from '../../react/LoadWrapper';
import { applicationPropType } from '../advancedLegalPropTypes';

export default function LegalDashboardPage(props) {
  const {
    applications,
    components,
    loadApplications,
    loading,
    loadError,
    isAuthorized
  } = props;

  useEffect(() => {
    if (isAuthorized) {
      loadApplications();
    }
  }, []);

  const authErrorMessage = `It appears you do not have permission to access this page.
    If you believe this to be incorrect, please contact your administrator.`;

  const hasError = isAuthorized ? loadError : authErrorMessage;

  return (
    <LoadWrapper loading={ loading } error={ hasError } retryHandler={ loadApplications }>
      <aside id="legal-dashboard-filter-container" className="nx-page-sidebar">
        <LegalDashboardFilterContainer />
      </aside>
      <main id="legal-dashboard-container" className="nx-page-main nx-viewport-sized">
        <div className="nx-page-title nx-page-title__actions">
          <h1 className="nx-h1">Legal Obligations</h1>
          <div className="nx-btn-bar">
            <NxButton variant="primary">Create Attribution Report</NxButton>
          </div>
        </div>
        <div className="nx-tile nx-viewport-sized__container">
          <div className="nx-tile-content nx-viewport-sized__container">
            <NxStatefulTabs className="nx-viewport-sized__container" defaultActiveTab={0} onTabSelect={() => {}}>
              <NxTabList>
                <NxTab>Applications</NxTab>
                <NxTab>Components</NxTab>
              </NxTabList>
              <NxTabPanel className="nx-viewport-sized__container">
                <LegalDashboardApplicationsTab applications = { applications } />
              </NxTabPanel>
              <NxTabPanel className="nx-viewport-sized__container">
                <LegalDashboardComponentsTab components = { components } />
              </NxTabPanel>
            </NxStatefulTabs>
          </div>
        </div>
      </main>
    </LoadWrapper>
  );
}

LegalDashboardPage.propTypes = {
  applications: PropTypes.arrayOf(applicationPropType),
  components: PropTypes.any,
  loadApplications: PropTypes.func,
  isAuthorized: PropTypes.bool,
  loading: PropTypes.bool.isRequired,
  loadError: LoadWrapper.propTypes.error
};
