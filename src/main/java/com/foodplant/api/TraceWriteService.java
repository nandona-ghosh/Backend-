package com.foodplant.api;
import java.math.BigDecimal; import java.sql.Statement; import java.sql.Timestamp; import java.time.*; import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate; import org.springframework.jdbc.support.GeneratedKeyHolder; import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
@Service class TraceWriteService {
 private final JdbcTemplate db; private final BatchService batches;
 TraceWriteService(JdbcTemplate db,BatchService batches){this.db=db;this.batches=batches;}
 @Transactional long addLot(TraceWriteController.LotRequest r) {
  if(Boolean.FALSE.equals(db.queryForObject("select exists(select 1 from suppliers where id=?)",Boolean.class,r.supplierId()))) throw new MissingException("Supplier not found");
  return insert("insert into material_lots(supplier_id,material_name,lot_number,received_kg,received_date) values (?,?,?,?,?)",r.supplierId(),r.materialName(),r.lotNumber(),r.receivedKg(),r.receivedDate());
 }
 @Transactional long consume(long batchId,TraceWriteController.ConsumeRequest r) {
  batches.get(batchId);
  var amounts=db.query("select received_kg from material_lots where id=? for update",(rs,n)->rs.getBigDecimal(1),r.materialLotId());
  if(amounts.isEmpty()) throw new MissingException("Material lot not found");
  BigDecimal used=db.queryForObject("select coalesce(sum(used_kg),0) from batch_materials where material_lot_id=?",BigDecimal.class,r.materialLotId());
  if(amounts.get(0).subtract(used).compareTo(r.usedKg())<0) throw new ConflictException("Insufficient material in lot");
  return insert("insert into batch_materials(batch_id,material_lot_id,used_kg) values (?,?,?)",batchId,r.materialLotId(),r.usedKg());
 }
 @Transactional long quality(long batchId,TraceWriteController.QualityRequest r) {
  batches.get(batchId); if(r.recordedAt().isBefore(r.sampledAt())) throw new InvalidException("Recording time precedes sampling time");
  return insert("insert into quality_tests(batch_id,sampled_at,recorded_at,result,moisture_percent,note) values (?,?,?,?,?,?)",batchId,Timestamp.from(r.sampledAt()),Timestamp.from(r.recordedAt()),r.result(),r.moisturePercent(),r.note());
 }
 @Transactional long machineRun(long batchId,TraceWriteController.RunRequest r) {
  var batchLines=db.query("select line_id from batches where id=? for update",(rs,n)->rs.getLong(1),batchId);
  if(batchLines.isEmpty()) throw new MissingException("Batch not found");
  var lines=db.query("select line_id from machines where id=?",(rs,n)->rs.getLong(1),r.machineId());
  if(lines.isEmpty()) throw new MissingException("Machine not found");
  if(!Objects.equals(lines.get(0),batchLines.get(0))) throw new InvalidException("Machine belongs to a different line");
  if(r.endedAt().isBefore(r.startedAt())) throw new InvalidException("End time precedes start time");
  return insert("insert into machine_runs(batch_id,machine_id,started_at,ended_at) values (?,?,?,?)",batchId,r.machineId(),Timestamp.from(r.startedAt()),Timestamp.from(r.endedAt()));
 }
 private long insert(String sql,Object... args) { KeyHolder keys=new GeneratedKeyHolder(); db.update(connection -> {var stmt=connection.prepareStatement(sql,new String[]{"id"}); for(int i=0;i<args.length;i++) stmt.setObject(i+1,args[i]); return stmt;},keys); return Objects.requireNonNull(keys.getKey()).longValue(); }
}
