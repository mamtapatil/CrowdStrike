package com.crowdStrike.engineering.model;

import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name = "port")
public class Port {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "port_id", unique = true, nullable = false)
	private Integer portId;

	@Column(name = "port_number")
	private int portNumber;

	@Column(name = "protocol")
	private String protocol;
	
	@Column(name = "service")
	private String service;

	@OneToMany(fetch = FetchType.LAZY, mappedBy="port" ,cascade = CascadeType.ALL)
	private List<Scans> scanHistory;
	
	public Port(){	
	}
	
	public Port(int portNumber, String protocol, String service){	
		this.portNumber = portNumber;
		this.protocol = protocol;
		this.service = service;
	}
	
	public Integer getPortId() {
		return portId;
	}

	public void setPortId(Integer portId) {
		this.portId = portId;
	}

	public int getPortNumber() {
		return portNumber;
	}

	public void setPortNumber(int portNumber) {
		this.portNumber = portNumber;
	}

	public String getProtocol() {
		return protocol;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public String getService() {
		return service;
	}

	public void setService(String service) {
		this.service = service;
	}
	
	@Override
	public int hashCode()
	{
	    final int PRIME = 31;
	    int result = 1;
	    result = PRIME * result + getPortNumber();
	    result = PRIME * result + ((protocol == null) ? 0 : getProtocol().hashCode());
	    result = PRIME * result + ((service == null) ? 0 : getService().hashCode());
	    return result;
	}
	
	@Override
	public boolean equals(Object o) {
	    if(o == null)
	    {
	        return false;
	    }
	    if (o == this)
	    {
	        return true;
	    }
	    if (getClass() != o.getClass())
	    {
	        return false;
	    }
	     
	    Port port = (Port) o;
	    return (this.getPortNumber() == port.getPortNumber() && this.getProtocol() == port.getProtocol() && this.getService() == port.getService());
	}

}
	