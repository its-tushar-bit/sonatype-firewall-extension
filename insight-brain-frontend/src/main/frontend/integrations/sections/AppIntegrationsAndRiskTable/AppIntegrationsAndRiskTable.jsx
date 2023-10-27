/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useCallback, useEffect } from 'react';
import {
  NxFilterInput,
  NxFontAwesomeIcon,
  NxPagination,
  NxTable,
  NxTableContainer,
  NX_STANDARD_DEBOUNCE_TIME,
} from '@sonatype/react-shared-components';
import { useDispatch, useSelector } from 'react-redux';
import { actions, COLUMNS } from './appIntegrationsAndRiskSlice';
import { selectAppIntegrationsAndRiskSlice } from 'MainRoot/integrations/sections/AppIntegrationsAndRiskTable/appIntegrationsAndRiskSelectors';
import { debounce } from 'debounce';
import { faCheckCircle, faTimesCircle } from '@fortawesome/pro-solid-svg-icons';

const EnabledIcon = () => <NxFontAwesomeIcon icon={faCheckCircle} className="iq-integrations-and-risk-enabled" />;
const DisabledIcon = () => <NxFontAwesomeIcon icon={faTimesCircle} className="iq-integrations-and-risk-disabled" />;

export default function AppIntegrationsAndRiskTable() {
  const appIntegrationsAndRiskSlice = useSelector(selectAppIntegrationsAndRiskSlice);
  const { tableData, loading, loadError, currentPage, pageCount, sort, filter } = appIntegrationsAndRiskSlice;

  const dispatch = useDispatch();

  const handleChange = (page) => {
    dispatch(actions.setCurrentPage({ currentPage: page }));
    dispatch(actions.loadAppIntegrationsAndRisk());
  };

  useEffect(() => {
    dispatch(actions.loadAppIntegrationsAndRisk());
  }, []);

  const handleSort = (name) => {
    dispatch(actions.setSort(name));
    dispatch(actions.loadAppIntegrationsAndRisk());
  };

  const getSortDir = (name) => {
    if (!sort.includes(name)) return null;
    return sort.includes('-') ? 'desc' : 'asc';
  };

  const debouncedFilterNameChange = useCallback(
    debounce((value) => {
      dispatch(actions.loadAppIntegrationsAndRisk(value));
    }, NX_STANDARD_DEBOUNCE_TIME),
    []
  );

  const onFilterNameChange = (filter) => {
    dispatch(actions.setFilter(filter));
    debouncedFilterNameChange(filter);
  };

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
              <NxFilterInput searchIcon placeholder="Filter" onChange={onFilterNameChange} value={filter} />
            </NxTable.Cell>
            <NxTable.Cell />
            <NxTable.Cell />
            <NxTable.Cell />
            <NxTable.Cell />
            <NxTable.Cell />
          </NxTable.Row>
        </NxTable.Head>
        <NxTable.Body emptyMessage="No data found." isLoading={loading} error={loadError}>
          {tableData.map(
            ({
              applicationName,
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
                    {ciIntegrationEnabled ? <EnabledIcon /> : <DisabledIcon />}
                  </NxTable.Cell>
                  <NxTable.Cell className="iq-developer-app-integrations-header">
                    {automatedSourceControlFeedbackEnabled ? <EnabledIcon /> : <DisabledIcon />}
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

  function getCurrentPage() {
    if (pageCount === 0) {
      // NxPagination does not allow currentPage to numeric if pageCount is 0
      return null;
    } else {
      return currentPage;
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
