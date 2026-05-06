package org.adso.minimarket.mappers;

import org.adso.minimarket.dto.InventoryTransactionDTO;
import org.adso.minimarket.models.inventory.InventoryTransaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface InventoryMapper {

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "product.name")
    InventoryTransactionDTO toDto(InventoryTransaction transaction);

    List<InventoryTransactionDTO> toDtoList(List<InventoryTransaction> transactions);
}
