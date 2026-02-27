package com.bolota.syssentinel.Resource;

import com.bolota.syssentinel.Entities.UserEntities.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.nio.charset.Charset;

public interface UserEntityResources extends JpaRepository<UserEntity, Long> {
    boolean existsByLogin(String login);
    void deleteByLogin(String login);
    UserEntity getUserEntityByLogin(String login);

}
    