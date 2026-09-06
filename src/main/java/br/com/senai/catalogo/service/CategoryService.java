package br.com.senai.catalogo.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.senai.catalogo.dto.CategoryRequest;
import br.com.senai.catalogo.dto.CategoryResponse;
import br.com.senai.catalogo.entity.Category;
import br.com.senai.catalogo.exception.DuplicateResourceException;
import br.com.senai.catalogo.exception.ResourceNotFoundException;
import br.com.senai.catalogo.repository.CategoryRepository;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository){
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> findAll(){
        return categoryRepository.findAll().stream().map(CategoryService::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse findById(UUID id){
        Category category = categoryRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada: " + id));

        return toResponse(category);
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request){
        if(categoryRepository.existsByNameIgnoreCase(request.name())){
            throw new DuplicateResourceException("Já existe uma categoria com o nome: " + request.name());
        }

        Category category = new Category(request.name());
        category.setDescription(request.description());

        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse update(UUID id, CategoryRequest request){
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada: " + id));

        if(categoryRepository.existsByNameIgnoreCaseAndIdNot(request.name(), id)){
            throw new DuplicateResourceException("Já existe uma categoria com o nome: " + request.name());
        }

        category.setName(request.name());
        category.setDescription(request.description());

        return toResponse(category);
    }

    @Transactional
    public void delete(UUID id){
        if(!categoryRepository.existsById(id)){
            throw new ResourceNotFoundException("Categoria não encontrada: " + id);
        }

        categoryRepository.deleteById(id);
    }

    static CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription());
    }
}
