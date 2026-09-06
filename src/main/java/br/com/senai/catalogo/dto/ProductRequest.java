package br.com.senai.catalogo.dto;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ProductRequest(

        @NotBlank(message = "O nome do produto é obrigatório")
        @Size(max = 255, message = "O nome deve ter no máximo 255 caracteres")
        String name,

        @NotNull(message = "A quantidade em estoque é obrigatória")
        @PositiveOrZero(message = "O estoque não pode ser negativo")
        Integer stockQuantity,

        @NotNull(message = "O preço é obrigatório")
        @Positive(message = "O preço deve ser maior que zero")
        @Digits(integer = 8, fraction = 2, message = "Preço fora do formato 10,2")
        BigDecimal price,

        @NotNull(message = "A categoria é obrigatória")
        UUID categoryId

) {

    public ProductRequest {
        if (name != null) {
            name = name.trim();
        }
    }
}
