package com.crowdStrike.engineering.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.crowdStrike.engineering.dao.AddressDao;
import com.crowdStrike.engineering.dao.OpenPortsDao;
import com.crowdStrike.engineering.dao.ScanHistoryDao;
import com.crowdStrike.engineering.exception.InvalidInputException;
import com.crowdStrike.engineering.exception.NMapException;
import com.crowdStrike.engineering.model.Address;
import com.crowdStrike.engineering.model.Nmaprun;
import com.crowdStrike.engineering.model.Nmaprun.Host.Ports.Port;
import com.crowdStrike.engineering.model.PortDisplayDTO;
import com.crowdStrike.engineering.model.Scans;
import com.google.common.net.InetAddresses;
import com.google.common.net.InternetDomainName;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Resource;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import org.nmap4j.Nmap4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class AppService {

	@Resource
	AddressDao addressDao;

	@Resource
	ScanHistoryDao scanHistoryDao;

	@Resource
	OpenPortsDao openPortsDao;

	Logger logger = LoggerFactory.getLogger(AppService.class);
	
	List<com.crowdStrike.engineering.model.Port> currentPortsList = new ArrayList<>();
	List<com.crowdStrike.engineering.model.Port> prevPortsList = new ArrayList<>();

	public boolean isValidAddress(String address) {
		if (!InternetDomainName.isValid(address) && !InetAddresses.isInetAddress(address)) {
			throw new InvalidInputException("Invalid input. Please enter a valid ip address or host name.");
		}
		return true;
	}

	public List<PortDisplayDTO> getOpenPorts(String address) {
		Nmap4j nmap4j = new Nmap4j("/usr/local");
		nmap4j.includeHosts(address);
		nmap4j.addFlags("--open");
		Nmaprun nmapOutput = null;
		try {
			nmap4j.execute();
			if (!nmap4j.hasError()) {
				String nmapRun = nmap4j.getOutput();
				JAXBContext jaxbContext = JAXBContext.newInstance(Nmaprun.class);
				Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
				nmapOutput = (Nmaprun) jaxbUnmarshaller.unmarshal(new StringReader(nmapRun));
				System.out.println(nmapOutput);
			} else {
				System.out.println(nmap4j.getExecutionResults().getErrors());
			}
		} catch (Exception e) {
			throw new NMapException("Error while running NMap.");
		}
		List<Port> portList = new ArrayList<Port>();
		if (nmapOutput.getRunstats().getHosts().getUp() == 0) {
			throw new NMapException("Host is down. No open ports");
		}
		if (nmapOutput.getHost().getPorts().getPort() != null && !nmapOutput.getHost().getPorts().getPort().isEmpty()) {
			for (Port port : nmapOutput.getHost().getPorts().getPort()) {
				if (port.getPortid() >= 0 && port.getPortid() <= 1000) {
					portList.add(port);
				}
			}
			persistToDB(address, portList);
			return generateDisplayDTO(portList);
		} else {
			throw new NMapException("No open ports");  
		}	
	}

	@Transactional
	public void persistToDB(String address, List<Port> portList) {
		currentPortsList = new ArrayList<>();
		logger.info("Persisting to database");
		Address addressObj = addressDao.findByAddress(address);
		if (addressObj == null) {
			addressObj = new Address(address);
			addressDao.save(addressObj);
		}
		int addressInput = addressObj.getAddressId();
		Integer lastscanId = scanHistoryDao.findLastScan(addressInput);
		int nextScanId = 1;
		if (lastscanId != null) {
			nextScanId = lastscanId + 1;
			generatePrevPortsList(lastscanId, addressObj);
		}
		for (Port port : portList) {
			com.crowdStrike.engineering.model.Port openPort = openPortsDao.findPort(port.getPortid(),
					port.getProtocol(), port.getService().getName());
			if (openPort == null) {
				openPort = new com.crowdStrike.engineering.model.Port(port.getPortid(), port.getProtocol(),
						port.getService().getName());
				openPortsDao.save(openPort);
			}
			currentPortsList.add(openPort);
			Scans scanHistory = new Scans(nextScanId);
			scanHistory.setPort(openPort);
			scanHistory.setAddress(addressObj);
			scanHistoryDao.save(scanHistory);
		}
	}

	public List<PortDisplayDTO> generateDisplayDTO(List<Port> portList) {
		logger.info("Generating output for current scan");
		List<PortDisplayDTO> portListDisplay = new ArrayList<PortDisplayDTO>();
		PortDisplayDTO portDisplayDTO = null;

		if (portList != null && !portList.isEmpty()) {
			for (Port port : portList) {
				if (port.getState().getState().equals("open")) {
					portDisplayDTO = new PortDisplayDTO();
					portDisplayDTO.setPortId(port.getPortid());
					portDisplayDTO.setProtocol(port.getProtocol());
					portDisplayDTO.setService(port.getService().getName());
					portListDisplay.add(portDisplayDTO);
				}
			}
		}
		return portListDisplay;
	}
	
	public List<Map<String,List<PortDisplayDTO>>> generatePreviousScans(String address) {
		logger.info("Getting previous history");
		List<Map<String,List<PortDisplayDTO>>> previousScans = new ArrayList<>();
		PortDisplayDTO portDisplayDTO = null;
		Address addressObj = addressDao.findByAddress(address);
		if (addressObj != null) {
			int addressInput = addressObj.getAddressId();
			List<Integer> scanIdList = scanHistoryDao.getAllScanId(addressInput);
			for (int scanId : scanIdList) {
				List<Scans> scanList = scanHistoryDao.getAllScans(scanId, addressObj);
				List<PortDisplayDTO> previousScan = new ArrayList<PortDisplayDTO>();
				for (Scans scan : scanList) {
					com.crowdStrike.engineering.model.Port openPort = scan.getPort();
					portDisplayDTO = new PortDisplayDTO();
					portDisplayDTO.setPortId(openPort.getPortNumber());
					portDisplayDTO.setProtocol(openPort.getProtocol());
					portDisplayDTO.setService(openPort.getService());
					previousScan.add(portDisplayDTO);
				}
				Map<String,List<PortDisplayDTO>> map = new HashMap<>();
				map.put(scanList.get(0).getAddedDate().toString(),previousScan);
				previousScans.add(map);
			}
		}
		return previousScans;
	}
	
	public void generatePrevPortsList(int lastscanId, Address address){
		prevPortsList = new ArrayList<>();
		List<Scans> scanList = scanHistoryDao.getAllScans(lastscanId, address);
		for (Scans scan : scanList) {		
			prevPortsList.add(scan.getPort());
		}
	}		
	
	public List<PortDisplayDTO> getNewPortsList(){		
		List<PortDisplayDTO> newPortsDisplayDTO = new ArrayList<PortDisplayDTO>();
		ArrayList<com.crowdStrike.engineering.model.Port> newPortsList = new ArrayList<>(currentPortsList);
		newPortsList.removeAll(prevPortsList);	
		for (com.crowdStrike.engineering.model.Port newPort : newPortsList) {
			PortDisplayDTO portDisplayDTO = new PortDisplayDTO();
			portDisplayDTO.setPortId(newPort.getPortNumber());
			portDisplayDTO.setProtocol(newPort.getProtocol());
			portDisplayDTO.setService(newPort.getService());
			newPortsDisplayDTO.add(portDisplayDTO);
		}
		return newPortsDisplayDTO;
	}
	
	public List<PortDisplayDTO> getRemovedPortsList(){
		List<PortDisplayDTO> removedDisplayDTO = new ArrayList<PortDisplayDTO>();
		prevPortsList.removeAll(currentPortsList);
		for (com.crowdStrike.engineering.model.Port newPort : prevPortsList) {
			PortDisplayDTO portDisplayDTO = new PortDisplayDTO();
			portDisplayDTO.setPortId(newPort.getPortNumber());
			portDisplayDTO.setProtocol(newPort.getProtocol());
			portDisplayDTO.setService(newPort.getService());
			removedDisplayDTO.add(portDisplayDTO);
		}
		return removedDisplayDTO;
	}
	
}