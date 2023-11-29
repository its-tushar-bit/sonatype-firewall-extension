/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { NxH3, NxP, NxTextLink, NxCopyToClipboard, NxCard, NxDescriptionList } from '@sonatype/react-shared-components';
import '../CiCdWizard.scss';
import PropTypes from 'prop-types';

export default function BambooWizard({ iqOrganization, iqApplication }) {
  const snippet = String.raw`- any-task:
      plugin-key: com.sonatype.clm.ci.bamboo:clm-scan-task
          description: Bamboo Task
      configuration:
          failOnClmFailures: 'true'
          failOnScanningErrors: 'false'
          clmOrgIdType: specified
              clmOrgId: ${iqOrganization}
          clmAppIdType: specified
              clmAppId: ${iqApplication}
              clmStageType: specified
          clmStageTypeId: build
          clmScanTargets: '**/*.jar'        
          clmModuleExcludes: '**/my-module/target/**'`;

  const downloadUrl = 'https://links.sonatype.com/products/nxiq/doc/integrations/bamboo/marketplace';
  const installUrl = 'https://links.sonatype.com/products/clm/bamboo/docs/installation';
  const reviewUrl = 'https://links.sonatype.com//products/clm/bamboo/docs/evaluate-policies-review-results';

  return (
    <div id="iq-integrations-cicd-wizard">
      <NxH3 className="iq-integrations-cicd-wizard-header">Overview</NxH3>
      <NxP id="iq-integrations-cicd-wizard-paragraph">
        Sonatype for Bamboo integrates with Atlassian Bamboo to run policy evaluations in the build workspace. It
        provides instant analysis of open-source components used in every Bamboo build.
      </NxP>
      <NxH3 className="iq-integrations-cicd-wizard-header">Set up your Bamboo configuration in 3 steps</NxH3>
      <NxCard.Container>
        <NxCard className="iq-integrations-card-cicd-wizard nx-card--equal" aria-label="Install / Configure">
          <NxCard.Content>
            <NxCard.Text className="iq-integrations-card--align-center">
              <NxH3>1. Download</NxH3>
              <NxP>Download plugin</NxP>
            </NxCard.Text>
          </NxCard.Content>
          <NxCard.Footer className="iq-integrations-card--align-center">
            <NxTextLink
              newTab={true}
              href={downloadUrl}
              data-analytics-id="sonatype-developer-cicd-bamboo-download-card"
            >
              View documentation
            </NxTextLink>
          </NxCard.Footer>
        </NxCard>
        <NxCard className="iq-integrations-card-cicd-wizard nx-card--equal" aria-label="Configure">
          <NxCard.Content>
            <NxCard.Text className="iq-integrations-card--align-center">
              <NxH3>2. Install</NxH3>
              <NxP>Install and configure</NxP>
            </NxCard.Text>
          </NxCard.Content>
          <NxCard.Footer className="iq-integrations-card--align-center">
            <NxTextLink newTab={true} href={installUrl} data-analytics-id="sonatype-developer-cicd-bamboo-install-card">
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
            <NxTextLink newTab={true} href={reviewUrl} data-analytics-id="sonatype-developer-cicd-bamboo-review-card">
              View documentation
            </NxTextLink>
          </NxCard.Footer>
        </NxCard>
      </NxCard.Container>
      <NxCopyToClipboard
        className="iq-integrations-copy-to-clipboard-cicd"
        label="Example script"
        id="Bamboo-pipeline-script"
        content={snippet}
      />
      <NxH3 className="iq-integrations-cicd-wizard-header">Parameter Description</NxH3>
      <NxDescriptionList className="iq-integrations-description-list-cicd nx-scrollable">
        <NxDescriptionList.Item>
          <NxDescriptionList.Term>Application ID (clmAppId)</NxDescriptionList.Term>
          <NxDescriptionList.Description>{iqApplication}</NxDescriptionList.Description>
          <NxDescriptionList.Description>The Server identifier for applications</NxDescriptionList.Description>
        </NxDescriptionList.Item>
        <NxDescriptionList.Item>
          <NxDescriptionList.Term>Scan patterns</NxDescriptionList.Term>
          <NxDescriptionList.Description>**/*.jar</NxDescriptionList.Description>
          <NxDescriptionList.Description>
            A comma-separated list of Ant-style patterns to analyze
          </NxDescriptionList.Description>
        </NxDescriptionList.Item>
        <NxDescriptionList.Item>
          <NxDescriptionList.Term>Stage</NxDescriptionList.Term>
          <NxDescriptionList.Description></NxDescriptionList.Description>
          <NxDescriptionList.Description>
            Options [develop, source, build, stage, release, operate]
          </NxDescriptionList.Description>
        </NxDescriptionList.Item>
        <NxDescriptionList.Item>
          <NxDescriptionList.Term>Org ID (clmOrgId)</NxDescriptionList.Term>
          <NxDescriptionList.Description>{iqOrganization}</NxDescriptionList.Description>
          <NxDescriptionList.Description>
            The Server identifier to the applications organization.
          </NxDescriptionList.Description>
        </NxDescriptionList.Item>
        <NxDescriptionList.Item>
          <NxDescriptionList.Term>plugin-key</NxDescriptionList.Term>
          <NxDescriptionList.Description>com.sonatype.clm.ci.bamboo:clm-scan-task</NxDescriptionList.Description>
          <NxDescriptionList.Description>
            The plugin identifier within Bamboo: The plugin's module used in the task. You can find the Sonatype plugin
            information in the Add-ons/apps administration section in Bamboo.
          </NxDescriptionList.Description>
        </NxDescriptionList.Item>
        <NxDescriptionList.Item>
          <NxDescriptionList.Term>failOnClmFailures</NxDescriptionList.Term>
          <NxDescriptionList.Description>true or false(default)</NxDescriptionList.Description>
          <NxDescriptionList.Description>
            If set to true the build will fail when an Sonatype evaluation can’t be performed or if for any reason the
            evaluation is not generated.
          </NxDescriptionList.Description>
        </NxDescriptionList.Item>
        <NxDescriptionList.Item>
          <NxDescriptionList.Term>failOnScanningErrors</NxDescriptionList.Term>
          <NxDescriptionList.Description>true or false(default)</NxDescriptionList.Description>
          <NxDescriptionList.Description>
            If set to true, the build will fail when errors are encountered during a scan such as malformed files.
          </NxDescriptionList.Description>
        </NxDescriptionList.Item>
        <NxDescriptionList.Item>
          <NxDescriptionList.Term>clmOrgIdType</NxDescriptionList.Term>
          <NxDescriptionList.Description>specified (default) or selected</NxDescriptionList.Description>
          <NxDescriptionList.Description>
            Whether the Organization ID is specified or selected from a list. In the Bamboo Specs scope any of the
            accepted values is valid.
          </NxDescriptionList.Description>
        </NxDescriptionList.Item>
        <NxDescriptionList.Item>
          <NxDescriptionList.Term>clmAppIdType</NxDescriptionList.Term>
          <NxDescriptionList.Description>specified or selected</NxDescriptionList.Description>
          <NxDescriptionList.Description>
            Whether the Application ID is specified or selected from a list. In the Bamboo Specs scope any of the
            accepted values is valid.
          </NxDescriptionList.Description>
        </NxDescriptionList.Item>
        <NxDescriptionList.Item>
          <NxDescriptionList.Term>clmStageType</NxDescriptionList.Term>
          <NxDescriptionList.Description>specified or selected</NxDescriptionList.Description>
          <NxDescriptionList.Description>
            Whether the Stage the policy evaluation runs is specified or selected from a list. In the Bamboo Specs scope
            any of the accepted values is valid.
          </NxDescriptionList.Description>
        </NxDescriptionList.Item>
      </NxDescriptionList>
      <NxP className="iq-integration-cicd-wizard-p">
        <b>For more information, visit </b>
        <NxTextLink
          data-analytics-id="sonatype-developer-cicd-bamboo-more-info"
          href="https://links.sonatype.com/products/nxiq/doc/integrations/bamboo"
          newTab
        >
          Sonatype Documentation
        </NxTextLink>
      </NxP>
    </div>
  );
}

BambooWizard.propTypes = {
  iqOrganization: PropTypes.string.isRequired,
  iqApplication: PropTypes.string.isRequired,
};
