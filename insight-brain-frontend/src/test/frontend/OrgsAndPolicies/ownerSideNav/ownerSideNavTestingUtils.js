/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { within } from 'TestRoot/SpecUtil';
import { isNilOrEmpty } from 'MainRoot/util/jsUtil';
import { isNil } from 'ramda';

export const fakeRouterState = (url, params) => {
  if (!url) {
    return '#';
  }
  if (url.includes('management')) {
    if (url.includes('tree')) {
      return '#/management/view/tree';
    }
    if (url.includes('view')) {
      if (url.includes('repositories')) {
        return '#/management/view/repositories';
      } else if (url.includes('organization')) {
        const organizationId = params.organizationId;

        return organizationId ? `#/management/view/organization/${organizationId}` : '#/management/view/organization/';
      } else if (url.includes('application')) {
        const applicationPublicId = params.applicationPublicId;

        return applicationPublicId
          ? `#/management/view/application/${applicationPublicId}`
          : '#/management/view/application/';
      } else if (url.includes('repository_container')) {
        return `#/management/view/repository_container/${params.repositoryContainerId}`;
      } else if (url.includes('repository_manager')) {
        return `#/management/view/repository_manager/${params.repositoryManagerId}`;
      } else if (url.includes('repository')) {
        return `#/management/view/repository/${params.repositoryId}`;
      } else {
        return '#';
      }
    }
  }

  if (url.includes('scmOnboardingOrg')) {
    const organizationId = params.organizationId;
    return `#/onboarding/${organizationId}`;
  }

  return '#';
};

export const verifyOwnersMenuSection = (ownerMenu, children, ownerType, selectedApp = null) => {
  const triggerButton = within(ownerMenu).getByRole('button', {
    name: ownerType === 'organization' ? 'Organizations' : 'Applications',
  });
  expect(triggerButton).toBeVisible();

  if (isNilOrEmpty(children)) {
    expect(triggerButton).toHaveAttribute('aria-expanded', 'false');
    expect(triggerButton).toBeDisabled();
  } else {
    expect(triggerButton).toHaveAttribute('aria-expanded', 'true');
    expect(triggerButton).not.toBeDisabled();

    const menuItems = within(ownerMenu).getAllByRole('menuitem');
    expect(menuItems).toHaveLength(children.length);

    children.forEach((owner) => {
      const ownerItem = within(ownerMenu).getByRole('menuitem', {
        name: ownerType === 'organization' ? owner.name + ' (5)' : owner.name,
      });
      expect(ownerItem).toBeVisible();
      if (ownerType === 'organization') {
        expect(ownerItem).toHaveAttribute('href', `#/management/view/organization/${owner.id}`);
      } else if (ownerType === 'application') {
        expect(ownerItem).toHaveAttribute('href', `#/management/view/application/${owner.publicId}`);
        if (!isNil(selectedApp) && selectedApp.name === owner.name) {
          expect(ownerItem).toHaveClass('active');
        }
      }
    });
  }
};
