/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen } from 'TestRoot/SpecUtil';
import * as repositoriesSelectors from 'MainRoot/OrgsAndPolicies/repositories/repositoriesConfigurationSelectors';
import { actions as repositoriesActions } from 'MainRoot/OrgsAndPolicies/repositories/repositoriesConfigurationSlice';
import axios from 'axios';
import RepositoriesConfigurationTile from 'MainRoot/OrgsAndPolicies/repositories/RepositoriesConfigurationTile';
import { getRepositoriesUrl, getRepositoryInfoUrl } from 'MainRoot/util/CLMLocation';
import { fireEvent } from '@testing-library/react';

describe('RepositoriesConfigurationTile', () => {
  let renderComponent,
    repositoriesSlice,
    mockAxiosCalls,
    selectIsLoadingSpy,
    selectRepositoriesLoadErrorSpy,
    selectRepositoriesDeleteErrorSpy,
    openDeleteModalSpy,
    selectRepositoriesSpy,
    selectDeleteModalSpy,
    selectDeleteModalInfoSpy,
    deleteRepositorySpy,
    loadRepositoriesSpy;

  repositoriesSlice = {
    repositories: [
      {
        oldestEvalTimestamp: null,
        managerInstanceId: 'managerInstanceIdA',
        repository: {
          id: 'repositoryA',
          repositoryManagerId: 'repositoryManagerIdA',
          publicId: 'repositoryNameA',
          enabled: true,
          quarantineEnabled: true,
          format: 'maven2',
        },
      },
      {
        oldestEvalTimestamp: null,
        managerInstanceId: 'managerInstanceIdB',
        repository: {
          id: 'repositoryB',
          repositoryManagerId: 'repositoryManagerIdB',
          publicId: 'repositoryNameB',
          enabled: false,
          quarantineEnabled: true,
          format: 'maven2',
        },
      },
    ],
    loading: false,
    loadError: null,
    deleteError: null,
    showDeleteModal: false,
    submitMaskState: null,
    deleteModalInfo: {
      id: null,
      publicId: null,
    },
  };

  beforeEach(() => {
    mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);

    selectRepositoriesSpy = spyOn(repositoriesSelectors, 'selectRepositories').and.callThrough();
    selectRepositoriesLoadErrorSpy = spyOn(repositoriesSelectors, 'selectRepositoriesLoadError').and.callThrough();
    selectIsLoadingSpy = spyOn(repositoriesSelectors, 'selectRepositoriesLoading').and.callThrough();
    selectDeleteModalSpy = spyOn(repositoriesSelectors, 'selectDeleteModal').and.callThrough();
    selectRepositoriesDeleteErrorSpy = spyOn(repositoriesSelectors, 'selectRepositoriesDeleteError').and.callThrough();
    selectDeleteModalInfoSpy = spyOn(repositoriesSelectors, 'selectDeleteModalInfo').and.callThrough();

    deleteRepositorySpy = spyOn(repositoriesActions, 'deleteRepository').and.callThrough();
    loadRepositoriesSpy = spyOn(repositoriesActions, 'loadRepositories').and.callThrough();
    openDeleteModalSpy = spyOn(repositoriesActions, 'openDeleteModal').and.callThrough();

    mockAxiosCalls({
      get: {
        [getRepositoriesUrl()]: Promise.resolve({ data: repositoriesSlice }),
      },
      del: {
        [getRepositoryInfoUrl('repositoryA')]: Promise.resolve(),
      },
    });

    renderComponent = () => render(<RepositoriesConfigurationTile />);
  });

  describe('when data are being loaded', () => {
    beforeEach(() => {
      selectIsLoadingSpy.and.returnValue(true);
    });

    it('renders loading message', () => {
      renderComponent();

      expect(screen.queryByText('Loading…')).toBeInTheDocument();
    });
  });

  describe('when the page has a loading error', () => {
    beforeEach(() => {
      selectIsLoadingSpy.and.returnValue(false);
      selectRepositoriesLoadErrorSpy.and.returnValue('Test loading error');
    });

    it('renders error section', () => {
      renderComponent();

      const retryButton = screen.queryByText('Retry');
      expect(screen.getByText('An error occurred loading data. Test loading error')).toBeVisible();
      fireEvent.click(retryButton);
      expect(loadRepositoriesSpy).toHaveBeenCalled();
    });
  });

  describe('when there are no repositories ', () => {
    beforeEach(() => {
      selectRepositoriesSpy.and.returnValue([]);
      selectIsLoadingSpy.and.returnValue(false);
    });

    it('renders default empty message', () => {
      renderComponent();

      expect(screen.queryByText('Loading…')).not.toBeInTheDocument();
      expect(screen.getByText('There are no repositories registered with the server.')).toBeInTheDocument();
    });
  });

  describe('when repositories exist', () => {
    beforeEach(() => {
      selectRepositoriesSpy.and.returnValue(repositoriesSlice.repositories);
      selectIsLoadingSpy.and.returnValue(false);
    });

    it('renders page with all elements on the table', () => {
      renderComponent();

      const repositories = [
        {
          publicId: screen.getByText('repositoryNameA'),
          managerInstanceId: screen.getByText('managerInstanceIdA'),
          enabled: screen.getByText('Enabled'),
        },
        {
          publicId: screen.getByText('repositoryNameB'),
          managerInstanceId: screen.getByText('managerInstanceIdB'),
          enabled: screen.getByText('Disabled'),
        },
      ];

      expect(repositories[0].publicId).toBeVisible();
      expect(repositories[0].managerInstanceId).toBeVisible();
      expect(repositories[0].enabled).toBeVisible();

      expect(repositories[1].publicId).toBeVisible();
      expect(repositories[1].managerInstanceId).toBeVisible();
      expect(repositories[1].enabled).toBeVisible();
    });

    describe('Delete modal', () => {
      it('renders delete modal warning for corresponding repository', () => {
        const REPOSITORY_A_WARNING =
          'Are you sure you want to remove the Repository with ID "repositoryNameA"? This action is not reversible.';
        renderComponent();
        const deleteIcons = screen.queryAllByTestId('repository-delete-button');

        expect(deleteIcons).toHaveSize(2);
        fireEvent.click(deleteIcons[0]);

        const deleteModal = screen.getByTestId('delete-modal');

        expect(openDeleteModalSpy).toHaveBeenCalled();
        expect(deleteModal).toBeVisible();
        expect(screen.getByText(REPOSITORY_A_WARNING)).toBeInTheDocument();
      });
    });
  });

  describe('handles deleting error', () => {
    beforeEach(() => {
      selectDeleteModalSpy.and.returnValue(true);
      selectRepositoriesDeleteErrorSpy.and.returnValue('Test deleting error');
    });

    it('shows deleting error section', () => {
      renderComponent();
      const retryButton = screen.queryByText('Retry');

      expect(screen.getByText(/An error occurred saving data. Test deleting error/i)).toBeInTheDocument();
      fireEvent.click(retryButton);
      expect(deleteRepositorySpy).toHaveBeenCalled();
    });
  });

  describe('deleting repository', () => {
    beforeEach(() => {
      selectDeleteModalSpy.and.returnValue(true);
      selectDeleteModalInfoSpy.and.returnValue({ id: 'repositoryA', publicId: 'repositoryNameA' });
    });

    it('deletes repository from the table', () => {
      renderComponent();

      const deleteButton = screen.getByRole('button', { name: 'Continue' });
      fireEvent.click(deleteButton);

      expect(deleteRepositorySpy).toHaveBeenCalledTimes(1);
    });
  });
});
