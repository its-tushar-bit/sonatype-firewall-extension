/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useCallback } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { allPass, always, cond, dec, equals, flip, gt, inc, lt, min, T } from 'ramda';
import debounce from 'debounce';
import moment from 'moment';

import {
  NxBinaryDonutChart,
  NxFilterInput,
  NxPagination,
  NxSmallThreatCounter,
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

const LOAD_APPLICATIONS_DEBOUNCE_TIMEOUT_MS = 300;

export default function SbomApplicationsTable() {
  const dispatch = useDispatch();
  const uiRouterState = useRouterState();

  const loadApplications = () => dispatch(actions.loadApplications());

  const { loading, errorMessage, applications, totalApplicationsCount, sortConfiguration, pagination } = useSelector(
    selectSbomApplicationsTable
  );

  useEffect(() => {
    loadApplications();

    moment.defineLocale('custom', {
      relativeTime: {
        d: '%dD',
        dd: '%dD',
        M: '%dM',
        MM: '%dM',
        y: '%dY',
        yy: '%dY',
      },
    });

    moment.locale('custom');

    return () => {
      moment.locale('en-us');
    };
  }, []);

  const debouncedLoadApplications = useCallback(debounce(loadApplications, LOAD_APPLICATIONS_DEBOUNCE_TIMEOUT_MS), []);

  const loadSortedApplications = (sortBy) => {
    dispatch(actions.setSortByAndCycleDirection(sortBy));
    debouncedLoadApplications();
  };

  const setCurrentPageAndLoadApplications = (page) => {
    dispatch(actions.setCurrentPage(page));
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
          versionId: application.latestVersion,
        });

        return (
          <NxTable.Row key={application.applicationPublicId} data-testid="sbom-manager-applications-table">
            <NxTable.Cell>
              <NxTooltip
                title={application.name}
                className="sbom-manager-applications-table__tooltip--application-name"
              >
                <NxTextLink className="sbom-manager-applications-table__application-name" href={applicationHref}>
                  {application.name}
                </NxTextLink>
              </NxTooltip>
            </NxTable.Cell>
            <NxTable.Cell>
              <NxTooltip
                title={application.latestVersion}
                className="sbom-manager-applications-table__tooltip--latest-version"
              >
                <NxTextLink className="sbom-manager-applications-table__latest-version" href={sbomHref}>
                  {application.latestVersion}
                </NxTextLink>
              </NxTooltip>
            </NxTable.Cell>
            <NxTable.Cell> {moment(application.importDate).fromNow()}</NxTable.Cell>
            <NxTable.Cell>
              <NxSmallThreatCounter
                className="sbom-manager-applications-table__violations"
                criticalCount={application.criticalCount}
                severeCount={application.severeCount}
                moderateCount={application.moderateCount}
              />
            </NxTable.Cell>
            <NxTable.Cell>
              <div className="sbom-manager-applications-table__annotated">
                <NxBinaryDonutChart value={application.annotated} aria-label={`${application.annotated}% annotated`} />
                <span>{application.annotated}%</span>
              </div>
            </NxTable.Cell>
          </NxTable.Row>
        );
      })
    : null;

  const showTableContent = !loading && !errorMessage && hasApplications;
  const paginationSection = () => {
    if (showTableContent) {
      const status = cond([
        [equals(0), always(min(APPLICATIONS_PER_PAGE, totalApplicationsCount))],
        [
          allPass([flip(gt)(0), flip(lt)(dec(pagination.pageCount))]),
          always(
            `${formatNumberLocale(inc(pagination.currentPage * APPLICATIONS_PER_PAGE))}—${formatNumberLocale(
              inc(pagination.currentPage) * APPLICATIONS_PER_PAGE
            )}`
          ),
        ],
        [T, always(formatNumberLocale(totalApplicationsCount))],
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
            data-testid="applications-table-pagination-status"
          >
            Showing {status} of {formatNumberLocale(totalApplicationsCount)} applications
          </div>
        </div>
      );
    }
    return null;
  };

  const createColumnSortHandler = (field) =>
    showTableContent && totalApplicationsCount > 1
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
    <div className="sbom-manager-applications nx-table-container">
      <NxTable id="sbom-manager-applications-table" className="sbom-manager-applications-table__table">
        <NxTable.Head>
          <NxTable.Row>
            <NxTable.Cell {...createColumnSortHandler(SORT_BY_FIELDS.name)}>Name</NxTable.Cell>
            <NxTable.Cell {...createColumnSortHandler(SORT_BY_FIELDS.latestVersion)}>Latest Version</NxTable.Cell>
            <NxTable.Cell {...createColumnSortHandler(SORT_BY_FIELDS.importDate)}>Import Date</NxTable.Cell>
            <NxTable.Cell>Violations</NxTable.Cell>
            <NxTable.Cell {...createColumnSortHandler(SORT_BY_FIELDS.annotated)}>Annotated</NxTable.Cell>
          </NxTable.Row>
          <NxTable.Row isFilterHeader>
            <NxTable.Cell>
              <NxFilterInput
                className="sbom-manager-applications-table__filter-input"
                placeholder="Filter by name"
                value=""
              />
            </NxTable.Cell>
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
