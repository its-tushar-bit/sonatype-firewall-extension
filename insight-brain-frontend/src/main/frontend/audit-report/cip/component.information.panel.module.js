/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular */

import cipLabelEditorModule from '../../cip/cip.label.editor/cip.label.editor.module';
import cipLicenseEditorModule from '../../cip/cip.license.editor/cip.license.editor.module';
import cipVersionGraphModule from '../../cip/cip.version.graph/cip.version.graph.module';
import cipPolicyViolationsModule from '../../cip/cip.policy.violations/cip.policy.violations.module';
import cipVulnerabilityEditorModule from './cip.vulnerability.editor/cip.vulnerability.editor.module';
import cipTabsWidgetModule from '../../components/cipTabsWidget/module';

import repositoryPolicyViolationsService from './repository.policy.violations.service';

export default angular
  .module('component.information.panel', [
    cipLabelEditorModule.name,
    cipPolicyViolationsModule.name,
    cipVulnerabilityEditorModule.name,
    cipLicenseEditorModule.name,
    cipVersionGraphModule.name,
    cipTabsWidgetModule.name,
  ])
  .service('PolicyViolations', repositoryPolicyViolationsService);
