package com.printcreations.printcreations.catalog;

import com.printcreations.printcreations.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@Import({ProductCatalogService.class, SecurityConfig.class})
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsTheApprovedProductCatalog() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(28)))
                .andExpect(jsonPath("$[?(@.categoryId == 1)]", hasSize(10)))
                .andExpect(jsonPath("$[?(@.categoryId == 2)]", hasSize(11)))
                .andExpect(jsonPath("$[?(@.categoryId == 3)]", hasSize(4)))
                .andExpect(jsonPath("$[?(@.categoryId == 4)]", hasSize(3)))
                .andExpect(jsonPath(
                        "$[?(@.id == 12 && @.name == 'Business Cards' && @.categoryId == 2)]",
                        hasSize(1)
                ));
    }
}
