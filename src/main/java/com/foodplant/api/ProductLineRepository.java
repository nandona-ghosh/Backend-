package com.foodplant.api;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
interface ProductLineRepository extends JpaRepository<ProductLine,Long>, JpaSpecificationExecutor<ProductLine> {}
