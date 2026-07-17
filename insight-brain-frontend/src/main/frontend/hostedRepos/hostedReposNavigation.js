/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { isNexusOneBundle } from 'MainRoot/util/urlUtil';

/**
 * The Hosted Repos pages are shared between the Classic and Nexus One bundles
 * (Nexus One embeds them via `mountClassicComponent`). Each bundle registers
 * the same page hierarchy under different state names and URLs, so any
 * `stateGo` / `href` call inside these shared components must resolve to the
 * state name for the bundle it's currently running in. CLM-42184.
 */
const CLASSIC_TO_NEXUS_ONE = {
  hostedRepos: 'nexusOneRepositories',
  hostedRepositories: 'nexusOneRepositoriesDetail',
  hostedRepoComponents: 'nexusOneRepositoriesComponents',
};

/**
 * Returns the router state name for the active bundle. Pass the Classic state
 * name; in the Nexus One bundle it maps to the Nexus One equivalent.
 *
 * @param {'hostedRepos' | 'hostedRepositories' | 'hostedRepoComponents'} classicStateName
 * @returns {string}
 */
export function hostedReposState(classicStateName) {
  return isNexusOneBundle() ? CLASSIC_TO_NEXUS_ONE[classicStateName] : classicStateName;
}
