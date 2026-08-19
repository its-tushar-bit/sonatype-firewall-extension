/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useState, useRef, useCallback } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { useRouterState } from 'MainRoot/react/RouterStateContext';
// The Classic bundle pulls this SCSS partial through `scss/scss.scss`; the Nexus One
// bundle (which embeds this page natively) does not. Importing it here makes the page
// self-contained for any host bundle. CLM-42184.
import './_hostedReposListPage.scss';
import {
  NxLoadWrapper,
  NxFilterInput,
  NxFormSelect,
  NxTable,
  NxBreadcrumb,
  NxPageTitle,
  NxH1,
  NxTile,
} from '@sonatype/react-shared-components';
import { formatTimeAgo } from 'MainRoot/util/dateUtils';
import { selectRepositoryManagerId } from 'MainRoot/reduxUiRouter/routerSelectors';
import { stateGo } from 'MainRoot/reduxUiRouter/routerActions';
import { selectIsHostedRepositoryEvaluationEnabled } from 'MainRoot/productFeatures/productFeaturesSelectors';
import { hostedReposState } from './hostedReposNavigation';
import { actions } from './hostedReposListSlice';
import {
  selectRepositories,
  selectLoading,
  selectLoadError,
  selectRepositoryFormatsFilter,
  selectRepositoryManager,
  selectAvailableFormats,
  selectSortConfiguration,
  selectTotalCount,
  selectSearchText,
} from './hostedReposListSelectors';

function HostedRepositoriesListPage() {
  const dispatch = useDispatch();
  const uiRouterState = useRouterState();
  const repositoryManagerId = useSelector(selectRepositoryManagerId);
  const isFeatureEnabled = useSelector(selectIsHostedRepositoryEvaluationEnabled);
  const repositories = useSelector(selectRepositories);
  const loading = useSelector(selectLoading);
  const loadError = useSelector(selectLoadError);
  const formatsFilter = useSelector(selectRepositoryFormatsFilter);
  const repositoryManager = useSelector(selectRepositoryManager);
  const availableFormats = useSelector(selectAvailableFormats);
  const sortConfiguration = useSelector(selectSortConfiguration);
  const totalCount = useSelector(selectTotalCount);
  const searchText = useSelector(selectSearchText);
  const [inputText, setInputText] = useState(searchText);

  const debounceTimer = useRef(null);

  useEffect(() => {
    if (repositoryManagerId && isFeatureEnabled) {
      dispatch(actions.loadAvailableFormats(repositoryManagerId));
    }
  }, [repositoryManagerId, isFeatureEnabled, dispatch]);

  const doLoad = useCallback(() => {
    if (repositoryManagerId && isFeatureEnabled) {
      const sortBy = sortConfiguration[0]?.key;
      const sortDir = sortConfiguration[0]?.dir;
      const format = formatsFilter || undefined;

      dispatch(
        actions.loadRepositories({
          repositoryManagerId,
          searchText: searchText || undefined,
          format,
          sortBy,
          sortDir,
        })
      );
    }
  }, [repositoryManagerId, isFeatureEnabled, searchText, formatsFilter, sortConfiguration, dispatch]);

  useEffect(() => {
    doLoad();
  }, [doLoad]);

  const handleSearchChange = useCallback(
    (value) => {
      setInputText(value);

      if (debounceTimer.current) {
        clearTimeout(debounceTimer.current);
      }

      debounceTimer.current = setTimeout(() => {
        dispatch(actions.setSearchText(value));
      }, 300);
    },
    [dispatch]
  );

  useEffect(() => {
    return () => {
      if (debounceTimer.current) {
        clearTimeout(debounceTimer.current);
      }
    };
  }, []);

  const handleFormatChange = (value) => {
    dispatch(actions.setRepositoryFormatsFilter(value));
  };

  const handleSort = (column) => {
    dispatch(actions.sortRepositories(column));
  };

  const handleRepoClick = (repo) => {
    dispatch(
      stateGo(hostedReposState('hostedRepoComponents'), {
        repositoryManagerId,
        repositoryId: repo.id,
        repositoryPublicId: repo.publicId,
      })
    );
  };

  const getSortDirection = (columnKey) => {
    const sortConfig = sortConfiguration[0];
    if (!sortConfig || sortConfig.key !== columnKey) {
      return null;
    }
    return sortConfig.dir;
  };

  const formatLastScanned = (lastScannedTime, hasQueuedScans) => {
    if (hasQueuedScans) {
      return 'In queue';
    }
    if (!lastScannedTime) {
      return '--';
    }
    return formatTimeAgo(lastScannedTime) || '--';
  };

  const breadcrumbs = repositoryManager?.instanceId
    ? [
        { name: 'Repository Managers', href: uiRouterState.href(hostedReposState('hostedRepos')) },
        { name: repositoryManager.name || repositoryManager.instanceId },
      ]
    : [];

  if (!isFeatureEnabled) {
    return null;
  }

  return (
    <main className="nx-page-main iq-hosted-repos-list-page">
      {breadcrumbs.length > 0 && <NxBreadcrumb crumbs={breadcrumbs} />}

      <NxPageTitle>
        <NxH1 className="iq-hosted-repos-list-page__title">
          {repositoryManager?.name || repositoryManager?.instanceId || 'Repository Manager'}
        </NxH1>
        {repositoryManager?.baseUrl && <NxPageTitle.Description>{repositoryManager.baseUrl}</NxPageTitle.Description>}
      </NxPageTitle>

      <NxTile>
        <NxTile.Content>
          <div className="iq-hosted-repos-list-page__filters">
            <NxFilterInput
              id="repository-search-input"
              className="iq-hosted-repos-list-page__search"
              searchIcon
              placeholder="Search repositories"
              value={inputText}
              onChange={handleSearchChange}
            />
            <NxFormSelect
              className="iq-hosted-repos-list-page__format-filter"
              value={formatsFilter}
              onChange={handleFormatChange}
            >
              <option value="">All</option>
              {availableFormats.map((format) => (
                <option key={format} value={format}>
                  {format}
                </option>
              ))}
            </NxFormSelect>
          </div>

          <NxLoadWrapper loading={loading} error={loadError} retryHandler={doLoad}>
            <div className="iq-hosted-repos-list-page__count-bar">
              <span className="iq-hosted-repos-list-page__count">
                {repositories && totalCount > repositories.length
                  ? `${repositories.length} of ${totalCount} repositories`
                  : `${totalCount} ${totalCount === 1 ? 'repository' : 'repositories'}`}
              </span>
            </div>
            <NxTable className="iq-hosted-repos-list-page__table">
              <NxTable.Head>
                <NxTable.Row>
                  <NxTable.Cell
                    isSortable
                    sortDir={getSortDirection('publicId')}
                    onClick={() => handleSort('publicId')}
                  >
                    NAME
                  </NxTable.Cell>
                  <NxTable.Cell isSortable sortDir={getSortDirection('format')} onClick={() => handleSort('format')}>
                    FORMAT
                  </NxTable.Cell>
                  <NxTable.Cell
                    isSortable
                    sortDir={getSortDirection('lastScannedTime')}
                    onClick={() => handleSort('lastScannedTime')}
                  >
                    LATEST COMPONENT EVALUATION
                  </NxTable.Cell>
                  <NxTable.Cell chevron />
                </NxTable.Row>
              </NxTable.Head>
              <NxTable.Body emptyMessage="No repositories found">
                {repositories &&
                  repositories.map((repo, index) => (
                    <NxTable.Row key={repo.publicId ?? index} isClickable onClick={() => handleRepoClick(repo)}>
                      <NxTable.Cell>{repo.publicId}</NxTable.Cell>
                      <NxTable.Cell>{repo.format}</NxTable.Cell>
                      <NxTable.Cell>{formatLastScanned(repo.lastScannedTime, repo.hasQueuedScans)}</NxTable.Cell>
                      <NxTable.Cell chevron />
                    </NxTable.Row>
                  ))}
              </NxTable.Body>
            </NxTable>
          </NxLoadWrapper>
        </NxTile.Content>
      </NxTile>
    </main>
  );
}

export default HostedRepositoriesListPage;
