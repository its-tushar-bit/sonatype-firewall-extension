/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import {noPayloadActionCreator, payloadParamActionCreator} from '../../util/reduxUtil';
import axios from 'axios';
import {getManifestScanConfigUrl, getOrganizationsUrl} from '../../util/CLMLocation';

export const SCM_ONBOARDING_LOAD_CONFIG_REQUESTED = 'SCM_ONBOARDING_LOAD_CONFIG_REQUESTED';
export const SCM_ONBOARDING_LOAD_CONFIG_FULFILLED = 'SCM_ONBOARDING_LOAD_CONFIG_FULFILLED';
export const SCM_ONBOARDING_LOAD_CONFIG_FAILED = 'SCM_ONBOARDING_CONFIG_LOAD_FAILED';

export const SCM_ONBOARDING_LOAD_ORGANIZATIONS_REQUESTED = 'SCM_ONBOARDING_LOAD_ORGANIZATIONS_REQUESTED';
export const SCM_ONBOARDING_LOAD_ORGANIZATIONS_FULFILLED = 'SCM_ONBOARDING_LOAD_ORGANIZATIONS_FULFILLED';
export const SCM_ONBOARDING_LOAD_ORGANIZATIONS_FAILED = 'SCM_ONBOARDING_CONFIG_ORGS_FAILED';

export const SCM_ONBOARDING_LOAD_REPOSITORIES_REQUESTED = 'SCM_ONBOARDING_LOAD_REPOSITORIES_REQUESTED';
export const SCM_ONBOARDING_LOAD_REPOSITORIES_FULFILLED = 'SCM_ONBOARDING_LOAD_REPOSITORIES_FULFILLED';
export const SCM_ONBOARDING_LOAD_REPOSITORIES_FAILED = 'SCM_ONBOARDING_CONFIG_REPOSITORIES_FAILED';

export function loadConfig() {
  return function(dispatch) {
    dispatch(loadConfigRequested());

    return axios.get(getManifestScanConfigUrl())
        .then(({ data }) => { dispatch(loadConfigFulfilled(data)); })
        .catch(error => { dispatch(loadConfigFailed(error)); });
  };
}

export function loadOrganizations() {
  return function(dispatch) {
    dispatch(loadOrganizationsRequested());

    return axios.get(getOrganizationsUrl())
        .then(({ data }) => { dispatch(loadOrganizationsFulfilled(data)); })
        .catch(error => { dispatch(loadOrganizationsFailed(error)); });
  };
}

const repositories = [
  {project: 'insight-brain', namespace: 'sonatype', description: 'Nexus IQ server'},
  {project: 'nexus-internal', namespace: 'sonatype', description: 'Sonatype Nexus; Private internal project'},
  {project: 'sonatype-react-shared-components', namespace: 'sonatype', description: ''},
  {
    project: 'ossindex-terraform-service',
    namespace: 'sonatype',
    description: 'The terraform code for setting up an OSS Index environment (ECS, Kinesis, RDS, etc.)'
  },
  {project: 'security-research-dashboard', namespace: 'sonatype', description: ''},
  {project: 'data-legal-etl', namespace: 'sonatype', description: ''},
  {project: 'cx-org-manager-service', namespace: 'sonatype', description: ''},
  {project: 'hosted-data-services', namespace: 'sonatype', description: 'Nexus IQ Hosted Data Services (HDS)'},
  {project: 'analytics-whitelist', namespace: 'sonatype', description: 'The analytics service whitelist'},
  {
    project: 'low-quality-association-service',
    namespace: 'sonatype',
    description: 'Service that provide read-only access to Low Quality Association data through REST APIs'
  },
  {project: 'check-security-sources', namespace: 'sonatype', description: 'Check security publications for updates'},
  {project: 'ci-jenkins-configs', namespace: 'sonatype', description: 'stored job configurations for safe keeping'},
  {
    project: 'nbm-export',
    namespace: 'sonatype',
    description: 'Process that populates the database with Low Quality Association data'
  },
  {project: 'cx-orders-manager', namespace: 'sonatype', description: ''},
  {project: 'backup-iam', namespace: 'sonatype', description: 'A method to backup IAM in all the org accounts.'}
];

export function loadRepositories() {
  return function(dispatch) {
    dispatch(loadRepositoriesRequested());

    // TODO replace with real REST call in INT-34??
    return new Promise((resolve) => {
      let wait = setTimeout(() => {
        clearTimeout(wait);
        dispatch(loadRepositoriesFulfilled(repositories));
        resolve();
      }, 1000);
    });
  };
}

const loadConfigRequested = noPayloadActionCreator(SCM_ONBOARDING_LOAD_CONFIG_REQUESTED);
const loadConfigFulfilled = payloadParamActionCreator(SCM_ONBOARDING_LOAD_CONFIG_FULFILLED);
const loadConfigFailed = payloadParamActionCreator(SCM_ONBOARDING_LOAD_CONFIG_FAILED);

const loadOrganizationsRequested = noPayloadActionCreator(SCM_ONBOARDING_LOAD_ORGANIZATIONS_REQUESTED);
const loadOrganizationsFulfilled = payloadParamActionCreator(SCM_ONBOARDING_LOAD_ORGANIZATIONS_FULFILLED);
const loadOrganizationsFailed = payloadParamActionCreator(SCM_ONBOARDING_LOAD_ORGANIZATIONS_FAILED);

const loadRepositoriesRequested = noPayloadActionCreator(SCM_ONBOARDING_LOAD_REPOSITORIES_REQUESTED);
const loadRepositoriesFulfilled = payloadParamActionCreator(SCM_ONBOARDING_LOAD_REPOSITORIES_FULFILLED);
// TODO enable with real REST call in INT-34??
// const loadRepositoriesFailed = payloadParamActionCreator(SCM_ONBOARDING_LOAD_REPOSITORIES_FAILED);

export const SCM_ONBOARDING_SET_TARGET_ORGANIZATION = 'SCM_ONBOARDING_SET_TARGET_ORGANIZATION';
export const setSelectedOrganization = payloadParamActionCreator(SCM_ONBOARDING_SET_TARGET_ORGANIZATION);
