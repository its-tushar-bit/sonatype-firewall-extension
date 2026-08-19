/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { getComponentName } from 'MainRoot/util/componentNameUtils';
import { ComponentNameSource } from 'MainRoot/nosc/dashboard/tabs/previewDashboardTypes';

export function componentNameInput(
  row: ComponentNameSource,
): Parameters<typeof getComponentName>[0] {
  return {
    displayName: row.displayName,
    filename: row.filename,
    componentIdentifier: row.componentIdentifier,
    derivedComponentName: row.derivedComponentName,
  };
}

export function previewDashboardComponentLabel(row: ComponentNameSource): string {
  // `getComponentName` does not consider `derivedComponentName` (it reads displayName / filename(s) /
  // componentIdentifier / componentName only), but the dashboard violations feed supplies the component name
  // exclusively via `derivedComponentName`. So this prefers it explicitly; without the short-circuit those
  // rows would fall through to the "Unknown" default.
  if (row.derivedComponentName) return row.derivedComponentName;
  return getComponentName(componentNameInput(row));
}
