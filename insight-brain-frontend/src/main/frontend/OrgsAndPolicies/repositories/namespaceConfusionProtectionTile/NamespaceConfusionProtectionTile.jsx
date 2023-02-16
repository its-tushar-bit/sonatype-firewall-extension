/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { useDispatch, useSelector } from 'react-redux';
import {
  NxH2,
  NxTable,
  NxTextLink,
  NxTile,
  NxTableContainer,
  NxFilterInput,
  NxOverflowTooltip,
  NxIndeterminatePagination,
} from '@sonatype/react-shared-components';
import { actions } from './namespaceConfusionProtectionTileSlice';
import {
  selectSortFields,
  selectSearchFiltersValues,
  selectComponentNamePatterns,
  selectLoadingComponentNamePatterns,
  selectErrorComponentsTable,
  selectCurrentPage,
  selectHasNextPage,
} from './namespaceConfusionProtectionTileSelectors';

const NamespaceConfusionProtectionTile = () => {
  const dispatch = useDispatch();

  const sortFields = useSelector(selectSortFields);
  const searchFiltersValues = useSelector(selectSearchFiltersValues);
  const componentNamePatterns = useSelector(selectComponentNamePatterns);
  const loadingComponentNamePatterns = useSelector(selectLoadingComponentNamePatterns);
  const errorComponentsTable = useSelector(selectErrorComponentsTable);
  const currentPage = useSelector(selectCurrentPage);
  const hasNextPage = useSelector(selectHasNextPage);

  const loadRepositories = () => dispatch(actions.getComponentNamePatterns());
  const searchComponents = (policyName) => dispatch(actions.searchComponents(policyName));
  const loadPreviousPage = () => dispatch(actions.loadPreviousPage());
  const loadNextPage = () => dispatch(actions.loadNextPage());
  const sortComponents = (columnName) => dispatch(actions.sortComponents(columnName));

  React.useEffect(() => {
    loadRepositories();
  }, []);

  const namePageTableRow = componentNamePatterns.map((row) => (
    <NxTable.Row key={row.id}>
      <NxTable.Cell className="iq-repository-cell--name-space">
        <NxOverflowTooltip title={row.namespacePattern || row.namePattern}>
          <div className="iq-repository-cell--name-space-text">{row.namespacePattern || row.namePattern}</div>
        </NxOverflowTooltip>
      </NxTable.Cell>
      <NxTable.Cell className="iq-repository-cell-manager">
        <NxOverflowTooltip title={row.repositoryManagerInstanceId}>
          <div className="iq-repository-cell-manager--text">{row.repositoryManagerInstanceId}</div>
        </NxOverflowTooltip>
      </NxTable.Cell>
      <NxTable.Cell className="iq-repository-cell-repository">
        <NxOverflowTooltip title={row.repositoryPublicId}>
          <div className="iq-repository-cell-repository--text">{row.repositoryPublicId}</div>
        </NxOverflowTooltip>
      </NxTable.Cell>
    </NxTable.Row>
  ));

  const getHighlightedArrowState = (columnName) => {
    const currentHighligtedColumn = sortFields[0];
    return currentHighligtedColumn.columnName === columnName ? currentHighligtedColumn.dir : null;
  };

  return (
    <NxTile id="repositories-pill-configurations" data-testid="repositories-pill-configurations">
      <NxTile.Header>
        <NxTile.HeaderTitle>
          <NxH2>Namespace Confusion Protection</NxH2>
        </NxTile.HeaderTitle>
        <NxTile.HeaderSubtitle>
          {'This list shows the current status of '}
          <NxTextLink newTab external>
            namespace confusion protection.
          </NxTextLink>
        </NxTile.HeaderSubtitle>
      </NxTile.Header>
      <NxTile.Content>
        <NxTableContainer id="iq-repository-component-name-table">
          <NxTable data-testid="iq-repository-component-name-table">
            <NxTable.Head>
              <NxTable.Row>
                <NxTable.Cell
                  isSortable
                  sortDir={getHighlightedArrowState('PROPRIETARY_COMPONENT_NAMESPACE_OR_NAME')}
                  onClick={() => sortComponents('PROPRIETARY_COMPONENT_NAMESPACE_OR_NAME')}
                  className="iq-repository-column--name-space"
                >
                  Component Namespaces
                </NxTable.Cell>
                <NxTable.Cell
                  isSortable
                  sortDir={getHighlightedArrowState('REPOSITORY_MANAGER_INSTANCE_ID')}
                  onClick={() => sortComponents('REPOSITORY_MANAGER_INSTANCE_ID')}
                  className="iq-repository-column--manager"
                >
                  Repository Manager
                </NxTable.Cell>
                <NxTable.Cell
                  isSortable
                  sortDir={getHighlightedArrowState('REPOSITORY_PUBLIC_ID')}
                  onClick={() => sortComponents('REPOSITORY_PUBLIC_ID')}
                  className="iq-repository-column--repository"
                >
                  Repository
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
                <NxTable.Cell />
                <NxTable.Cell />
              </NxTable.Row>
            </NxTable.Head>
            <NxTable.Body
              id="iq-proprietary-table-body"
              retryHandler={loadRepositories}
              isLoading={loadingComponentNamePatterns}
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
