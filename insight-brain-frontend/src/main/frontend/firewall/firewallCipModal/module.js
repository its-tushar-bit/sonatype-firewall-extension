/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import '../../version-graph/appcheck';
import pv from '../../lib/protovis/protovis.min';

import firewallCipModal from './firewallCipModal';
import cipModalModule from '../../applicationReport/results/cipModal/module';
import componentUpdateController from '../../audit-report/audit.module/component.update.controller';
import componentUpdateOptionalController from '../../audit-report/audit.module/component.update.optional.controller';
import componentUpdateService from '../../audit-report/audit.module/component.update.service';

export default angular
  .module('firewallCipModal', [cipModalModule.name])
  .component('firewallCipModal', firewallCipModal)
  .controller('component.update.controller', componentUpdateController)
  .controller('component.update.optional.controller', componentUpdateOptionalController)
  .service('ComponentUpdateService', componentUpdateService);

window.pv = pv;
