/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import {
  filter,
  groupBy,
  prop,
  pipe,
  toPairs,
  map,
  sortWith,
  descend,
  ascend,
  length,
  take,
  drop,
  flatten,
  includes,
  cond,
  equals,
  always,
} from 'ramda';
import { NxTextLink } from '@sonatype/react-shared-components';

/**
 * @typedef {import('./types').Repository} Repository
 */

export const stepsIds = {
  RULES: 'rules',
  SELECT_PROXY: 'select_proxy',
  SELECT_HOSTED: 'select_hosted',
  PROTECT: 'protect',
};

const rules = {
  id: stepsIds.RULES,
  index: 0,
  name: 'Protection Rules Selection',
};
const selectProxy = {
  id: stepsIds.SELECT_PROXY,
  index: 1,
  name: 'Enabling Protection (open source components)',
};
const selectHosted = {
  id: stepsIds.SELECT_HOSTED,
  index: 2,
  name: 'Enabling Protection (proprietary components)',
};
const protect = {
  id: stepsIds.PROTECT,
  index: 3,
  name: 'Review',
};

export const steps = [rules, selectProxy, selectHosted, protect];
export const stepsById = { rules, selectProxy, selectHosted, protect };

export const next = (step) => steps[step.index + 1];
export const prev = (step) => steps[step.index - 1];

export const ALLOWED_REPOSITORY_TYPES = ['proxy', 'hosted'];

/**
 * Groups repositories by format and sorts them by:
 * 1. Supported formats first
 * 2. Number of repositories in the group
 * 3. Format name
 *
 * The function also adds an additional 'other' column if there are more than 3 groups. Everything that doesn't fit
 * into the first 3 groups will overflow into the 'other' group, limiting the maximum number of columns to 4.
 *
 * @param {Repository[]} repositories list of repositories to group
 * @param {string[]} supportedFormats list of supported repository formats
 * @returns {{format: string, repositories: Repository[]}[]}
 */
export const groupAndSortByFormat = (repositories, supportedFormats) => {
  /** The maximium number of groups to create. All other repositories will be added to an additional 'other' column */
  const maximumColumnsCount = 3;

  const sortAndGroupsRepositories =
    /** @type {(list: readonly Repository[]) => {format: string, repositories: Repository[]}[]} */
    (pipe(
      groupBy(prop('format')),
      toPairs,
      map(([format, repositories]) => ({ format, repositories })),
      sortWith([
        descend((repository) => includes(repository.format, supportedFormats)),
        descend(pipe(prop('repositories'), length)),
        ascend(prop('format')),
      ])
    ));

  const topSupportedFormats = take(
    maximumColumnsCount,
    pipe(
      filter((item) => supportedFormats.includes(item.format)),
      sortAndGroupsRepositories
    )(repositories)
  );

  const otherFormats = pipe(
    filter((repository) => !topSupportedFormats.some((group) => group.format === repository.format)),
    sortAndGroupsRepositories
  )(repositories);

  const topFormats = take(maximumColumnsCount - topSupportedFormats.length, otherFormats);

  const overflowRepositories = /** @type {Repository[]} */ (pipe(
    drop(maximumColumnsCount - topSupportedFormats.length),
    map(prop('repositories')),
    flatten
  )(otherFormats));

  // use the format of the first repository in the overflow list as the format for the overflow column or 'other' if
  // the list contains repositories with different formats
  const getOverflowFormat = () =>
    overflowRepositories.every((item) => item.format === overflowRepositories[0].format)
      ? overflowRepositories[0].format
      : 'other';

  const overflowColumn =
    overflowRepositories.length > 0 ? [{ format: getOverflowFormat(), repositories: overflowRepositories }] : [];

  return [...topSupportedFormats, ...topFormats, ...overflowColumn];
};

/**
 * @param {Repository[]} repositoriesList
 * @param {{id: string, key: string, value: string}[]} updateRepositories
 */
export const updateRepositories = (repositoriesList, updateRepositories) => {
  return repositoriesList.map((repository) => {
    const selectedRepo = updateRepositories.find(({ id }) => id === repository.id);
    if (selectedRepo) {
      const { key, value } = selectedRepo;
      return {
        ...repository,
        [key]: value,
      };
    }
    return repository;
  });
};

export const NAMESPACE_CONFUSION_PROTECTION_URL =
  'http://links.sonatype.com/products/nxiq/doc/preventing-namespace-confusion';

export const MALICIOUS_COMPONENTS_PROTECTION_URL =
  'http://links.sonatype.com/nexus-firewall/protection-from-pending-and-suspicious-components';

/**
 * @param {Boolean} supplyChainAttacksProtectionEnabled
 * @param {Boolean} namespaceConfusionProtectionEnabled
 * @returns {String}
 */
const getProxyPageTitle = (supplyChainAttacksProtectionEnabled) =>
  !supplyChainAttacksProtectionEnabled
    ? 'You have not enabled recommended protection'
    : 'Enable protection from malicious components';

/**
 * @param {Boolean} supplyChainAttacksProtectionEnabled
 * @param {Boolean} namespaceConfusionProtectionEnabled
 * @returns {String}
 */
const getProxyPageSubtitle = (supplyChainAttacksProtectionEnabled, namespaceConfusionProtectionEnabled) =>
  cond([
    [
      equals([true, true]),
      always(
        <>
          The selected proxy repositories will have{' '}
          <NxTextLink href={MALICIOUS_COMPONENTS_PROTECTION_URL} external>
            supply chain attacks protection
          </NxTextLink>{' '}
          and{' '}
          <NxTextLink href={NAMESPACE_CONFUSION_PROTECTION_URL} external>
            namespace confusion protection
          </NxTextLink>{' '}
          enabled.
        </>
      ),
    ],
    [
      equals([true, false]),
      always(
        <>
          The selected proxy repositories will have{' '}
          <NxTextLink href={MALICIOUS_COMPONENTS_PROTECTION_URL} external>
            supply chain attacks protection
          </NxTextLink>{' '}
          enabled. <br />
          You can also enable{' '}
          <NxTextLink href={NAMESPACE_CONFUSION_PROTECTION_URL} external>
            namespace confusion protection
          </NxTextLink>{' '}
          by going back to the previous step.
        </>
      ),
    ],
    [
      equals([false, true]),
      always(
        <>
          The selected proxy repositories will have{' '}
          <NxTextLink href={NAMESPACE_CONFUSION_PROTECTION_URL} external>
            namespace confusion protection
          </NxTextLink>{' '}
          enabled. <br />
          You can also enable{' '}
          <NxTextLink href={MALICIOUS_COMPONENTS_PROTECTION_URL} external>
            supply chain attacks protection
          </NxTextLink>{' '}
          by going back to the previous step.
        </>
      ),
    ],
    [
      equals([false, false]),
      always(
        <>
          The selected proxy repositories will not have{' '}
          <NxTextLink href={MALICIOUS_COMPONENTS_PROTECTION_URL} external>
            supply chain attacks protection
          </NxTextLink>{' '}
          or{' '}
          <NxTextLink href={NAMESPACE_CONFUSION_PROTECTION_URL} external>
            namespace confusion protection
          </NxTextLink>{' '}
          enabled. <br />
          You can enable protection by going back to the previous step.
        </>
      ),
    ],
  ])([supplyChainAttacksProtectionEnabled, namespaceConfusionProtectionEnabled]);

const getHostedPageTitle = () => 'Protect your internal components from namespace attacks';

/**
 * @param {Boolean} namespaceConfusionProtectionEnabled
 * @returns {React.ReactNode}
 */
const getHostedPageSubtitle = (_, namespaceConfusionProtectionEnabled) =>
  namespaceConfusionProtectionEnabled ? (
    <>
      The component names from the selected hosted repositories will be used to protect against{' '}
      <NxTextLink href={NAMESPACE_CONFUSION_PROTECTION_URL} external>
        namespace confusion
      </NxTextLink>{' '}
      attacks against your proxy repositories. <br />
      <br />
      This capability should only be turned on for repositories with proprietary components only. Enabling it on hosted
      repositories containing open-source components <b>will</b> cause those namespaces to be quarantined.
    </>
  ) : (
    <>
      The component names from the selected hosted repositories will not be used to protect against{' '}
      <NxTextLink href={NAMESPACE_CONFUSION_PROTECTION_URL} external>
        namespace confusion
      </NxTextLink>{' '}
      attacks against your proxy repositories. You can enable namespace confusion protection by going back to the
      previous step. <br />
      <br />
      This capability should only be turned on for repositories with proprietary components only. Enabling it on hosted
      repositories containing open-source components <b>will</b> cause those namespaces to be quarantined.
    </>
  );

export const getRepoPageTitleByFormat = {
  proxy: {
    title: getProxyPageTitle,
    subtitle: getProxyPageSubtitle,
  },
  hosted: {
    title: getHostedPageTitle,
    subtitle: getHostedPageSubtitle,
  },
};
