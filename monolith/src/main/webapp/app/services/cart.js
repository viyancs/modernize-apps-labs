'use strict';

angular.module("app")

.factory('cart', ['$http', '$q', 'COOLSTORE_CONFIG', 'Auth', '$location', function($http, $q, COOLSTORE_CONFIG, $auth, $location) {
	var factory = {}, cart, products, cartId, baseUrl;
	
	alert("Monolith Cart = " + COOLSTORE_CONFIG.MONOLITH_CART);
	console.log(COOLSTORE_CONFIG.MICRPO_CART_API_SECURE_ENDPOINT);
	console.log(COOLSTORE_CONFIG.MICRPO_CART_API_ENDPOINT);
	if(COOLSTORE_CONFIG.MONOLITH_CART === true) {
		baseUrl='/services/cart';
	} else {
		if ($location.protocol() === 'https') {
			baseUrl = (COOLSTORE_CONFIG.MICRPO_CART_API_SECURE_ENDPOINT.startsWith("https://") ? COOLSTORE_CONFIG.MICRPO_CART_API_SECURE_ENDPOINT : "https://" + COOLSTORE_CONFIG.MICRPO_CART_API_SECURE_ENDPOINT + '.' + $location.host().replace(/^.*?\.(.*)/g,"$1")) + '/services/cart';
		} else {
			baseUrl = (COOLSTORE_CONFIG.MICRPO_CART_API_ENDPOINT.startsWith("http://") ? COOLSTORE_CONFIG.MICRPO_CART_API_ENDPOINT : "http://" + COOLSTORE_CONFIG.MICRPO_CART_API_ENDPOINT + '.' + $location.host().replace(/^.*?\.(.*)/g,"$1")) + '/services/cart';
		}
	}

	factory.checkout = function() {
		var deferred = $q.defer();
		$http({
			   method: 'POST',
			   url: baseUrl + '/checkout/' + cartId
		   }).then(function(resp) {
			    cart = resp.data;
			   	deferred.resolve(resp.data);
		   }, function(err) {
			   	deferred.reject(err);
		   });
		return deferred.promise;
	};

	factory.reset = function() {
		cart = {
			shoppingCartItemList: []
		};
		var tmpId = localStorage.getItem('cartId');
		var authId = $auth.userInfo ? $auth.userInfo.sub : null;

		if (tmpId && authId) {
			// transfer cart
			cartId = authId;
			this.setCart(tmpId).then(function(result) {
				localStorage.removeItem('cartId');
			}, function(err) {
				console.log("could not transfer cart " + tmpId + " to cart " +  authId + ": " + err);
			});
			return;
		}

		if (tmpId && !authId) {
			cartId = tmpId;
		}

		if (!tmpId && authId) {
			cartId = authId;
		}

		if (!tmpId && !authId) {
			tmpId = 'id-' + Math.random();
			localStorage.setItem('cartId', tmpId);
			cartId = tmpId;
		}

		cart.shoppingCartItemList = [];
		$http({
			   method: 'GET',
			   url: baseUrl + '/' + cartId
		   }).then(function(resp) {
			    cart = resp.data;
		   }, function(err) {
		});

	};

	factory.getCart = function() {
		return cart;
	};

	factory.removeFromCart = function(product, quantity) {
		var deferred = $q.defer();
		$http({
			method: 'DELETE',
			url: baseUrl + '/' + cartId + '/' + product.itemId + '/' + quantity
		}).then(function(resp) {
			cart = resp.data;
			deferred.resolve(resp.data);
		}, function(err) {
			deferred.reject(err);
		});
		return deferred.promise;

	};

	factory.setCart = function(id) {
		var deferred = $q.defer();
		$http({
			method: 'POST',
			url: baseUrl + '/' + cartId + '/' + id
		}).then(function(resp) {
			cart = resp.data;
			deferred.resolve(resp.data);
		}, function(err) {
			deferred.reject(err);
		});
		return deferred.promise;

	};

	factory.addToCart = function(product, quantity) {
		var deferred = $q.defer();
		$http({
			   method: 'POST',
			   url: baseUrl + '/' + cartId + '/' + product.itemId + '/' + quantity
		   }).then(function(resp) {
			    cart = resp.data;
			   	deferred.resolve(resp.data);
		   }, function(err) {
			   	deferred.reject(err);
		   });
		return deferred.promise;

	};

	factory.reset();
	return factory;
}]);