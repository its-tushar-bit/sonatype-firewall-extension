/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import LoadWrapper from '../../../react/LoadWrapper';
import React, { Fragment, useState } from 'react';
import * as PropTypes from 'prop-types';
import { organizationPropType, repositoryPropType } from '../scmPropTypes';
import NxButton from '@sonatype/react-shared-components/components/NxButton/NxButton';
import { NxFontAwesomeIcon, NxTooltip, NxSubmitMask, NxTextLink } from '@sonatype/react-shared-components';
import { faPlus, faQuestionCircle } from '@fortawesome/pro-solid-svg-icons';
import ResultsTable from './ResultsTable';
import TargetOrganizationDropdown from './TargetOrganizationDropdown';
import { textInputPropType } from '../scmPropTypes';
import RepoStatus from './RepoStatus';
import LoadError from '../../../react/LoadError';
import GitHostModal from './GitHostModal';
import { displayName } from '../utils/providers';
import CredentialsError from './CredentialsError';
import ownerConstant from '../../../utility/services/owner.constant';
import { actions as ownerModalActions } from 'MainRoot/OrgsAndPolicies/ownerModal/ownerModalSlice';
import { useDispatch } from 'react-redux';
import OwnerModal from 'MainRoot/OrgsAndPolicies/ownerModal/OwnerModal';

/*
 The tile which contains the repository list and all other associated UI elements
 */
export default function RepositoryPane(props) {
  const {
    loadingRepositories,
    loadingPage,
    repositories,
    totalRepositories,
    organizations,
    selectedOrganization,
    onRepositorySelectionChanged,
    loadRepositoriesErrorCode,
    generalError,
    scmProvider,
    currentHostUrlState,
    defaultHostUrl,
    isGitHostNeeded,
    isSelectingOrganization,
    isScmTokenConfigured,
    isImporting,
    $state,

    // sorting
    sortConfiguration,

    // actions
    setSorting,
    setSortingParameters,
    importSelectedRepositories,
    loadRepositories,
    setShowHostDialog,
  } = props;

  const dispatch = useDispatch();
  const openOwnerEditorModal = () => dispatch(ownerModalActions.openModal({ isApp: false }));

  const orgsAndPoliciesHref = !selectedOrganization
    ? ''
    : $state.href($state.get('management.edit.organization.edit-source-control'), {
        organizationId: selectedOrganization.organization.id,
      });
  const orgsAndPoliciesRootOrgHref = $state.href($state.get('management.edit.organization.edit-source-control'), {
    organizationId: ownerConstant.ROOT_ORGANIZATION_ID,
  });
  const tokenNotConfiguredMessage = () => {
    if (!selectedOrganization) {
      return null;
    }
    return (
      <span>
        We could not find a token. You can configure a token to be shared across organizations in the Root
        Organization&apos;s <NxTextLink href={orgsAndPoliciesRootOrgHref}>Source Control Configuration</NxTextLink>{' '}
        page, or you can provide a custom token for the{' '}
        <NxTextLink href={orgsAndPoliciesHref}>{selectedOrganization.organization.name}</NxTextLink> Organization.
      </span>
    );
  };
  const gitHostUrlMessage = (addModalLink = false) => {
    return (
      <div>
        {loadRepositoriesErrorCode && <p>{scmAuthenticationErrorFragment(loadRepositoriesErrorCode)}</p>}
        {!defaultHostUrl && !loadRepositoriesErrorCode && (
          <p>
            IQ Server was unable to identify the URL for your {displayName(scmProvider)} host. You need to{' '}
            {scmUrlLink(addModalLink)} in order to proceed.
          </p>
        )}
        {updateScmConfigMessage()}
      </div>
    );
  };

  const scmUrlLink = (addModalLink) => {
    const linkText = 'provide a SCM URL';
    if (addModalLink) {
      return <NxTextLink onClick={() => setShowHostDialog(true)}>{linkText}</NxTextLink>;
    }
    return linkText;
  };

  const updateScmConfigMessage = () => {
    if (selectedOrganization && selectedOrganization.organization && selectedOrganization.name) {
      return (
        <p>
          IQ Server was unable to connect to {defaultHostUrl} using the credentials associated with the{' '}
          {selectedOrganization.organization.name} Organization. You may try a different host URL or manage your SCM
          configuration in the <NxTextLink href={orgsAndPoliciesHref}>Orgs & Policies</NxTextLink> page.
        </p>
      );
    }
    if (defaultHostUrl) {
      return (
        <span>
          IQ Server was unable to connect to {defaultHostUrl}. You may try a different host URL or manage your SCM
          configuration in the <NxTextLink href={orgsAndPoliciesHref}>Orgs & Policies</NxTextLink> page.
        </span>
      );
    }
  };

  const loadRepoGitHostMessage = (errorText) => {
    if (errorText) {
      return (
        <Fragment>
          {errorText}
          <p>
            <NxTextLink onClick={() => setShowHostDialog(true)}>Click here</NxTextLink> to change the git host URL.
          </p>
        </Fragment>
      );
    }
  };

  const scmAuthenticationErrorFragment = (errorCode, inLoadWrapper = false) => {
    return (
      <CredentialsError
        hostUrlClicked={() => setShowHostDialog(true)}
        {...{
          errorCode,
          inLoadWrapper,
          $state,
          selectedOrganization,
          scmProvider,
        }}
      />
    );
  };

  const resultsTableError = !isScmTokenConfigured
    ? tokenNotConfiguredMessage()
    : isGitHostNeeded
    ? gitHostUrlMessage(true)
    : loadRepositoriesErrorCode
    ? scmAuthenticationErrorFragment(loadRepositoriesErrorCode, true)
    : loadRepoGitHostMessage(generalError ? generalError.message : null);

  const [isAllChecked, setIsAllChecked] = useState(false),
    [selectedRepositories, setSelectedRepositories] = useState([]);

  function handleImportSelectedRepositories() {
    const prevImportedCount = totalRepositories - repositories.length;
    const orgId = selectedOrganization.organization.id;
    importSelectedRepositories(orgId, totalRepositories, prevImportedCount, selectedRepositories);
    setSelectedRepositories([]);
    setIsAllChecked(false);
  }

  const retryLoadRepos = () => {
    if (isGitHostNeeded) {
      setShowHostDialog(true);
    } else {
      loadRepositories(selectedOrganization.organization.id, currentHostUrlState.value);
    }
  };

  const repositoryCount = repositories ? repositories.length : 0;

  return (
    <Fragment>
      <header className="nx-tile-header">
        <div className="nx-tile-header__title">
          <h2 className="nx-h2">
            Import Repositories into this IQ Organization
            <NxTooltip
              id="import-label-tooltip"
              title={
                'IQ Server will attempt to connect to ' +
                displayName(scmProvider) +
                ' using the credentials associated with the target organization'
              }
            >
              <span id="import-label-question-icon">
                <NxFontAwesomeIcon icon={faQuestionCircle} color="blue" />
              </span>
            </NxTooltip>
          </h2>
        </div>
      </header>
      <div className="nx-tile-content">
        <div className="scm-org-selection">
          <TargetOrganizationDropdown
            {...{
              organizations,
              selectedOrganization,
              loadingOrganizations: loadingPage,
              $state,
            }}
          />
          <NxButton onClick={() => openOwnerEditorModal()} id="repository-pane-add-org">
            <NxFontAwesomeIcon icon={faPlus} /> New Organization
          </NxButton>
          <OwnerModal shouldRedirectToNewOrg />
          <RepoStatus {...{ repositories, totalRepositories }} />
        </div>
        <div id="scm-repo-table">
          <LoadWrapper
            loading={loadingRepositories || isSelectingOrganization}
            error={resultsTableError}
            retryHandler={retryLoadRepos}
          >
            <ResultsTable
              {...{
                repositories,
                loadingRepositories,
                sortConfiguration,
                setSorting,
                setSortingParameters,
                isAllChecked,
                setIsAllChecked,
                selectedRepositories,
                setSelectedRepositories,
                onRepositorySelectionChanged,
              }}
            />
          </LoadWrapper>
        </div>
        <GitHostModal {...props} errorText={gitHostUrlMessage()} />
      </div>
      {repositoryCount > 0 && (
        <footer className="nx-footer">
          <div className="nx-btn-bar">
            <div>
              <span id="scm-repo-to-import-count">
                {selectedRepositories.length} of {repositoryCount} repositories
              </span>{' '}
              selected to import
            </div>
            <NxButton
              id="iq-scm-import-button"
              variant="primary"
              disabled={selectedRepositories.length <= 0}
              onClick={handleImportSelectedRepositories}
            >
              Import Repositories
            </NxButton>
          </div>
        </footer>
      )}
      {isImporting && <NxSubmitMask fullscreen />}
    </Fragment>
  );
}

RepositoryPane.propTypes = {
  loadingRepositories: PropTypes.bool.isRequired,
  loadingPage: PropTypes.bool.isRequired,
  repositories: PropTypes.arrayOf(PropTypes.shape(repositoryPropType)),
  organizations: PropTypes.arrayOf(PropTypes.shape(organizationPropType)).isRequired,
  totalRepositories: PropTypes.number,
  selectedRepositoryCount: PropTypes.number.isRequired,
  importedRepositoryCount: PropTypes.number,
  sortConfiguration: PropTypes.shape({
    sortFields: PropTypes.arrayOf(PropTypes.string),
    dir: PropTypes.string,
    key: PropTypes.string,
  }),
  scmConfigurationHref: PropTypes.string,
  scmProvider: PropTypes.string,
  // textInputPropType is implied required, but this val is optional
  currentHostUrlState: PropTypes.oneOfType([PropTypes.object, PropTypes.shape(textInputPropType)]),
  defaultHostUrl: PropTypes.string,
  isGitHostNeeded: PropTypes.bool,
  isSelectingOrganization: PropTypes.bool,
  isImporting: PropTypes.bool,
  $state: PropTypes.object.isRequired,
  isNewOrganizationModalVisible: PropTypes.bool.isRequired,
  isScmTokenConfigured: PropTypes.bool,
  isRootScmConfigured: PropTypes.bool,

  // actions
  setSorting: PropTypes.func,
  setSortingParameters: PropTypes.func,
  importSelectedRepositories: PropTypes.func.isRequired,
  loadRepositories: PropTypes.func.isRequired,
  setSelectedOrganization: PropTypes.func.isRequired,
  selectedOrganization: PropTypes.shape(organizationPropType),
  onRepositorySelectionChanged: PropTypes.func.isRequired,
  setShowHostDialog: PropTypes.func,
  addOrganization: PropTypes.func.isRequired,
  setIsNewOrganizationModalVisible: PropTypes.func.isRequired,

  // errors
  generalError: LoadWrapper.propTypes.error,
  loadRepositoriesErrorCode: PropTypes.string,
  addOrganizationError: LoadError.propTypes.error,
};
