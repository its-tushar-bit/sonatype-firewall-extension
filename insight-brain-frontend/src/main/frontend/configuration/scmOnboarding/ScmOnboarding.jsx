/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, {useEffect, Fragment, useState} from 'react';

import * as PropTypes from 'prop-types';
import {NxBackButton} from '@sonatype/react-shared-components';
import LoadWrapper from '../../react/LoadWrapper';
import RepositoryPane from './components/RepositoryPane';
import {NxSuccessAlert, NxErrorAlert, NxInfoAlert} from '@sonatype/react-shared-components/components/NxAlert/NxAlert';
import ReportsCta from './components/ReportsCta';
import {displayName} from './utils/providers';

const iqAuthorizationErrorMessage = `It appears you do not have permission to access this page.
        If you believe this to be incorrect please contact your administrator.`,
    scmFeatureDisabledErrorMessage = `This feature has not been enabled.
        If you believe this to be incorrect please contact your administrator.`;

export default function ScmOnboarding(props) {
  const {
    // actions
    loadPage,

    // configuration state
    loadingPage,
    isScmOnboardingFeatureEnabled,
    isScmTokenConfigured,
    isScmTokenOverridden,
    scmProvider,

    // orgs
    selectedOrganization,

    // repositories state
    repositories,
    totalRepositories,
    newlyImportedRepos,
    failedImportCount,

    // from angular URL router
    isAuthorized,
    preselectedOrganizationId,
    $state
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
                <span>Import Applications from {displayName(scmProvider)}</span>
              </h1>
              {alreadyImportedCount > 0 &&
              <div className="nx-btn-bar">
                <ReportsCta { ...{$state}} id="scm-reports-cta"/>
              </div>
              }
            </Fragment>
            }
          </div>
          <section className="nx-tile">
            <RepositoryPane { ...props } />
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

ScmOnboarding.propTypes = {
  // config
  loadingPage: PropTypes.bool.isRequired,
  isScmOnboardingFeatureEnabled: PropTypes.bool,
  $state: PropTypes.object.isRequired,
  isScmTokenConfigured: PropTypes.bool,
  isScmTokenOverridden: PropTypes.bool,
  scmProvider: PropTypes.string,

  // organizations
  selectedOrganization: PropTypes.shape(organizationPropType),

  // repositories
  repositories: PropTypes.arrayOf(PropTypes.shape(repositoryPropType)),
  totalRepositories: PropTypes.number,
  importedRepositoryCount: PropTypes.number,
  failedImportCount: PropTypes.number,
  newlyImportedRepos: PropTypes.arrayOf(PropTypes.shape(repositoryPropType)).isRequired,

  // from angular router
  isAuthorized: PropTypes.bool.isRequired,
  preselectedOrganizationId: PropTypes.string,

  // actions
  loadPage: PropTypes.func.isRequired
};
