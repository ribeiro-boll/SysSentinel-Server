package com.bolota.syssentinel.Resource;

import com.bolota.syssentinel.Entities.DTOs.SystemEntityDTO;
import com.bolota.syssentinel.Entities.DTOs.SystemVolatileEntityDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SystemEntityResources extends JpaRepository<SystemEntityDTO, Long> {
    Page<SystemEntityDTO> findBy(Pageable pageable);
    Page<SystemEntityDTO> findByUUID(String uuid, Pageable pageable);
    SystemEntityDTO getByUUID(String uuid);
    boolean existsByUUID(String uuid);
    void deleteByUUID(String uuid);
}
