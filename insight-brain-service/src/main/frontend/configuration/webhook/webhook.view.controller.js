/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
function WebhookViewController(isAuthorized) {
  var vm = this;
  vm.isAuthorized = isAuthorized;
}

WebhookViewController.$inject = ['isAuthorized'];

angular.module('webhook.module').controller('webhook.view.controller', WebhookViewController);
