package com.foodplant.api;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
@Service @Transactional(readOnly=true)
class ProductLineService {
    private final ProductLineRepository repo; 
    ProductLineService(ProductLineRepository repo) { this.repo=repo;  }
    ProductLine get(long id) { return repo.findById(id).orElseThrow(() -> new MissingException("ProductLine not found")); }
    Page<ProductLine> list(Specification<ProductLine> filter, Pageable page) { return repo.findAll(filter,page); }
    @Transactional ProductLine create(ProductLineRequest request) {  ProductLine item=new ProductLine(); item.code=request.code(); item.name=request.name(); return repo.saveAndFlush(item); }
    @Transactional ProductLine update(long id, ProductLineRequest request) { ProductLine item=get(id);  item.code=request.code(); item.name=request.name(); return repo.saveAndFlush(item); }
    @Transactional void delete(long id) { ProductLine item=get(id); repo.delete(item); repo.flush(); }
}
