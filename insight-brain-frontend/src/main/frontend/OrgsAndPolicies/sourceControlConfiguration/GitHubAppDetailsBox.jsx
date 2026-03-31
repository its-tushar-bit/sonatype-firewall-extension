/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import PropTypes from 'prop-types';
import React from 'react';
import { NxButton, NxTextLink } from '@sonatype/react-shared-components';
import {
  isPersonalAccount,
  getCleanAccountName,
  getGitHubAppInstallationUrl,
  GITHUB_ACCOUNT_DISPLAY_LABELS,
} from './utils';
import './_gitHubAppDetailsBox.scss';

/**
 * Displays GitHub App configuration details in a formatted box
 * Shows: organization/account name, app name, installation link, configuration date
 * shows a Reconfigure button and custom repository URL
 */
const GitHubAppDetailsBox = ({ githubApp, linkText, repositoryUrl, onReconfigure, disabled }) => {
  if (!githubApp?.installationId) {
    return null;
  }

  const installationUrl = getGitHubAppInstallationUrl(githubApp.accountName, githubApp.installationId);
  // Use custom repository URL if provided, otherwise use installation URL
  const displayUrl = repositoryUrl || installationUrl;

  // Check if this is a personal account and get clean display name
  const isPersonal = isPersonalAccount(githubApp.accountName);
  const displayAccountName = getCleanAccountName(githubApp.accountName);

  return (
    <>
      <dl className="iq-github-app-details-box">
        <dt>
          {isPersonal ? `${GITHUB_ACCOUNT_DISPLAY_LABELS.PERSONAL}:` : `${GITHUB_ACCOUNT_DISPLAY_LABELS.ORGANIZATION}:`}
        </dt>
        <dd>{displayAccountName}</dd>
        {githubApp.name && (
          <>
            <dt>App:</dt>
            <dd>{githubApp.name}</dd>
          </>
        )}
        {displayUrl && (
          <>
            <dt>Repositories:</dt>
            <dd>
              <NxTextLink id="github-app-view-repos-link" href={displayUrl} external>
                {linkText || (repositoryUrl ? 'Go to GitHub Repositories' : 'View GitHub App configuration')}
              </NxTextLink>
            </dd>
          </>
        )}
        {githubApp.configurationDate && (
          <>
            <dt>Configuration Date:</dt>
            <dd>
              {new Date(githubApp.configurationDate).toLocaleString('en-US', {
                year: 'numeric',
                month: 'short',
                day: 'numeric',
                hour: 'numeric',
                minute: '2-digit',
                hour12: true,
                timeZoneName: 'short',
              })}
            </dd>
          </>
        )}
        <div className="iq-github-app-details-box__reconfigure">
          <NxButton
            id={onReconfigure ? 'github-app-reconfigure-button' : undefined}
            variant="tertiary"
            type="button"
            onClick={onReconfigure}
            disabled={disabled || !onReconfigure}
          >
            Reconfigure
          </NxButton>
        </div>
      </dl>
    </>
  );
};

GitHubAppDetailsBox.propTypes = {
  // Required props
  githubApp: PropTypes.shape({
    installationId: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
    accountName: PropTypes.string,
    accountType: PropTypes.string,
    name: PropTypes.string,
    configurationDate: PropTypes.string,
  }),
  // Optional props
  linkText: PropTypes.string,
  repositoryUrl: PropTypes.string,
  onReconfigure: PropTypes.func,
  disabled: PropTypes.bool,
};

export default GitHubAppDetailsBox;
