package br.com.senai.catalogo.dto;

public record CategoryRequest(String name, String description) {

    public CategoryRequest {
        if (name != null) {
            name = name.trim();
        }
        if (description != null) {
            description = description.trim();
        }
    }
}
