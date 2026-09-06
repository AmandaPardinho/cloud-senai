package br.com.senai.catalogo.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductResponse(UUID id, String name, int stockQuantity, BigDecimal price, CategoryResponse category) {
}
