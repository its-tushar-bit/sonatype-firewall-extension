/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { NxH3, NxP, NxTextLink, NxCopyToClipboard, NxCard, NxDescriptionList } from '@sonatype/react-shared-components';
import React from 'react';
import '../CiCdWizard.scss';
import PropTypes from 'prop-types';

export default function GitlabWizard({ iqApplication }) {
  const configureUrl = 'https://links.sonatype.com/products/nxiq/doc/integrations/gitlab-ci-configuration';
  const connectUrl = 'https://links.sonatype.com/products/nxiq/doc/integrations/gitlab-ci-connect-info';
  const dockerUrl = 'https://links.sonatype.com/products/nxiq/doc/integrations/gitlab-docker-image';

  const snippet = String.raw`
  iq_policy_eval:
      stage: test
      image: sonatype/gitlab-nexus-iq-pipeline:latest
      script:
          - /sonatype/evaluate -i ${iqApplication} target/{target_file}
    artifacts:
        name: "policy-eval-$CI_JOB_NAME-$CI_COMMIT_REF_NAME"
        paths:
            - ${iqApplication}-policy-eval-report.html`;
  return (
    <div id="iq-integrations-cicd-wizard">
      <NxH3 className="iq-integrations-cicd-wizard-header">Overview</NxH3>
      <NxP id="iq-integrations-cicd-wizard-paragraph">
        The Docker image for Sonatype Policy Evaluation allows you to perform policy evaluations against one or more
        build artifacts during a GitLab CI/CD pipeline run.
      </NxP>
      <NxH3 className="iq-integrations-cicd-wizard-header">Set up your GitLab configuration in 2 steps</NxH3>
      <NxCard.Container>
        <NxCard className="iq-integrations-card-cicd-wizard nx-card--equal" aria-label="Install / Configure">
          <NxCard.Content>
            <NxCard.Text className="iq-integrations-card--align-center">
              <NxH3>1. Create</NxH3>
              <NxP>Create GitLab pipeline job</NxP>
            </NxCard.Text>
          </NxCard.Content>
          <NxCard.Footer className="iq-integrations-card--align-center">
            <NxTextLink
              newTab
              href={configureUrl}
              data-analytics-id="sonatype-developer-cicd-gitlab-create-pipeline-card"
            >
              View documentation
            </NxTextLink>
          </NxCard.Footer>
        </NxCard>
        <NxCard className="iq-integrations-card-cicd-wizard nx-card--equal" aria-label="Connect">
          <NxCard.Content>
            <NxCard.Text className="iq-integrations-card--align-center">
              <NxH3>2. Configure</NxH3>
              <NxP>Configure CI/CD settings</NxP>
            </NxCard.Text>
          </NxCard.Content>
          <NxCard.Footer className="iq-integrations-card--align-center">
            <NxTextLink newTab href={connectUrl} data-analytics-id="sonatype-developer-cicd-gitlab-configure-card">
              View documentation
            </NxTextLink>
          </NxCard.Footer>
        </NxCard>
      </NxCard.Container>
      <NxCopyToClipboard
        label="Example script"
        className="iq-integrations-copy-to-clipboard-cicd"
        id="gitlab-pipeline-script"
        content={snippet}
      />
      <NxH3 className="iq-integrations-cicd-wizard-header">Parameter Description</NxH3>
      <NxDescriptionList className="iq-integrations-description-list-cicd">
        <NxDescriptionList.Item>
          <NxDescriptionList.Term>Name</NxDescriptionList.Term>
          <NxDescriptionList.Description>policyEval</NxDescriptionList.Description>
          <NxDescriptionList.Description>The name of the job</NxDescriptionList.Description>
        </NxDescriptionList.Item>
        <NxDescriptionList.Item>
          <NxDescriptionList.Term>Stage</NxDescriptionList.Term>
          <NxDescriptionList.Description>test</NxDescriptionList.Description>
          <NxDescriptionList.Description>
            The pipeline stage in which to run the policy evaluation
          </NxDescriptionList.Description>
        </NxDescriptionList.Item>
        <NxDescriptionList.Item>
          <NxDescriptionList.Term>Image</NxDescriptionList.Term>
          <NxDescriptionList.Description>sonatype/gitlab-nexus-iq-pipeline:latest</NxDescriptionList.Description>
          <NxDescriptionList.Description>
            The docker image that will be used during the execution of this job
          </NxDescriptionList.Description>
        </NxDescriptionList.Item>
        <NxDescriptionList.Item>
          <NxDescriptionList.Term>ApplicationId</NxDescriptionList.Term>
          <NxDescriptionList.Description>{iqApplication}</NxDescriptionList.Description>
          <NxDescriptionList.Description>
            The unique Id of the application being evaluated
          </NxDescriptionList.Description>
        </NxDescriptionList.Item>
        <NxDescriptionList.Item>
          <NxDescriptionList.Term>Artifacts path</NxDescriptionList.Term>
          <NxDescriptionList.Description>target/web-app-x.war</NxDescriptionList.Description>
          <NxDescriptionList.Description>
            The path to the build artifact that will be evaluated
          </NxDescriptionList.Description>
        </NxDescriptionList.Item>
      </NxDescriptionList>
      <NxP className="iq-integration-cicd-wizard-p">
        See the
        <NxTextLink newTab href={dockerUrl} data-analytics-id="sonatype-developer-cicd-gitlab-docker-url">
          {' '}
          documentation provided with the image on Docker Hub{' '}
        </NxTextLink>
        for details about other options available during policy evaluation
      </NxP>
      <NxP className="iq-integration-cicd-wizard-p">
        <b>For more information, visit </b>
        <NxTextLink
          newTab
          data-analytics-id="sonatype-developer-cicd-gitlab-more-info"
          href="https://links.sonatype.com/products/nxiq/doc/integrations/gitlab"
        >
          Sonatype Documentation
        </NxTextLink>
      </NxP>
    </div>
  );
}

GitlabWizard.propTypes = {
  iqApplication: PropTypes.string.isRequired,
};
