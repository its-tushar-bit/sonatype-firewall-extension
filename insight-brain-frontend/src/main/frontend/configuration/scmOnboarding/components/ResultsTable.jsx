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
  NxTableRow
} from '@sonatype/react-shared-components';
import React, {Fragment, useState} from 'react';
import * as PropTypes from 'prop-types';
import {repositoryPropType} from '../ScmOnboarding';
import NxButton from '@sonatype/react-shared-components/components/NxButton/NxButton';
import NxFilterInput from '@sonatype/react-shared-components/components/NxFilterInput/NxFilterInput';

export default function ResultsTable(props) {
  const {
    loadingRepositories,
    repositories,
    importedRepositoryCount,

    // actions
    importSelectedRepositories
  } = props;

  function importPercentage() {
    if (repositories && repositories.length > 0) {
      return Math.round(importedRepositoryCount / repositories.length * 100.0);
    }
    return 0.0;
  }

  const [filters, setFilters] = useState({project: '', namespace: '', description: ''}),
      [isAllChecked, setIsAllChecked] = useState(false),
      [selectedRepositories, setSelectedRepositories] = useState([]);

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

  return (
    <Fragment>
      <div className="iq-tile-header">
        <h2 className="iq-tile-header__title">Import Repositories</h2>
        {repositories.length > 0 &&
          <div className='iq-scmonboarding-stats'>
            <div className='iq-scmonboarding-stats-row'>
              <h3 id='repository-count'
                  className='iq-caption_text iq-scmonboarding-stats-highlight'>{repositories.length}</h3>
              <div className='iq-scmonboarding-stats-column'>
                <h3 className='iq-caption_text'>REPOSITORIES</h3>
                <div className='iq-caption_subtext'>found</div>
              </div>
            </div>
            <div className='iq-scmonboarding-stats-row'>
              <h3 className='iq-caption_text iq-scmonboarding-stats-highlight'>{importedRepositoryCount}</h3>
              <div className='iq-scmonboarding-stats-column'>
                <h3 className='iq-caption_text'>ALREADY IMPORTED</h3>
                <div className='iq-caption_subtext'>({importPercentage()}%)</div>
              </div>
            </div>
          </div>
        }
      </div>
      <hr/>
      <LoadWrapper loading={loadingRepositories}>
        <div className='nx-scrollable nx-scrollable-table-container'>
          <NxTable id="iq-scm-onboarding-repositories" className="nx-table--scrollable nx-table--scm-onboarding">
            <NxTableHead>
              <NxTableRow isHeader={true}>
                <NxTableCell isHeader isSortable={true}>Namespace</NxTableCell>
                <NxTableCell isHeader isSortable={true}>Project</NxTableCell>
                <NxTableCell isHeader isSortable={true}>Description</NxTableCell>
                <NxTableCell isHeader isSortable={true}>Selection</NxTableCell>
              </NxTableRow>
              <NxTableRow isHeader>
                <NxTableCell className="nx-cell--filter">
                  <NxFilterInput
                      value={ filters.namespace }
                      onChange={ filterValue => changeFilter('namespace', filterValue)}
                      onClear={ () => changeFilter('namespace', '') }/>
                </NxTableCell>
                <NxTableCell hasIcon={true} className="nx-cell--filter">
                  <NxFilterInput
                      id="project-filter"
                      value={ filters.project }
                      onChange={ filterValue => changeFilter('project', filterValue)}
                      onClear={ () => changeFilter('project', '') }/>
                </NxTableCell>
                <NxTableCell className="nx-cell--filter">
                  <NxFilterInput
                      value={ filters.description }
                      onChange={ filterValue => changeFilter('description', filterValue)}
                      onClear={ () => changeFilter('description', '') }/>
                </NxTableCell>
                <NxTableCell className="nx-cell--select-all">
                  <NxCheckbox checkboxId='select-all'
                              isChecked={isAllChecked}
                              onChange={toggleSelectAll}>All</NxCheckbox>
                </NxTableCell>
              </NxTableRow>
            </NxTableHead>
            <NxTableBody>
              { repositories.filter(repo => isRepositorySelectedByFilter(repo)).map(repo =>
                <RepositoryRow repo={repo} key={repo.httpCloneUrl} rowKey={repo.httpCloneUrl}
                               selectedRepositories={selectedRepositories}
                               setSelectedRepositories={setSelectedRepositories} />
              )}
            </NxTableBody>
          </NxTable>
        </div>
      </LoadWrapper>
      {repositories.length > 0 &&
      <div className="nx-footer nx-footer--scmonboarding">
        <div className='iq-scmonboarding-stats-row'>
          <h3 className='iq-caption_text iq-scmonboarding-stats-highlight'
              id="selected-repository-count">{selectedRepositories.length}</h3>
          <div className='iq-scmonboarding-stats-column'>
            <h3 id='selected-total-count' className='iq-caption_text'>OF {repositories.length} REPOSITORIES</h3>
            <div className='iq-caption_subtext'>selected to import</div>
          </div>
        </div>
        <div className="nx-btn-bar">
          <NxButton
            id="iq-scm-import-button"
            variant="primary"
            disabled={selectedRepositories.length <= 0}
            onClick={() => importSelectedRepositories()}>
            Import Repositories
          </NxButton>
        </div>
      </div>
      }
    </Fragment>
  );
}

ResultsTable.propTypes = {
  loadingRepositories: PropTypes.bool.isRequired,
  repositories: PropTypes.arrayOf(PropTypes.shape(repositoryPropType)),
  selectedRepositoryCount: PropTypes.number.isRequired,
  importedRepositoryCount: PropTypes.number,

  // actions
  onRepositorySelectionChanged: PropTypes.func.isRequired,
  importSelectedRepositories: PropTypes.func.isRequired
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
      <NxTableCell className='iq-scm-repository-namespace'>{repo.namespace}</NxTableCell>
      <NxTableCell className='iq-scm-repository-project'>{repo.project}</NxTableCell>
      <NxTableCell className='iq-scm-repository-description'>{repo.description}</NxTableCell>
      <NxTableCell>
        <NxCheckbox checkboxId={rowKey} isChecked={selectedRepositories.includes(repo)}
                    onChange={toggleSelection}/>
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
