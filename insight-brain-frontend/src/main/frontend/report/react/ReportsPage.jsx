/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Fragment, useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import {
  NxButton,
  NxFilterInput,
  NxOverflowTooltip,
  NxPageMain,
  NxTable,
  NxTile,
  NxPageTitle,
  NxLoadWrapper,
  NxLoadingSpinner,
  NxFontAwesomeIcon,
} from '@sonatype/react-shared-components';
import { faExclamationCircle } from '@fortawesome/pro-solid-svg-icons';
import {
  selectReportsLoadError,
  selectReportsLoading,
  selectApplicationsInformationList,
  selectHasMoreReports,
  selectReportsStages,
  selectAppliedSortReports,
  selectReportsFilter,
  selectLoadingPublicIds,
} from './reportsSelectors';
import { selectIsDeveloperDashboardEnabled } from 'MainRoot/productFeatures/productFeaturesSelectors';
import { actions } from './reportsSlice';
import ReportPageViolationCell from './ReportsPageViolationCell';
import { useRouterState } from 'MainRoot/react/RouterStateContext';
import { extractSortFieldName } from '../../util/sortUtils';
import { equals } from 'ramda';
import { selectIsDeveloper } from 'MainRoot/reduxUiRouter/routerSelectors';

// supported stages
const allStages = ['source', 'build', 'stage-release', 'release'];

export default function ReportsPage() {
  // System
  const dispatch = useDispatch();
  const uiRouterState = useRouterState();

  // Selectors
  const loading = useSelector(selectReportsLoading);
  const loadError = useSelector(selectReportsLoadError);
  const applicationsInformation = useSelector(selectApplicationsInformationList);
  const hasMoreResults = useSelector(selectHasMoreReports);
  const availStages = useSelector(selectReportsStages);
  const appliedSort = useSelector(selectAppliedSortReports);
  const appFilter = useSelector(selectReportsFilter);
  const loadingPublicIds = useSelector(selectLoadingPublicIds);
  const isDeveloperDashboardEnabled = useSelector(selectIsDeveloperDashboardEnabled);
  const isDeveloper = useSelector(selectIsDeveloper);

  // Actions
  const loadStagesAndReports = () => dispatch(actions.loadStagesAndReports());
  const reloadReports = () => dispatch(actions.loadReports());
  const loadMore = () => dispatch(actions.loadMore());
  const sortReports = (field) => dispatch(actions.sortReports(field));
  const filterReports = (filter) => dispatch(actions.filterReports(filter));
  const loadContactName = (appPublicId) => dispatch(actions.loadContactName(appPublicId));

  const sortedColumn = extractSortFieldName(appliedSort),
    isSortReversed = appliedSort && appliedSort.includes('-');

  useEffect(() => {
    loadStagesAndReports();
  }, []);

  const filteredStages = () =>
    availStages.filter((stage) => allStages.find((stageId) => stage.stageTypeId === stageId));

  const showContactButton = (publicId) => {
    return (
      <button className="nx-text-link iq-violation-show-contact-name" onClick={() => loadContactName(publicId)}>
        Show Contact
      </button>
    );
  };

  /*
    Render Show Contact button with click handler, loading state,
    error state, or contact display name
  */
  const renderContact = ({ contact, publicId }) => {
    // If no contact defined, render nothing
    if (!contact?.internalName || !publicId) {
      return null;
    }

    if (loadingPublicIds.has(publicId)) {
      // If contact in loading state, show loading spinner only
      return <NxLoadingSpinner />;
    } else if (contact?.error) {
      // If error exists, display Show Contact button and error
      return (
        <Fragment>
          {showContactButton(publicId)}
          <NxOverflowTooltip>
            <div className="nx-truncate-ellipsis iq-violation-contact-name-error">
              <NxFontAwesomeIcon className="iq-violation-contact-name-error-icon" icon={faExclamationCircle} />
              <span className="iq-violation-contact-name-error-text">Error loading contact</span>
            </div>
          </NxOverflowTooltip>
        </Fragment>
      );
    } else if (contact?.displayName) {
      // If contact name exists, show contact name only
      return (
        <NxOverflowTooltip>
          <div className="nx-truncate-ellipsis iq-violation-contact-name">{contact?.displayName}</div>
        </NxOverflowTooltip>
      );
    } else {
      // Display Show Contact button by default
      return showContactButton(publicId);
    }
  };

  const violationHeader = (stage) => {
    const stageObj = availStages.find((elem) => elem.stageTypeId === stage);
    return stageObj ? (
      <NxTable.Cell key={stage} className="iq-report-violation-cell">
        {stageObj.stageName}
      </NxTable.Cell>
    ) : null;
  };

  const violationCells = (report) =>
    allStages.map((currStage) =>
      availStages.find((elem) => elem.stageTypeId === currStage)
        ? ReportPageViolationCell({
            stage: currStage,
            app: report,
            hrefUiRouterState: uiRouterState.href,
            isDeveloperDashboardEnabled,
            isDeveloper,
          })
        : null
    );

  const violationTableRows = applicationsInformation.map((report) => (
    <NxTable.Row key={report.id} className="iq-violation-table-row">
      <NxTable.Cell className="iq-report-app-cell">
        <NxOverflowTooltip>
          <div className="nx-truncate-ellipsis iq-violation-table-report-name">{report.name}</div>
        </NxOverflowTooltip>
        {renderContact(report)}
      </NxTable.Cell>
      <NxTable.Cell className="iq-report-org-cell">
        <NxOverflowTooltip>
          <div className="nx-truncate-ellipsis iq-violation-table-organization-name">{report.organizationName}</div>
        </NxOverflowTooltip>
      </NxTable.Cell>
      {violationCells(report)}
    </NxTable.Row>
  ));

  const loadMoreResultsRow = (
    <NxTable.Row>
      <NxTable.Cell colSpan={2 + filteredStages().length} className="reports-load-more-cell">
        <NxLoadWrapper retryHandler={reloadReports} loading={loading} error={loadError}>
          <NxButton type="button" variant="primary" id="iq-report-list-load-button" onClick={() => loadMore()}>
            Load More Results
          </NxButton>
        </NxLoadWrapper>
      </NxTable.Cell>
    </NxTable.Row>
  );

  const doSort = (columnField) => {
    if (!applicationsInformation?.length) {
      return;
    }

    if (equals(columnField, appliedSort)) {
      if (isSortReversed) {
        sortReports(columnField.substring(1));
      } else {
        sortReports(`-${columnField}`);
      }
    } else {
      sortReports(columnField);
    }
    reloadReports();
  };

  const getColumnDirection = (columnField) => {
    if (!applicationsInformation?.length || loadError) {
      return null;
    }

    const columnName = extractSortFieldName(columnField),
      isCurrentColumnSorted = columnName === sortedColumn,
      isUp = isCurrentColumnSorted && !isSortReversed,
      isDown = isCurrentColumnSorted && isSortReversed;

    return isUp ? 'asc' : isDown ? 'desc' : null;
  };

  return (
    <NxPageMain id="iq-report-container" className="nx-viewport-sized">
      <NxPageTitle>
        <h1 className="nx-h1" id="iq-report-header">
          {isDeveloper ? 'Priorities' : 'Reports'}
        </h1>
      </NxPageTitle>
      <NxTile className="nx-viewport-sized__container">
        <NxTile.Content id="iq-violation-report-tile" className="nx-viewport-sized__container">
          <div className="nx-form-group">
            <NxFilterInput
              className="violations-search-input nx-text-input--long"
              searchIcon
              onChange={filterReports}
              placeholder="Search by application or organization name"
              id="iq-report-list-filter"
              value={appFilter}
            />
          </div>
          <div
            className="nx-table-container nx-scrollable nx-viewport-sized__scrollable"
            id="iq-violation-table-container"
          >
            <NxTable className="nx-table--fixed-layout" id="iq-violation-table">
              <NxTable.Head>
                <NxTable.Row>
                  <NxTable.Cell
                    onClick={() => doSort('name')}
                    sortDir={getColumnDirection('name')}
                    className="iq-report-app-cell"
                    isSortable
                  >
                    Application
                  </NxTable.Cell>
                  <NxTable.Cell
                    onClick={() => doSort('organizationName')}
                    sortDir={getColumnDirection('organizationName')}
                    className="iq-report-org-cell"
                    isSortable
                  >
                    Organization
                  </NxTable.Cell>
                  {allStages.map((currStage) => violationHeader(currStage))}
                </NxTable.Row>
              </NxTable.Head>
              <NxTable.Body emptyMessage="No data found." id="iq-violation-table-body">
                {violationTableRows}
                {hasMoreResults && loadMoreResultsRow}
              </NxTable.Body>
            </NxTable>
          </div>
        </NxTile.Content>
      </NxTile>
    </NxPageMain>
  );
}
