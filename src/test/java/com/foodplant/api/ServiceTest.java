package com.foodplant.api;
import java.util.Optional; import org.junit.jupiter.api.Test; import static org.mockito.Mockito.*; import static org.junit.jupiter.api.Assertions.*;
class ServiceTest {
 @Test void missingAndCrud() { var repo=mock(SupplierRepository.class); var service=new SupplierService(repo); when(repo.findById(1L)).thenReturn(Optional.empty()); assertThrows(MissingException.class,()->service.get(1)); when(repo.saveAndFlush(any())).thenAnswer(i->i.getArgument(0)); var req=new SupplierRequest("S","Name",null); var entity=service.create(req); assertEquals("Name",entity.name); when(repo.findById(2L)).thenReturn(Optional.of(entity)); assertEquals("Updated",service.update(2,new SupplierRequest("S","Updated",null)).name); service.delete(2); verify(repo).delete(entity); verify(repo).flush(); }
}
