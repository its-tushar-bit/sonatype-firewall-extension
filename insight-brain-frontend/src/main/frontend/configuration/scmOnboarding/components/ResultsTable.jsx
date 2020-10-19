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
    selectedRepositoryCount,
    importedRepositoryCount,

    // actions
    onRepositorySelectionChanged,
    importSelectedRepositories
  } = props;

  function importPercentage() {
    if (repositories && repositories.length > 0) {
      return Math.round(importedRepositoryCount / repositories.length * 100.0);
    }
    return 0.0;
  }

  function filterRepository(repo) {
    return repo.project.includes(projectFilter)
        && repo.namespace.includes(namespaceFilter)
        && repo.description.includes(descriptionFilter);
  }

  const [projectFilter, setProjectFilter] = useState(''),
      [namespaceFilter, setNamespaceFilter] = useState(''),
      [descriptionFilter, setDescriptionFilter] = useState('');

  let isAllChecked = false;
  function toggleSelectAll() {
    // TODO handle select all, INT-3479
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
                      value={ namespaceFilter }
                      onChange={ newValue => setNamespaceFilter(newValue)} />
                </NxTableCell>
                <NxTableCell hasIcon={true} className="nx-cell--filter">
                  <NxFilterInput
                      value={ projectFilter }
                      onChange={ newValue => setProjectFilter(newValue)} />
                </NxTableCell>
                <NxTableCell className="nx-cell--filter">
                  <NxFilterInput
                      value={ descriptionFilter }
                      onChange={ newValue => setDescriptionFilter(newValue)} />
                </NxTableCell>
                <NxTableCell className="nx-cell--select-all">
                  <NxCheckbox checkboxId='selectAll'
                              isChecked={isAllChecked}
                              onChange={toggleSelectAll}>All</NxCheckbox>
                </NxTableCell>
              </NxTableRow>
            </NxTableHead>
            <NxTableBody>
              { repositories.filter(repo => filterRepository(repo)).map(repo =>
                <RepositoryRow repo={repo} key={repo.httpCloneUrl} rowKey={repo.httpCloneUrl}
                               onSelectionChanged={() => onRepositorySelectionChanged(repo)}
                />
              )}
            </NxTableBody>
          </NxTable>
        </div>
      </LoadWrapper>
      {repositories.length > 0 &&
      <div className="nx-footer nx-footer--scmonboarding">
        <div className='iq-scmonboarding-stats-row'>
          <h3 className='iq-caption_text iq-scmonboarding-stats-highlight'>{selectedRepositoryCount}</h3>
          <div className='iq-scmonboarding-stats-column'>
            <h3 id='selected-total-count' className='iq-caption_text'>OF {repositories.length} REPOSITORIES</h3>
            <div className='iq-caption_subtext'>selected to import</div>
          </div>
        </div>
        <div className="nx-btn-bar">
          <NxButton
            id="iq-scm-import-button"
            variant="primary"
            disabled={selectedRepositoryCount <= 0}
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
    onSelectionChanged
  } = props;

  const [isCheckedState, setIsCheckedState] = useState(false),
      toggleSelection = () => {
        repo.isSelected = !isCheckedState;
        setIsCheckedState(!isCheckedState);
        onSelectionChanged();
      };

  return (
    <NxTableRow key={rowKey}>
      <NxTableCell className='iq-scm-repository-namespace'>{repo.namespace}</NxTableCell>
      <NxTableCell className='iq-scm-repository-project'>{repo.project}</NxTableCell>
      <NxTableCell className='iq-scm-repository-description'>{repo.description}</NxTableCell>
      <NxTableCell>
        <NxCheckbox checkboxId={rowKey} isChecked={isCheckedState} onChange={toggleSelection}/>
      </NxTableCell>
    </NxTableRow>
  );
}
RepositoryRow.propTypes = {
  rowKey: PropTypes.string,
  repo: PropTypes.shape(repositoryPropType).isRequired,
  onSelectionChanged: PropTypes.func
};
