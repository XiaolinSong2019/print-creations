package com.printcreations.printcreations.catalog;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

    @RestController
    public class ProductController {

        private final ProductCatalogService catalogService;

        public ProductController(ProductCatalogService catalogService) {
            this.catalogService = catalogService;
        }

        @GetMapping("/api/products")
        public List<ProductResponse> getProducts() {

            return catalogService.findAll();
        }
    }

