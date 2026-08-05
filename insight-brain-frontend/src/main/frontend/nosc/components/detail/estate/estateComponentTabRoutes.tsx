/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { EstateComponentOverviewTab } from './EstateComponentOverviewTab';
import { EstateComponentLegalTab } from './EstateComponentLegalTab';
import { EstateComponentViolationsTab } from './EstateComponentViolationsTab';
import { EstateComponentApplicationsTab } from './EstateComponentApplicationsTab';
import { EstateComponentOrganizationsTab } from './EstateComponentOrganizationsTab';

/** UI-Router child route: Overview tab (CLM-43961). */
export function EstateComponentOverviewRoute(): JSX.Element {
  return <EstateComponentOverviewTab />;
}

/** UI-Router child route: Legal tab. */
export function EstateComponentLegalRoute(): JSX.Element {
  return <EstateComponentLegalTab />;
}

/** UI-Router child route: Policy Violations tab. */
export function EstateComponentViolationsRoute(): JSX.Element {
  return <EstateComponentViolationsTab />;
}

/** UI-Router child route: Applications where-used tab. */
export function EstateComponentApplicationsRoute(): JSX.Element {
  return <EstateComponentApplicationsTab />;
}

/** UI-Router child route: Organizations where-used tab. */
export function EstateComponentOrganizationsRoute(): JSX.Element {
  return <EstateComponentOrganizationsTab />;
}
