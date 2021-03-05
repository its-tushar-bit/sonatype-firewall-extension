/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, {useEffect, Fragment} from 'react';

import * as PropTypes from 'prop-types';
import {NxBackButton} from '@sonatype/react-shared-components';
import LoadWrapper from '../../react/LoadWrapper';
import RepositoryPane from './components/RepositoryPane';
import ReportsCta from './components/ReportsCta';
import {displayName} from './utils/providers';
import ImportStatusModal from './components/ImportStatusModal';

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

    // repositories state
    repositories,
    totalRepositories,

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
          <ImportStatusModal {...props} />
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
  isNewOrganizationModalVisible: PropTypes.bool.isRequired,

  // repositories
  repositories: PropTypes.arrayOf(PropTypes.shape(repositoryPropType)),
  totalRepositories: PropTypes.number,
  importedRepositoryCount: PropTypes.number,

  // from angular router
  isAuthorized: PropTypes.bool.isRequired,
  preselectedOrganizationId: PropTypes.string,

  // actions
  loadPage: PropTypes.func.isRequired
};
