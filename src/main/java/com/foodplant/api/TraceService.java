package com.foodplant.api;
import java.math.BigDecimal; import java.time.*; import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
@Service @Transactional(readOnly=true)
class TraceService {
 private final JdbcTemplate jdbc; private final BatchService batches;
 TraceService(JdbcTemplate jdbc,BatchService batches){this.jdbc=jdbc;this.batches=batches;}
 public record Material(long lotId,String material,String lotNumber,long supplierId,String supplier,BigDecimal usedKg) {}
 public record MachineRun(long machineId,String machine,Instant startedAt,Instant endedAt) {}
 public record Quality(long id,Instant sampledAt,Instant recordedAt,String result,BigDecimal moisturePercent,String note) {}
 public record Trace(Batch batch,List<Material> materials,List<MachineRun> machineRuns,List<Quality> qualityTests) {}
 public record Machine(long id,long lineId,String code,String name) {}
 public record Inventory(long lotId,String material,String lotNumber,long supplierId,String supplier,BigDecimal receivedKg,BigDecimal usedKg,BigDecimal availableKg) {}
 Trace trace(long id) {
  Batch batch=batches.get(id);
  var materials=jdbc.query("select l.id,l.material_name,l.lot_number,s.id,s.name,bm.used_kg from batch_materials bm join material_lots l on l.id=bm.material_lot_id join suppliers s on s.id=l.supplier_id where bm.batch_id=? order by l.id",(r,n)->new Material(r.getLong(1),r.getString(2),r.getString(3),r.getLong(4),r.getString(5),r.getBigDecimal(6)),id);
  var runs=jdbc.query("select m.id,m.name,r.started_at,r.ended_at from machine_runs r join machines m on m.id=r.machine_id where r.batch_id=? order by r.id",(r,n)->new MachineRun(r.getLong(1),r.getString(2),r.getTimestamp(3).toInstant(),r.getTimestamp(4).toInstant()),id);
  var tests=jdbc.query("select id,sampled_at,recorded_at,result,moisture_percent,note from quality_tests where batch_id=? order by id",(r,n)->new Quality(r.getLong(1),r.getTimestamp(2).toInstant(),r.getTimestamp(3).toInstant(),r.getString(4),r.getBigDecimal(5),r.getString(6)),id);
  return new Trace(batch,materials,runs,tests);
 }
 List<Machine> machines(int page,int size) {
  return jdbc.query("select id,line_id,code,name from machines order by id limit ? offset ?",(r,n)->new Machine(r.getLong(1),r.getLong(2),r.getString(3),r.getString(4)),size,(long)page*size);
 }
 List<Inventory> inventory(int page,int size) {
  return jdbc.query("select l.id,l.material_name,l.lot_number,s.id,s.name,l.received_kg,coalesce(sum(bm.used_kg),0),l.received_kg-coalesce(sum(bm.used_kg),0) from material_lots l join suppliers s on s.id=l.supplier_id left join batch_materials bm on bm.material_lot_id=l.id group by l.id,l.material_name,l.lot_number,s.id,s.name,l.received_kg order by l.id limit ? offset ?",(r,n)->new Inventory(r.getLong(1),r.getString(2),r.getString(3),r.getLong(4),r.getString(5),r.getBigDecimal(6),r.getBigDecimal(7),r.getBigDecimal(8)),size,(long)page*size);
 }
}
