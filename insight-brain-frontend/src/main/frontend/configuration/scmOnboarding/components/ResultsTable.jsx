/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import {
  NxCheckbox,
  NxTable,
  NxTableBody,
  NxTableCell,
  NxTableHead,
  NxTableRow,
  NxTooltip,
  NxPagination,
  NxFontAwesomeIcon,
  NxOverflowTooltip,
  NxTextLink,
} from '@sonatype/react-shared-components';
import React, { useMemo, useState } from 'react';
import * as PropTypes from 'prop-types';
import { repositoryPropType } from '../scmPropTypes';
import NxFilterInput from '@sonatype/react-shared-components/components/NxFilterInput/NxFilterInput';
import { propSet } from '../../../util/jsUtil';
import { faQuestionCircle } from '@fortawesome/pro-solid-svg-icons';

export default function ResultsTable(props) {
  const {
    loadingRepositories,
    repositories,

    // sorting
    sortConfiguration,

    // selection
    isAllChecked,
    selectedRepositories,

    // actions
    setSortingParameters,
    setIsAllChecked,
    setSelectedRepositories,
  } = props;

  const [filters, setFilters] = useState({
      project: '',
      namespace: '',
      description: '',
    }),
    [page, setPage] = useState(0);

  const maxRowsPerPage = 15;

  const filteredRepos = useMemo(
    () => (repositories === null ? null : repositories.filter(isRepositorySelectedByFilters)),
    [repositories, filters]
  );

  function getPageCount() {
    return Math.ceil(filteredRepos.length / maxRowsPerPage);
  }

  // gets page without 'undefined'
  function getDefinedPage() {
    return page === undefined ? 0 : page;
  }

  const currentPagedRepos = useMemo(() => {
    let currentPage = getDefinedPage();
    return filteredRepos.slice(currentPage * maxRowsPerPage, (currentPage + 1) * maxRowsPerPage);
  }, [page, filteredRepos]);

  function getCurrentPage() {
    if (getPageCount() <= 0) {
      return undefined;
    }
    let currentPage = getDefinedPage();
    return Math.max(Math.min(currentPage, getPageCount() - 1), 0);
  }

  function setCurrentPage(newPage) {
    // when page is changed, reset selections
    setSelectedRepositories([]);
    setIsAllChecked(false);
    setPage(newPage);
  }

  function canRenderPagination() {
    return !loadingRepositories && getPageCount() > 0 && page !== undefined;
  }

  const sortDirProject = getDirection(sortConfiguration, 'project');
  const sortDirNamespace = getDirection(sortConfiguration, 'namespace');
  const sortDirDescription = getDirection(sortConfiguration, 'description');
  const sortDirDefaultBranch = getDirection(sortConfiguration, 'defaultBranch');

  function getDirection(sortConfig, key) {
    return sortConfig && sortConfig.key === key ? sortConfig.dir : null;
  }

  const sortSettingsProject = {
    key: 'project',
    sortingOrder: ['project', 'namespace', 'description', 'defaultBranch'],
  };
  const sortSettingsNamespace = {
    key: 'namespace',
    sortingOrder: ['namespace', 'project', 'description', 'defaultBranch'],
  };
  const sortSettingsDescription = {
    key: 'description',
    sortingOrder: ['description', 'namespace', 'project', 'defaultBranch'],
  };
  const sortSettingsDefaultBranch = {
    key: 'defaultBranch',
    sortingOrder: ['defaultBranch', 'namespace', 'project', 'description'],
  };

  function requestSort(settings) {
    let direction = 'asc';
    if (sortConfiguration && sortConfiguration.key === settings.key && sortConfiguration.dir === 'asc') {
      direction = 'desc';
    }
    const sortingOrder = settings.sortingOrder;
    if (direction === 'desc' && !sortingOrder[0].startsWith('-')) {
      sortingOrder[0] = '-'.concat(sortingOrder[0]);
    }
    if (direction === 'asc' && sortingOrder[0].startsWith('-')) {
      sortingOrder[0] = sortingOrder[0].substring(1);
    }
    setSortingParameters(settings.key, sortingOrder, direction);
  }

  function isRepositorySelectedByFilters(repository) {
    return (
      isRepositorySelectedByFilter(repository, 'project', filters.project) &&
      isRepositorySelectedByFilter(repository, 'namespace', filters.namespace) &&
      isRepositorySelectedByFilter(repository, 'description', filters.description)
    );
  }

  function isRepositorySelectedByFilter(repository, filterName, filterValue) {
    // under some cases, fields like 'description' may be null. In this case, treat them as if they were ''
    if (!repository || !repository[filterName]) {
      return !filterValue;
    }
    return repository[filterName].toLowerCase().includes(filterValue.toLowerCase());
  }

  function toggleSelectAll() {
    setIsAllChecked(!isAllChecked);
    setSelectedRepositories(currentPagedRepos.filter(() => !isAllChecked));
  }

  function changeFilter(filterName, filterValue) {
    setFilters(propSet(filterName, filterValue, filters));
    setSelectedRepositories(
      repositories.filter(
        (repo) => isRepositorySelectedByFilter(repo, filterName, filterValue) && selectedRepositories.includes(repo)
      )
    );

    // when filters change, always reset to the first page
    setPage(0);
  }

  return (
    <div className="nx-table-container onboarding-repo-table">
      <NxTable id="iq-scm-onboarding-repositories">
        <NxTableHead>
          <NxTableRow>
            <NxTableCell>Selection</NxTableCell>
            <NxTableCell
              id="namespace-header"
              isSortable
              sortDir={sortDirNamespace}
              onClick={() => requestSort(sortSettingsNamespace)}
            >
              Namespace
            </NxTableCell>
            <NxTableCell
              id="project-header"
              isSortable
              sortDir={sortDirProject}
              onClick={() => requestSort(sortSettingsProject)}
            >
              Project
            </NxTableCell>
            <NxTableCell
              id="description-header"
              isSortable
              sortDir={sortDirDescription}
              onClick={() => requestSort(sortSettingsDescription)}
            >
              Description
            </NxTableCell>
            <NxTableCell
              id="defaultBranch-header"
              isSortable
              sortDir={sortDirDefaultBranch}
              onClick={() => requestSort(sortSettingsDefaultBranch)}
            >
              Default Branch
            </NxTableCell>
          </NxTableRow>
          <NxTableRow isFilterHeader>
            <NxTableCell className="iq-scmonboarding__select-all-cell">
              <NxCheckbox checkboxId="iq-scmonboarding-select-all" isChecked={isAllChecked} onChange={toggleSelectAll}>
                All
              </NxCheckbox>
            </NxTableCell>
            <NxTableCell className="iq-scmonboarding__filter-cell iq-scmonboarding-overflow-tooltip-cell">
              <NxFilterInput
                id="iq-scmonboarding-namespace-filter"
                value={filters.namespace}
                onChange={(filterValue) => changeFilter('namespace', filterValue)}
              />
            </NxTableCell>
            <NxTableCell className="iq-scmonboarding__filter-cell iq-scmonboarding-overflow-tooltip-cell">
              <NxFilterInput
                id="iq-scmonboarding-project-filter"
                value={filters.project}
                onChange={(filterValue) => changeFilter('project', filterValue)}
              />
            </NxTableCell>
            <NxTableCell className="iq-scmonboarding__filter-cell iq-scmonboarding-overflow-tooltip-cell">
              <NxFilterInput
                id="iq-scmonboarding-description-filter"
                value={filters.description}
                onChange={(filterValue) => changeFilter('description', filterValue)}
              />
            </NxTableCell>
            <NxTableCell />
          </NxTableRow>
        </NxTableHead>
        <NxTableBody emptyMessage="No matching repositories.">
          {currentPagedRepos.map((repo) => (
            <RepositoryRow
              repo={repo}
              key={repo.httpCloneUrl}
              rowKey={repo.httpCloneUrl}
              selectedRepositories={selectedRepositories}
              setSelectedRepositories={setSelectedRepositories}
            />
          ))}
        </NxTableBody>
      </NxTable>
      {canRenderPagination() && (
        <div className="nx-table-container__footer">
          <NxPagination onChange={setCurrentPage} pageCount={getPageCount()} currentPage={getCurrentPage()} />
        </div>
      )}
    </div>
  );
}

ResultsTable.propTypes = {
  loadingRepositories: PropTypes.bool,
  repositories: PropTypes.arrayOf(PropTypes.shape(repositoryPropType)),
  totalRepositories: PropTypes.number,
  selectedRepositoryCount: PropTypes.number,
  importedRepositoryCount: PropTypes.number,
  selectedRepositories: PropTypes.arrayOf(PropTypes.shape(repositoryPropType)),
  isAllChecked: PropTypes.bool,
  sortConfiguration: PropTypes.shape({
    sortFields: PropTypes.arrayOf(PropTypes.string),
    dir: PropTypes.string,
    key: PropTypes.string,
  }),

  // actions
  setSortingParameters: PropTypes.func,
  onRepositorySelectionChanged: PropTypes.func.isRequired,
  setIsAllChecked: PropTypes.func.isRequired,
  setSelectedRepositories: PropTypes.func.isRequired,
};

export function RepositoryRow(props) {
  const { rowKey, repo, setSelectedRepositories, selectedRepositories } = props;
  const DEFAULT_BRANCH_NOT_DEFINED = '';
  const UNKNOWN_DEFAULT_BRANCH = 'UNKNOWN_DEFAULT_BRANCH';

  const toggleSelection = () => {
    setSelectedRepositories(
      selectedRepositories.includes(repo)
        ? selectedRepositories.filter((selectedRepo) => selectedRepo !== repo)
        : selectedRepositories.concat([repo])
    );
  };

  function isDefaultBranchValid() {
    return !isDefaultBranchNotDefined() && !isDefaultBranchUnknown();
  }

  function isDefaultBranchNotDefined() {
    return repo.defaultBranch === DEFAULT_BRANCH_NOT_DEFINED;
  }

  function isDefaultBranchUnknown() {
    return repo.defaultBranch === UNKNOWN_DEFAULT_BRANCH;
  }

  function getDefaultBranchTooltipMessage() {
    if (isDefaultBranchNotDefined()) {
      return 'Repository does not have a default branch configured.';
    }

    if (isDefaultBranchUnknown()) {
      return 'The default branch is not yet known. It will be retrieved on import.';
    }
  }

  function getDefaultBranchText() {
    if (isDefaultBranchNotDefined()) {
      return 'Not defined';
    }

    if (isDefaultBranchUnknown()) {
      return 'Unknown';
    }

    return repo.defaultBranch;
  }

  function renderDefaultBranch() {
    let defaultBranchText = getDefaultBranchText();

    if (isDefaultBranchValid()) {
      return repo.defaultBranch;
    }

    return renderDefaultBranchWithTooltip(defaultBranchText);
  }

  function renderDefaultBranchWithTooltip(defaultBranchText) {
    return (
      <div>
        {defaultBranchText}
        <NxTooltip id="default-branch-tooltip" title={getDefaultBranchTooltipMessage()}>
          <span id="default-branch-question-icon">
            <NxFontAwesomeIcon icon={faQuestionCircle} color="blue" />
          </span>
        </NxTooltip>
      </div>
    );
  }

  return (
    <NxTableRow key={rowKey}>
      <NxTableCell>
        <NxCheckbox checkboxId={rowKey} isChecked={selectedRepositories.includes(repo)} onChange={toggleSelection} />
      </NxTableCell>
      <NxTableCell className="iq-scm-repository-namespace iq-scmonboarding-overflow-tooltip-cell">
        <NxOverflowTooltip className="iq-scm-repo-namespace-tooltip">
          <div className="nx-truncate-ellipsis">{repo.namespace}</div>
        </NxOverflowTooltip>
      </NxTableCell>
      <NxTableCell className="iq-scm-repository-project iq-scmonboarding-overflow-tooltip-cell">
        <NxOverflowTooltip className="iq-scm-repo-project-tooltip">
          <div className="nx-truncate-ellipsis">
            <NxTextLink external href={repo.httpCloneUrl}>
              {repo.project}
            </NxTextLink>
          </div>
        </NxOverflowTooltip>
      </NxTableCell>
      <NxTableCell className="iq-scm-repository-description iq-scmonboarding-overflow-tooltip-cell">
        <NxOverflowTooltip className="iq-scm-repo-description-tooltip">
          <div className="nx-truncate-ellipsis">{repo.description}</div>
        </NxOverflowTooltip>
      </NxTableCell>
      <NxTableCell className="iq-scm-repository-default-branch">{renderDefaultBranch()}</NxTableCell>
    </NxTableRow>
  );
}
RepositoryRow.propTypes = {
  rowKey: PropTypes.string,
  repo: PropTypes.shape(repositoryPropType).isRequired,
  setSelectedRepositories: PropTypes.func.isRequired,
  selectedRepositories: PropTypes.array.isRequired,
};
