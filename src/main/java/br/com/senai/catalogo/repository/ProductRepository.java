package br.com.senai.catalogo.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import br.com.senai.catalogo.entity.Product;

public interface ProductRepository extends JpaRepository<Product, UUID>{
    
    List<Product> findByCategoryId(UUID categoryId);

    List<Product> findByNameContainingIgnoreCase(String name);

}
