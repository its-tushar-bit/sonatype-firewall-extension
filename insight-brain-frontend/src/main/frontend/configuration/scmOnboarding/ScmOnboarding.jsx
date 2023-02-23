/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, Fragment } from 'react';

import * as PropTypes from 'prop-types';
import LoadWrapper from '../../react/LoadWrapper';
import RepositoryPane from './components/RepositoryPane';
import ReportsCta from './components/ReportsCta';
import { displayName } from './utils/providers';
import ImportStatusModal from './components/ImportStatusModal';
import { repositoryPropType } from './scmPropTypes';

export default function ScmOnboarding(props) {
  const {
    // actions
    loadPage,

    //permissions state
    loadingPermissions,
    loadingPermissionsError,

    // configuration state
    loadingPage,
    scmProvider,

    // repositories state
    repositories,
    totalRepositories,

    // from angular URL router
    preselectedOrganizationId,
    $state,
  } = props;

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
      {
        <LoadWrapper loading={loadingPermissions || loadingPage} error={loadingPermissionsError} retryHandler={load}>
          <ImportStatusModal {...props} />
          <div className="nx-page-title iq-scmonboarding-title">
            {scmProvider && (
              <Fragment>
                <h1 className="nx-h1">
                  <span>Import Applications from {displayName(scmProvider)}</span>
                </h1>
                {alreadyImportedCount > 0 && (
                  <div className="nx-btn-bar">
                    <ReportsCta {...{ $state }} id="scm-reports-cta" />
                  </div>
                )}
              </Fragment>
            )}
          </div>
          <section className="nx-tile">
            <RepositoryPane {...props} />
          </section>
        </LoadWrapper>
      }
    </main>
  );
}

ScmOnboarding.propTypes = {
  //permissions
  loadingPermissions: PropTypes.bool.isRequired,
  loadingPermissionsError: LoadWrapper.propTypes.error,

  // config
  loadingPage: PropTypes.bool.isRequired,
  $state: PropTypes.object.isRequired,
  isScmTokenOverridden: PropTypes.bool,
  scmProvider: PropTypes.string,

  // organizations
  isNewOrganizationModalVisible: PropTypes.bool.isRequired,

  // repositories
  repositories: PropTypes.arrayOf(PropTypes.shape(repositoryPropType)),
  totalRepositories: PropTypes.number,
  importedRepositoryCount: PropTypes.number,

  // from angular router
  preselectedOrganizationId: PropTypes.string,

  // actions
  loadPage: PropTypes.func.isRequired,
};
