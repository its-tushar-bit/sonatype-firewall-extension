/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { NxButton, NxStatefulTabs, NxTab, NxTabList, NxTabPanel } from '@sonatype/react-shared-components';
import LegalDashboardApplicationsTab from './LegalDashboardApplicationsTab';
import LegalDashboardComponentsTab from './LegalDashboardComponentsTab';
import * as PropTypes from 'prop-types';
import LegalDashboardFilterContainer from './filter/LegalDashboardFilterContainer';
import LoadWrapper from '../../react/LoadWrapper';
import { applicationsTabPropType } from '../advancedLegalPropTypes';

export default function LegalDashboardPage(props) {
  const {
    applications,
    components,
    filtersAreDirty,
    loadResults,
    loading,
    loadError,
    fetchBackendPage,
    changeSortField
  } = props;

  return (
    <div id="legal-dashboard" className="nx-page-content">
      <LoadWrapper loading={ loading } error={ loadError } retryHandler={ loadResults }>
        <aside id="legal-dashboard-filter-container" className="nx-page-sidebar">
          <LegalDashboardFilterContainer />
        </aside>
        <main id="legal-dashboard-container" className="nx-page-main">
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
                  <LegalDashboardApplicationsTab applications = { applications }
                                                 fetchBackendPage = { fetchBackendPage }
                                                 filtersAreDirty = { filtersAreDirty }
                                                 changeSortField = { changeSortField } />
                </NxTabPanel>
                <NxTabPanel className="nx-viewport-sized__container">
                  <LegalDashboardComponentsTab components = { components }
                                               filtersAreDirty = { filtersAreDirty } />
                </NxTabPanel>
              </NxStatefulTabs>
            </div>
          </div>
        </main>
      </LoadWrapper>
    </div>
  );
}

LegalDashboardPage.propTypes = {
  applications: applicationsTabPropType,
  components: PropTypes.any,
  filtersAreDirty: PropTypes.bool,
  loadResults: PropTypes.func,
  isAuthorized: PropTypes.bool,
  loading: PropTypes.bool.isRequired,
  loadError: LoadWrapper.propTypes.error,
  fetchBackendPage: PropTypes.func.isRequired,
  changeSortField: PropTypes.func.isRequired
};
