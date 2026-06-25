/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useRef, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import {
  NxPageMain,
  NxLoadWrapper,
  NxH1,
  NxPageTitle,
  NxTable,
  NxTableContainer,
  NxFilterInput,
  NxOverflowTooltip,
  NxTile,
  NxPagination,
  NxSmallThreatCounter,
  NxBreadcrumb,
  NxInfoAlert,
} from '@sonatype/react-shared-components';
import { useRouterState } from 'MainRoot/react/RouterStateContext';
import { selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';
import { formatTimeAgo } from 'MainRoot/util/dateUtils';
import { goToComponentReport, goToComponentPriorities } from './hostedReposActions';
import {
  selectComponents,
  selectTotalCount,
  selectPageSize,
  selectCurrentPage,
  selectFilter,
  selectLoading,
  selectError,
  selectHasQueuedScans,
} from './repositoryComponentsSelectors';
import { selectRepositoryManager } from './hostedReposListSelectors';
import { loadComponents, setFilter, reset } from './repositoryComponentsSlice';
import { actions as hostedReposListActions } from './hostedReposListSlice';

export default function RepositoryComponentsList() {
  const dispatch = useDispatch();
  const uiRouterState = useRouterState();
  const params = useSelector(selectRouterCurrentParams);
  const components = useSelector(selectComponents);
  const totalCount = useSelector(selectTotalCount);
  const pageSize = useSelector(selectPageSize);
  const currentPage = useSelector(selectCurrentPage);
  const filter = useSelector(selectFilter);
  const loading = useSelector(selectLoading);
  const error = useSelector(selectError);
  const hasQueuedScans = useSelector(selectHasQueuedScans);

  const repositoryManager = useSelector(selectRepositoryManager);
  const { repositoryId, repositoryManagerId, repositoryPublicId } = params;
  const [inputValue, setInputValue] = useState('');
  const debounceTimer = useRef(null);

  const doLoad = () => {
    if (!repositoryManagerId || !repositoryId) return;
    dispatch(loadComponents({ repositoryManagerId, repositoryId, page: 1, filter }));
  };

  useEffect(() => {
    doLoad();
    return () => {
      dispatch(reset());
      if (debounceTimer.current) clearTimeout(debounceTimer.current);
    };
  }, [repositoryId]);

  // On direct page load or refresh, repositoryManager.name is null because the Redux state
  // was not populated via navigation. Fetch repositories (which also returns manager info)
  // to restore the breadcrumb name.
  // Note: repositoryManager is intentionally not in the dependency array — including it
  // would re-trigger this effect each time the name populates, creating an infinite loop.
  // The !repositoryManager?.name guard prevents re-fetching once the name is loaded.
  const managerName = repositoryManager?.name;
  useEffect(() => {
    if (repositoryManagerId && !managerName) {
      dispatch(hostedReposListActions.loadRepositories({ repositoryManagerId }));
    }
  }, [repositoryManagerId, managerName]);

  const handleFilterChange = (value) => {
    setInputValue(value);
    if (debounceTimer.current) clearTimeout(debounceTimer.current);
    debounceTimer.current = setTimeout(() => {
      dispatch(setFilter(value));
      dispatch(loadComponents({ repositoryManagerId, repositoryId, page: 1, filter: value }));
    }, 300);
  };

  const pageCount = Math.ceil(totalCount / (pageSize || 25)) || 1;

  const handlePageChange = (page) => {
    dispatch(loadComponents({ repositoryManagerId, repositoryId, page, filter }));
  };

  const [isBreadcrumbOpen, setIsBreadcrumbOpen] = useState(false);

  const breadcrumbs = [
    { name: 'Repository Managers', href: uiRouterState.href('hostedRepos') },
    {
      name: repositoryManager?.name || repositoryManagerId,
      href: uiRouterState.href('hostedRepositories', { repositoryManagerId }),
    },
    { name: repositoryPublicId || repositoryId, href: '' },
  ];

  return (
    <NxPageMain id="iq-hosted-repos-components-page">
      <NxBreadcrumb
        crumbs={breadcrumbs}
        isDropdownOpen={isBreadcrumbOpen}
        onToggleDropdown={() => setIsBreadcrumbOpen((o) => !o)}
      />
      <NxPageTitle>
        <NxH1>{repositoryPublicId || repositoryId}</NxH1>
      </NxPageTitle>
      {hasQueuedScans && (
        <NxInfoAlert>
          This repository is currently being evaluated. Report and Priorities results may be incomplete until the queue
          is cleared.
        </NxInfoAlert>
      )}
      <NxLoadWrapper
        loading={loading && components.length === 0}
        error={components.length === 0 ? error : null}
        retryHandler={doLoad}
      >
        <NxTile>
          <NxTile.Content>
            <NxFilterInput
              placeholder="Search by repository component name"
              value={inputValue}
              onChange={handleFilterChange}
              id="iq-hosted-repos-components-filter"
              className="iq-hosted-repos-components__filter"
            />
            <NxTableContainer>
              <NxTable>
                <NxTable.Head>
                  <NxTable.Row>
                    <NxTable.Cell className="iq-hosted-repos-components__col--name">
                      REPOSITORY COMPONENT NAME
                    </NxTable.Cell>
                    <NxTable.Cell className="iq-hosted-repos-components__col--count">COMPONENTS</NxTable.Cell>
                    <NxTable.Cell className="iq-hosted-repos-components__col--report">REPORT</NxTable.Cell>
                  </NxTable.Row>
                </NxTable.Head>
                <NxTable.Body
                  isLoading={loading}
                  error={components.length === 0 ? error : null}
                  retryHandler={doLoad}
                  emptyMessage="No components found"
                >
                  {components.map((component) => (
                    <NxTable.Row key={component.id}>
                      <NxTable.Cell className="iq-hosted-repos-components__col--name">
                        <NxOverflowTooltip>
                          <div className="nx-truncate-ellipsis">{component.displayName}</div>
                        </NxOverflowTooltip>
                      </NxTable.Cell>
                      <NxTable.Cell className="iq-hosted-repos-components__col--count">
                        {component.componentCount ?? 0}
                      </NxTable.Cell>
                      <NxTable.Cell className="iq-hosted-repos-components__col--report">
                        {component.scanId && (
                          <div className="iq-hosted-repos-components__report-cell">
                            {component.violationCount > 0 ? (
                              <NxSmallThreatCounter
                                criticalCount={component.criticalViolationCount || null}
                                severeCount={component.severeViolationCount || null}
                                moderateCount={component.moderateViolationCount || null}
                              />
                            ) : (
                              <span className="iq-hosted-repos-components__report-no-violations">No violations</span>
                            )}
                            <div className="iq-hosted-repos-components__report-meta">
                              {(component.stageTypeId || component.lastEvaluationTime) && (
                                <span className="iq-hosted-repos-components__report-stage">
                                  {[
                                    component.stageTypeId &&
                                      component.stageTypeId.charAt(0).toUpperCase() +
                                        component.stageTypeId.slice(1).toLowerCase(),
                                    component.lastEvaluationTime && formatTimeAgo(component.lastEvaluationTime),
                                  ]
                                    .filter(Boolean)
                                    .join(' | ')}
                                </span>
                              )}
                              <span className="iq-hosted-repos-components__report-links">
                                <button
                                  className="nx-text-link iq-hosted-repos-components__report-link"
                                  disabled={!component.applicationPublicId || !component.scanId || hasQueuedScans}
                                  title={hasQueuedScans ? 'In queue — results not yet available' : undefined}
                                  onClick={(e) => {
                                    e.stopPropagation();
                                    dispatch(
                                      goToComponentReport(
                                        component.applicationPublicId,
                                        component.scanId,
                                        repositoryManagerId,
                                        repositoryId,
                                        repositoryPublicId,
                                        component.displayName
                                      )
                                    );
                                  }}
                                >
                                  Report
                                </button>
                                {' | '}
                                <button
                                  className="nx-text-link iq-hosted-repos-components__report-link"
                                  disabled={!component.applicationPublicId || !component.scanId || hasQueuedScans}
                                  title={hasQueuedScans ? 'In queue — results not yet available' : undefined}
                                  onClick={(e) => {
                                    e.stopPropagation();
                                    dispatch(goToComponentPriorities(component.applicationPublicId, component.scanId));
                                  }}
                                >
                                  Priorities
                                </button>
                              </span>
                            </div>
                          </div>
                        )}
                      </NxTable.Cell>
                    </NxTable.Row>
                  ))}
                </NxTable.Body>
              </NxTable>
            </NxTableContainer>
            {pageCount > 1 && (
              <NxPagination
                pageCount={pageCount}
                currentPage={currentPage - 1}
                onChange={(page) => handlePageChange(page + 1)}
              />
            )}
          </NxTile.Content>
        </NxTile>
      </NxLoadWrapper>
    </NxPageMain>
  );
}
