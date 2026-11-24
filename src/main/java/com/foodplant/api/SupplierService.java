package com.foodplant.api;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
@Service @Transactional(readOnly=true)
class SupplierService {
    private final SupplierRepository repo; 
    SupplierService(SupplierRepository repo) { this.repo=repo;  }
    Supplier get(long id) { return repo.findById(id).orElseThrow(() -> new MissingException("Supplier not found")); }
    Page<Supplier> list(Specification<Supplier> filter, Pageable page) { return repo.findAll(filter,page); }
    @Transactional Supplier create(SupplierRequest request) {  Supplier item=new Supplier(); item.code=request.code(); item.name=request.name(); item.contact=request.contact(); return repo.saveAndFlush(item); }
    @Transactional Supplier update(long id, SupplierRequest request) { Supplier item=get(id);  item.code=request.code(); item.name=request.name(); item.contact=request.contact(); return repo.saveAndFlush(item); }
    @Transactional void delete(long id) { Supplier item=get(id); repo.delete(item); repo.flush(); }
}
