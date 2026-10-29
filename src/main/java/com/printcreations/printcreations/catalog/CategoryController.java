package com.printcreations.printcreations.catalog;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CategoryController {
    @GetMapping("/api/categories")
    public List<CategoryResponse> getCategories() {
        return List.of(
                new CategoryResponse(1L, "Marketing Materials"),
                new CategoryResponse(2L, "Stationery & Forms"),
                new CategoryResponse(3L, "Posters, Banners & Signs"),
                new CategoryResponse(4L, "Events & Calendars")
                );
    }
}
