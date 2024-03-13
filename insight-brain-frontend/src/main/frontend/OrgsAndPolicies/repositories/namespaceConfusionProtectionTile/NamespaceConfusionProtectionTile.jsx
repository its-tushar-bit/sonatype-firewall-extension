/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { useDispatch, useSelector } from 'react-redux';
import {
  NxFilterInput,
  NxH2,
  NxIndeterminatePagination,
  NxLoadWrapper,
  NxOverflowTooltip,
  NxTable,
  NxTableContainer,
  NxTextLink,
  NxTile,
  NxToggle,
} from '@sonatype/react-shared-components';
import { actions, DEFAULT_KEY_FILTER_SECTION } from './namespaceConfusionProtectionTileSlice';
import {
  selectComponentNamePatterns,
  selectCurrentPage,
  selectErrorComponentsTable,
  selectErrorUpdatingComponentNamePattern,
  selectHasNextPage,
  selectSearchFiltersValues,
  selectSortFields,
  selectUpdatingComponentNamePattern,
} from './namespaceConfusionProtectionTileSelectors';
import {
  selectIsRepositoryContainer,
  selectIsRepositoryManager,
  selectIsRepository,
} from 'MainRoot/reduxUiRouter/routerSelectors';

const NamespaceConfusionProtectionTile = ({ sortFilterSectionValues = DEFAULT_KEY_FILTER_SECTION }) => {
  const dispatch = useDispatch();

  const sortFields = useSelector(selectSortFields);
  const searchFiltersValues = useSelector(selectSearchFiltersValues);
  const componentNamePatterns = useSelector(selectComponentNamePatterns);
  const errorComponentsTable = useSelector(selectErrorComponentsTable);
  const currentPage = useSelector(selectCurrentPage);
  const hasNextPage = useSelector(selectHasNextPage);
  const updatingComponentNamePattern = useSelector(selectUpdatingComponentNamePattern);
  const errorUpdatingComponentNamePattern = useSelector(selectErrorUpdatingComponentNamePattern);
  const isRepositoryManager = useSelector(selectIsRepositoryManager);
  const isRepositoryContainer = useSelector(selectIsRepositoryContainer);
  const isRepository = useSelector(selectIsRepository);

  const loadRepositories = () => dispatch(actions.getComponentNamePatterns());
  const searchComponents = (policyName) => dispatch(actions.searchComponents(policyName));
  const loadPreviousPage = () => dispatch(actions.loadPreviousPage());
  const loadNextPage = () => dispatch(actions.loadNextPage());
  const sortComponents = (columnName) => dispatch(actions.sortComponents(columnName));
  const setEnabledStatus = (componentId) => dispatch(actions.setEnabledStatus(componentId));
  const setCurrentFilterKey = () => dispatch(actions.setCurrentFilterKey(sortFilterSectionValues));

  React.useEffect(() => {
    setCurrentFilterKey();
    loadRepositories();
  }, []);

  const namePageTableRow = componentNamePatterns.map((row) => (
    <NxTable.Row key={row.id}>
      <NxTable.Cell className="iq-repository-cell--name-space">
        <NxOverflowTooltip title={row.namespacePattern || row.namePattern}>
          <div className="iq-repository-cell--name-space-text nx-truncate-ellipsis">
            {row.namespacePattern || row.namePattern}
          </div>
        </NxOverflowTooltip>
      </NxTable.Cell>
      {isRepositoryContainer && (
        <NxTable.Cell className="iq-repository-cell-manager">
          <NxOverflowTooltip title={row.repositoryManagerName || row.repositoryManagerInstanceId}>
            <div className="iq-repository-cell-manager--text nx-truncate-ellipsis">
              {row.repositoryManagerName || row.repositoryManagerInstanceId}
            </div>
          </NxOverflowTooltip>
        </NxTable.Cell>
      )}
      {!isRepository && (
        <NxTable.Cell className="iq-repository-cell-repository">
          <NxOverflowTooltip title={row.repositoryPublicId}>
            <div className="iq-repository-cell-repository--text nx-truncate-ellipsis">{row.repositoryPublicId}</div>
          </NxOverflowTooltip>
        </NxTable.Cell>
      )}
      <NxTable.Cell className="iq-repository-cell-enabled">
        <NxLoadWrapper retryHandler={setEnabledStatus} error={errorUpdatingComponentNamePattern}>
          <NxToggle
            id="iq-repository-component-enabled-toggle"
            isChecked={row.enabled}
            onChange={() => setEnabledStatus(row.id)}
            disabled={updatingComponentNamePattern}
          />
        </NxLoadWrapper>
      </NxTable.Cell>
    </NxTable.Row>
  ));

  const getHighlightedArrowState = (columnName) => {
    const currentHighligtedColumn = sortFields[0];
    return currentHighligtedColumn.columnName === columnName ? currentHighligtedColumn.dir : null;
  };

  const conditionalClassName = isRepositoryManager
    ? 'iq-repository-component-name-table-rm-level'
    : isRepository
    ? 'iq-repository-component-name-table-repository-level'
    : '';

  return (
    <NxTile
      id="namespace-confusion-protection-pill-configuration"
      data-testid="namespace-confusion-protection-pill-configuration"
    >
      <NxTile.Header>
        <NxTile.HeaderTitle>
          <NxH2>Namespace Confusion Protection</NxH2>
        </NxTile.HeaderTitle>
        <NxTile.HeaderSubtitle>
          {'This list shows the current status of '}
          <NxTextLink newTab external href="https://links.sonatype.com/nexus-firewall/preventing-namespace-confusion">
            namespace confusion protection
          </NxTextLink>
        </NxTile.HeaderSubtitle>
      </NxTile.Header>
      <NxTile.Content>
        <NxTableContainer id="iq-repository-component-name-table">
          <NxTable data-testid="iq-repository-component-name-table" className={conditionalClassName}>
            <NxTable.Head className="nx-table-head">
              <NxTable.Row>
                <NxTable.Cell
                  isSortable
                  sortDir={getHighlightedArrowState('PROPRIETARY_COMPONENT_NAMESPACE_OR_NAME')}
                  onClick={() => sortComponents('PROPRIETARY_COMPONENT_NAMESPACE_OR_NAME')}
                  className="iq-repository-column--name-space"
                >
                  <span className="nx-truncate-ellipsis">Component Namespace</span>
                </NxTable.Cell>
                {isRepositoryContainer && (
                  <NxTable.Cell
                    isSortable
                    sortDir={getHighlightedArrowState('REPOSITORY_MANAGER_INSTANCE_ID_OR_NAME')}
                    onClick={() => sortComponents('REPOSITORY_MANAGER_INSTANCE_ID_OR_NAME')}
                    className="iq-repository-column--manager"
                  >
                    <span className="nx-truncate-ellipsis">Repository Manager</span>
                  </NxTable.Cell>
                )}
                {!isRepository && (
                  <NxTable.Cell
                    isSortable
                    sortDir={getHighlightedArrowState('REPOSITORY_PUBLIC_ID')}
                    onClick={() => sortComponents('REPOSITORY_PUBLIC_ID')}
                    className="iq-repository-column--repository"
                  >
                    <span className="nx-truncate-ellipsis">Repository</span>
                  </NxTable.Cell>
                )}
                <NxTable.Cell
                  isSortable
                  sortDir={getHighlightedArrowState('ENABLED')}
                  onClick={() => sortComponents('ENABLED')}
                  className="iq-repository-column--enabled"
                >
                  <span className="nx-truncate-ellipsis">Enabled</span>
                </NxTable.Cell>
              </NxTable.Row>
              <NxTable.Row isFilterHeader>
                <NxTable.Cell className="iq-repository-filter--name-space">
                  <NxFilterInput
                    name="PROPRIETARY_COMPONENT_NAMESPACE_OR_NAME"
                    placeholder="Filter"
                    id="nx-repository-name-space-filter"
                    onChange={(filterValue) =>
                      searchComponents({ filterValue, filterName: 'PROPRIETARY_COMPONENT_NAMESPACE_OR_NAME' })
                    }
                    value={searchFiltersValues['PROPRIETARY_COMPONENT_NAMESPACE_OR_NAME']}
                  />
                </NxTable.Cell>
                {!isRepositoryManager && <NxTable.Cell />}
                <NxTable.Cell />
                <NxTable.Cell />
              </NxTable.Row>
            </NxTable.Head>
            <NxTable.Body
              id="iq-proprietary-table-body"
              className="nx-table-body"
              retryHandler={loadRepositories}
              error={errorComponentsTable}
              emptyMessage="No results"
            >
              {namePageTableRow}
            </NxTable.Body>
          </NxTable>
          <div className="nx-table-container__footer">
            <NxIndeterminatePagination
              className="testButton"
              data-testid="components-table-pagination"
              isFirstPage={currentPage === 1}
              isLastPage={!hasNextPage}
              onPrevPageSelect={loadPreviousPage}
              onNextPageSelect={loadNextPage}
            />
          </div>
        </NxTableContainer>
      </NxTile.Content>
    </NxTile>
  );
};

export default NamespaceConfusionProtectionTile;
