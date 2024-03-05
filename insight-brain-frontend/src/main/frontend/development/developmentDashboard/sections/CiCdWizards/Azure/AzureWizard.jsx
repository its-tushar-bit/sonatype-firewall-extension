/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { NxH3, NxP, NxTextLink, NxCard, NxDescriptionList } from '@sonatype/react-shared-components';
import PropTypes from 'prop-types';
import '../CiCdWizard.scss';

export default function AzureWizard({ iqOrganization, iqApplication }) {
  const downloadUrl = 'https://links.sonatype.com/products/nxiq/doc/integrations/azure-devops/marketplace';
  const installUrl = 'https://links.sonatype.com/products/nxiq/doc/integrations/azure-devops/installation';
  const evaluationUrl = 'https://links.sonatype.com/products/nxiq/doc/integrations/azure-devops/evaluation';

  return (
    <div id="iq-integrations-cicd-wizard">
      <NxH3 className="iq-integrations-cicd-wizard-header">Overview</NxH3>
      <NxP id="iq-integrations-cicd-wizard-paragraph">
        You must log in to your Azure DevOps account to install the Sonatype Platform plugin. Follow the steps in the
        documentation for details.
      </NxP>
      <NxH3 className="iq-integrations-cicd-wizard-header">Set up your Azure DevOps configuration in 3 steps</NxH3>
      <NxCard.Container>
        <NxCard className="iq-integrations-card-cicd-wizard nx-card--equal" aria-label="Download">
          <NxCard.Content>
            <NxCard.Text className="iq-integrations-card--align-center">
              <NxH3>1. Download</NxH3>
              <NxP>Download from Marketplace</NxP>
            </NxCard.Text>
          </NxCard.Content>
          <NxCard.Footer className="iq-integrations-card--align-center">
            <NxTextLink
              newTab={true}
              href={downloadUrl}
              data-analytics-id="sonatype-developer-cicd-azure-download-card"
            >
              View documentation
            </NxTextLink>
          </NxCard.Footer>
        </NxCard>
        <NxCard className="iq-integrations-card-cicd-wizard nx-card--equal" aria-label="Install & Configure">
          <NxCard.Content>
            <NxCard.Text className="iq-integrations-card--align-center">
              <NxH3>2. Install</NxH3>
              <NxP>Install & set up plugin</NxP>
            </NxCard.Text>
          </NxCard.Content>
          <NxCard.Footer className="iq-integrations-card--align-center">
            <NxTextLink newTab={true} href={installUrl} data-analytics-id="sonatype-developer-cicd-azure-install-card">
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
            <NxTextLink
              newTab={true}
              href={evaluationUrl}
              data-analytics-id="sonatype-developer-cicd-azure-review-card"
            >
              View documentation
            </NxTextLink>
          </NxCard.Footer>
        </NxCard>
      </NxCard.Container>
      <NxH3 className="iq-integrations-cicd-wizard-header">Parameter Description</NxH3>
      <NxDescriptionList className="iq-integrations-description-list-cicd">
        <NxDescriptionList.Item>
          <NxDescriptionList.Term>Organization ID</NxDescriptionList.Term>
          <NxDescriptionList.Description>{iqOrganization}</NxDescriptionList.Description>
          <NxDescriptionList.Description>
            The ID of the organization under which the application will be created if the automatic application creation
            is enabled and the application does not exist.
          </NxDescriptionList.Description>
        </NxDescriptionList.Item>
        <NxDescriptionList.Item>
          <NxDescriptionList.Term>Application ID</NxDescriptionList.Term>
          <NxDescriptionList.Description>{iqApplication}</NxDescriptionList.Description>
          <NxDescriptionList.Description>
            ID of the application to evaluate against as configured in Nexus IQ
          </NxDescriptionList.Description>
        </NxDescriptionList.Item>
        <NxDescriptionList.Item>
          <NxDescriptionList.Term>Stage</NxDescriptionList.Term>
          <NxDescriptionList.Description>options [build, stage, release, operate]</NxDescriptionList.Description>
          <NxDescriptionList.Description>Stage in Sonatype for the evaluation</NxDescriptionList.Description>
        </NxDescriptionList.Item>
        <NxDescriptionList.Item>
          <NxDescriptionList.Term>Scan Targets</NxDescriptionList.Term>
          <NxDescriptionList.Description>**/*.jar, **/*.json</NxDescriptionList.Description>
          <NxDescriptionList.Description>
            Targets to perform policy evaluation are listed as comma-separated glob patterns
          </NxDescriptionList.Description>
        </NxDescriptionList.Item>
        <NxDescriptionList.Item>
          <NxDescriptionList.Term>
            Use $&#123;Pipeline.Workspace&#125; as the base folder for scanning
          </NxDescriptionList.Term>
          <NxDescriptionList.Description></NxDescriptionList.Description>
          <NxDescriptionList.Description>
            If checked, the above scan targets are evaluated against the $&#123;Pipeline.Workspace&#125; folder and its
            descendants
          </NxDescriptionList.Description>
        </NxDescriptionList.Item>
        <NxDescriptionList.Item>
          <NxDescriptionList.Term>Ignore Sonatype's system errors</NxDescriptionList.Term>
          <NxDescriptionList.Description></NxDescriptionList.Description>
          <NxDescriptionList.Description>
            Controls the pipeline outcome when the scan or evaluation fails to produce results for some (possibly
            intermittent) connection problem
          </NxDescriptionList.Description>
        </NxDescriptionList.Item>
        <NxDescriptionList.Item>
          <NxDescriptionList.Term>Ignore Sonatype's Scanning errors</NxDescriptionList.Term>
          <NxDescriptionList.Description></NxDescriptionList.Description>
          <NxDescriptionList.Description>
            Controls the pipeline outcome when there are scanning errors such as malformed files
          </NxDescriptionList.Description>
        </NxDescriptionList.Item>
        <NxDescriptionList.Item>
          <NxDescriptionList.Term>Enable Debug Logging</NxDescriptionList.Term>
          <NxDescriptionList.Description></NxDescriptionList.Description>
          <NxDescriptionList.Description>
            Enables debug logging for Sonatype policy evaluation
          </NxDescriptionList.Description>
        </NxDescriptionList.Item>
        <NxDescriptionList.Item>
          <NxDescriptionList.Term>Java System Properties</NxDescriptionList.Term>
          <NxDescriptionList.Description></NxDescriptionList.Description>
          <NxDescriptionList.Description>
            Command line arguments to alter the behavior of the JVM
          </NxDescriptionList.Description>
        </NxDescriptionList.Item>
      </NxDescriptionList>
      <NxP className="iq-integration-cicd-wizard-p">
        <b>For more information, visit </b>
        <NxTextLink
          data-analytics-id="sonatype-developer-cicd-azure-more-info"
          newTab={true}
          href="https://links.sonatype.com/products/nxiq/doc/integrations/azure-devops"
        >
          Sonatype Documentation
        </NxTextLink>
      </NxP>
    </div>
  );
}

AzureWizard.propTypes = {
  iqOrganization: PropTypes.string.isRequired,
  iqApplication: PropTypes.string.isRequired,
};
