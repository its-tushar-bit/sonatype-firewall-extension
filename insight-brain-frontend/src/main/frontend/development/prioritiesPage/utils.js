/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { keys } from 'ramda';

export const defaultIntegrationParamsMap = {
  cli: {
    filterOnPolicyActions: true,
  },
  maven: {
    filterOnPolicyActions: true,
  },
  azure: {
    filterOnPolicyActions: true,
  },
  bamboo: {
    filterOnPolicyActions: true,
  },
  jenkins: {
    filterOnPolicyActions: true,
  },
  'jira-dc': {
    filterOnPolicyActions: false,
  },
  'jira-cloud': {
    filterOnPolicyActions: false,
  },
  gitlab: {
    filterOnPolicyActions: true,
  },
  'github-actions': {
    filterOnPolicyActions: true,
  },
};

export const validIntegrationTypes = keys(defaultIntegrationParamsMap);
