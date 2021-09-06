package com.crowdStrike.engineering.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonBackReference;

@Entity
@Table(name = "scans")
public class Scans {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "scan_history_id", unique = true, nullable = false)
	private Integer scanHistoryId;

	@ManyToOne
	@JoinColumn(name = "address_id")
	private Address address;
	
	@ManyToOne
	@JoinColumn(name = "port_id")
	private Port port ;

	@Column(name = "scan_id")
	private int scanId;
	
	@Column(name = "added_date")
	private Date addedDate;

	public Scans(){	
	}
	
	public Scans(int scanId){	
		this.scanId = scanId;
		addedDate = new Date();
	}
	
	public Integer getScanHistoryId() {
		return scanHistoryId;
	}

	public void setScanHistoryId(Integer scanHistoryId) {
		this.scanHistoryId = scanHistoryId;
	}

	public Address getAddress() {
		return address;
	}

	public void setAddress(Address address) {
		this.address = address;
	}

	public Port getPort() {
		return port;
	}

	public void setPort(Port port) {
		this.port = port;
	}

	public int getScanId() {
		return scanId;
	}

	public void setScanId(int scanId) {
		this.scanId = scanId;
	}

	public Date getAddedDate() {
		return addedDate;
	}

	public void setAddedDate(Date addedDate) {
		this.addedDate = addedDate;
	}	
}