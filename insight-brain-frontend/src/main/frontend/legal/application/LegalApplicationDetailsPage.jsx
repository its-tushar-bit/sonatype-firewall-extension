/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import * as PropTypes from 'prop-types';
import LoadWrapper from '../../react/LoadWrapper';
import {
  NxButton,
  NxFilterInput,
  NxFontAwesomeIcon,
  NxTable,
  NxTableBody,
  NxTableCell,
  NxTableHead,
  NxTableRow,
} from '@sonatype/react-shared-components';
import { Messages } from '../../utilAngular/CommonServices';
import LegalApplicationDetailsComponentRow from './LegalApplicationDetailsComponentRow';
import LegalApplicationDetailsFilterContainer from './filter/LegalApplicationDetailsFilterContainer';
import { faFilter } from '@fortawesome/pro-solid-svg-icons';
import MenuBarBackButton from '../../mainHeader/MenuBar/MenuBarBackButton';
import { expandedProgressOptions } from 'MainRoot/legal/dashboard/legalDashboardConstants';

export default function LegalApplicationDetailsPage(props) {
  const {
    applicationPublicId,
    stageTypeId,
    error,
    loading,
    applicationName,
    stageName,
    components,
    componentFilter,
    licenseFilter,
    filterSidebarOpen,
    selected,
    toggleFilterSidebar,
    sort,
    $state,
    fetchLegalApplicationDetailsData,
    changeComponentNameFilter,
    changeLicenseNameFilter,
    updateLegalSortOrder,
    stateGo,
  } = props;

  useEffect(() => {
    if (applicationPublicId && stageTypeId) {
      fetchLegalApplicationDetailsData(applicationPublicId, stageTypeId);
    }
  }, [applicationPublicId, stageTypeId]);

  const allOrNoFilterOptionsSelected = (selectedFilter, totalOptions) =>
    selectedFilter.size === totalOptions || selectedFilter.size === 0;

  const showDirtyAsterisk =
    !allOrNoFilterOptionsSelected(selected.licenseThreatGroups, components.licenseThreatGroups.length) ||
    !allOrNoFilterOptionsSelected(selected.progressOptions, expandedProgressOptions.length);

  const componentSortOrder = sort.column === 'component' ? sort.sortOrder : null;
  const licensesSortOrder = sort.column === 'licenses' ? sort.sortOrder : null;
  const progressSortOrder = sort.column === 'progress' ? sort.sortOrder : null;
  const statusSortOrder = sort.column === 'status' ? sort.sortOrder : null;

  const invertSortOrder = (order) => (order === 'asc' ? 'desc' : 'asc');

  const updateComponentSortOrder = () =>
    updateLegalSortOrder({
      column: 'component',
      sortOrder: invertSortOrder(componentSortOrder),
    });
  const updateLicenseSortOrder = () =>
    updateLegalSortOrder({
      column: 'licenses',
      sortOrder: invertSortOrder(licensesSortOrder),
    });
  const updateProgressSortOrder = () =>
    updateLegalSortOrder({
      column: 'progress',
      sortOrder: invertSortOrder(progressSortOrder),
    });
  const updateStatusSortOrder = () =>
    updateLegalSortOrder({
      column: 'status',
      sortOrder: invertSortOrder(statusSortOrder),
    });

  return (
    <main id="legal-application-details-container" className="nx-page-main nx-viewport-sized">
      <LoadWrapper
        loading={loading}
        error={error}
        retryHandler={() => fetchLegalApplicationDetailsData(applicationPublicId, stageTypeId)}
      >
        <MenuBarBackButton href={$state.href('legal.dashboard')} text="Back" />
        {filterSidebarOpen && <LegalApplicationDetailsFilterContainer />}
        <div className="nx-page-title">
          <h1 className="nx-h1">{applicationName} Obligations</h1>
          <div className="nx-btn-bar">
            <NxButton
              variant="primary"
              onClick={() => {
                stateGo('legal.attributionReport', {
                  applicationPublicId,
                  stageTypeId,
                });
              }}
            >
              Create Attribution Report
            </NxButton>
          </div>
          <div className="nx-page-title__description">
            <div className="nx-tile-header__subtitle">{stageName} Stage</div>
          </div>
        </div>
        <div className="nx-scrollable nx-table-container nx-viewport-sized__scrollable">
          <section className="nx-tile">
            <header className="nx-tile-header">
              <div className="nx-tile__actions">
                <NxButton id="filter-toggle" className="btn" onClick={() => toggleFilterSidebar(!filterSidebarOpen)}>
                  <NxFontAwesomeIcon icon={faFilter} />
                  <span>Filter{showDirtyAsterisk && <span id="filter-toggle-dirty-asterisk">*</span>}</span>
                </NxButton>
              </div>
            </header>
            <div className="nx-tile-content">
              <NxTable id="legal-application-details-table" className="legal-dashboard-table">
                <NxTableHead>
                  <NxTableRow>
                    <NxTableCell
                      isSortable
                      sortDir={componentSortOrder}
                      onClick={updateComponentSortOrder}
                      className="legal-application-details-table-component"
                    >
                      Component
                    </NxTableCell>
                    <NxTableCell
                      isSortable
                      sortDir={licensesSortOrder}
                      onClick={updateLicenseSortOrder}
                      className="legal-application-details-table-licenses"
                    >
                      Licenses
                    </NxTableCell>
                    <NxTableCell
                      isSortable
                      sortDir={progressSortOrder}
                      onClick={updateProgressSortOrder}
                      className="legal-application-details-table-review-progress"
                    >
                      Completed Obligations
                    </NxTableCell>
                    <NxTableCell
                      isSortable
                      sortDir={statusSortOrder}
                      onClick={updateStatusSortOrder}
                      className="legal-application-details-table-review-status"
                    >
                      Review Status
                    </NxTableCell>
                    <NxTableCell chevron />
                  </NxTableRow>
                </NxTableHead>
                <NxTableBody emptyMessage="No components found">
                  <NxTableRow key="__filter">
                    <NxTableCell>
                      <NxFilterInput
                        id="legal-application-component-filter"
                        value={componentFilter || ''}
                        placeholder="Filter components"
                        onChange={(newVal) => changeComponentNameFilter({ filter: newVal })}
                      />
                    </NxTableCell>
                    <NxTableCell>
                      <NxFilterInput
                        id="legal-application-license-filter"
                        value={licenseFilter || ''}
                        placeholder="Filter licenses"
                        onChange={(newVal) => changeLicenseNameFilter({ filter: newVal })}
                      />
                    </NxTableCell>
                    <NxTableCell />
                    <NxTableCell />
                    <NxTableCell />
                  </NxTableRow>
                  {components.filteredResults.map((row, index) => (
                    <LegalApplicationDetailsComponentRow
                      key={index}
                      applicationPublicId={applicationPublicId}
                      stageTypeId={stageTypeId}
                      row={row}
                      stateGo={stateGo}
                    />
                  ))}
                </NxTableBody>
              </NxTable>
            </div>
          </section>
        </div>
      </LoadWrapper>
    </main>
  );
}

LegalApplicationDetailsPage.propTypes = {
  applicationPublicId: PropTypes.string,
  stageTypeId: PropTypes.string,
  loading: PropTypes.bool,
  error: LoadWrapper.propTypes.error,
  applicationName: PropTypes.string,
  stageName: PropTypes.string,
  components: PropTypes.shape({
    filteredResults: PropTypes.arrayOf(LegalApplicationDetailsComponentRow.propTypes.row),
    licenseThreatGroups: PropTypes.array,
    results: PropTypes.arrayOf(LegalApplicationDetailsComponentRow.propTypes.row),
  }),
  componentFilter: PropTypes.string,
  licenseFilter: PropTypes.string,
  filterSidebarOpen: PropTypes.bool,
  selected: PropTypes.shape({
    licenseThreatGroups: PropTypes.instanceOf(Set),
    progressOptions: PropTypes.instanceOf(Set),
  }),
  toggleFilterSidebar: PropTypes.func.isRequired,
  sort: PropTypes.shape({
    column: PropTypes.string,
    sortOrder: PropTypes.string,
  }),
  $state: PropTypes.object.isRequired,
  fetchLegalApplicationDetailsData: PropTypes.func.isRequired,
  stateGo: PropTypes.func.isRequired,
  changeComponentNameFilter: PropTypes.func.isRequired,
  changeLicenseNameFilter: PropTypes.func.isRequired,
  updateLegalSortOrder: PropTypes.func.isRequired,
};
