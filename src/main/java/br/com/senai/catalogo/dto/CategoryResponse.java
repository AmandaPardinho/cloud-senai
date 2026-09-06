package br.com.senai.catalogo.dto;

import java.util.UUID;

public record CategoryResponse(UUID id, String name, String description) {
}
