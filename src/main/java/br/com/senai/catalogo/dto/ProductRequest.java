package br.com.senai.catalogo.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductRequest(String name, int stockQuantity, BigDecimal price, UUID categoryId) {

    public ProductRequest {
        if (name != null) {
            name = name.trim();
        }
    }
}
