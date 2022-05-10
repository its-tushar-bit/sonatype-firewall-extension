/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import CLMLocationModule from '../../util/CLMLocation';
import AngularCommonModule from '../../utilAngular/AngularCommon';

import messages from './move.application.messages';
import moveApplicationService from './move.application.service';
import moveApplicationModalController from './move.application.modal.controller';
import moveApplicationModalService from './move.application.modal.service';
import moveApplicationErrorModalService from './move.application.error.modal.service';
import moveApplicationSuccessModalService from './move.application.success.modal.service';

export default angular //
  .module('move.application.module', [AngularCommonModule.name, CLMLocationModule.name]) //
  .constant('move.application.messages.constant', messages) //
  .service('move.application.service', moveApplicationService) //
  .controller('move.application.modal.controller', moveApplicationModalController) //
  .service('move.application.modal.service', moveApplicationModalService) //
  .service('move.application.error.modal.service', moveApplicationErrorModalService) //
  .service('move.application.success.modal.service', moveApplicationSuccessModalService); //
