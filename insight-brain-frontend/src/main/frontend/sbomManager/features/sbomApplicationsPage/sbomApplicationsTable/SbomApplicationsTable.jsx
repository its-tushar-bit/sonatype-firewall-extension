/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useCallback } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { allPass, always, cond, dec, equals, flip, gt, inc, lt, min, pick, T } from 'ramda';
import debounce from 'debounce';
import moment from 'moment';
import {
  NxBinaryDonutChart,
  NxFilterInput,
  NxPagination,
  NxSmallThreatCounter,
  NxSmallVulnerabilityCounter,
  NxTable,
  NxTextLink,
  NxTooltip,
} from '@sonatype/react-shared-components';

import { useRouterState } from 'MainRoot/react/RouterStateContext';
import { isNilOrEmpty } from 'MainRoot/util/jsUtil';
import { formatNumberLocale } from 'MainRoot/util/formatUtils';

import { selectSbomApplicationsTable } from './sbomApplicationsTableSelectors.js';
import { actions, APPLICATIONS_PER_PAGE, SORT_BY_FIELDS, SORT_DIRECTION } from './sbomApplicationsTableSlice.js';

import './SbomApplicationsTable.scss';
import { selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors.js';

const LOAD_APPLICATIONS_DEBOUNCE_TIMEOUT_MS = 300;

export default function SbomApplicationsTable() {
  const dispatch = useDispatch();
  const uiRouterState = useRouterState();
  const routerCurrentParams = useSelector(selectRouterCurrentParams);

  const loadApplications = () => dispatch(actions.loadApplications());

  const {
    loading,
    errorMessage,
    applications,
    applicationsTotalCount,
    sortConfiguration,
    pagination,
    applicationNameRawFilterTerm,
  } = useSelector(selectSbomApplicationsTable);

  useEffect(() => {
    dispatch(actions.resetConfigurations());
    dispatch(actions.setSortByAndDirection(pick(['sortBy', 'sortDirection'], routerCurrentParams ?? {})));
    loadApplications();
  }, [routerCurrentParams]);

  const debouncedLoadApplications = useCallback(debounce(loadApplications, LOAD_APPLICATIONS_DEBOUNCE_TIMEOUT_MS), []);

  const loadSortedApplications = (sortBy) => {
    dispatch(actions.setSortByAndCycleDirection(sortBy));
    debouncedLoadApplications();
  };

  const setCurrentPageAndLoadApplications = (page) => {
    dispatch(actions.setCurrentPage(page));
    debouncedLoadApplications();
  };

  const filterApplicationName = (filterTerm) => {
    dispatch(actions.setApplicationNameRawFilterTerm(filterTerm));
    dispatch(actions.setCurrentPage(0));
    debouncedLoadApplications();
  };

  const hasApplications = !isNilOrEmpty(applications);
  const applicationRows = hasApplications
    ? applications.map((application) => {
        const applicationHref = uiRouterState.href('sbomManager.management.view.application', {
          applicationPublicId: application.applicationPublicId,
        });

        const sbomHref = uiRouterState.href('sbomManager.management.view.bom', {
          applicationPublicId: application.applicationPublicId,
          versionId: application.sbomVersion,
        });

        return (
          <NxTable.Row key={application.applicationPublicId}>
            <NxTable.Cell>
              <NxTooltip
                title={application.applicationName}
                className="sbom-manager-applications-table__tooltip--application-name"
              >
                <NxTextLink className="sbom-manager-applications-table__application-name" href={applicationHref}>
                  {application.applicationName}
                </NxTextLink>
              </NxTooltip>
            </NxTable.Cell>
            <NxTable.Cell>
              <NxTooltip
                title={application.sbomVersion}
                className="sbom-manager-applications-table__tooltip--latest-version"
              >
                <NxTextLink className="sbom-manager-applications-table__latest-version" href={sbomHref}>
                  {application.sbomVersion}
                </NxTextLink>
              </NxTooltip>
            </NxTable.Cell>
            <NxTable.Cell>
              {typeof application.releaseStatusPercentage === 'number' ? (
                <div className="sbom-manager-applications-table__releaseStatusPercentage">
                  <NxBinaryDonutChart
                    className="sbom-manager-applications-table__releaseStatusPercentageDonut"
                    value={application.releaseStatusPercentage}
                    aria-label={`${application.releaseStatusPercentage}% release ready`}
                    innerRadiusPercent={80}
                  />
                  <span>{application.releaseStatusPercentage}%</span>
                </div>
              ) : null}
            </NxTable.Cell>
            <NxTable.Cell> {moment(application.importDate).fromNow()}</NxTable.Cell>
            <NxTable.Cell>
              {application.vulnerabilitySummary ? (
                <NxSmallVulnerabilityCounter
                  className="sbom-manager-applications-table__vulnerabilities"
                  criticalCount={application.vulnerabilitySummary.critical}
                  highCount={application.vulnerabilitySummary.high}
                  mediumCount={application.vulnerabilitySummary.medium}
                />
              ) : null}
            </NxTable.Cell>
            <NxTable.Cell>
              {application.policyViolationSummary ? (
                <NxSmallThreatCounter
                  className="sbom-manager-applications-table__violations"
                  criticalCount={application.policyViolationSummary.critical}
                  severeCount={application.policyViolationSummary.severe}
                  moderateCount={application.policyViolationSummary.moderate}
                />
              ) : null}
            </NxTable.Cell>
          </NxTable.Row>
        );
      })
    : null;

  const showTableContent = !loading && !errorMessage && hasApplications;
  const paginationSection = () => {
    if (showTableContent) {
      const status = cond([
        [equals(0), always(min(APPLICATIONS_PER_PAGE, applicationsTotalCount))],
        [
          allPass([flip(gt)(0), flip(lt)(dec(pagination.pageCount))]),
          always(
            `${formatNumberLocale(inc(pagination.currentPage * APPLICATIONS_PER_PAGE))}—${formatNumberLocale(
              inc(pagination.currentPage) * APPLICATIONS_PER_PAGE
            )}`
          ),
        ],
        [T, always(formatNumberLocale(applicationsTotalCount))],
      ])(pagination.currentPage);

      return (
        <div className="sbom-manager-applications-table__pagination-section">
          <div className="sbom-manager-applications-table__pagination-wrapper">
            <NxPagination
              className="sbom-manager-applications-table__pagination"
              aria-controls="sbom-manager-applications-table"
              pageCount={pagination.pageCount}
              currentPage={pagination.currentPage}
              onChange={setCurrentPageAndLoadApplications}
            />
          </div>
          <div
            className="sbom-manager-applications-table__pagination-status"
            data-testid="sbom-applications-table-pagination-status"
          >
            Showing {status} of {formatNumberLocale(applicationsTotalCount)} applications
          </div>
        </div>
      );
    }
    return null;
  };

  const createColumnSortHandler = (field) =>
    showTableContent && applicationsTotalCount > 1
      ? {
          sortDir: field === sortConfiguration.sortBy ? sortConfiguration.sortDirection : SORT_DIRECTION.DEFAULT,
          onClick: () => loadSortedApplications(field),
          isSortable: true,
        }
      : {};

  const tableBodyProps = {
    isLoading: loading,
    retryHandler: loadApplications,
    ...(errorMessage && { error: errorMessage }),
    ...(!hasApplications && { emptyMessage: 'No applications found' }),
  };

  return (
    <div className="sbom-manager-applications-table nx-table-container">
      <NxTable
        id="sbom-manager-applications-table"
        data-testid="sbom-manager-applications-table"
        className="sbom-manager-applications-table__table"
      >
        <NxTable.Head>
          <NxTable.Row>
            <NxTable.Cell {...createColumnSortHandler(SORT_BY_FIELDS.name)}>Name</NxTable.Cell>
            <NxTable.Cell {...createColumnSortHandler(SORT_BY_FIELDS.latestVersion)}>Latest Version</NxTable.Cell>
            <NxTable.Cell {...createColumnSortHandler(SORT_BY_FIELDS.releaseStatusPercentage)}>
              Release Status
            </NxTable.Cell>
            <NxTable.Cell {...createColumnSortHandler(SORT_BY_FIELDS.importDate)}>Import Date</NxTable.Cell>
            <NxTable.Cell {...createColumnSortHandler(SORT_BY_FIELDS.vulnerabilities)}>vulnerabilities</NxTable.Cell>
            <NxTable.Cell>Violations</NxTable.Cell>
          </NxTable.Row>
          <NxTable.Row isFilterHeader>
            <NxTable.Cell>
              <NxFilterInput
                id="application-name-filter"
                className="sbom-manager-applications-table__filter-input"
                aria-label="Application Name Filter"
                placeholder="Filter by name"
                value={applicationNameRawFilterTerm}
                onChange={filterApplicationName}
                data-testid="application-name-filter"
              />
            </NxTable.Cell>
            <NxTable.Cell></NxTable.Cell>
            <NxTable.Cell></NxTable.Cell>
            <NxTable.Cell></NxTable.Cell>
            <NxTable.Cell></NxTable.Cell>
            <NxTable.Cell></NxTable.Cell>
          </NxTable.Row>
        </NxTable.Head>
        <NxTable.Body {...tableBodyProps}>{applicationRows}</NxTable.Body>
      </NxTable>
      <div className="nx-table-container__footer">{paginationSection()}</div>
    </div>
  );
}
