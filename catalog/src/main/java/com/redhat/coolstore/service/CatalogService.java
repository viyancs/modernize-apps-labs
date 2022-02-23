package com.redhat.coolstore.service;

import com.redhat.coolstore.model.Inventory;
import com.redhat.coolstore.model.Product;
import com.redhat.coolstore.client.InventoryClient;
import feign.hystrix.FallbackFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CatalogService {

    @Autowired
    private ProductRepository repository;

    //Autowire Inventory Client
    @Autowired
    InventoryClient inventoryClient;

    public Product read(String id) {
        Product product = repository.findById(id);
        //Update the quantity for the product by calling the Inventory service
        product.setQuantity(inventoryClient.getInventoryStatus(product.getItemId()).getQuantity());
        return product;
    }

    public List<Product> readAll() {
        List<Product> productList = repository.readAll();
        //Update the quantity for the products by calling the Inventory service
        productList.parallelStream()
            .forEach(p -> {
                p.setQuantity(inventoryClient.getInventoryStatus(p.getItemId()).getQuantity());
            });
        return productList; 
    }

    //TODO: Add Callback Factory Component
    

}
