/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import {
  NxButton,
  NxFontAwesomeIcon,
  NxStatefulTabs,
  NxTab,
  NxTabList,
  NxTabPanel,
} from '@sonatype/react-shared-components';
import LegalDashboardApplicationsTab from './LegalDashboardApplicationsTab';
import LegalDashboardComponentsTab from './LegalDashboardComponentsTab';
import * as PropTypes from 'prop-types';
import LegalDashboardFilterContainer from './filter/LegalDashboardFilterContainer';
import LoadWrapper from '../../react/LoadWrapper';
import { applicationsTabPropType } from '../advancedLegalPropTypes';
import { faFilter } from '@fortawesome/pro-solid-svg-icons';
import { DEFAULT_FILTER_NAME } from './filter/defaultFilter';

export default function LegalDashboardPage(props) {
  const {
    appliedFilterName,
    applications,
    components,
    filtersAreDirty,
    loadFilter,
    loadResults,
    loading,
    loadError,
    fetchBackendPage,
    changeSortField,
    stateGo,
    toggleFilterSidebar,
    filterSidebarOpen,
    showDirtyAsterisk,
    filterLoading,
    router,
  } = props;

  const tabIndexes = ['applications', 'components'];
  const componentTabEnabled = router.currentParams.legalComponentsTabEnabled;

  useEffect(() => {
    loadFilter();
  }, []);

  useEffect(() => {
    if (!filterLoading) {
      loadResults('applications');
    }
  }, [filterLoading]);

  const loadTabContents = (index) => loadResults(tabIndexes[index]);

  return (
    <main id="legal-dashboard-container" className="nx-page-main">
      <LoadWrapper loading={loading} error={loadError} retryHandler={loadResults}>
        {filterSidebarOpen && <LegalDashboardFilterContainer />}
        <div className="nx-page-title nx-page-title__actions">
          <h1 className="nx-h1">Legal Obligations</h1>
        </div>
        <NxStatefulTabs className="nx-viewport-sized__container" defaultActiveTab={0} onTabSelect={loadTabContents}>
          <NxTabList>
            <NxTab>Applications</NxTab>
            {componentTabEnabled && <NxTab>Components</NxTab>}
          </NxTabList>
          <NxTabPanel className="nx-viewport-sized__container">
            <div className="nx-tile nx-viewport-sized__container">
              <div className="nx-tile-content nx-viewport-sized__container">
                <div className="nx-btn-bar">
                  <NxButton id="filter-toggle" className="btn" onClick={() => toggleFilterSidebar(!filterSidebarOpen)}>
                    <NxFontAwesomeIcon icon={faFilter} />
                    <span>
                      Filter: {showDirtyAsterisk && <span id="filter-toggle-dirty-asterisk">*</span>}
                      {appliedFilterName || DEFAULT_FILTER_NAME}
                    </span>
                  </NxButton>
                </div>
                <LegalDashboardApplicationsTab
                  applications={applications}
                  fetchBackendPage={fetchBackendPage}
                  filtersAreDirty={filtersAreDirty}
                  changeSortField={changeSortField}
                  stateGo={stateGo}
                />
              </div>
            </div>
          </NxTabPanel>
          {componentTabEnabled && (
            <NxTabPanel className="nx-viewport-sized__container">
              <div className="  nx-tile nx-viewport-sized__container">
                <div className="nx-tile-content nx-viewport-sized__container">
                  <LegalDashboardComponentsTab
                    components={components}
                    fetchBackendPage={fetchBackendPage}
                    stateGo={stateGo}
                  />
                </div>
              </div>
            </NxTabPanel>
          )}
        </NxStatefulTabs>
      </LoadWrapper>
    </main>
  );
}

LegalDashboardPage.propTypes = {
  appliedFilterName: PropTypes.string,
  applications: applicationsTabPropType,
  components: PropTypes.any,
  filtersAreDirty: PropTypes.bool,
  loadFilter: PropTypes.func,
  filterLoading: PropTypes.bool,
  loadResults: PropTypes.func,
  isAuthorized: PropTypes.bool,
  loading: PropTypes.bool.isRequired,
  loadError: LoadWrapper.propTypes.error,
  fetchBackendPage: PropTypes.func.isRequired,
  changeSortField: PropTypes.func.isRequired,
  stateGo: PropTypes.func.isRequired,
  toggleFilterSidebar: PropTypes.func.isRequired,
  filterSidebarOpen: PropTypes.bool,
  showDirtyAsterisk: PropTypes.bool,
  router: PropTypes.shape({
    currentParams: PropTypes.object,
  }),
};
