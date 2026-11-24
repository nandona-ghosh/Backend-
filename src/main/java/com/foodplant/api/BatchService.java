package com.foodplant.api;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
@Service @Transactional(readOnly=true)
class BatchService {
    private final BatchRepository repo; private final ProductLineRepository lines; private final org.springframework.jdbc.core.JdbcTemplate jdbc;
    BatchService(BatchRepository repo, ProductLineRepository lines, org.springframework.jdbc.core.JdbcTemplate jdbc) { this.repo=repo; this.lines=lines; this.jdbc=jdbc; }
    Batch get(long id) { return repo.findById(id).orElseThrow(() -> new MissingException("Batch not found")); }
    Page<Batch> list(Specification<Batch> filter, Pageable page) { return repo.findAll(filter,page); }
    @Transactional Batch create(BatchRequest request) { if (!lines.existsById(request.lineId())) throw new MissingException("Product line not found"); Batch item=new Batch(); item.batchNumber=request.batchNumber(); item.numberingEpoch=request.numberingEpoch(); item.lineId=request.lineId(); item.productionDate=request.productionDate(); item.shift=request.shift(); item.status=request.status(); item.quantityKg=request.quantityKg(); return repo.saveAndFlush(item); }
    @Transactional Batch update(long id, BatchRequest request) { var locked=jdbc.query("select line_id from batches where id=? for update",(r,n)->r.getLong(1),id); if (locked.isEmpty()) throw new MissingException("Batch not found"); Batch item=get(id); if (!locked.get(0).equals(request.lineId()) && jdbc.queryForObject("select count(*) from machine_runs where batch_id=?",Long.class,id)>0) throw new ConflictException("Cannot change the line of a batch with machine runs"); if (!lines.existsById(request.lineId())) throw new MissingException("Product line not found"); item.batchNumber=request.batchNumber(); item.numberingEpoch=request.numberingEpoch(); item.lineId=request.lineId(); item.productionDate=request.productionDate(); item.shift=request.shift(); item.status=request.status(); item.quantityKg=request.quantityKg(); return repo.saveAndFlush(item); }
    @Transactional void delete(long id) { Batch item=get(id); repo.delete(item); repo.flush(); }
}
