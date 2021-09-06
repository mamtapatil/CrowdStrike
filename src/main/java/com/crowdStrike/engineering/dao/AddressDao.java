package com.crowdStrike.engineering.dao;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.crowdStrike.engineering.model.Address;

public interface AddressDao extends CrudRepository<Address, Integer> {

    @Query("select a from Address a where a.ipAddress = :addressInput")
    Address findByAddress(@Param("addressInput") String addressInput);

    
}