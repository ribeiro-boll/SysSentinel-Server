package com.bolota.syssentinel.Service;

import com.bolota.syssentinel.Entities.DTOs.SystemEntityDTO;
import com.bolota.syssentinel.Entities.SystemEntities.SystemEntity;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SysSentinelService {
    public static String genJSON(Object o){
        ObjectMapper objectMapper = new ObjectMapper();
        try{
            return objectMapper.writeValueAsString(o);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    public static SystemEntityDTO sysToSysDTO(SystemEntity se){
        return new SystemEntityDTO(se.getUUID(),se.getName(),se.getOs(),se.getHost(),se.getCpu(),se.getGpu(),se.getMemRamMax());
    }
}
