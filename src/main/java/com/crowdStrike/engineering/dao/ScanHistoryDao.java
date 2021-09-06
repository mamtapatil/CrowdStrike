package com.crowdStrike.engineering.dao;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.crowdStrike.engineering.model.Address;
import com.crowdStrike.engineering.model.Scans;


public interface ScanHistoryDao extends CrudRepository<Scans, Integer> {

	 @Query(value="SELECT scan_id FROM Scans st WHERE st.address_id = :addressId ORDER BY scan_id DESC limit 1", nativeQuery = true)
	 Integer findLastScan(@Param("addressId") int addressId);
	 
	 @Query(value="SELECT distinct scan_id FROM Scans st WHERE st.address_id = :addressId ORDER BY scan_id DESC", nativeQuery = true)
	 List<Integer> getAllScanId(@Param("addressId") int addressId);
	 
	 @Query(value="SELECT s FROM Scans s WHERE s.scanId = :scanId and s.address = :address")
	 List<Scans> getAllScans(@Param("scanId") int scanId, @Param("address") Address address);
	 
}