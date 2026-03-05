/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import PropTypes from 'prop-types';
import React from 'react';
import { NxButton, NxTextLink } from '@sonatype/react-shared-components';
import './_gitHubAppDetailsBox.scss';

/**
 * Helper function to generate GitHub App installation URL based on account type
 */
const getGitHubAppUrl = (githubApp) => {
  if (!githubApp?.installationId) return null;
  if (githubApp.accountType === 'personal') {
    return `https://github.com/settings/installations/${githubApp.installationId}`;
  }
  return `https://github.com/organizations/${githubApp.accountName}/settings/installations/${githubApp.installationId}`;
};

/**
 * Displays GitHub App configuration details in a formatted box
 * Shows: organization/account name, app name, installation link, configuration date
 * shows a Reconfigure button and custom repository URL
 */
const GitHubAppDetailsBox = ({ githubApp, linkText, repositoryUrl, onReconfigure, disabled }) => {
  if (!githubApp?.installationId) {
    return null;
  }

  const installationUrl = getGitHubAppUrl(githubApp);
  // Use custom repository URL if provided, otherwise use installation URL
  const displayUrl = repositoryUrl || installationUrl;

  return (
    <>
      <dl className="iq-github-app-details-box">
        <dt>Organization:</dt>
        <dd>{githubApp.accountName || ''}</dd>
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
              <NxTextLink href={displayUrl} external>
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
        {
          <div className="iq-github-app-details-box__reconfigure">
            <NxButton variant="tertiary" type="button" onClick={onReconfigure} disabled={disabled}>
              Reconfigure
            </NxButton>
          </div>
        }
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
