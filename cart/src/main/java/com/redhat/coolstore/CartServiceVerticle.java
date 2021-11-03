package com.redhat.coolstore;

import com.redhat.coolstore.model.Product;
import com.redhat.coolstore.model.ShoppingCart;
import com.redhat.coolstore.model.ShoppingCartItem;
import com.redhat.coolstore.model.impl.ShoppingCartImpl;
import com.redhat.coolstore.model.impl.ShoppingCartItemImpl;
import com.redhat.coolstore.utils.Generator;
import com.redhat.coolstore.utils.Transformers;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.handler.StaticHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("SameParameterValue")
public class CartServiceVerticle extends AbstractVerticle {

    /**
     * This is the HashMap that holds the shopping cart. This should be replace with
     * a replicated cache like Infinispan etc
     */
    private final static Map<String, ShoppingCart> carts = new ConcurrentHashMap<>();

    private final Logger logger = LoggerFactory.getLogger(CartServiceVerticle.class.getName());

    static {
        carts.put("99999", Generator.generateShoppingCart("99999"));
    }

    @Override
    public void start() {
        logger.info("Starting " + this.getClass().getSimpleName());
        Integer serverPort = config().getInteger("http.port", 10080);
        logger.info("Starting the HTTP Server on port " + serverPort);

        // Create Router
        Router router = Router.router(vertx);

        // Create hello router
        router.get("/hello")
                .handler(rc -> rc.response().setStatusCode(200).putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                        .end(new JsonObject().put("message", "Hello").encode()));
        // TODO: Create carts router

        // Create cart router
        router.get("/services/cart/:cartId").handler(this::getCart);
        router.get("/services/carts").handler(this::getCarts);

        // TODO: Create checkout router
        // Create add router
        router.post("/services/cart/:cartId/:itemId/:quantity").handler(this::addToCart);

        // Create remove router
        router.delete("/services/cart/:cartId/:itemId/:quantity").handler(this::removeShoppingCartItem);

        // Create static router
        router.get("/*").handler(StaticHandler.create());

        // Create HTTP Server
        vertx.createHttpServer().requestHandler(router::accept).listen(serverPort);
    }

    // Add handler for getting a list of shoppingCarts
    private void getCarts(RoutingContext rc) {
        logger.info("Retrieved " + rc.request().method().name() + " request to " + rc.request().absoluteURI());
        JsonArray cartList = new JsonArray();
        carts.keySet().forEach(cartId -> cartList.add(Transformers.shoppingCartToJson(carts.get(cartId))));
        rc.response().setStatusCode(200).putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .end(cartList.encodePrettily());
    }

    // Add handler for getting a shoppingCart by id
    private void getCart(RoutingContext rc) {
        logger.info("Retrieved " + rc.request().method().name() + " request to " + rc.request().absoluteURI());
        String cartId = rc.pathParam("cartId");
        ShoppingCart cart = getCart(cartId);
        this.getShippingFee(cart, message -> {
            if(message.succeeded()) {
                cart.setShippingTotal(message.result());
                sendCart(cart,rc);
            } else {
                sendError(rc);
            }
        
        });
    }

    // Add handler for adding a Item to the cart
    private void addToCart(RoutingContext rc) {
        logger.info("Retrieved " + rc.request().method().name() + " request to " + rc.request().absoluteURI());

        String cartId = rc.pathParam("cartId");
        String itemId = rc.pathParam("itemId");
        int quantity = Integer.parseInt(rc.pathParam("quantity"));

        ShoppingCart cart = getCart(cartId);

        boolean productAlreadyInCart = cart.getShoppingCartItemList().stream()
                .anyMatch(i -> i.getProduct().getItemId().equals(itemId));

        if (productAlreadyInCart) {
            cart.getShoppingCartItemList().forEach(item -> {
                if (item.getProduct().getItemId().equals(itemId)) {
                    item.setQuantity(item.getQuantity() + quantity);
                    this.getShippingFee(cart, message -> {
                        if(message.succeeded()) {
                            cart.setShippingTotal(message.result());
                            sendCart(cart,rc);
                        } else {
                            sendError(rc);
                        }
                    
                    });// update the shipping fee
                }
            });
        } else {
            ShoppingCartItem newItem = new ShoppingCartItemImpl();
            newItem.setQuantity(quantity);
            // Get product from Catalog service and add it to the ShoppingCartItem
            this.getProduct(itemId, reply -> {
                if (reply.succeeded()) {
                    newItem.setProduct(reply.result());
                    cart.addShoppingCartItem(newItem);
                    sendCart(cart,rc); //TODO: update the shipping fee, here as well
                } else {
                    sendError(rc);
                }
            });
        }
    }

    // Add handler for removing an item from the cart
    private void removeShoppingCartItem(RoutingContext rc) {
        logger.info("Retrieved " + rc.request().method().name() + " request to " + rc.request().absoluteURI());
        String cartId = rc.pathParam("cartId");
        String itemId = rc.pathParam("itemId");
        int quantity = Integer.parseInt(rc.pathParam("quantity"));
        ShoppingCart cart = getCart(cartId);
    
        //If all quantity with the same Id should be removed then remove it from the list completely. The is the normal use-case
        cart.getShoppingCartItemList().removeIf(i -> i.getProduct().getItemId().equals(itemId) && i.getQuantity()<=quantity);
    
        //If not all quantities should be removed we need to update the list
        cart.getShoppingCartItemList().forEach(i ->  {
                if(i.getProduct().getItemId().equals(itemId))
                    i.setQuantity(i.getQuantity()-quantity);
            }
        );
        sendCart(cart,rc);
    }

    // TODO: Add handler for checking out a shopping cart

    //Add method for getting products
    private void getProduct(String itemId, Handler<AsyncResult<Product>> resultHandler) {
        WebClient client = WebClient.create(vertx);
        Integer port = config().getInteger("catalog.service.port", 8080);
        String hostname = config().getString("catalog.service.hostname", "localhost");
        Integer timeout = config().getInteger("catalog.service.timeout", 0);
        client.get(port, hostname,"/services/product/"+itemId)
            .timeout(timeout)
            .send(handler -> {
                if(handler.succeeded()) {
                    Product product = Transformers.jsonToProduct(handler.result().body().toJsonObject());
                    resultHandler.handle(Future.succeededFuture(product));
                } else {
                    resultHandler.handle(Future.failedFuture(handler.cause()));
                }
    
    
            });
    }

    // Add method for getting the shipping fee
    private void getShippingFee(ShoppingCart cart, Handler<AsyncResult<Double>> resultHandler) {
        EventBus eb = vertx.eventBus();
    
        eb.send("shipping",
            Transformers.shoppingCartToJson(cart).encode(),
            reply -> {
                if(reply.succeeded()) {
                    resultHandler.handle(Future.succeededFuture(((JsonObject)reply.result().body()).getDouble("shippingFee")));
    
                } else {
                    resultHandler.handle(Future.failedFuture(reply.cause()));
                }
            }
        );
    }

    private void sendCart(ShoppingCart cart, RoutingContext rc) {
        sendCart(cart, rc, 200);
    }

    private void sendCart(ShoppingCart cart, RoutingContext rc, int status) {
        rc.response().setStatusCode(status).putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .end(Transformers.shoppingCartToJson(cart).encodePrettily());
    }

    private void sendError(RoutingContext rc) {
        sendError("Unknown", rc);
    }

    private void sendError(String reason, RoutingContext rc) {
        logger.error("Error processing " + rc.request().method().name() + " request to " + rc.request().absoluteURI()
                + " with reason " + reason);
        rc.response().setStatusCode(500).end();
    }

    private static ShoppingCart getCart(String cartId) {
        if (carts.containsKey(cartId)) {
            return carts.get(cartId);
        } else {
            ShoppingCart cart = new ShoppingCartImpl();
            cart.setCartId(cartId);
            carts.put(cartId, cart);
            return cart;
        }

    }
}
