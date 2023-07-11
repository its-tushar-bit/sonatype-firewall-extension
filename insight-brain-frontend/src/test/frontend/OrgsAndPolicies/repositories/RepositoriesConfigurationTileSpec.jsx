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
import { fireEvent, within } from '@testing-library/react';
import { groupBy, prop } from 'ramda';

describe('RepositoriesConfigurationTile', () => {
  let renderComponent,
    repos,
    mockAxiosCalls,
    selectIsLoadingSpy,
    selectRepositoriesLoadErrorSpy,
    selectRepositoriesDeleteErrorSpy,
    selectEditRepositoryManagerNameErrorSpy,
    openDeleteModalSpy,
    openEditRepositoryManagerNameModalSpy,
    selectRepositoriesByManagerInstanceIdSpy,
    selectRepositoriesSpy,
    selectDeleteModalSpy,
    selectDeleteModalInfoSpy,
    selectShowEditRepositoryManagerNameModalSpy,
    selectEditRepositoryManagerNameModalInfoSpy,
    deleteRepositorySpy,
    loadRepositoriesSpy,
    editRepositoryManagerNameSpy;

  repos = [
    {
      oldestEvalTimestamp: null,
      managerInstanceId: 'managerInstanceIdA',
      managerName: 'managerNameA',
      repository: {
        id: 'repositoryA',
        repositoryManagerId: 'repositoryManagerIdA',
        publicId: 'repositoryNameA',
        auditEnabled: true,
        quarantineEnabled: true,
        format: 'maven',
        repositoryType: 'proxy',
      },
    },
    {
      oldestEvalTimestamp: null,
      managerInstanceId: 'managerInstanceIdB',
      repository: {
        id: 'repositoryB',
        repositoryManagerId: 'repositoryManagerIdB',
        publicId: 'repositoryNameB',
        auditEnabled: false,
        quarantineEnabled: false,
        namespaceConfusionProtectionEnabled: true,
        format: 'npm',
        repositoryType: 'hosted',
      },
    },
  ];

  beforeEach(() => {
    mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);
    selectRepositoriesByManagerInstanceIdSpy = spyOn(
      repositoriesSelectors,
      'selectRepositoriesByManagerInstanceId'
    ).and.callThrough();
    selectRepositoriesSpy = spyOn(repositoriesSelectors, 'selectRepositories').and.callThrough();
    selectRepositoriesLoadErrorSpy = spyOn(repositoriesSelectors, 'selectRepositoriesLoadError').and.callThrough();
    selectIsLoadingSpy = spyOn(repositoriesSelectors, 'selectRepositoriesLoading').and.callThrough();
    selectDeleteModalSpy = spyOn(repositoriesSelectors, 'selectDeleteModal').and.callThrough();
    selectRepositoriesDeleteErrorSpy = spyOn(repositoriesSelectors, 'selectRepositoriesDeleteError').and.callThrough();
    selectEditRepositoryManagerNameErrorSpy = spyOn(
      repositoriesSelectors,
      'selectEditRepositoryManagerNameError'
    ).and.callThrough();
    selectDeleteModalInfoSpy = spyOn(repositoriesSelectors, 'selectDeleteModalInfo').and.callThrough();
    selectShowEditRepositoryManagerNameModalSpy = spyOn(
      repositoriesSelectors,
      'selectShowEditRepositoryManagerNameModal'
    ).and.callThrough();
    selectEditRepositoryManagerNameModalInfoSpy = spyOn(
      repositoriesSelectors,
      'selectEditRepositoryManagerNameModalInfo'
    ).and.callThrough();

    deleteRepositorySpy = spyOn(repositoriesActions, 'deleteRepository').and.callThrough();
    loadRepositoriesSpy = spyOn(repositoriesActions, 'loadRepositories').and.callThrough();
    editRepositoryManagerNameSpy = spyOn(repositoriesActions, 'editRepositoryManagerName').and.callThrough();
    openDeleteModalSpy = spyOn(repositoriesActions, 'openDeleteModal').and.callThrough();
    openEditRepositoryManagerNameModalSpy = spyOn(
      repositoriesActions,
      'openEditRepositoryManagerNameModal'
    ).and.callThrough();

    mockAxiosCalls({
      get: {
        [getRepositoriesUrl()]: Promise.resolve({ data: repos }),
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
      selectRepositoriesByManagerInstanceIdSpy.and.returnValue(groupBy(prop('managerInstanceId'))(repos));
      selectIsLoadingSpy.and.returnValue(false);
    });

    it('renders page with all elements on the table', () => {
      renderComponent();

      const repositories = [
        {
          managerLabel: screen.getByText('managerNameA'),
          publicId: screen.getByText('repositoryNameA'),
          format: screen.getByText('maven'),
          repositoryType: screen.getByText('proxy'),
          enablement: screen.getByText('Audit, Quarantine'),
        },
        {
          managerLabel: screen.getByText('managerInstanceIdB'),
          publicId: screen.getByText('repositoryNameB'),
          format: screen.getByText('npm'),
          repositoryType: screen.getByText('hosted'),
          enablement: screen.getByText('Namespace Scanning'),
        },
      ];

      expect(repositories[0].managerLabel).toBeVisible();
      expect(repositories[0].publicId).toBeVisible();
      expect(repositories[0].publicId.parentElement).toHaveClassName('nx-text-link');
      expect(repositories[0].format).toBeVisible();
      expect(repositories[0].repositoryType).toBeVisible();
      expect(repositories[0].enablement).toBeVisible();

      expect(repositories[1].managerLabel).toBeVisible();
      expect(repositories[1].publicId).toBeVisible();
      expect(repositories[1].publicId.parentElement).not.toHaveClassName('nx-text-link');
      expect(repositories[1].format).toBeVisible();
      expect(repositories[1].repositoryType).toBeVisible();
      expect(repositories[1].enablement).toBeVisible();
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

    describe('Edit repository manager name modal', () => {
      it('renders edit repository manager instance id and name for corresponding repository', async () => {
        renderComponent();
        const row = screen.queryByText('managerNameA').closest('tr');
        expect(row).toBeVisible();
        const button = within(row).getByRole('button');
        expect(button).toBeVisible();
        fireEvent.click(button);

        const editRepositoryManagerNameModal = screen.getByTestId('edit-repository-manager-name-modal');
        expect(editRepositoryManagerNameModal).toBeVisible();
        expect(openEditRepositoryManagerNameModalSpy).toHaveBeenCalled();

        expect(screen.getByText('managerInstanceIdA')).toBeVisible();
        expect(screen.getByText('managerNameA')).toBeVisible();
        expect(
          screen.getByText('Any changes made will apply to all repositories for this repository manager.')
        ).toBeInTheDocument();
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

  describe('handles edit repository manager name error', () => {
    beforeEach(() => {
      selectShowEditRepositoryManagerNameModalSpy.and.returnValue(true);
      selectEditRepositoryManagerNameErrorSpy.and.returnValue('Test edit repository manager name error');
    });

    it('shows edit repository manager name error section', () => {
      renderComponent();
      const retryButton = screen.queryByText('Retry');

      expect(
        screen.getByText(/An error occurred saving data. Test edit repository manager name error/i)
      ).toBeInTheDocument();
      fireEvent.click(retryButton);
      expect(editRepositoryManagerNameSpy).toHaveBeenCalled();
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

  describe('editing a repository manager name', () => {
    beforeEach(() => {
      selectShowEditRepositoryManagerNameModalSpy.and.returnValue(true);
      selectEditRepositoryManagerNameModalInfoSpy.and.returnValue({
        managerInstanceId: 'someManagerInstanceId',
        managerName: 'someManagerName',
      });
    });

    it('validates empty input', () => {
      renderComponent();

      const editRepositoryManagerNameModal = screen.getByTestId('edit-repository-manager-name-modal');
      const editRepositoryManagerNameTextBox = within(editRepositoryManagerNameModal).getByRole('textbox');
      fireEvent.change(editRepositoryManagerNameTextBox, { target: { value: '' } });

      expect(screen.getByText('Must be non-empty')).toBeVisible();
    });

    it('edits a repository manager name', () => {
      renderComponent();

      const editRepositoryManagerNameModal = screen.getByTestId('edit-repository-manager-name-modal');
      const editRepositoryManagerNameButton = within(editRepositoryManagerNameModal).getByRole('button', {
        name: 'Update',
      });
      fireEvent.click(editRepositoryManagerNameButton);

      expect(editRepositoryManagerNameSpy).toHaveBeenCalledTimes(1);
    });
  });
});
