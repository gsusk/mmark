package org.adso.minimarket.mappers;

import org.adso.minimarket.dto.BasicUser;
import org.adso.minimarket.dto.DetailedUser;
import org.adso.minimarket.models.user.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {
    @Mapping(target = "firstName", source = "name")
    @Mapping(target = "role", source = "role")
    BasicUser toResponseDto(User user);

    @Mapping(target = "firstName", source = "name")
    DetailedUser toDetailedUserDto(User user);
}
