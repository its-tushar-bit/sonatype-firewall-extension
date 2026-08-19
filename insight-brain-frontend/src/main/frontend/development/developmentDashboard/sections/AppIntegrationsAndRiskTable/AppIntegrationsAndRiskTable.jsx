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
  NxButton,
  NxTooltip,
  NxTextLink,
} from '@sonatype/react-shared-components';
import { useDispatch, useSelector } from 'react-redux';
import { actions, COLUMNS } from 'MainRoot/development/developmentDashboard/slices/appIntegrationsAndRiskSlice';
import { selectAppIntegrationsAndRiskSlice } from 'MainRoot/development/developmentDashboard/selectors/appIntegrationsAndRiskSelectors';
import { debounce } from 'debounce';
import { faCheckCircle } from '@fortawesome/pro-solid-svg-icons';
import DeveloperConfigurationModal from 'MainRoot/development/developmentDashboard/sections/DeveloperConfigurationModal/DeveloperConfigurationModal';
import {
  createCiCdTabs,
  createScmTabs,
} from 'MainRoot/development/developmentDashboard/sections/DeveloperConfigurationModal/DeveloperConfiguratgionModalUtils';
import { faInfoCircle } from '@fortawesome/free-solid-svg-icons';
import PropTypes from 'prop-types';
import { useRouterState } from 'MainRoot/react/RouterStateContext';

const EnabledIcon = () => <NxFontAwesomeIcon icon={faCheckCircle} className="iq-integrations-and-risk-enabled" />;
export default function AppIntegrationsAndRiskTable() {
  const uiRouterState = useRouterState();

  const appIntegrationsAndRiskSlice = useSelector(selectAppIntegrationsAndRiskSlice);
  const { tableData, loading, loadError, currentPage, pageCount, sort, nameFilter } = appIntegrationsAndRiskSlice;
  const dispatch = useDispatch();

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
            <NxTable.Cell className="iq-developer-app-integrations-header">CI/CD</NxTable.Cell>
            <NxTable.Cell className="iq-developer-app-integrations-header">SCM Feedback</NxTable.Cell>
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
            <NxTable.Cell>
              <span>Priorities</span>
              <NxTooltip title="Priorities view identifies and prioritizes actionable vulnerabilities in your applications">
                <NxFontAwesomeIcon icon={faInfoCircle} className="iq-developer-dashboard-info-tooltip" />
              </NxTooltip>
            </NxTable.Cell>
          </NxTable.Row>
          <NxTable.Row isFilterHeader>
            <NxTable.Cell>
              <NxFilterInput
                data-analytics-id="sonatype-development-dashboard-overview-app-integration-table-search-input-box"
                searchIcon
                placeholder="Search by name"
                onChange={onFilterNameChange}
                value={nameFilter}
              />
            </NxTable.Cell>
            <NxTable.Cell></NxTable.Cell>
            <NxTable.Cell></NxTable.Cell>
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
              hasPrioritiesReport,
              lastScanId,
            }) => {
              function getPrioritiesReportCellProps() {
                return { applicationPublicId, hasPrioritiesReport, lastScanId };
              }

              const appManagementHref = uiRouterState.href('management.view.application', {
                applicationPublicId,
              });

              return (
                <NxTable.Row key={applicationName}>
                  <NxTable.Cell className="iq-integrations-applications-table__name-cell">
                    <NxTextLink
                      external
                      href={appManagementHref}
                      data-analytics-id="sonatype-developer-dashboard-app-management"
                    >
                      {applicationName}
                    </NxTextLink>
                  </NxTable.Cell>
                  <NxTable.Cell className="iq-developer-app-integrations-header">
                    {ciIntegrationEnabled ? (
                      <EnabledIcon />
                    ) : (
                      <>
                        {' '}
                        <NxButton
                          data-analytics-id="iq-developer-app-integrations-cicd-configure-button"
                          className="iq-developer-app-integrations-cicd-configure-button"
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
                          data-analytics-id="iq-developer-app-integrations-scm-configure-button"
                          className="iq-developer-app-integrations-scm-configure-button"
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
                  <NxTable.Cell>
                    <PrioritiesReportCell {...getPrioritiesReportCellProps()} />
                  </NxTable.Cell>
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
}

function formatTimestampToDate(timestamp) {
  if (timestamp === 0) {
    return 'None';
  }

  return new Date(timestamp).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  });
}

function PrioritiesReportCell(props) {
  const uiRouterState = useRouterState();
  const { applicationPublicId, hasPrioritiesReport, lastScanId } = props;
  if (!hasPrioritiesReport) {
    return <span>N/A</span>;
  }

  const prioritiesHref = uiRouterState.href('prioritiesPageFromDashboard', {
    publicAppId: applicationPublicId,
    scanId: lastScanId,
  });

  return (
    <div className="iq-developer-dashboard-priorities-cell-container">
      <NxTextLink
        data-analytics-id="sonatype-development-app-integration-dashboard-view-link-clicked"
        href={prioritiesHref}
      >
        View
      </NxTextLink>
    </div>
  );
}

PrioritiesReportCell.propTypes = {
  applicationPublicId: PropTypes.string,
  hasPrioritiesReport: PropTypes.bool,
  lastScanId: PropTypes.string,
};
