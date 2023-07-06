/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
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
} from 'ramda';

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
  name: 'Enable rules',
  title: 'Select your protection rules',
};
const selectProxy = {
  id: stepsIds.SELECT_PROXY,
  index: 1,
  name: 'Select',
  title: 'Select proxy repositories',
};
const selectHosted = {
  id: stepsIds.SELECT_HOSTED,
  index: 2,
  name: 'Select',
  title: 'Select hosted repositories',
};
const protect = {
  id: stepsIds.PROTECT,
  index: 3,
  name: 'Protect',
  title: 'Inspect and complete onboarding',
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
