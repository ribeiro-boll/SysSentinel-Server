package com.bolota.syssentinel.Resource;

import com.bolota.syssentinel.Entities.DTOs.SystemVolatileEntityDTO;
import com.bolota.syssentinel.Entities.UserEntities.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserEntityResources extends JpaRepository<UserEntity, Long> {
    boolean existsByLogin(String login);
    boolean deleteByLogin(String login);
    UserEntity getUserEntityByLogin(String login);
}
    