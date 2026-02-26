package com.bolota.syssentinel.Resource;

import com.bolota.syssentinel.Entities.DTOs.SystemVolatileEntityDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemVolatileEntityResources extends JpaRepository<SystemVolatileEntityDTO, Long> {
    Page<SystemVolatileEntityDTO> findBy(Pageable pageable);
    boolean existsByUUID(String uuid);
    void deleteByUUID(String uuid);
    SystemVolatileEntityDTO getByUUID(String uuid);
}