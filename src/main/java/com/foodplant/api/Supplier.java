package com.foodplant.api;
import jakarta.persistence.*;
@Entity @Table(name="suppliers") public class Supplier {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    @Column(nullable=false, unique=true) public String code;
    @Column(nullable=false) public String name;
    @Column(nullable=true) public String contact;
    protected Supplier() {}
}
