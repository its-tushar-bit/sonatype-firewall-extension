/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, {useEffect, Fragment, useState} from 'react';

import * as PropTypes from 'prop-types';
import {NxBackButton, NxTextInput} from '@sonatype/react-shared-components';
import LoadWrapper from '../../react/LoadWrapper';
import RepositoryPane from './components/RepositoryPane';
import NxButton from '@sonatype/react-shared-components/components/NxButton/NxButton';
import {NxSuccessAlert, NxErrorAlert, NxInfoAlert} from '@sonatype/react-shared-components/components/NxAlert/NxAlert';
import {validateHostUrl} from './utils/validators';
import {hasValidationErrors} from '../../util/validationUtil';
import ReportsCta from './components/ReportsCta';

const iqAuthorizationErrorMessage = `It appears you do not have permission to access this page.
        If you believe this to be incorrect please contact your administrator.`,
    scmFeatureDisabledErrorMessage = `This feature has not been enabled.
        If you believe this to be incorrect please contact your administrator.`;

export default function ScmOnboarding(props) {
  const {
    // sorting
    sortConfiguration,

    // actions
    setSortingParameters,
    loadPage,
    loadRepositories,
    validateScmHostUrl,
    importSelectedRepositories,
    onRepositorySelectionChanged,
    setCurrentHostUrl,
    setSelectedOrganization,

    // configuration state
    loadingPage,
    isScmOnboardingFeatureEnabled,
    isScmTokenConfigured,
    isScmTokenOverridden,
    scmProvider,

    // orgs
    selectedOrganization,
    organizations,

    // repositories state
    repositories,
    loadingRepositories,
    selectedRepositoryCount,
    totalRepositories,
    newlyImportedRepos,
    failedImportCount,

    // host URL
    defaultHostUrl,
    currentHostUrlState,

    // from angular URL router
    isAuthorized,
    preselectedOrganizationId,
    $state,

    // errors
    generalError,
    loadRepositoriesAuthError
  } = props;

  const scmConfigurationHref = $state.href($state.get('management.edit.organization.edit-source-control'), {
        organizationId: isScmTokenOverridden ? preselectedOrganizationId : 'ROOT_ORGANIZATION_ID'
      }),
      tokenNotConfiguredFragment = (
        <Fragment>
          The selected Organization does not have SCM configured. You can configure it{' '}
          <a href={scmConfigurationHref}>here</a>.
        </Fragment>
      ),
      pageError = !isAuthorized ? iqAuthorizationErrorMessage :
        isScmOnboardingFeatureEnabled === false ? scmFeatureDisabledErrorMessage :
          isScmTokenConfigured === false ? tokenNotConfiguredFragment :
            null;

  function load() {
    loadPage(preselectedOrganizationId);
  }
  useEffect(() => {
    load();
  }, []);

  useEffect(() => {
    if (preselectedOrganizationId && defaultHostUrl) {
      loadRepositories(preselectedOrganizationId, currentHostUrlState.value);
    }
  }, [defaultHostUrl]);

  const [isSuccessMessageOpen, setIsSuccessMessageOpen] = useState(false),
      [isFormErrorOpen, setIsFormErrorOpen] = useState(false),
      [isFormInfoOpen, setIsFormInfoOpen] = useState(false);

  useEffect(() => {
    setIsSuccessMessageOpen(newlyImportedRepos.length > 0 && failedImportCount === 0);
    setIsFormErrorOpen(newlyImportedRepos.length === 0 && failedImportCount > 0);
    setIsFormInfoOpen(newlyImportedRepos.length > 0 && failedImportCount > 0);
  }, [failedImportCount, newlyImportedRepos]);

  function dismissSuccessMessage() {
    setIsSuccessMessageOpen(false);
  }

  function dismissFormError() {
    setIsFormErrorOpen(false);
  }

  function dismissFormInfo() {
    setIsFormInfoOpen(false);
  }

  function handleLoadRepositories(event) {
    event.preventDefault();
    loadRepositories(preselectedOrganizationId, currentHostUrlState.value ? currentHostUrlState.value : defaultHostUrl);
  }

  function validateAndSetCurrentHostUrl(value) {
    setCurrentHostUrl(value);
    if (value && !hasValidationErrors(validateHostUrl(value))) {
      validateScmHostUrl(scmProvider, value);
    }
  }

  const repositoryCount = repositories ? repositories.length : 0;
  const alreadyImportedCount = totalRepositories - repositoryCount;

  return (
    <main id="scm-onboarding-container" className="nx-page-main">
      <NxBackButton
          href={$state.href($state.get('management.view.organization'),
              {organizationId: preselectedOrganizationId})}
          targetPageTitle={$state.get('management.view.organization').data.title} />
      {
        <LoadWrapper
            loading={loadingPage}
            error={pageError} retryHandler={load}>
          {!!selectedOrganization &&
          <Fragment>
            {isSuccessMessageOpen &&
            <NxSuccessAlert onClose={dismissSuccessMessage}>
              {newlyImportedRepos.length} repositories were successfully imported to IQ Server as applications under
              the {selectedOrganization.organization.name} Organization.
            </NxSuccessAlert>
            }
            {isFormInfoOpen &&
            <NxInfoAlert onClose={dismissFormInfo}>
              {newlyImportedRepos.length} repositories were successfully imported to IQ Server as applications under
              the {selectedOrganization.organization.name} Organization.<br/>
              {failedImportCount} repositories failed to import.
            </NxInfoAlert>
            }
            {isFormErrorOpen &&
            <NxErrorAlert onClose={dismissFormError}>
              {failedImportCount} repositories failed to import.
            </NxErrorAlert>
            }
          </Fragment>
          }
          <div className="nx-page-title iq-scmonboarding-title">
            { scmProvider &&
            <Fragment>
              <h1 className="nx-h1">
                <span>Import Applications from {scmProvider}</span>
              </h1>
              {alreadyImportedCount > 0 &&
              <div className="nx-btn-bar">
                <ReportsCta { ...{$state}} id="scm-reports-cta"/>
              </div>
              }
            </Fragment>
            }
          </div>
          <section className="nx-tile host-url-tile">
            <form className="nx-form">
              <div className='nx-tile-content'>
                <div className="nx-form-row">
                  <div className="nx-form-group">
                    <label className="nx-label">
                      <span className="nx-label__text">Host URL</span>
                      <NxTextInput id="iq-scm-default-host-field"
                                   { ...currentHostUrlState }
                                   onChange={validateAndSetCurrentHostUrl}
                                   validatable={true}
                                   placeholder={defaultHostUrl}/>
                    </label>
                  </div>
                  <div className="nx-btn-bar">
                    <NxButton
                        id="iq-scm-load-button"
                        variant="primary"
                        disabled={loadingRepositories || hasValidationErrors(currentHostUrlState.validationErrors) }
                        onClick={handleLoadRepositories}>
                      Reload Repositories
                    </NxButton>
                  </div>
                </div>
              </div>
            </form>
          </section>
          <section className="nx-tile">
            <RepositoryPane { ...{
              repositories,
              loadingRepositories,
              selectedRepositoryCount,
              totalRepositories,
              onRepositorySelectionChanged,
              importSelectedRepositories,
              loadRepositories,
              sortConfiguration,
              scmProvider,
              setSortingParameters,
              setSelectedOrganization,
              selectedOrganization,
              organizations,
              loadRepositoriesAuthError,
              generalError,
              scmConfigurationHref,
              isScmTokenOverridden,
              currentHostUrlState
            }} />
          </section>
        </LoadWrapper>
      }
    </main>
  );
}

export const organizationPropType = {
  id: PropTypes.string,
  name: PropTypes.string
};

export const repositoryPropType = {
  httpCloneUrl: PropTypes.string.isRequired,
  namespace: PropTypes.string,
  project: PropTypes.string,
  description: PropTypes.string,
  isSelected: PropTypes.bool,
  isImported: PropTypes.bool
};

const textInputPropType = {
  value: PropTypes.string.isRequired,
  trimmedValue: PropTypes.string.isRequired,
  isPristine: PropTypes.bool.isRequired,
  validationErrors: PropTypes.oneOfType([PropTypes.arrayOf(PropTypes.string.isRequired), PropTypes.string])
};

ScmOnboarding.propTypes = {
  // config
  loadingPage: PropTypes.bool.isRequired,
  isScmOnboardingFeatureEnabled: PropTypes.bool,
  $state: PropTypes.object.isRequired,
  isScmTokenConfigured: PropTypes.bool,
  isScmTokenOverridden: PropTypes.bool,
  scmProvider: PropTypes.string,

  // organizations
  organizations: PropTypes.arrayOf(PropTypes.shape(organizationPropType)),
  setSelectedOrganization: PropTypes.func.isRequired,
  selectedOrganization: PropTypes.shape(organizationPropType),

  // repositories
  loadRepositories: PropTypes.func.isRequired,
  loadingRepositories: PropTypes.bool.isRequired,
  repositories: PropTypes.arrayOf(PropTypes.shape(repositoryPropType)),
  selectedRepositoryCount: PropTypes.number.isRequired,
  totalRepositories: PropTypes.number,
  importedRepositoryCount: PropTypes.number,
  failedImportCount: PropTypes.number,
  newlyImportedRepos: PropTypes.arrayOf(PropTypes.shape(repositoryPropType)).isRequired,

  // from angular router
  isAuthorized: PropTypes.bool.isRequired,
  preselectedOrganizationId: PropTypes.string,

  // sorting
  sortConfiguration: PropTypes.shape({
    sortFields: PropTypes.arrayOf(PropTypes.string),
    dir: PropTypes.string,
    key: PropTypes.string
  }),

  // actions
  setSortingParameters: PropTypes.func,
  loadPage: PropTypes.func.isRequired,
  importSelectedRepositories: PropTypes.func.isRequired,
  onRepositorySelectionChanged: PropTypes.func.isRequired,
  setCurrentHostUrl: PropTypes.func.isRequired,
  validateScmHostUrl: PropTypes.func.isRequired,

  // base URL
  defaultHostUrl: PropTypes.string,
  currentHostUrlState: PropTypes.shape(textInputPropType),

  // errors
  generalError: LoadWrapper.propTypes.error,
  loadRepositoriesAuthError: LoadWrapper.propTypes.error
};
