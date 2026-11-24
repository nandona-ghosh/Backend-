package com.foodplant.api;
import jakarta.persistence.*;
@Entity @Table(name="product_lines") public class ProductLine {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    @Column(nullable=false, unique=true) public String code;
    @Column(nullable=false) public String name;
    protected ProductLine() {}
}
