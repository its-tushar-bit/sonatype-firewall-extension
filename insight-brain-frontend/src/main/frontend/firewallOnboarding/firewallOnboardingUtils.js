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
  reduce,
  assoc,
} from 'ramda';

export const stepsIds = { SELECT: 'select', PROTECT: 'protect' };
const select = {
  id: stepsIds.SELECT,
  index: 0,
  name: 'Select',
  title: 'Select proxy repositories',
  subTitle: 'Choose which proxy repositories you would like to apply your protection rules to.',
};
const protect = {
  id: stepsIds.PROTECT,
  index: 1,
  name: 'Protect',
  title: 'Inspect and complete onboarding',
};

export const steps = [select, protect];

export const next = (step) => steps[step.index + 1];
export const prev = (step) => steps[step.index - 1];

export const ALLOWED_REPOSITORY_TYPES = ['proxy']; // TODO CLM-25565 - for external release just add 'hosted' type to the array

export const groupRepositoriesByTypes = (repositories = []) => {
  if (!repositories) return reduce((obj, item) => assoc(item, [], obj), {}, ALLOWED_REPOSITORY_TYPES);

  const repositoriesByTypes = {};
  ALLOWED_REPOSITORY_TYPES.forEach((type) => {
    repositoriesByTypes[type] = repositories.filter((repo) => repo.repositoryType === type);
  });
  return repositoriesByTypes;
};

export const groupAndSortByFormat = (repositories, allowedFormats) => {
  // TODO CLM-24832
  // for unsupported formats add here logic to get them this way
  // const unsupportedRepositories = filter((item) => !allowedFormats.includes(item.format), repositories);
  const filteredRepositories = filter((item) => allowedFormats.includes(item.format), repositories);
  const groupedRepositories = groupBy(prop('format'), filteredRepositories);
  const sortedGroups = pipe(
    toPairs,
    map(([format, repositories]) => ({ format, repositories })),
    sortWith([descend(pipe(prop('repositories'), length)), ascend(prop('format'))])
  )(groupedRepositories);

  // TODO CLM-24832
  // add the unsupported to otherGroups.repositories array
  const topGroups = take(3, sortedGroups);
  const otherGroups = drop(3, sortedGroups);

  const result = otherGroups.length
    ? [...topGroups, { format: 'other', repositories: flatten(map(prop('repositories'), otherGroups)) }]
    : [...topGroups];

  return result;
};

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
