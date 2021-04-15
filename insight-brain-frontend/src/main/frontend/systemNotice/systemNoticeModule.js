/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import systemNoticeService from './systemNoticeService';
import systemNotice from './systemNotice';

export default angular
  .module('systemNoticeModule', [])
  .service('systemNoticeService', systemNoticeService)
  .component('systemNotice', systemNotice);
