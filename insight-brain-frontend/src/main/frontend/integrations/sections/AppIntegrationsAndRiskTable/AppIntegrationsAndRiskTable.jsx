/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useCallback, useEffect, useState } from 'react';
import {
  NxFilterInput,
  NxFontAwesomeIcon,
  NxPagination,
  NxTable,
  NxTableContainer,
  NX_STANDARD_DEBOUNCE_TIME,
  NxFilterDropdown,
  NxStatefulFilterDropdown,
  NxButton,
} from '@sonatype/react-shared-components';
import { useDispatch, useSelector } from 'react-redux';
import { actions, COLUMNS } from './appIntegrationsAndRiskSlice';
import { selectAppIntegrationsAndRiskSlice } from 'MainRoot/integrations/sections/AppIntegrationsAndRiskTable/appIntegrationsAndRiskSelectors';
import { debounce } from 'debounce';
import { faCheckCircle, faTimesCircle } from '@fortawesome/pro-solid-svg-icons';
import DeveloperConfigurationModal from 'MainRoot/integrations/sections/DeveloperConfigurationModal/DeveloperConfigurationModal';
import {
  createCiCdTabs,
  createScmTabs,
} from 'MainRoot/integrations/sections/DeveloperConfigurationModal/DeveloperConfiguratgionModalUtils';

const EnabledIcon = () => <NxFontAwesomeIcon icon={faCheckCircle} className="iq-integrations-and-risk-enabled" />;
export default function AppIntegrationsAndRiskTable() {
  const scmFilterOptions = [
    { id: 'withScm', displayName: 'Configured apps' },
    { id: 'withoutScm', displayName: 'Non-configured apps' },
  ];

  const ciCdFilterOptions = [
    { id: 'withCiCd', displayName: 'Configured apps' },
    { id: 'withoutCiCd', displayName: 'Non-configured apps' },
  ];

  const appIntegrationsAndRiskSlice = useSelector(selectAppIntegrationsAndRiskSlice);
  const { tableData, loading, loadError, currentPage, pageCount, sort, nameFilter } = appIntegrationsAndRiskSlice;
  const dispatch = useDispatch();
  const [scmFilterValues, setSCMFilters] = useState(new Set());
  const [ciCdFilterValues, setCiCdFilters] = useState(new Set());

  const [selectedAppId, setSelectedAppId] = useState('');
  const [showScmModal, setScmShowModal] = useState(false);
  const [showCicdModal, setCicdShowModal] = useState(false);
  const cicdModalCloseHandler = () => setCicdShowModal(false);
  const scmModalCloseHandler = () => setScmShowModal(false);

  const setModalDetails = (type, appId) => {
    if (type === 'cicd') {
      setCicdShowModal(true);
    } else if (type === 'scm') {
      setScmShowModal(true);
    }
    setSelectedAppId(appId);
  };

  useEffect(() => {
    dispatch(actions.loadAppIntegrationsAndRisk());
  }, []);

  const debouncedFilterNameChange = useCallback(
    debounce((value) => {
      dispatch(actions.loadAppIntegrationsAndRisk(value));
    }, NX_STANDARD_DEBOUNCE_TIME),
    []
  );

  return (
    <NxTableContainer id="iq-developer-app-integrations-and-risk-table">
      <NxTable>
        <NxTable.Head>
          <NxTable.Row>
            <NxTable.Cell isSortable onClick={() => handleSort(COLUMNS.NAME)} sortDir={getSortDir(COLUMNS.NAME)}>
              APPLICATIONS
            </NxTable.Cell>
            <NxTable.Cell>CI/CD</NxTable.Cell>
            <NxTable.Cell>SCM Feedback</NxTable.Cell>
            <NxTable.Cell isSortable onClick={() => handleSort(COLUMNS.COMMIT)} sortDir={getSortDir(COLUMNS.COMMIT)}>
              LAST COMMIT
            </NxTable.Cell>
            <NxTable.Cell
              isSortable
              onClick={() => handleSort(COLUMNS.EVALUATION)}
              sortDir={getSortDir(COLUMNS.EVALUATION)}
            >
              LAST EVALUATION
            </NxTable.Cell>
            <NxTable.Cell
              isSortable
              onClick={() => handleSort(COLUMNS.TOTAL_RISK)}
              sortDir={getSortDir(COLUMNS.TOTAL_RISK)}
            >
              TOTAL RISK
            </NxTable.Cell>
          </NxTable.Row>
          <NxTable.Row isFilterHeader>
            <NxTable.Cell>
              <NxFilterInput searchIcon placeholder="Search by name" onChange={onFilterNameChange} value={nameFilter} />
            </NxTable.Cell>
            <NxTable.Cell className="iq-developer-dashboard-filter-cell">
              <NxStatefulFilterDropdown
                placeholder="Filter"
                id="iq-developer-app-integrations-cicd-filter"
                data-testid="cicd-filter"
                showReset={false}
                options={ciCdFilterOptions}
                selectedIds={ciCdFilterValues}
                onChange={onCiCdFilterChange}
              />
            </NxTable.Cell>
            <NxTable.Cell className="iq-developer-dashboard-filter-cell">
              <NxStatefulFilterDropdown
                placeholder="Filter"
                id="iq-developer-app-integrations-scm-filter"
                data-testid="scm-filter"
                showReset={false}
                options={scmFilterOptions}
                selectedIds={scmFilterValues}
                onChange={onSCMFilterChange}
              />
            </NxTable.Cell>
            <NxTable.Cell />
            <NxTable.Cell />
            <NxTable.Cell />
          </NxTable.Row>
        </NxTable.Head>
        <NxTable.Body
          emptyMessage="No data available given the applied filters and permissions."
          isLoading={loading}
          error={loadError}
        >
          {tableData.map(
            ({
              applicationName,
              applicationPublicId,
              organizationId,
              ciIntegrationEnabled,
              automatedSourceControlFeedbackEnabled,
              lastCommitTimestamp,
              lastEvaluationTimestamp,
              totalRiskScore,
            }) => {
              return (
                <NxTable.Row key={applicationName.concat(totalRiskScore)}>
                  <NxTable.Cell className="iq-integrations-applications-table__name-cell">
                    {applicationName}
                  </NxTable.Cell>
                  <NxTable.Cell className="iq-developer-app-integrations-header">
                    {ciIntegrationEnabled ? (
                      <EnabledIcon />
                    ) : (
                      <>
                        {' '}
                        <NxButton
                          id="iq-developer-app-integrations-cicd-configure-button"
                          onClick={() => setModalDetails('cicd', applicationPublicId)}
                          variant="tertiary"
                        >
                          Configure
                        </NxButton>
                        <DeveloperConfigurationModal
                          id="iq-developer-app-integrations-cicd-configuration-modal"
                          title="CI/CD Configuration"
                          tabs={createCiCdTabs(applicationPublicId, organizationId)}
                          showModal={showCicdModal && selectedAppId === applicationPublicId}
                          onClose={cicdModalCloseHandler}
                        />
                      </>
                    )}
                  </NxTable.Cell>
                  <NxTable.Cell className="iq-developer-app-integrations-header">
                    {automatedSourceControlFeedbackEnabled ? (
                      <EnabledIcon />
                    ) : (
                      <>
                        <NxButton
                          id="iq-developer-app-integrations-scm-configure-button"
                          onClick={() => setModalDetails('scm', applicationPublicId)}
                          variant="tertiary"
                        >
                          Configure
                        </NxButton>
                        <DeveloperConfigurationModal
                          id="iq-developer-app-integrations-scm-configuration-modal"
                          title="SCM Integrations"
                          tabs={createScmTabs(applicationPublicId)}
                          showModal={showScmModal && selectedAppId === applicationPublicId}
                          onClose={scmModalCloseHandler}
                        />
                      </>
                    )}
                  </NxTable.Cell>
                  <NxTable.Cell>{formatTimestampToDate(lastCommitTimestamp)}</NxTable.Cell>
                  <NxTable.Cell>{formatTimestampToDate(lastEvaluationTimestamp)}</NxTable.Cell>
                  <NxTable.Cell>{totalRiskScore}</NxTable.Cell>
                </NxTable.Row>
              );
            }
          )}
        </NxTable.Body>
      </NxTable>
      <div className="nx-table-container__footer">
        <NxPagination pageCount={pageCount} currentPage={getCurrentPage()} onChange={handleChange} />
      </div>
    </NxTableContainer>
  );

  function onFilterNameChange(filter) {
    dispatch(actions.setNameFilter(filter));
    debouncedFilterNameChange(filter);
  }

  function handleChange(page) {
    dispatch(actions.setCurrentPage({ currentPage: page }));
    dispatch(actions.loadAppIntegrationsAndRisk());
  }

  function handleSort(name) {
    dispatch(actions.setSort(name));
    dispatch(actions.loadAppIntegrationsAndRisk());
  }

  function getSortDir(name) {
    if (!sort.includes(name)) return null;
    return sort.includes('-') ? 'desc' : 'asc';
  }

  function onSCMFilterChange(filter) {
    // Only none or one of the filter options can be selected at once
    const newScmFilterValue =
      scmFilterValues != null && filter.size > 1 ? new Set([...filter].filter((x) => !scmFilterValues.has(x))) : filter;

    setSCMFilters(newScmFilterValue);
    dispatch(actions.setSCMFilter(getBooleanFromFilterSet(newScmFilterValue)));
    dispatch(actions.loadAppIntegrationsAndRisk());
  }

  function onCiCdFilterChange(filter) {
    // Only none or one of the filter options can be selected at once
    const newCiCdFilterValues =
      ciCdFilterValues != null && filter.size > 1
        ? new Set([...filter].filter((x) => !ciCdFilterValues.has(x)))
        : filter;

    setCiCdFilters(newCiCdFilterValues);
    dispatch(actions.setCiCdFilter(getBooleanFromFilterSet(newCiCdFilterValues)));
    dispatch(actions.loadAppIntegrationsAndRisk());
  }

  function getCurrentPage() {
    if (pageCount === 0) {
      // NxPagination does not allow currentPage to numeric if pageCount is 0
      return null;
    } else {
      if (currentPage >= pageCount) {
        dispatch(actions.setCurrentPage({ currentPage: 0 }));
        dispatch(actions.loadAppIntegrationsAndRisk());

        return null;
      }
      return currentPage;
    }
  }

  function getBooleanFromFilterSet(filterSet) {
    if (filterSet === null || filterSet.size === 0) {
      return null;
    } else {
      const [firstValue] = filterSet;
      // If the set value contains the element 'without' then it should be false
      return !firstValue.includes('without');
    }
  }
}

function formatTimestampToDate(timestamp) {
  if (timestamp === 0) {
    return 'N/A';
  }

  return new Date(timestamp).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  });
}
