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
    <div id="iq-integrations-cicd-jenkins">
      <NxH3>Overview</NxH3>
      <NxP className="iq-integrations-cicd-jenkins__full-width-text">
        A Jenkins administrator is required to install and connect the Sonatype Platform plugin for Jenkins to IQ server
        for the first time. Follow the steps in the documentation for details.
      </NxP>
      <NxH3>Easy steps for Jenkins Configuration</NxH3>
      <NxCard.Container>
        <NxCard className="iq-integrations-card-cicd" aria-label="Install / Configure">
          <NxCard.Content>
            <NxCard.Text className="iq-integrations-card--align-center">
              <NxH3>Install / Configure</NxH3>
              <NxP>Install the plugin</NxP>
            </NxCard.Text>
          </NxCard.Content>
          <NxCard.Footer className="iq-integrations-card--align-center">
            <NxTextLink newTab href={installUrl} data-analytics-id="sonatype-developer-cicd-jenkins-install-card">
              Link
            </NxTextLink>
          </NxCard.Footer>
        </NxCard>
        <NxCard className="iq-integrations-card-cicd" aria-label="Connect">
          <NxCard.Content>
            <NxCard.Text className="iq-integrations-card--align-center">
              <NxH3>Connect</NxH3>
              <NxP>Connect to Sonatype IQ</NxP>
            </NxCard.Text>
          </NxCard.Content>
          <NxCard.Footer className="iq-integrations-card--align-center">
            <NxTextLink newTab href={connectUrl} data-analytics-id="sonatype-developer-cicd-jenkins-connect-card">
              Link
            </NxTextLink>
          </NxCard.Footer>
        </NxCard>
        <NxCard className="iq-integrations-card-cicd" aria-label="Review">
          <NxCard.Content>
            <NxCard.Text className="iq-integrations-card--align-center">
              <NxH3>Review</NxH3>
              <NxP>
                Reviewing Evaluations <br /> Results
              </NxP>
            </NxCard.Text>
          </NxCard.Content>
          <NxCard.Footer className="iq-integrations-card--align-center">
            <NxTextLink newTab href={evaluationUrl} data-analytics-id="sonatype-developer-cicd-jenkins-review-card">
              Link
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
      <NxH3>Parameter Description</NxH3>
      <NxDescriptionList className="iq-integrations-description-list-cicd">
        <NxDescriptionList.Item>
          <NxDescriptionList.Term>IQ Application</NxDescriptionList.Term>
          <NxDescriptionList.Description>the IQ server identifier for applications</NxDescriptionList.Description>
        </NxDescriptionList.Item>
        <NxDescriptionList.Item>
          <NxDescriptionList.Term>IQ Instance</NxDescriptionList.Term>
          <NxDescriptionList.Description>the Jenkins configuration setting for IQ Server</NxDescriptionList.Description>
        </NxDescriptionList.Item>
        <NxDescriptionList.Item>
          <NxDescriptionList.Term>
            IQ Scan patterns <div className="iq-integrations-description-list-thinner-text">(Optional)</div>
          </NxDescriptionList.Term>
          <NxDescriptionList.Description>
            ecosystem specific patterns to match components to analyze
          </NxDescriptionList.Description>
        </NxDescriptionList.Item>
        <NxDescriptionList.Item>
          <NxDescriptionList.Term>
            IQ Stage <div className="iq-integrations-description-list-thinner-text">(Optional)</div>
          </NxDescriptionList.Term>
          <NxDescriptionList.Description>options [build, stage, release, operate]</NxDescriptionList.Description>
        </NxDescriptionList.Item>
        <NxDescriptionList.Item>
          <NxDescriptionList.Term>
            IQ Organization <div className="iq-integrations-description-list-thinner-text">(Optional)</div>
          </NxDescriptionList.Term>
          <NxDescriptionList.Description>
            the IQ Server identifier to the applications organization.
          </NxDescriptionList.Description>
        </NxDescriptionList.Item>
      </NxDescriptionList>
      <NxP>
        <b>For more information, visit </b>
        <NxTextLink
          data-analytics-id="sonatype-developer-cicd-jenkins-more-info"
          href="https://links.sonatype.com/products/nxiq/doc/integrations/jenkins"
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
