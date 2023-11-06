/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { NxCard, NxCode, NxH3, NxList, NxTextLink } from '@sonatype/react-shared-components';
import * as PropTypes from 'prop-types';
import { useSelector } from 'react-redux';
import { selectShouldDisplayNotice } from 'MainRoot/configuration/baseUrl/baseUrlConfigurationSelectors';
import './scmWizard.scss';
import { formatSCMProvider, getSCMProviderTokenDocUrl, getSCMProviderTokenUrl } from './scmWizardUtil';

export default function ScmWizard({ scmProvider, applicationPublicId }) {
  const formattedScmProvider = formatSCMProvider(scmProvider);
  const tokenUrl = getSCMProviderTokenUrl(scmProvider);
  const tokenDocUrl = getSCMProviderTokenDocUrl(scmProvider);

  const shouldDisplayNotice = useSelector(selectShouldDisplayNotice);

  const permissionUrl = 'http://links.sonatype.com/products/nxiq/doc/scm-token-permissions';
  const applicationSourceControlPage = `/assets/#/management/edit/application/${applicationPublicId}/source-control`;
  const automaticSourceControlConfigurationPage = '/assets/#/automaticSourceControlConfiguration';
  const configureSourceControlHelpLink = 'http://links.sonatype.com/products/nxiq/doc/scm-connect-iq';

  return (
    <NxCard className="iq-integrations-scm-wizard-card" aria-label="SCM Wizard">
      <div className="iq-integrations-scm-wizard-card-sections">
        <NxH3>Create Access Token</NxH3>
        <NxList bulleted>
          <NxList.Item>
            You can create access token in <NxCode>{tokenUrl}</NxCode>
          </NxList.Item>
          <NxList.Item>
            You can find more information about how to create access token in{' '}
            <NxTextLink href={tokenDocUrl} newTab>
              here
            </NxTextLink>
          </NxList.Item>
          <NxList.Item>
            <NxTextLink href={permissionUrl} newTab>
              Check required permissions here
            </NxTextLink>
          </NxList.Item>
        </NxList>
      </div>

      {shouldDisplayNotice && (
        <div className="iq-integrations-scm-wizard-card-sections">
          <NxH3>Configure Base URL</NxH3>
          <NxList bulleted>
            <NxList.Item>
              <NxTextLink href="/assets/#/baseUrl" newTab>
                Click here{' '}
              </NxTextLink>
              to configure base URL for the organization
            </NxList.Item>
          </NxList>
        </div>
      )}
      <div className="iq-integrations-scm-wizard-card-sections">
        <NxH3>Application Source Control Configuration</NxH3>
        <NxList bulleted>
          <NxList.Item>
            <NxList.Text>
              You can{' '}
              <NxTextLink href={applicationSourceControlPage} newTab>
                Click here{' '}
              </NxTextLink>
              to go to your application source control configuration page
            </NxList.Text>
            <NxList bulleted>
              <NxList.Item>Make sure you set up your repository url</NxList.Item>
              <NxList.Item>
                Make sure {formattedScmProvider} is selected as your source control management system
              </NxList.Item>
              <NxList.Item>Make sure your access token is set up with the required permissions</NxList.Item>
              <NxList.Item>Double check the default branch name is correct</NxList.Item>
              <NxList.Item>
                You can also enable{' '}
                <NxTextLink href={automaticSourceControlConfigurationPage} newTab>
                  Automatic Source Control
                </NxTextLink>{' '}
                , which the repository URL will be automatically discovered from the git project information and
                configured for the IQ application
              </NxList.Item>
              <NxList.Item>
                More information about how to configure source control for your application can be found{' '}
                <NxTextLink href={configureSourceControlHelpLink} newTab>
                  here
                </NxTextLink>
              </NxList.Item>
            </NxList>
          </NxList.Item>
        </NxList>
      </div>
    </NxCard>
  );
}

ScmWizard.propTypes = {
  scmProvider: PropTypes.string.isRequired,
  applicationPublicId: PropTypes.string.isRequired,
};
