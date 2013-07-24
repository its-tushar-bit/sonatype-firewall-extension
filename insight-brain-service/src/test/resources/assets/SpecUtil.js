var SpecUtil = {
	getTemplate : function (url) {
		url = url.split('/');
		if (url[0] === '..') {
			url.splice(0, 1);
		}
		if (url[0] === 'policy-assets') {
			url[0] = 'policy';
		} else if (url[0] === 'organization-assets') {
			url[0] = 'organization';
		} else if (url[0] === 'application-assets') {
			url[0] = 'application';
		}

		if (location.hostname) {
			url = 'src/main/resources/assets/' + url.join('/');
		} else {
			url = 'src/' + url.join('/');
		}

		var data = null;
		$.ajax({
			async: false,
			dataType: 'html',
			url: url,
			success: function(responseData) {
				data = responseData;
			}
		});
		return data;
	},
	toRegExp : function toRegExp(url) {
		return new RegExp(url + '\\?timestamp=[0-9]+')
	}

};