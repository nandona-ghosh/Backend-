package com.foodplant.api;
import jakarta.persistence.*;
@Entity @Table(name="batches") public class Batch {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    @Column(nullable=false) public String batchNumber;
    @Column(nullable=false) public Long lineId;
    @Column(nullable=false) public java.time.LocalDate productionDate;
    @Column(nullable=false) public String shift;
    @Column(nullable=false) public String status;
    @Column(nullable=false) public java.math.BigDecimal quantityKg;
    @Column(nullable=false, length=32) public String numberingEpoch;
    protected Batch() {}
}
