package com.printcreations.printcreations.catalog;

import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ProductCatalogService {

    private static final List<ProductResponse> PRODUCTS = List.of(
            new ProductResponse(1L, "Booklets", 1L),
            new ProductResponse(2L, "Brochures", 1L),
            new ProductResponse(3L, "Coupons & Gift Certificates", 1L),
            new ProductResponse(4L, "Door Hangers", 1L),
            new ProductResponse(5L, "Flyers", 1L),
            new ProductResponse(6L, "Menus", 1L),
            new ProductResponse(7L, "Newsletters", 1L),
            new ProductResponse(8L, "Postcards", 1L),
            new ProductResponse(9L, "Sell Sheets", 1L),
            new ProductResponse(10L, "Table Tents", 1L),
            new ProductResponse(11L, "Appointment Cards", 2L),
            new ProductResponse(12L, "Business Cards", 2L),
            new ProductResponse(13L, "Bookmarks", 2L),
            new ProductResponse(14L, "Carbonless Forms", 2L),
            new ProductResponse(15L, "Envelopes", 2L),
            new ProductResponse(16L, "Folders", 2L),
            new ProductResponse(17L, "Labels", 2L),
            new ProductResponse(18L, "Letterheads", 2L),
            new ProductResponse(19L, "Notecards", 2L),
            new ProductResponse(20L, "Notepads", 2L),
            new ProductResponse(21L, "Rack Cards", 2L),
            new ProductResponse(22L, "Banners & Flags", 3L),
            new ProductResponse(23L, "Posters", 3L),
            new ProductResponse(24L, "Stickers & Decals", 3L),
            new ProductResponse(25L, "Signs", 3L),
            new ProductResponse(26L, "Calendars", 4L),
            new ProductResponse(27L, "Event Tickets", 4L),
            new ProductResponse(28L, "Invitations", 4L)
    );

    public List<ProductResponse> findAll() {
        return PRODUCTS;
    }
}
