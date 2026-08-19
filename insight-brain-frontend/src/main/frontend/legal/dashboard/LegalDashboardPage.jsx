/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useState, Fragment } from 'react';
import './_legalDashboard.scss';
import {
  NxButton,
  NxFontAwesomeIcon,
  NxStatefulTabs,
  NxTab,
  NxTabList,
  NxTabPanel,
  NxModal,
  NxP,
} from '@sonatype/react-shared-components';
import LegalDashboardApplicationsTab from './LegalDashboardApplicationsTab';
import LegalDashboardComponentsTab from './LegalDashboardComponentsTab';
import * as PropTypes from 'prop-types';
import LegalDashboardFilterContainer from './filter/LegalDashboardFilterContainer';
import LoadWrapper from '../../react/LoadWrapper';
import { applicationsTabPropType } from '../advancedLegalPropTypes';
import { faFilter } from '@fortawesome/pro-solid-svg-icons';
import { DEFAULT_FILTER_NAME } from './filter/defaultFilter';
import { LEGAL_PARENT_ROUTE, LEGAL_SBOM_MANAGER_PARENT_ROUTE } from 'MainRoot/legal/legalUtility';

export default function LegalDashboardPage(props) {
  const {
    isSbomManager,
    appliedFilterName,
    applications,
    components,
    filtersAreDirty,
    loadResults,
    loadDashboardUI,
    loading,
    loadError,
    fetchBackendPage,
    changeSortField,
    stateGo,
    toggleFilterSidebar,
    filterSidebarOpen,
    showDirtyAsterisk,
    router,
    searchByComponentName,
    legalDashboardSetPage,
    setComponentSearchInputValue,
  } = props;

  const tabIndexes = ['applications', 'components'];
  const prefix = isSbomManager ? LEGAL_SBOM_MANAGER_PARENT_ROUTE : LEGAL_PARENT_ROUTE;
  const stateIndexes = [`${prefix}.applicationsDashboard`, `${prefix}.componentsDashboard`];
  const defaultActiveTab = tabIndexes.findIndex((tab) => router.currentState?.data?.activeTab === tab);
  const disableCreateAttributionReportBtn =
    router.currentState?.data?.disableCreateAttributionReportBtn === true || applications.totalResultsCount < 1;
  const [showMultiAppAttributionReportModal, setShowMultiAppAttributionReportModal] = useState(false);
  const modalCloseHandler = () => setShowMultiAppAttributionReportModal(false);

  useEffect(() => {
    loadDashboardUI(tabIndexes[defaultActiveTab]);
  }, []);

  const loadTabContents = (index) => {
    stateGo(stateIndexes[index]);
  };

  const cancelGenerateReport = () => setShowMultiAppAttributionReportModal(false);

  return (
    <Fragment>
      {filterSidebarOpen && <LegalDashboardFilterContainer />}
      <main id="legal-dashboard-container" className="nx-page-main">
        <LoadWrapper loading={loading} error={loadError} retryHandler={loadResults}>
          <div className="nx-page-title nx-page-title__actions">
            <h1 className="nx-h1">Legal Obligations</h1>
            <div className="nx-btn-bar">
              <NxButton
                id="filter-toggle"
                variant="tertiary"
                className="btn"
                onClick={() => toggleFilterSidebar(!filterSidebarOpen)}
              >
                <NxFontAwesomeIcon icon={faFilter} />
                <span>
                  Filter: {showDirtyAsterisk && <span id="filter-toggle-dirty-asterisk">*</span>}
                  {appliedFilterName || DEFAULT_FILTER_NAME}
                </span>
              </NxButton>

              <NxButton
                id="create-attribution-report-btn"
                variant="primary"
                className={disableCreateAttributionReportBtn ? 'disabled' : ''}
                title={
                  router.currentState?.data?.activeTab !== tabIndexes[0]
                    ? 'Only available for applications. Switch to Applications to use.'
                    : ''
                }
                onClick={() => {
                  if (!disableCreateAttributionReportBtn) {
                    if (applications.totalResultsCount === 1) {
                      stateGo(`${prefix}.attributionReportMultiApp`);
                    } else {
                      setShowMultiAppAttributionReportModal(true);
                    }
                  }
                }}
              >
                Create Attribution Report
              </NxButton>

              {showMultiAppAttributionReportModal && (
                <NxModal onCancel={modalCloseHandler} aria-labelledby="modal-header-text">
                  <header className="nx-modal-header">
                    <h2 className="nx-h2" id="modal-header-text">
                      Multiple Applications Increase Load Time
                    </h2>
                  </header>
                  <div className="nx-modal-content">
                    <NxP>
                      Based on the current filter settings there are {applications.totalResultsCount} applications in
                      view. Do you want to generate attributions for {applications.totalResultsCount} applications? This
                      operation could take some time.
                    </NxP>
                  </div>
                  <div className="nx-footer">
                    <div className="nx-btn-bar">
                      <NxButton
                        variant="secondary"
                        id="create-report-cancel-button"
                        type="button"
                        onClick={cancelGenerateReport}
                      >
                        Back
                      </NxButton>
                      <NxButton
                        type="button"
                        id="create-report-generate-report-button"
                        onClick={() => {
                          setShowMultiAppAttributionReportModal(false);
                          stateGo(`${prefix}.attributionReportMultiApp`);
                        }}
                        variant="primary"
                      >
                        Generate Report
                      </NxButton>
                    </div>
                  </div>
                </NxModal>
              )}
            </div>
          </div>
          <NxStatefulTabs
            className="nx-viewport-sized__container"
            defaultActiveTab={defaultActiveTab}
            onTabSelect={loadTabContents}
          >
            <NxTabList>
              <NxTab>Applications</NxTab>
              <NxTab>Components</NxTab>
            </NxTabList>
            <NxTabPanel className="nx-viewport-sized__container">
              <div className="nx-tile nx-viewport-sized__container">
                <div className="nx-tile-content nx-viewport-sized__container">
                  <LegalDashboardApplicationsTab
                    applications={applications}
                    fetchBackendPage={fetchBackendPage}
                    filtersAreDirty={filtersAreDirty}
                    changeSortField={changeSortField}
                    stateGo={stateGo}
                    legalDashboardSetPage={legalDashboardSetPage}
                    isSbomManager={isSbomManager}
                  />
                </div>
              </div>
            </NxTabPanel>

            <NxTabPanel className="nx-viewport-sized__container">
              <div className="  nx-tile nx-viewport-sized__container">
                <div className="nx-tile-content nx-viewport-sized__container">
                  <LegalDashboardComponentsTab
                    components={components}
                    fetchBackendPage={fetchBackendPage}
                    changeSortField={changeSortField}
                    stateGo={stateGo}
                    loadResults={loadResults}
                    setComponentSearchInputValue={setComponentSearchInputValue}
                    searchByComponentName={searchByComponentName}
                    legalDashboardSetPage={legalDashboardSetPage}
                    isSbomManager={isSbomManager}
                  />
                </div>
              </div>
            </NxTabPanel>
          </NxStatefulTabs>
        </LoadWrapper>
      </main>
    </Fragment>
  );
}

LegalDashboardPage.propTypes = {
  isSbomManager: PropTypes.bool,
  appliedFilterName: PropTypes.string,
  applications: applicationsTabPropType,
  components: PropTypes.any,
  filtersAreDirty: PropTypes.bool,
  filterLoading: PropTypes.bool,
  loadResults: PropTypes.func,
  loadDashboardUI: PropTypes.func,
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
    currentState: PropTypes.object,
  }),
  searchByComponentName: PropTypes.func.isRequired,
  legalDashboardSetPage: PropTypes.func.isRequired,
  setComponentSearchInputValue: PropTypes.func.isRequired,
};
