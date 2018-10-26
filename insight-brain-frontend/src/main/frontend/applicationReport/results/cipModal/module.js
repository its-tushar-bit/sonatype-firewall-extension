/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import './cipGlobals';
import '../../../version-graph/appcheck';
import pv from '../../../lib/protovis/protovis.min';

import cipTabsWidgetModule from '../../../components/cipTabsWidget/module';
import versionGraphModule from '../../../version-graph/version.graph/version.graph.module';
import cipComponentUtilModule from '../../../cip/cip-component-util';
import proprietaryMatchersModule from '../../../cip/proprietary.matchers.modal/proprietary.matchers.module';
import cipPolicyViolationsModule from '../../../cip/cip.policy.violations/cip.policy.violations.module';
import cipLicenseEditorModule from '../../../cip/cip.license.editor/cip.license.editor.module';

import cipModal from './cipModal';

export default angular.module('cipModal',
    [
      cipTabsWidgetModule.name, versionGraphModule.name, cipComponentUtilModule.name, proprietaryMatchersModule.name,
      cipPolicyViolationsModule.name, cipLicenseEditorModule.name
    ])
    .component('cipModal', cipModal)
    .service('OwnerContext', OwnerContext);

// context service needed for CIP
function OwnerContext() {
  return {
    ownerType: 'application',
    ownerId: null,
    setOwnerId(id) {
      this.ownerId = id;
    }
  };
}

window.pv = pv;
