package br.com.senai.catalogo.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.senai.catalogo.dto.ProductRequest;
import br.com.senai.catalogo.dto.ProductResponse;
import br.com.senai.catalogo.entity.Category;
import br.com.senai.catalogo.entity.Product;
import br.com.senai.catalogo.exception.ResourceNotFoundException;
import br.com.senai.catalogo.repository.CategoryRepository;
import br.com.senai.catalogo.repository.ProductRepository;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> findAll() {
        return productRepository.findAll()
                .stream()
                .map(ProductService::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse findById(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado: " + id));
        return toResponse(product);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> findByCategory(UUID categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException("Categoria não encontrada: " + categoryId);
        }
        return productRepository.findByCategoryId(categoryId)
                .stream()
                .map(ProductService::toResponse)
                .toList();
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Categoria não encontrada: " + request.categoryId()));

        Product product = new Product(
                request.name(),
                request.stockQuantity(),
                request.price(),
                category);

        return toResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse update(UUID id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado: " + id));

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Categoria não encontrada: " + request.categoryId()));

        product.setName(request.name());
        product.setStockQuantity(request.stockQuantity());
        product.setPrice(request.price());
        product.setCategory(category);

        return toResponse(product);
    }

    @Transactional
    public void delete(UUID id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Produto não encontrado: " + id);
        }

        productRepository.deleteById(id);
    }

    static ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getStockQuantity(),
                product.getPrice(),
                CategoryService.toResponse(product.getCategory()));
    }
}
