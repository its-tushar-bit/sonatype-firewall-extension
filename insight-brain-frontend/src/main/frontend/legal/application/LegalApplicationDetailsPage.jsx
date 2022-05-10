/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import * as PropTypes from 'prop-types';
import LoadWrapper from '../../react/LoadWrapper';
import { chain, map, pipe, prop, uniq } from 'ramda';
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
import { getLicenseThreatGroupsFromLicense } from '../legalUtility';
import LegalApplicationDetailsComponentRow from './LegalApplicationDetailsComponentRow';
import LegalApplicationDetailsFilterContainer from './filter/LegalApplicationDetailsFilterContainer';
import { faFilter } from '@fortawesome/pro-solid-svg-icons';
import MenuBarBackButton from '../../mainHeader/MenuBar/MenuBarBackButton';

export default function LegalApplicationDetailsPage(props) {
  const {
    applicationPublicId,
    stageTypeId,
    application,
    stageType,
    components,
    componentFilter,
    licenseFilter,
    filterSidebarOpen,
    toggleFilterSidebar,
    sort,
    $state,
    loadApplication,
    changeComponentNameFilter,
    changeLicenseNameFilter,
    updateLegalSortOrder,
    stateGo,
  } = props;

  useEffect(() => {
    if (applicationPublicId && stageTypeId) {
      loadApplication(applicationPublicId, stageTypeId);
    }
  }, [applicationPublicId, stageTypeId]);

  const getLicenseThreatGroupsFromComponents = pipe(
    chain(prop('licenses')),
    chain(getLicenseThreatGroupsFromLicense),
    map(prop('licenseThreatGroupName')),
    uniq
  );

  const errorLoading = application.error || stageType.error;

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
        loading={application.loading || stageType.loading}
        error={errorLoading}
        retryHandler={() => loadApplication(applicationPublicId, stageTypeId)}
      >
        <MenuBarBackButton href={$state.href('legal.dashboard')} text="Back" />
        {filterSidebarOpen && (
          <LegalApplicationDetailsFilterContainer
            licenseThreatGroups={getLicenseThreatGroupsFromComponents(components.filteredResults)}
          />
        )}
        <div className="nx-page-title">
          <h1 className="nx-h1">{application.name} Obligations</h1>
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
            <div className="nx-tile-header__subtitle">{stageType.name} Stage</div>
          </div>
        </div>
        <div className="nx-scrollable nx-table-container nx-viewport-sized__scrollable">
          <section className="nx-tile">
            <header className="nx-tile-header">
              <div className="nx-tile__actions">
                <NxButton id="filter-toggle" className="btn" onClick={() => toggleFilterSidebar(!filterSidebarOpen)}>
                  <NxFontAwesomeIcon icon={faFilter} />
                  <span>Filter</span>
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
                <NxTableBody
                  emptyMessage="No components found"
                  isLoading={components.loading}
                  error={Messages.getHttpErrorMessage(components.error)}
                >
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
  application: PropTypes.shape({
    name: PropTypes.string,
    loading: PropTypes.bool,
    error: LoadWrapper.propTypes.error,
  }),
  stageType: PropTypes.shape({
    name: PropTypes.string,
    loading: PropTypes.bool,
    error: LoadWrapper.propTypes.error,
  }),
  components: PropTypes.shape({
    filteredResults: PropTypes.arrayOf(LegalApplicationDetailsComponentRow.propTypes.row),
    loading: PropTypes.bool,
    error: LoadWrapper.propTypes.error,
  }),
  componentFilter: PropTypes.string,
  licenseFilter: PropTypes.string,
  filterSidebarOpen: PropTypes.bool,
  toggleFilterSidebar: PropTypes.func.isRequired,
  sort: PropTypes.shape({
    column: PropTypes.string,
    sortOrder: PropTypes.string,
  }),
  $state: PropTypes.object.isRequired,
  loadApplication: PropTypes.func.isRequired,
  stateGo: PropTypes.func.isRequired,
  changeComponentNameFilter: PropTypes.func.isRequired,
  changeLicenseNameFilter: PropTypes.func.isRequired,
  updateLegalSortOrder: PropTypes.func.isRequired,
};
