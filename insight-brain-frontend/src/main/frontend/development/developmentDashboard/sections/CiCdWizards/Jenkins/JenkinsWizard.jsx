/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { NxH3, NxP, NxTextLink, NxCopyToClipboard, NxCard, NxDescriptionList } from '@sonatype/react-shared-components';
import '../CiCdWizard.scss';
import PropTypes from 'prop-types';

export default function JenkinsWizard({ iqOrganization, iqApplication }) {
  const snippet = String.raw` nexusPolicyEvaluation(
                             iqApplication: '${iqApplication}',
                             iqInstanceId: 'MyNexusIQServer1',
                             iqScanPatterns: [[scanPattern: ‘*/.js’], [scanPattern: ‘*/.zip’]],
                             iqStage: 'build'
                             iqOrganization: '${iqOrganization}' 
)`;
  const installUrl = 'https://links.sonatype.com/products/nxiq/doc/integrations/jenkins/installation';
  const connectUrl = 'https://links.sonatype.com/products/nxiq/doc/integrations/jenkins/integrating';
  const evaluationUrl = 'https://links.sonatype.com/products/nxiq/doc/integrations/jenkins/evaluation';

  return (
    <div id="iq-integrations-cicd-wizard">
      <NxH3 className="iq-integrations-cicd-wizard-header">Overview</NxH3>
      <NxP id="iq-integrations-cicd-wizard-paragraph">
        A Jenkins administrator is required to install and connect the Sonatype Platform plugin for Jenkins to Sonatype
        server for the first time. Follow the steps in the documentation for details.
      </NxP>
      <NxH3 className="iq-integrations-cicd-wizard-header">Set up your Jenkins configuration in 3 steps</NxH3>
      <NxCard.Container>
        <NxCard className="iq-integrations-card-cicd-wizard nx-card--equal" aria-label="Install / Configure">
          <NxCard.Content>
            <NxCard.Text className="iq-integrations-card--align-center">
              <NxH3>1. Install</NxH3>
              <NxP>Install & set up the plugin</NxP>
            </NxCard.Text>
          </NxCard.Content>
          <NxCard.Footer className="iq-integrations-card--align-center">
            <NxTextLink newTab href={installUrl} data-analytics-id="sonatype-developer-cicd-jenkins-install-card">
              View documentation
            </NxTextLink>
          </NxCard.Footer>
        </NxCard>
        <NxCard className="iq-integrations-card-cicd-wizard nx-card--equal" aria-label="Connect">
          <NxCard.Content>
            <NxCard.Text className="iq-integrations-card--align-center">
              <NxH3>2. Connect</NxH3>
              <NxP>Connect to Sonatype IQ</NxP>
            </NxCard.Text>
          </NxCard.Content>
          <NxCard.Footer className="iq-integrations-card--align-center">
            <NxTextLink newTab href={connectUrl} data-analytics-id="sonatype-developer-cicd-jenkins-connect-card">
              View documentation
            </NxTextLink>
          </NxCard.Footer>
        </NxCard>
        <NxCard className="iq-integrations-card-cicd-wizard nx-card--equal" aria-label="Review">
          <NxCard.Content>
            <NxCard.Text className="iq-integrations-card--align-center">
              <NxH3>3. Review</NxH3>
              <NxP>Reviewing evaluation results</NxP>
            </NxCard.Text>
          </NxCard.Content>
          <NxCard.Footer className="iq-integrations-card--align-center">
            <NxTextLink newTab href={evaluationUrl} data-analytics-id="sonatype-developer-cicd-jenkins-review-card">
              View documentation
            </NxTextLink>
          </NxCard.Footer>
        </NxCard>
      </NxCard.Container>
      <NxCopyToClipboard
        className="iq-integrations-copy-to-clipboard-cicd"
        label="Example script"
        id="jenkins-pipeline-script"
        content={snippet}
      />
      <NxH3 className="iq-integrations-cicd-wizard-header">Parameter Description</NxH3>
      <NxDescriptionList className="iq-integrations-description-list-cicd">
        <NxDescriptionList.Item>
          <NxDescriptionList.Term>Application ID</NxDescriptionList.Term>
          <NxDescriptionList.Description>The server identifier for applications</NxDescriptionList.Description>
        </NxDescriptionList.Item>
        <NxDescriptionList.Item>
          <NxDescriptionList.Term>Instance</NxDescriptionList.Term>
          <NxDescriptionList.Description>
            The Jenkins configuration setting for Sonatype Server
          </NxDescriptionList.Description>
        </NxDescriptionList.Item>
        <NxDescriptionList.Item>
          <NxDescriptionList.Term>
            Scan patterns <div className="iq-integrations-description-list-thinner-text">(Optional)</div>
          </NxDescriptionList.Term>
          <NxDescriptionList.Description>
            Ecosystem specific patterns to match components to analyze
          </NxDescriptionList.Description>
        </NxDescriptionList.Item>
        <NxDescriptionList.Item>
          <NxDescriptionList.Term>
            Stage <div className="iq-integrations-description-list-thinner-text">(Optional)</div>
          </NxDescriptionList.Term>
          <NxDescriptionList.Description>Options [build, stage, release, operate]</NxDescriptionList.Description>
        </NxDescriptionList.Item>
        <NxDescriptionList.Item>
          <NxDescriptionList.Term>
            Organization <div className="iq-integrations-description-list-thinner-text">(Optional)</div>
          </NxDescriptionList.Term>
          <NxDescriptionList.Description>
            The Server identifier to the applications organization.
          </NxDescriptionList.Description>
        </NxDescriptionList.Item>
      </NxDescriptionList>
      <NxP className="iq-integration-cicd-wizard-p">
        <b>For more information, visit </b>
        <NxTextLink
          data-analytics-id="sonatype-developer-cicd-jenkins-more-info"
          href="https://links.sonatype.com/products/nxiq/doc/integrations/jenkins"
          newTab
        >
          Sonatype Documentation
        </NxTextLink>
      </NxP>
    </div>
  );
}

JenkinsWizard.propTypes = {
  iqOrganization: PropTypes.string.isRequired,
  iqApplication: PropTypes.string.isRequired,
};
