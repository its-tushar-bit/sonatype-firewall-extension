/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { has } from 'ramda';
import {
  NxTable, NxButton, NxFontAwesomeIcon, NxP, NxH1,
  NxPageMain, NxPageTitle, NxTile, NxTag, NxTooltip,
} from '@sonatype/react-shared-components';
import { faTrash, faPlus } from '@fortawesome/pro-solid-svg-icons';
import { faExternalLink } from '@fortawesome/pro-regular-svg-icons';
import { actions as gitHubAppActions } from 'MainRoot/configuration/githubApp/gitHubAppConfigurationSlice';
import { actions as toastActions } from 'MainRoot/toastContainer/toastSlice';
import { selectSelectedOwner } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import { selectIsModalOpen as selectIsRegistrationModalOpen } from 'MainRoot/configuration/githubApp/gitHubAppConfigurationSelectors';
import GitHubAppRegistrationModal from 'MainRoot/OrgsAndPolicies/sourceControlConfiguration/GitHubAppRegistrationModal';
import { selectGitHubApps, selectLoading, selectError } from './manageGitHubAppsSelectors';
import { fetchGitHubApps, openDeleteModal, resetState } from './manageGitHubAppsSlice';
import ManageGitHubAppsDeleteModal from './ManageGitHubAppsDeleteModal';

export default function ManageGitHubApps() {
  const dispatch = useDispatch();
  const owner = useSelector(selectSelectedOwner);
  const githubApps = useSelector(selectGitHubApps);
  const loading = useSelector(selectLoading);
  const error = useSelector(selectError);
  const isRegistrationModalOpen = useSelector(selectIsRegistrationModalOpen);

  const isApplication = owner && has('publicId', owner);
  const shouldDisableAddButton = isApplication && githubApps.length >= 1;

  useEffect(() => {
    if (owner?.id) {
      dispatch(fetchGitHubApps(owner.id));
    }

    try {
      const returnToRaw = sessionStorage.getItem('githubAppReturnTo');
      if (returnToRaw) {
        const { returnTo } = JSON.parse(returnToRaw);
        sessionStorage.removeItem('githubAppReturnTo');
        if (returnTo === 'manage') {
          dispatch(toastActions.addToast({ type: 'success', message: 'GitHub App added successfully.' }));
        }
      }
    } catch {
      sessionStorage.removeItem('githubAppReturnTo');
    }

    return () => dispatch(resetState());
  }, [dispatch, owner?.id]);

  const handleAddGitHubApp = () => {
    sessionStorage.setItem('githubAppReturnTo', JSON.stringify({ returnTo: 'manage', ownerId: owner.id }));
    dispatch(gitHubAppActions.openModal());
  };


  const handleDelete = (app) => {
    dispatch(openDeleteModal(app));
  };

  const formatOrgName = (name) => {
    if (name && name.endsWith('(personal)')) {
      return { displayName: name.replace('(personal)', '').trim(), isPersonal: true };
    }
    return { displayName: name, isPersonal: false };
  };

  const formatDate = (isoString) => {
    if (!isoString) return '';
    return new Date(isoString).toLocaleDateString();
  };

  return (
    <div id="manage-github-applications">
      <NxPageTitle>
        <NxH1>Manage GitHub Applications</NxH1>
        <NxPageTitle.Description>
          Configures the integration with GitHub Apps for {owner?.name}
        </NxPageTitle.Description>
      </NxPageTitle>

      <NxTile>
        <NxTile.Header>
          <NxTile.HeaderActions>
            {shouldDisableAddButton ? (
              <NxTooltip title="IQ applications can only have one GitHub App associated">
                <span>
                  <NxButton variant="primary" disabled>
                    <NxFontAwesomeIcon icon={faPlus} />
                    <span>Add GitHub App</span>
                  </NxButton>
                </span>
              </NxTooltip>
            ) : (
              <NxButton variant="primary" onClick={handleAddGitHubApp}>
                <NxFontAwesomeIcon icon={faPlus} />
                <span>Add GitHub App</span>
              </NxButton>
            )}
          </NxTile.HeaderActions>
        </NxTile.Header>
        {loading && <NxP>Loading...</NxP>}
        {error && <NxP className="nx-text--error">{error?.message || (typeof error === 'string' ? error : JSON.stringify(error))}</NxP>}
        {!loading && githubApps.length === 0 && (
          <NxP>There are no GitHub Apps configured for this organization.</NxP>
        )}
        {!loading && githubApps.length > 0 && (
          <NxTable>
            <NxTable.Head>
              <NxTable.Row>
                <NxTable.Cell>GitHub Organization</NxTable.Cell>
                <NxTable.Cell>GitHub Application</NxTable.Cell>
                <NxTable.Cell>Date</NxTable.Cell>
                <NxTable.Cell>Actions</NxTable.Cell>
              </NxTable.Row>
            </NxTable.Head>
            <NxTable.Body>
              {githubApps.map((app) => {
                const { displayName, isPersonal } = formatOrgName(app.githubOrganizationName);
                return (
                  <NxTable.Row key={app.id}>
                    <NxTable.Cell>
                      {displayName}
                      {isPersonal && <span className="nx-badge"> (personal)</span>}
                    </NxTable.Cell>
                    <NxTable.Cell>{app.slug}</NxTable.Cell>
                    <NxTable.Cell>{formatDate(app.lastUpdatedAt)}</NxTable.Cell>
                    <NxTable.Cell>
                      {app.installationUrl && (
                        <a
                          href={app.installationUrl}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="nx-text-link"
                          aria-label={`Go to GitHub for ${app.slug}`}
                        >
                          <NxFontAwesomeIcon icon={faExternalLink} /> Go to GitHub
                        </a>
                      )}
                      <NxButton
                        variant="icon-only"
                        onClick={() => handleDelete(app)}
                        aria-label={`Delete GitHub App ${app.slug}`}
                      >
                        <NxFontAwesomeIcon icon={faTrash} />
                      </NxButton>
                    </NxTable.Cell>
                  </NxTable.Row>
                );
              })}
            </NxTable.Body>
          </NxTable>
        )}
      </NxTile>

      <ManageGitHubAppsDeleteModal />
      {isRegistrationModalOpen && <GitHubAppRegistrationModal />}
    </div>
  );
}
