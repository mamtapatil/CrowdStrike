package com.crowdStrike.engineering.dao;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import com.crowdStrike.engineering.model.Port;

public interface OpenPortsDao extends CrudRepository<Port, Integer> {

	 @Query("select p from Port p where p.portNumber = :portNumber and p.protocol = :protocol and p.service = :service")
	 Port findPort(@Param("portNumber") int portNumber, @Param("protocol") String protocol, @Param("service") String service);
	 
}
