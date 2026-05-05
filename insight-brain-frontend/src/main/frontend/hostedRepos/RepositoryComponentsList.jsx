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
} from '@sonatype/react-shared-components';
import { useRouterState } from 'MainRoot/react/RouterStateContext';
import { selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';
import { goToComponentReport } from './hostedReposActions';
import {
  selectComponents,
  selectTotalCount,
  selectPageSize,
  selectCurrentPage,
  selectFilter,
  selectLoading,
  selectError,
} from './repositoryComponentsSelectors';
import { loadComponents, setFilter, reset } from './repositoryComponentsSlice';

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

  const { repositoryId, repositoryManagerId, repositoryPublicId } = params;
  const [inputValue, setInputValue] = useState('');
  const debounceTimer = useRef(null);

  const doLoad = () => {
    dispatch(loadComponents({ repositoryManagerId, repositoryId, page: 1, filter }));
  };

  useEffect(() => {
    doLoad();
    return () => {
      dispatch(reset());
      if (debounceTimer.current) clearTimeout(debounceTimer.current);
    };
  }, [repositoryId]);

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
    { name: repositoryManagerId, href: uiRouterState.href('hostedRepositories', { repositoryManagerId }) },
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
      <NxFilterInput
        placeholder="Search by repository component name"
        value={inputValue}
        onChange={handleFilterChange}
        id="iq-hosted-repos-components-filter"
        className="iq-hosted-repos-components__filter"
      />
      <NxLoadWrapper loading={loading && components.length === 0} error={components.length === 0 ? error : null} retryHandler={doLoad}>
        <NxTile>
          <NxTile.Content>
            <NxTableContainer>
              <NxTable>
                <NxTable.Head>
                  <NxTable.Row>
                    <NxTable.Cell className="iq-hosted-repos-components__col--name">REPOSITORY COMPONENT NAME</NxTable.Cell>
                    <NxTable.Cell className="iq-hosted-repos-components__col--count">COMPONENTS</NxTable.Cell>
                    <NxTable.Cell className="iq-hosted-repos-components__col--report">REPORT</NxTable.Cell>
                  </NxTable.Row>
                </NxTable.Head>
                <NxTable.Body isLoading={loading} error={components.length === 0 ? error : null} retryHandler={doLoad} emptyMessage="No components found">
                  {components.map((component) => (
                    <NxTable.Row
                      key={component.id}
                    >
                      <NxTable.Cell className="iq-hosted-repos-components__col--name">
                        <NxOverflowTooltip>
                          <div className="nx-truncate-ellipsis">{component.displayName}</div>
                        </NxOverflowTooltip>
                      </NxTable.Cell>
                      <NxTable.Cell className="iq-hosted-repos-components__col--count">
                        {component.violationCount ?? 0}
                      </NxTable.Cell>
                      <NxTable.Cell className="iq-hosted-repos-components__col--report">
                        {component.violationCount > 0 && (
                          <div className="iq-hosted-repos-components__report-cell">
                            <NxSmallThreatCounter
                              criticalCount={component.criticalViolationCount || null}
                              severeCount={component.severeViolationCount || null}
                              moderateCount={component.moderateViolationCount || null}
                            />
                            <div className="iq-hosted-repos-components__report-meta">
                              <span className="iq-hosted-repos-components__report-links">
                                <button
                                  className="nx-text-link iq-hosted-repos-components__report-link"
                                  onClick={(e) => {
                                    e.stopPropagation();
                                    dispatch(goToComponentReport(repositoryId));
                                  }}
                                >
                                  Report
                                </button>
                                {' | '}
                                <button className="nx-text-link iq-hosted-repos-components__report-link">
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
