/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import * as enzymeUtils from '../../../enzymeUtils';
import ResultsTable, {
  RepositoryRow,
} from '../../../../../main/frontend/configuration/scmOnboarding/components/ResultsTable';
import {
  NxCheckbox,
  NxPagination,
  NxTable,
  NxTableBody,
  NxTableHead,
  NxTableRow,
  NxTooltip,
} from '@sonatype/react-shared-components';
import NxExternalLink from '../../../../../main/frontend/react/NxExternalLink';
import { createRepo } from './utils';

describe('ResultsTable', function () {
  let minimalPropsShallow, minimalPropsMounted, getShallowComponent, getMountedComponent, getShallowRepositoryRow;

  beforeEach(() => {
    minimalPropsShallow = {
      repositories: [],
    };
    minimalPropsMounted = {
      repositories: [],
      selectedRepositories: [],
    };

    getShallowComponent = enzymeUtils.getShallowComponent(ResultsTable, minimalPropsShallow);
    getMountedComponent = enzymeUtils.getMountedComponent(ResultsTable, minimalPropsMounted);
    getShallowRepositoryRow = enzymeUtils.getMountedComponent(RepositoryRow, minimalPropsMounted);
  });

  it('renders a table', () => {
    const component = getShallowComponent(),
      table = component.find(NxTable);

    expect(table).toExist();
  });

  describe('Renders RepositoryRow', () => {
    it('renders repositoryRow within table', () => {
      // given a repository
      const setSelectedRepositories = jasmine.createSpy('setSelectedRepositories');
      const repositories = [
        {
          httpCloneUrl: 'https://example.com/',
          namespace: 'namespace',
          project: 'project',
          description: 'description',
          defaultBranch: 'defaultBranch',
          isSelected: false,
          isImported: false,
        },
      ];
      const selectedRepositories = [];

      // when the results table is rendered
      const component = getShallowComponent({
          setSelectedRepositories,
          repositories,
          selectedRepositories,
        }),
        table = component.find(NxTable),
        tableBody = table.find(NxTableBody);

      // then it contains the repository
      expect(
        tableBody.containsMatchingElement(
          <RepositoryRow
            repo={repositories[0]}
            rowKey={'https://example.com/'}
            selectedRepositories={[]}
            setSelectedRepositories={setSelectedRepositories}
          />
        )
      ).toBeTruthy();
    });

    describe('Repository selection', () => {
      // given repositories with isSelected being true or false
      [true, false].forEach((checkboxData) => {
        const repository = {
          httpCloneUrl: 'https://example.com/',
          namespace: 'namespace',
          project: 'project',
          description: 'description',
          isSelected: checkboxData,
          isImported: false,
        };
        const repositories = [repository];
        const selectedRepositories = checkboxData ? [repository] : [];
        const setSelectedRepositories = jasmine.createSpy('setSelectedRepositories');

        it('creates row with checkbox set to: ' + checkboxData, () => {
          // when the component is rendered
          const component = getMountedComponent({
              repositories,
              selectedRepositories,
              setSelectedRepositories,
            }),
            table = component.find(NxTable),
            tableBody = table.find(NxTableBody),
            repositoryRow = table.find(RepositoryRow),
            checkbox = tableBody.find(NxCheckbox),
            namespaceCell = tableBody.find('.iq-scm-repository-namespace').first(),
            projectCell = tableBody.find('.iq-scm-repository-project').first(),
            descriptionCell = tableBody.find('.iq-scm-repository-description').first();

          // then properties are passed to RepositoryRow
          expect(repositoryRow.prop('rowKey')).toEqual('https://example.com/');
          expect(repositoryRow.prop('repo')).toEqual(repository);
          expect(repositoryRow.prop('setSelectedRepositories')).toEqual(setSelectedRepositories);
          expect(repositoryRow.prop('selectedRepositories')).toEqual(selectedRepositories);

          // and the checkbox matches expected values
          expect(checkbox.prop('checkboxId')).toEqual('https://example.com/');
          expect(checkbox.prop('isChecked')).toEqual(checkboxData);

          // and the cells to contain the expected text
          expect(namespaceCell.text()).toEqual('namespace');
          expect(projectCell.text().trim()).toEqual('project');
          expect(projectCell.find(NxExternalLink).prop('href')).toEqual('https://example.com/');
          expect(descriptionCell.text()).toEqual('description');
          expect(descriptionCell.find(NxTooltip).prop('title')).toEqual('description');
        });

        it('requests selection change from: ' + checkboxData, () => {
          const component = getMountedComponent({
              setSelectedRepositories,
              repositories,
              selectedRepositories,
            }),
            table = component.find(NxTable),
            tableBody = table.find(NxTableBody),
            checkbox = tableBody.find(NxCheckbox),
            checkboxInput = checkbox.find('input');

          // when the checkbox receives a change event
          checkboxInput.simulate('change');

          // then the redux action is triggered
          expect(setSelectedRepositories).toHaveBeenCalledWith(checkboxData ? [] : [repository]);
        });

        it('requests select all set to: ' + checkboxData, () => {
          // given an isAllChecked prop being true or false
          const isAllChecked = checkboxData;
          const setIsAllChecked = jasmine.createSpy('setIsAllChecked');
          const setSelectedRepositories = jasmine.createSpy('setSelectedRepositories');
          const props = {
            repositories,
            setIsAllChecked,
            isAllChecked,
            selectedRepositories,
            setSelectedRepositories,
          };

          // when the checkbox receives a change event
          const component = getMountedComponent(props),
            selectAllCheckbox = component.find('#iq-scmonboarding-select-all');
          selectAllCheckbox.simulate('change');

          // then the redux actions are triggered
          expect(setIsAllChecked).toHaveBeenCalledWith(!isAllChecked);
          expect(setSelectedRepositories).toHaveBeenCalledWith(isAllChecked ? [] : repositories);
        });
      });
    });
  });

  describe('Requests sorting', () => {
    ['asc', 'desc'].forEach((configuredDirection) => {
      ['namespace', 'project', 'description', 'defaultBranch'].forEach((selectedField) => {
        const expectedSortFields = {
          asc: {
            namespace: ['namespace', 'project', 'description', 'defaultBranch'],
            project: ['project', 'namespace', 'description', 'defaultBranch'],
            description: ['description', 'namespace', 'project', 'defaultBranch'],
            defaultBranch: ['defaultBranch', 'namespace', 'project', 'description'],
          },
          desc: {
            namespace: ['-namespace', 'project', 'description', 'defaultBranch'],
            project: ['-project', 'namespace', 'description', 'defaultBranch'],
            description: ['-description', 'namespace', 'project', 'defaultBranch'],
            defaultBranch: ['-defaultBranch', 'namespace', 'project', 'description'],
          },
        };

        it(`requests sort of the table ${configuredDirection} order of ${selectedField}`, () => {
          // given a sort configuration
          const sortConfiguration = {
            dir: configuredDirection,
            key: selectedField,
          };
          const setSortingParameters = jasmine.createSpy('setSortingParameters');

          const component = getMountedComponent({
              setSortingParameters,
              sortConfiguration,
            }),
            tableHead = component.find(NxTableHead),
            selectedHeader = tableHead.find(`#${selectedField}-header`).first();

          // when header is clicked
          selectedHeader.simulate('click');

          // then sort by description is requested
          const newDirection = configuredDirection === 'asc' ? 'desc' : 'asc';
          expect(setSortingParameters).toHaveBeenCalledWith(
            selectedField,
            expectedSortFields[newDirection][selectedField],
            newDirection
          );
        });
      });
    });
  });

  describe('pagination', () => {
    const REPO_COUNT = 40;
    const PAGE_SIZE = 15;
    const TEST_STEP_SIZE = 5;

    // given test repos sized 5 to 40
    const expectedPageCount = Array.from({ length: REPO_COUNT }).map((v, i) => Math.floor((i - 1) / PAGE_SIZE) + 1);
    for (let repoCount = TEST_STEP_SIZE; repoCount < REPO_COUNT; repoCount += TEST_STEP_SIZE) {
      const repositories = Array.from({ length: repoCount }).map((v, i) => createRepo(i));

      it(`displays ${expectedPageCount[repoCount]} pages for ${repoCount} repos`, () => {
        // when the component is rendered
        const component = getShallowComponent({ repositories }),
          pagination = component.find(NxPagination);

        // the rendered page count is correct
        expect(pagination.prop('pageCount')).toEqual(expectedPageCount[repoCount]);
      });
    }
  });

  describe('filters', () => {
    const repositories = ['aaaa', 'bbbb', 'aabb', 'BBBB', 'ABBB'].map((prefix) => createRepo(prefix));
    const setSelectedRepositories = jasmine.createSpy('setSelectedRepositories');
    const selectedRepositories = [];

    ['namespace', 'description', 'project'].forEach((filterName) => {
      it('filters repos by ' + filterName, () => {
        const component = getShallowComponent({
            repositories,
            selectedRepositories,
            setSelectedRepositories,
          }),
          filterInput = component.find(`#iq-scmonboarding-${filterName}-filter`);

        // when filter does not match any repos
        filterInput.simulate('change', 'doesntexist');

        // then no repository rows are generated
        expect(component.find(RepositoryRow).length).toBe(0);

        // when the filter matches exactly on repo
        filterInput.simulate('change', 'aaaa');

        // then only one repository row with the matching repo is generated
        expect(component.find(RepositoryRow).length).toBe(1);
        expect(component.find(RepositoryRow).prop('repo')).toEqual(repositories[0]);

        // when the filter matches multiple repos
        filterInput.simulate('change', 'aa');

        // then repository rows with the matching repos are generated
        expect(component.find(RepositoryRow).length).toBe(2);
        expect(component.find(RepositoryRow).first().prop('repo')).toEqual(repositories[0]);
        expect(component.find(RepositoryRow).last().prop('repo')).toEqual(repositories[2]);

        // when filter matches multiple repos using case-insensitive match
        filterInput.simulate('change', 'bb');

        // then every repo containing bb case-insensitively is shown
        expect(component.find(RepositoryRow).length).toBe(4);
        expect(component.find(RepositoryRow).map((row) => row.prop('repo').httpCloneUrl)).toEqual([
          'url-bbbb',
          'url-aabb',
          'url-BBBB',
          'url-ABBB',
        ]);
      });

      it('deselects filtered-out components when filtering by ' + filterName, () => {
        const component = getShallowComponent({
            repositories,
            selectedRepositories: repositories,
            setSelectedRepositories,
          }),
          filterInput = component.find(`#iq-scmonboarding-${filterName}-filter`);

        // when not filtering anything
        setSelectedRepositories.calls.reset();
        filterInput.simulate('change', '');

        // then everything remains selected
        expect(setSelectedRepositories).toHaveBeenCalledWith(repositories);

        // when applying a filter
        setSelectedRepositories.calls.reset();
        filterInput.simulate('change', 'aa');

        // then filtered-out repos are deselected
        expect(setSelectedRepositories).toHaveBeenCalledWith([repositories[0], repositories[2]]);

        // when using a filter that matches no repos
        setSelectedRepositories.calls.reset();
        filterInput.simulate('change', 'does not exist');

        // then everything is deselected
        expect(setSelectedRepositories).toHaveBeenCalledWith([]);

        // when filter matches multiple repos using case-insensitive match
        setSelectedRepositories.calls.reset();
        filterInput.simulate('change', 'bb');

        // then filtered-out repos are deselected (all except the first don't contain 'bb' or 'BB')
        expect(setSelectedRepositories).toHaveBeenCalledWith([
          repositories[1],
          repositories[2],
          repositories[3],
          repositories[4],
        ]);
      });
    });
  });

  describe('Repository row', () => {
    const repository = {
      httpCloneUrl: 'https://example.com/',
      namespace: 'namespace',
      project: 'project',
      description: 'description',
      defaultBranch: 'defaultBranch',
      isSelected: false,
      isImported: false,
    };

    it('Renders RepositoryRow fields', () => {
      const component = getShallowRepositoryRow({ rowKey: 'key', repo: repository });
      const row = component.find(NxTableRow),
        defaultBranch = row.find('.iq-scm-repository-default-branch').first(),
        description = row.find('.iq-scm-repository-description').first(),
        project = row.find('.iq-scm-repository-project').find(NxExternalLink).first(),
        namespace = row.find('.iq-scm-repository-namespace').first();

      expect(defaultBranch.text()).toEqual('defaultBranch');
      expect(description.text()).toEqual('description');
      expect(project.text().trim()).toEqual('project');
      expect(namespace.text()).toEqual('namespace');
    });
  });
});
