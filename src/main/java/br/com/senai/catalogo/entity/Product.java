package br.com.senai.catalogo.entity;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity 
@Table(name = "products")
public class Product {

    @Id 
    @GeneratedValue (strategy = GenerationType.UUID)
    private UUID id;
    
    @Column (nullable = false, length = 255)
    private String name;

    @Column (nullable = false)
    private int stockQuantity;

    @Column (nullable = false, precision = 10, scale = 2)
    private BigDecimal price; 
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    //constructors
    public Product() {
    }

    public Product(String name, int stockQuantity, BigDecimal price, Category category) {
        this.name = name;
        this.stockQuantity = stockQuantity;
        this.price = price;
        this.category = category;
    }

    //getters and setters
    public UUID getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(int stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    

}