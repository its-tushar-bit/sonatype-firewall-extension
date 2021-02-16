/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import LoadWrapper from '../../../react/LoadWrapper';
import React, {Fragment, useState} from 'react';
import * as PropTypes from 'prop-types';
import {organizationPropType, repositoryPropType} from '../ScmOnboarding';
import NxButton from '@sonatype/react-shared-components/components/NxButton/NxButton';
import {NxFontAwesomeIcon, NxTooltip} from '@sonatype/react-shared-components';
import {faQuestionCircle} from '@fortawesome/pro-solid-svg-icons';
import ResultsTable from './ResultsTable';
import TargetOrganizationDropdown from './TargetOrganizationDropdown';
import {textInputPropType} from './ImportApplicationsForm';
import RepoStatus from './RepoStatus';

/*
 The tile which contains the repository list and all other associated UI elements
 */
export default function RepositoryPane(props) {
  const {
    loadingRepositories,
    repositories,
    totalRepositories,
    organizations,
    selectedOrganization,
    onRepositorySelectionChanged,
    loadRepositoriesAuthError,
    generalError,
    scmConfigurationHref,
    isScmTokenOverridden,
    scmProvider,
    currentHostUrlState,

    // sorting
    sortConfiguration,

    // actions
    setSorting,
    setSortingParameters,
    importSelectedRepositories,
    loadRepositories,
    setSelectedOrganization
  } = props;
  const scmAuthenticationErrorFragment = error => (
    <Fragment>
      {error.message}. You can update your login credentials{' '}<a href={scmConfigurationHref}>here</a>.
    </Fragment>
      ),
      resultsTableError = loadRepositoriesAuthError ? scmAuthenticationErrorFragment(loadRepositoriesAuthError) :
        generalError;

  const [isAllChecked, setIsAllChecked] = useState(false),
      [selectedRepositories, setSelectedRepositories] = useState([]);

  function handleImportSelectedRepositories() {
    const prevImportedCount = totalRepositories - repositories.length;
    const orgId = selectedOrganization.organization.id;
    importSelectedRepositories(orgId, totalRepositories, prevImportedCount, selectedRepositories);
    setSelectedRepositories([]);
    setIsAllChecked(false);
  }

  const repositoryCount = repositories ? repositories.length : 0;

  return (
    <Fragment>
      <header className="nx-tile-header">
        <div className="nx-tile-header__title">
          <h2 className="nx-h2">
            Import Repositories into this IQ Organization
            <NxTooltip
                id="import-label-tooltip"
                title={'IQ Server will attempt to connect to ' +
                scmProvider + ' using the credentials associated with the target organization'}
            >
              <span id="import-label-question-icon"><NxFontAwesomeIcon icon={faQuestionCircle} color="blue"/></span>
            </NxTooltip>
          </h2>
        </div>
      </header>
      <div className="nx-tile-content">
        <div className="scm-org-selection">
          <TargetOrganizationDropdown { ...{
            organizations,
            selectedOrganization,
            setSelectedOrganization: (event) => setSelectedOrganization(event, isScmTokenOverridden)
          }}/>
          <RepoStatus {...{repositories, totalRepositories}} />
        </div>
        <LoadWrapper loading={loadingRepositories} error={resultsTableError}
                     retryHandler={() => loadRepositories(selectedOrganization.organization.id,
                         currentHostUrlState.value)}>
          <ResultsTable { ...{
            repositories,
            loadingRepositories,
            sortConfiguration,
            setSorting,
            setSortingParameters,
            isAllChecked,
            setIsAllChecked,
            selectedRepositories,
            setSelectedRepositories,
            onRepositorySelectionChanged
          }} />
        </LoadWrapper>
      </div>
      { repositoryCount > 0 &&
        <footer className="nx-footer">
          <div className="iq-scmonboarding-stats-row">
            <h3 id="selected-repository-count"
                className="iq-caption_text iq-scmonboarding-stats-highlight">
              {selectedRepositories.length}
            </h3>
            <div className="iq-scmonboarding-stats-column">
              <h3 id="selected-total-count" className="iq-caption_text">OF {repositoryCount} REPOSITORIES</h3>
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

RepositoryPane.propTypes = {
  loadingRepositories: PropTypes.bool.isRequired,
  repositories: PropTypes.arrayOf(PropTypes.shape(repositoryPropType)),
  organizations: PropTypes.arrayOf(PropTypes.shape(organizationPropType)).isRequired,
  totalRepositories: PropTypes.number,
  selectedRepositoryCount: PropTypes.number.isRequired,
  importedRepositoryCount: PropTypes.number,
  sortConfiguration: PropTypes.shape({
    sortFields: PropTypes.arrayOf(PropTypes.string),
    dir: PropTypes.string,
    key: PropTypes.string
  }),
  scmConfigurationHref: PropTypes.string,
  isScmTokenOverridden: PropTypes.bool,
  scmProvider: PropTypes.string,
  currentHostUrlState: PropTypes.shape(textInputPropType),

  // actions
  setSorting: PropTypes.func,
  setSortingParameters: PropTypes.func,
  importSelectedRepositories: PropTypes.func.isRequired,
  loadRepositories: PropTypes.func.isRequired,
  setSelectedOrganization: PropTypes.func.isRequired,
  selectedOrganization: PropTypes.shape(organizationPropType),
  onRepositorySelectionChanged: PropTypes.func.isRequired,

  // errors
  generalError: LoadWrapper.propTypes.error,
  loadRepositoriesAuthError: LoadWrapper.propTypes.error
};
