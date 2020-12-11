/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import LoadWrapper from '../../../react/LoadWrapper';
import {
  NxCheckbox,
  NxTable,
  NxTableBody,
  NxTableCell,
  NxTableHead,
  NxTableRow,
  NxTooltip
} from '@sonatype/react-shared-components';
import React, {Fragment, useState} from 'react';
import * as PropTypes from 'prop-types';
import {repositoryPropType} from '../ScmOnboarding';
import NxButton from '@sonatype/react-shared-components/components/NxButton/NxButton';
import NxFilterInput from '@sonatype/react-shared-components/components/NxFilterInput/NxFilterInput';
import NxExternalLink from '../../../react/NxExternalLink';

export default function ResultsTable(props) {
  const {
    loadingRepositories,
    repositories,
    totalRepositories,
    preselectedOrganizationId,

    // sorting
    sortConfiguration,

    // actions
    setSorting,
    setSortingParameters,
    importSelectedRepositories,
    loadRepositories
  } = props;

  function importPercentage() {
    if (repositories && totalRepositories > 0) {
      return Math.round(((totalRepositories - repositories.length) / totalRepositories) * 100.0);
    }
    return 0;
  }

  const [filters, setFilters] = useState({project: '', namespace: '', description: ''}),
      [isAllChecked, setIsAllChecked] = useState(false),
      [selectedRepositories, setSelectedRepositories] = useState([]);

  const sortDirProject = getDirection(sortConfiguration, 'project');
  const sortDirNamespace = getDirection(sortConfiguration, 'namespace');
  const sortDirDescription = getDirection(sortConfiguration, 'description');

  function getDirection(sortConfig, key) {
    return sortConfig && sortConfig.key === key ? sortConfig.dir : null;
  }

  const sortSettingsProject = {
    key: 'project',
    sortingOrder: ['project', 'namespace', 'description']
  };
  const sortSettingsNamespace = {
    key: 'namespace',
    sortingOrder: ['namespace', 'project', 'description']
  };
  const sortSettingsDescription = {
    key: 'description',
    sortingOrder: ['description', 'namespace', 'project']
  };

  function requestSort(settings) {
    let direction = 'asc';
    if (
      sortConfiguration &&
      sortConfiguration.key === settings.key &&
      sortConfiguration.dir === 'asc'
    ) {
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
    setSorting(sortingOrder, repositories);
  }

  function isRepositorySelectedByFilter(repository) {
    return repository.project.includes(filters.project)
        && repository.namespace.includes(filters.namespace)
        && repository.description.includes(filters.description);
  }

  function toggleSelectAll() {
    setIsAllChecked(!isAllChecked);
    setSelectedRepositories(repositories.filter(repo => isRepositorySelectedByFilter(repo) && !isAllChecked));
  }

  function changeFilter(filterName, filterValue) {
    setFilters(Object.assign({}, filters, {[filterName]: filterValue}));
    setSelectedRepositories(repositories.filter(repo => repo[filterName].includes(filterValue)
        && selectedRepositories.includes(repo)));
  }

  function handleImportSelectedRepositories() {
    importSelectedRepositories(preselectedOrganizationId, selectedRepositories);
    setSelectedRepositories([]);
  }

  return (
    <Fragment>
      <header className="nx-tile-header nx-tile-header--hrule">
        <div className="nx-tile-header__title">
          <h2 className="nx-h2">Import Repositories</h2>
        </div>
        { !loadingRepositories &&
          <div className="nx-tile-header__subtitle iq-scmonboarding-stats">
            <div className="iq-scmonboarding-stats-row">
              <h3 id="repository-count"
                  className="iq-caption_text iq-scmonboarding-stats-highlight">{repositories.length}</h3>
              <div className="iq-scmonboarding-stats-column">
                <h3 className="iq-caption_text">REPOSITORIES</h3>
                <div className="iq-caption_subtext">found</div>
              </div>
            </div>
            <div className="iq-scmonboarding-stats-row">
              <h3 id="scm-already-imported"
                  className='iq-caption_text iq-scmonboarding-stats-highlight'>{totalRepositories -
              repositories.length}</h3>
              <div className="iq-scmonboarding-stats-column">
                <h3 className="iq-caption_text">ALREADY IMPORTED</h3>
                <div id="scm-import-percentage" className="iq-caption_subtext">({importPercentage()}%)</div>
              </div>
            </div>
          </div>
        }
      </header>
      <div className="nx-tile-content">
        <LoadWrapper loading={loadingRepositories} retryHandler={loadRepositories}>
          <div className="nx-scrollable nx-scrollable--table-container">
            <NxTable id="iq-scm-onboarding-repositories" className="nx-table--scrollable nx-table--scm-onboarding">
              <NxTableHead>
                <NxTableRow>
                  <NxTableCell isSortable>Selection</NxTableCell>
                  <NxTableCell id='namespace-header' isSortable sortDir={sortDirNamespace}
                               onClick={() => requestSort(sortSettingsNamespace)}>Namespace</NxTableCell>
                  <NxTableCell id='project-header' isSortable sortDir={sortDirProject}
                               onClick={() => requestSort(sortSettingsProject)}>Project</NxTableCell>
                  <NxTableCell id='description-header' isSortable sortDir={sortDirDescription}
                               onClick={() => requestSort(sortSettingsDescription)}>Description</NxTableCell>
                </NxTableRow>
                <NxTableRow isFilterHeader>
                  <NxTableCell className="iq-scmonboarding__select-all-cell">
                    <NxCheckbox checkboxId="iq-scmonboarding-select-all"
                                isChecked={isAllChecked}
                                onChange={toggleSelectAll}>
                      All
                    </NxCheckbox>
                  </NxTableCell>
                  <NxTableCell className="iq-scmonboarding__filter-cell">
                    <NxFilterInput value={filters.namespace}
                                   onChange={filterValue => changeFilter('namespace', filterValue)} />
                  </NxTableCell>
                  <NxTableCell className="iq-scmonboarding__filter-cell">
                    <NxFilterInput id="project-filter"
                                   value={filters.project}
                                   onChange={filterValue => changeFilter('project', filterValue)} />
                  </NxTableCell>
                  <NxTableCell className="iq-scmonboarding__filter-cell">
                    <NxFilterInput value={filters.description}
                                   onChange={filterValue => changeFilter('description', filterValue)} />
                  </NxTableCell>
                </NxTableRow>
              </NxTableHead>
              <NxTableBody emptyMessage="No matching repositories.">
                { repositories.filter(repo => isRepositorySelectedByFilter(repo)).map(repo =>
                  <RepositoryRow repo={repo}
                                 key={repo.httpCloneUrl}
                                 rowKey={repo.httpCloneUrl}
                                 selectedRepositories={selectedRepositories}
                                 setSelectedRepositories={setSelectedRepositories} />
                )}
              </NxTableBody>
            </NxTable>
          </div>
        </LoadWrapper>
      </div>
      { repositories.length > 0 &&
        <footer className="nx-footer nx-footer--scmonboarding">
          <div className="iq-scmonboarding-stats-row">
            <h3 id="selected-repository-count"
                className="iq-caption_text iq-scmonboarding-stats-highlight">
              {selectedRepositories.length}
            </h3>
            <div className="iq-scmonboarding-stats-column">
              <h3 id="selected-total-count" className="iq-caption_text">OF {repositories.length} REPOSITORIES</h3>
              <div className="iq-caption_subtext">selected to import</div>
            </div>
          </div>
          <div className="nx-btn-bar">
            <NxButton id="iq-scm-import-button"
                      variant="primary"
                      disabled={selectedRepositories.length <= 0}
                      onClick={handleImportSelectedRepositories}>
              Import Repositories
            </NxButton>
          </div>
        </footer>
      }
    </Fragment>
  );
}

ResultsTable.propTypes = {
  loadingRepositories: PropTypes.bool.isRequired,
  repositories: PropTypes.arrayOf(PropTypes.shape(repositoryPropType)),
  totalRepositories: PropTypes.number,
  selectedRepositoryCount: PropTypes.number.isRequired,
  importedRepositoryCount: PropTypes.number,
  preselectedOrganizationId: PropTypes.string,
  sortConfiguration: PropTypes.shape({
    sortFields: PropTypes.arrayOf(PropTypes.string),
    dir: PropTypes.string,
    key: PropTypes.string
  }),

  // actions
  setSorting: PropTypes.func,
  setSortingParameters: PropTypes.func,
  onRepositorySelectionChanged: PropTypes.func.isRequired,
  importSelectedRepositories: PropTypes.func.isRequired,
  loadRepositories: PropTypes.func.isRequired
};

function RepositoryRow(props) {
  const {
    rowKey,
    repo,
    setSelectedRepositories,
    selectedRepositories
  } = props;

  const toggleSelection = () => {
    setSelectedRepositories(selectedRepositories.includes(repo)
      ? selectedRepositories.filter(selectedRepo => selectedRepo !== repo)
      : selectedRepositories.concat([repo]));
  };

  return (
    <NxTableRow key={rowKey}>
      <NxTableCell>
        <NxCheckbox checkboxId={rowKey} isChecked={selectedRepositories.includes(repo)}
                    onChange={toggleSelection}/>
      </NxTableCell>
      <NxTableCell className='iq-scm-repository-namespace'>{repo.namespace}</NxTableCell>
      <NxTableCell className='iq-scm-repository-project'>
        <NxExternalLink href={repo.httpCloneUrl}>{repo.project}</NxExternalLink>
      </NxTableCell>
      <NxTableCell className='iq-scm-repository-description'>
        <NxTooltip title={repo.description} className='iq-scm-repo-description-tooltip'>
          <div className="nx-truncate-ellipsis">{repo.description}</div>
        </NxTooltip>
      </NxTableCell>
    </NxTableRow>
  );
}
RepositoryRow.propTypes = {
  rowKey: PropTypes.string,
  repo: PropTypes.shape(repositoryPropType).isRequired,
  setSelectedRepositories: PropTypes.func.isRequired,
  selectedRepositories: PropTypes.array.isRequired
};
