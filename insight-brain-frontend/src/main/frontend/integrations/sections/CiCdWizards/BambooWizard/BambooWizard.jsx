/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import {
  NxH3,
  NxP,
  NxTextLink,
  NxCopyToClipboard,
  NxCard,
  NxTable,
  NxDescriptionList,
} from '@sonatype/react-shared-components';
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
    <div id="iq-integrations-cicd-Bamboo">
      <NxH3>Overview</NxH3>
      <NxP className="iq-integrations__full-width-text">
        Sonatype IQ for Bamboo integrates with Atlassian Bamboo to run policy evaluations in the build workspace. It
        provides instant analysis of open-source components used in every Bamboo build.
      </NxP>
      <NxH3>Easy steps for Bamboo Configuration</NxH3>
      <NxCard.Container className="iq-integrations-card-container">
        <NxCard className="iq-integrations-card-cicd" aria-label="Install / Configure">
          <NxCard.Content>
            <NxCard.Text className="iq-integrations-card--align-center">
              <NxH3>Download</NxH3>
              <NxP>Install & Start IQ Server</NxP>
            </NxCard.Text>
          </NxCard.Content>
          <NxCard.Footer className="iq-integrations-card--align-center">
            <NxTextLink newTab={true} href={downloadUrl}>
              Link
            </NxTextLink>
          </NxCard.Footer>
        </NxCard>
        <NxCard className="iq-integrations-card-cicd" aria-label="Configure">
          <NxCard.Content>
            <NxCard.Text className="iq-integrations-card--align-center">
              <NxH3>Install & Configure</NxH3>
              <NxP>
                Configure and <br /> add analysis
              </NxP>
            </NxCard.Text>
          </NxCard.Content>
          <NxCard.Footer className="iq-integrations-card--align-center">
            <NxTextLink newTab={true} href={installUrl}>
              Link
            </NxTextLink>
          </NxCard.Footer>
        </NxCard>
        <NxCard className="iq-integrations-card-cicd" aria-label="Review">
          <NxCard.Content>
            <NxCard.Text className="iq-integrations-card--align-center">
              <NxH3>Review</NxH3>
              <NxP>
                Review Evaluation <br /> Results
              </NxP>
            </NxCard.Text>
          </NxCard.Content>
          <NxCard.Footer className="iq-integrations-card--align-center">
            <NxTextLink newTab={true} href={reviewUrl}>
              Link
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
      <NxH3>Parameter Description</NxH3>
      <NxDescriptionList className="iq-integrations-description-list-cicd">
        <NxDescriptionList.Item>
          <NxDescriptionList.Term>IQ Application (clmAppId)</NxDescriptionList.Term>
          <NxDescriptionList.Description>{iqApplication}</NxDescriptionList.Description>
          <NxDescriptionList.Description>The IQ Server identifier for applications</NxDescriptionList.Description>
        </NxDescriptionList.Item>
        <NxDescriptionList.Item>
          <NxDescriptionList.Term>IQ Scan patterns</NxDescriptionList.Term>
          <NxDescriptionList.Description>**/*.jar</NxDescriptionList.Description>
          <NxDescriptionList.Description>
            A comma-separated list of Ant-style patterns to analyze
          </NxDescriptionList.Description>
        </NxDescriptionList.Item>
        <NxDescriptionList.Item>
          <NxDescriptionList.Term>IQ Stage</NxDescriptionList.Term>
          <NxDescriptionList.Description></NxDescriptionList.Description>
          <NxDescriptionList.Description>
            Options [develop, source, build, stage, release, operate]
          </NxDescriptionList.Description>
        </NxDescriptionList.Item>
        <NxDescriptionList.Item>
          <NxDescriptionList.Term>IQ Org ID (clmOrgId)</NxDescriptionList.Term>
          <NxDescriptionList.Description>{iqOrganization}</NxDescriptionList.Description>
          <NxDescriptionList.Description>
            the IQ Server identifier to the applications organization.
          </NxDescriptionList.Description>
        </NxDescriptionList.Item>
        <NxDescriptionList.Item>
          <NxDescriptionList.Term>plugin-key</NxDescriptionList.Term>
          <NxDescriptionList.Description>com.sonatype.clm.ci.bamboo:clm-scan-task</NxDescriptionList.Description>
          <NxDescriptionList.Description>
            The plugin identifier within Bamboo: The plugin's module used in the task. You can find the Nexus IQ plugin
            information in the Add-ons/apps administration section in Bamboo.
          </NxDescriptionList.Description>
        </NxDescriptionList.Item>
        <NxDescriptionList.Item>
          <NxDescriptionList.Term>failOnClmFailures</NxDescriptionList.Term>
          <NxDescriptionList.Description>true or false(default)</NxDescriptionList.Description>
          <NxDescriptionList.Description>
            If set to true the build will fail when an IQ evaluation can’t be performed or if for any reason the
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
            Whether the Nexus IQ Organization ID is specified or selected from a list. In the Bamboo Specs scope any of
            the accepted values is valid.
          </NxDescriptionList.Description>
        </NxDescriptionList.Item>
        <NxDescriptionList.Item>
          <NxDescriptionList.Term>clmAppIdType</NxDescriptionList.Term>
          <NxDescriptionList.Description>specified or selected</NxDescriptionList.Description>
          <NxDescriptionList.Description>
            Whether the Nexus IQ Application ID is specified or selected from a list. In the Bamboo Specs scope any of
            the accepted values is valid.
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
      <NxP>
        <b>For more information, visit </b>
        <NxTextLink href="https://links.sonatype.com/products/nxiq/doc/integrations/bamboo">
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
