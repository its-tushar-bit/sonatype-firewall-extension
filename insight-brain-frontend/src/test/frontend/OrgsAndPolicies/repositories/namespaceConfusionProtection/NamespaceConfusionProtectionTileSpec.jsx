/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen, axiosMockAdapter, queryByTextWithin } from 'TestRoot/SpecUtil';
import { fireEvent, waitFor } from '@testing-library/react';
import NamespaceConfusionProtectionTile from 'MainRoot/OrgsAndPolicies/repositories/namespaceConfusionProtectionTile/NamespaceConfusionProtectionTile';
import { getRepositoryComponentNameUrl } from 'MainRoot/util/CLMLocation';

describe('NamespaceConfusionProtectionTile', () => {
  let renderComponent, mock;
  const firstTableRowSelector = '#iq-proprietary-table-body .nx-table-row:nth-child(1)';

  let repositoryComponentNameUrlRequestBody = {
    page: 1,
    pageSize: 6,
    searchFilters: [],
    sortFields: [{ sortableField: 'PROPRIETARY_COMPONENT_NAMESPACE_OR_NAME', asc: true, sortPriority: 1 }],
  };

  let sucessfullRepositoryComponentNameUrlResponse = {
    proprietaryComponentNamePatterns: [
      {
        id: 'eb23d7dab5004c7496ba9195e5a4b862',
        format: 'maven',
        namespacePattern: 'Test',
        namePattern: null,
        repositoryManagerInstanceId: '9E111629-6B9EDCBA-B5989887-132718F9-8C354DFA',
        repositoryPublicId: 'maven-releases',
      },
      {
        id: '555fdab424144e7ebb2cf21428d8cc77',
        format: 'maven',
        namespacePattern: 'super.legal',
        namePattern: null,
        repositoryManagerInstanceId: '9E111629-6B9EDCBA-B5989887-132718F9-8C354DFB',
        repositoryPublicId: 'luis-maven-hosted',
      },
      {
        id: '8cdad19f87ef44d99915bada6bc59ec3',
        format: 'npm',
        namespacePattern: null,
        namePattern: 'express',
        repositoryManagerInstanceId: '9E111629-6B9EDCBA-B5989887-132718F9-8C354DFC',
        repositoryPublicId: 'luis-npm',
      },
    ],
    hasNextPage: false,
  };

  let repositoryComponentsDetails = {
    componentNamePatterns: [...sucessfullRepositoryComponentNameUrlResponse.proprietaryComponentNamePatterns],
    loadingComponentNamePatterns: false,
    errorComponentsTable: null,
    namePatternsTableConfig: {
      page: 1,
      pageSize: 6,
      searchFilters: [],
      sortFields: [
        {
          sortableField: 'PROPRIETARY_COMPONENT_NAMESPACE_OR_NAME',
          dir: 'asc',
        },
      ],
    },
    searchFiltersValues: {
      PROPRIETARY_COMPONENT_NAMESPACE_OR_NAME: '',
    },
  };

  beforeAll(() => {
    mock = axiosMockAdapter();
  });

  beforeEach(() => {
    mock
      .onPost(getRepositoryComponentNameUrl(), repositoryComponentNameUrlRequestBody)
      .reply(200, sucessfullRepositoryComponentNameUrlResponse);

    renderComponent = () => render(<NamespaceConfusionProtectionTile />);
  });

  describe('when data are being loaded', () => {
    it('renders loading spinner', () => {
      renderComponent();

      expect(screen.getByText('Loading…')).toBeVisible();
    });
  });

  describe('when the page has a loading error', () => {
    beforeEach(() => {
      mock.onPost(getRepositoryComponentNameUrl(), repositoryComponentNameUrlRequestBody).reply(500);
    });

    it('renders error section', async () => {
      renderComponent();
      await waitFor(() => expect(screen.getByText(/An error occurred loading data/)).toBeVisible());
      await waitFor(() => expect(screen.queryByText('Retry')).toBeVisible());
    });
  });

  describe('when there are no components', () => {
    beforeEach(() => {
      mock
        .onPost(getRepositoryComponentNameUrl(), repositoryComponentNameUrlRequestBody)
        .reply(200, { ...repositoryComponentsDetails, proprietaryComponentNamePatterns: [] });
    });

    it('renders default empty message', async () => {
      renderComponent();

      expect(screen.queryByText('Loading…')).toBeVisible();
      await waitFor(() => expect(screen.getByText('No results')).toBeVisible());
      expect(screen.queryByText('Loading…')).not.toBeInTheDocument();
    });
  });

  describe('when components exist', () => {
    it('renders table with all components', async () => {
      renderComponent();
      const { componentNamePatterns } = repositoryComponentsDetails;
      expect(screen.queryByText('Loading…')).toBeVisible();
      await waitFor(() => expect(screen.getByText(componentNamePatterns[0].repositoryManagerInstanceId)).toBeVisible());
      expect(
        screen.getByText(componentNamePatterns[0].namespacePattern || componentNamePatterns[0].namePattern)
      ).toBeVisible();
      expect(screen.getByText(componentNamePatterns[0].repositoryManagerInstanceId)).toBeVisible();
      expect(screen.getByText(componentNamePatterns[0].repositoryPublicId)).toBeVisible();
      expect(
        screen.getByText(componentNamePatterns[1].namespacePattern || componentNamePatterns[1].namePattern)
      ).toBeVisible();
      expect(screen.getByText(componentNamePatterns[1].repositoryManagerInstanceId)).toBeVisible();
      expect(screen.getByText(componentNamePatterns[1].repositoryPublicId)).toBeVisible();
      expect(
        screen.getByText(componentNamePatterns[2].namespacePattern || componentNamePatterns[2].namePattern)
      ).toBeVisible();
      expect(screen.getByText(componentNamePatterns[2].repositoryManagerInstanceId)).toBeVisible();
      expect(screen.getByText(componentNamePatterns[2].repositoryPublicId)).toBeVisible();
    });

    it('filters components by the name or policy', async () => {
      mock
        .onPost(getRepositoryComponentNameUrl(), {
          page: 1,
          pageSize: 6,
          searchFilters: [{ filterableField: 'PROPRIETARY_COMPONENT_NAMESPACE_OR_NAME', value: 'su' }],
          sortFields: [{ sortableField: 'PROPRIETARY_COMPONENT_NAMESPACE_OR_NAME', asc: true, sortPriority: 1 }],
        })
        .reply(200, {
          proprietaryComponentNamePatterns: [
            {
              id: '555fdab424144e7ebb2cf21428d8cc77',
              format: 'maven',
              namespacePattern: 'super.legal',
              namePattern: null,
              repositoryManagerInstanceId: '9E111629-6B9EDCBA-B5989887-132718F9-8C354DFB',
              repositoryPublicId: 'luis-maven-hosted',
            },
          ],
          hasNextPage: false,
        });

      renderComponent();

      const searchByNameInput = screen.getByPlaceholderText('Filter');

      fireEvent.change(searchByNameInput, { target: { value: 'su' } });

      const { componentNamePatterns } = repositoryComponentsDetails;
      await waitFor(() => {
        expect(screen.getByText(componentNamePatterns[0].repositoryManagerInstanceId)).toBeVisible();
      });
      expect(
        screen.getByText(componentNamePatterns[0].namespacePattern || componentNamePatterns[0].namePattern)
      ).toBeVisible();
      expect(screen.getByText(componentNamePatterns[0].repositoryManagerInstanceId)).toBeVisible();
      expect(screen.getByText(componentNamePatterns[0].repositoryPublicId)).toBeVisible();
    });

    it('sorts components by the selected field', async () => {
      const { proprietaryComponentNamePatterns } = sucessfullRepositoryComponentNameUrlResponse;

      mock
        .onPost(getRepositoryComponentNameUrl(), {
          page: 1,
          pageSize: 6,
          searchFilters: [],
          sortFields: [{ sortableField: 'PROPRIETARY_COMPONENT_NAMESPACE_OR_NAME', asc: false, sortPriority: 1 }],
        })
        .reply(200, {
          proprietaryComponentNamePatterns: [
            proprietaryComponentNamePatterns[2],
            proprietaryComponentNamePatterns[1],
            proprietaryComponentNamePatterns[0],
          ],
          hasNextPage: false,
        });

      mock
        .onPost(getRepositoryComponentNameUrl(), {
          page: 1,
          pageSize: 6,
          searchFilters: [],
          sortFields: [
            { sortableField: 'REPOSITORY_MANAGER_INSTANCE_ID', asc: true, sortPriority: 1 },
            { sortableField: 'PROPRIETARY_COMPONENT_NAMESPACE_OR_NAME', asc: false, sortPriority: 2 },
          ],
        })
        .reply(200, {
          proprietaryComponentNamePatterns: [
            proprietaryComponentNamePatterns[0],
            proprietaryComponentNamePatterns[1],
            proprietaryComponentNamePatterns[2],
          ],
          hasNextPage: false,
        });

      mock
        .onPost(getRepositoryComponentNameUrl(), {
          page: 1,
          pageSize: 6,
          searchFilters: [],
          sortFields: [
            { sortableField: 'REPOSITORY_PUBLIC_ID', asc: true, sortPriority: 1 },
            { sortableField: 'REPOSITORY_MANAGER_INSTANCE_ID', asc: true, sortPriority: 2 },
            { sortableField: 'PROPRIETARY_COMPONENT_NAMESPACE_OR_NAME', asc: false, sortPriority: 3 },
          ],
        })
        .reply(200, {
          proprietaryComponentNamePatterns: [
            proprietaryComponentNamePatterns[1],
            proprietaryComponentNamePatterns[2],
            proprietaryComponentNamePatterns[0],
          ],
          hasNextPage: false,
        });

      renderComponent();

      const assertFirstRowColsValues = (proprietaryComponentNamePatternsIndex) => {
        expect(
          queryByTextWithin(
            proprietaryComponentNamePatterns[proprietaryComponentNamePatternsIndex].namespacePattern ||
              proprietaryComponentNamePatterns[proprietaryComponentNamePatternsIndex].namePattern,
            firstTableRowSelector
          ).first
        ).toBeVisible();
        expect(
          queryByTextWithin(
            proprietaryComponentNamePatterns[proprietaryComponentNamePatternsIndex].repositoryManagerInstanceId,
            firstTableRowSelector
          ).first
        ).toBeVisible();
        return expect(
          queryByTextWithin(
            proprietaryComponentNamePatterns[proprietaryComponentNamePatternsIndex].repositoryPublicId,
            firstTableRowSelector
          ).first
        ).toBeVisible();
      };

      const sortByNamespacesButton = screen.getByRole('button', { name: /Component Namespaces ascending/i });
      const sortByManagerButton = screen.getByRole('button', { name: /Repository Manager unsorted/i });
      const sortByRepositoryButton = screen.getByRole('button', { name: /Repository unsorted/i });
      await waitFor(() => assertFirstRowColsValues(0));

      fireEvent.click(sortByNamespacesButton);
      expect(screen.getByRole('button', { name: /Component Namespaces descending/i })).toBeVisible();
      await waitFor(() => assertFirstRowColsValues(2));

      fireEvent.click(sortByManagerButton);
      expect(screen.getByRole('button', { name: /Repository Manager ascending/i })).toBeVisible();
      await waitFor(() => assertFirstRowColsValues(0));

      fireEvent.click(sortByRepositoryButton);
      expect(screen.getByRole('button', { name: /Repository ascending/i })).toBeVisible();
      await waitFor(() => assertFirstRowColsValues(1));
    });

    it('renders indeterminate pagination controls if there is more than one page of results', async () => {
      const customProprietaryComponentNamePatterns = [
        ...sucessfullRepositoryComponentNameUrlResponse.proprietaryComponentNamePatterns,
        {
          id: 'eb23d7dab5004c7496ba9195e5a4b863',
          format: 'maven',
          namespacePattern: 'neo4j',
          namePattern: null,
          repositoryManagerInstanceId: '9E111629-6B9EDCBA-B5989887-132718F9-8C354DFD',
          repositoryPublicId: 'maven-releases',
        },
        {
          id: '555fdab424142e7ebb2cf21428d8cc79',
          format: 'maven',
          namespacePattern: 'selenium',
          namePattern: null,
          repositoryManagerInstanceId: '9E111629-6B9EDCBA-B5989887-132718F9-8C354DFE',
          repositoryPublicId: 'luis-maven-hosted',
        },
        {
          id: '8cdad19f87ef44d99915bada6bc59ec7',
          format: 'npm',
          namespacePattern: null,
          namePattern: 'moment',
          repositoryManagerInstanceId: '9E111629-6B9EDCBA-B5989887-132718F9-8C354DFF',
          repositoryPublicId: 'luis-npm',
        },
        {
          id: 'eb23d7dab5004c7496ba9195e5a4b864',
          format: 'maven',
          namespacePattern: 'ant',
          namePattern: null,
          repositoryManagerInstanceId: '9E111629-6B9EDCBA-B5989887-132718F9-9C354DFD',
          repositoryPublicId: 'maven-releases',
        },
        {
          id: '555fdab424144e7ebb2cf21428d8cc78',
          format: 'maven',
          namespacePattern: 'selenide',
          namePattern: null,
          repositoryManagerInstanceId: '9E111629-6B9EDCBA-B5989887-132718F9-9C354DFE',
          repositoryPublicId: 'luis-maven-hosted',
        },
        {
          id: '8cdad19f87ef44d99915bada6bc59ec5',
          format: 'npm',
          namespacePattern: null,
          namePattern: 'polka',
          repositoryManagerInstanceId: '9E111629-6B9EDCBA-B5989887-132718F9-9C354DFF',
          repositoryPublicId: 'luis-npm',
        },
        {
          id: '8cdad19f87ef44d99915bada6bc59ec6',
          format: 'npm',
          namespacePattern: null,
          namePattern: 'lodash',
          repositoryManagerInstanceId: '9E111622-6B9EDCBA-B5989887-132718F9-9C354DFF',
          repositoryPublicId: 'luis-npm',
        },
        {
          id: '8cdad19f87ef44d99915bada6bc59ec7',
          format: 'npm',
          namespacePattern: null,
          namePattern: 'react',
          repositoryManagerInstanceId: '9E121622-6B9EDCBA-B5989887-132718F9-9C354DFF',
          repositoryPublicId: 'luis-npm',
        },
        {
          id: '8cdad19f87ef44d99915bada6bc59ec9',
          format: 'npm',
          namespacePattern: null,
          namePattern: 'react-testing-library',
          repositoryManagerInstanceId: '9E321622-6B9EDCBA-B5989887-132718F9-9C354DFF',
          repositoryPublicId: 'luis-npm',
        },
        {
          id: '8cdad19f87ef44d99915bada6bc59ec4',
          format: 'npm',
          namespacePattern: null,
          namePattern: 'solid',
          repositoryManagerInstanceId: '9E321721-6B9EDCBA-B5989887-132718F9-9C354DFF',
          repositoryPublicId: 'luis-npm',
        },
      ].sort((a, b) => {
        const aName = a.namespacePattern || a.namePattern;
        const bName = b.namespacePattern || b.namePattern;
        return aName.localeCompare(bName);
      });

      mock
        .onPost(getRepositoryComponentNameUrl(), {
          page: 1,
          pageSize: 6,
          searchFilters: [],
          sortFields: [{ sortableField: 'PROPRIETARY_COMPONENT_NAMESPACE_OR_NAME', asc: true, sortPriority: 1 }],
        })
        .reply(200, {
          proprietaryComponentNamePatterns: [
            customProprietaryComponentNamePatterns[0],
            customProprietaryComponentNamePatterns[1],
            customProprietaryComponentNamePatterns[2],
            customProprietaryComponentNamePatterns[3],
            customProprietaryComponentNamePatterns[4],
            customProprietaryComponentNamePatterns[5],
          ],
          hasNextPage: true,
        });

      mock
        .onPost(getRepositoryComponentNameUrl(), {
          page: 2,
          pageSize: 6,
          searchFilters: [],
          sortFields: [{ sortableField: 'PROPRIETARY_COMPONENT_NAMESPACE_OR_NAME', asc: true, sortPriority: 1 }],
        })
        .reply(200, {
          proprietaryComponentNamePatterns: [
            customProprietaryComponentNamePatterns[6],
            customProprietaryComponentNamePatterns[7],
            customProprietaryComponentNamePatterns[8],
            customProprietaryComponentNamePatterns[9],
            customProprietaryComponentNamePatterns[10],
            customProprietaryComponentNamePatterns[11],
          ],
          hasNextPage: true,
        });

      mock
        .onPost(getRepositoryComponentNameUrl(), {
          page: 3,
          pageSize: 6,
          searchFilters: [],
          sortFields: [{ sortableField: 'PROPRIETARY_COMPONENT_NAMESPACE_OR_NAME', asc: true, sortPriority: 1 }],
        })
        .reply(200, {
          proprietaryComponentNamePatterns: [customProprietaryComponentNamePatterns[12]],
          hasNextPage: false,
        });

      renderComponent();

      let pagination, prevButton, nextButton;

      const assertFirstRowColsValues = (proprietaryComponentNamePatternsIndex) => {
        return expect(
          queryByTextWithin(
            customProprietaryComponentNamePatterns[proprietaryComponentNamePatternsIndex].namespacePattern ||
              customProprietaryComponentNamePatterns[proprietaryComponentNamePatternsIndex].namePattern,
            firstTableRowSelector
          ).first
        ).toBeVisible();
      };

      await waitFor(() => assertFirstRowColsValues(0));
      pagination = screen.getByTestId('components-table-pagination');
      expect(pagination).toBeVisible();
      nextButton = document.querySelector('[aria-label="next page"]');
      prevButton = document.querySelector('[aria-label="previous page"]');
      expect(nextButton.classList.contains('hidden')).toBeFalse();
      expect(prevButton.classList.contains('hidden')).toBeTrue();

      fireEvent.click(nextButton);

      await waitFor(() => assertFirstRowColsValues(6));
      pagination = screen.getByTestId('components-table-pagination');
      expect(pagination).toBeVisible();
      nextButton = document.querySelector('[aria-label="next page"]');
      prevButton = document.querySelector('[aria-label="previous page"]');
      expect(nextButton.classList.contains('hidden')).toBeFalse();
      expect(prevButton.classList.contains('hidden')).toBeFalse();

      fireEvent.click(nextButton);

      await waitFor(() => assertFirstRowColsValues(12));
      pagination = screen.getByTestId('components-table-pagination');
      expect(pagination).toBeVisible();
      nextButton = document.querySelector('[aria-label="next page"]');
      prevButton = document.querySelector('[aria-label="previous page"]');
      expect(nextButton.classList.contains('hidden')).toBeTrue();
      expect(prevButton.classList.contains('hidden')).toBeFalse();
    });
  });
});
