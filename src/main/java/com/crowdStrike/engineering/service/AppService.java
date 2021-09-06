package com.crowdStrike.engineering.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.crowdStrike.engineering.controller.AppController;
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
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.nmap4j.Nmap4j;
import org.nmap4j.core.flags.ArgumentProperties;
import org.nmap4j.core.nmap.ExecutionResults;
import org.nmap4j.core.nmap.NMapExecutionException;
import org.nmap4j.core.nmap.NMapExecutor;
import org.nmap4j.core.nmap.NMapInitializationException;
import org.nmap4j.core.nmap.NMapProperties;
import org.nmap4j.data.NMapRun;
import org.nmap4j.data.nmaprun.Host;
import org.nmap4j.parser.NMapRunHandlerImpl;
import org.nmap4j.parser.NMapXmlHandler;
import org.nmap4j.parser.OnePassParser;
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
	
	public boolean isValidAddress(String address) {
		if (!InternetDomainName.isValid(address) && !InetAddresses.isInetAddress(address)) {
			throw new InvalidInputException("Invalid input. Please enter a valid ip address or hots name.");
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
		logger.info("Persisting to database");
		Address addressObj = addressDao.findByAddress(address);
		if (addressObj == null) {
			addressObj = new Address(address);
			addressDao.save(addressObj);
		}
		int addressInput = addressObj.getAddressId();
		Integer scanId = scanHistoryDao.findLastScan(addressInput);
		int nextScanId = 1;
		if (scanId != null) {
			nextScanId = scanId + 1;
		}
		for (Port port : portList) {
			com.crowdStrike.engineering.model.Port openPort = openPortsDao.findPort(port.getPortid(),
					port.getProtocol(), port.getService().getName());
			if (openPort == null) {
				openPort = new com.crowdStrike.engineering.model.Port(port.getPortid(), port.getProtocol(),
						port.getService().getName());
				openPortsDao.save(openPort);
			}
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
	
	public List<List<PortDisplayDTO>> generatePreviousScans(String address) {
		logger.info("Getting previous history");
		List<List<PortDisplayDTO>> previousScans = new ArrayList<>();
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
				previousScans.add(previousScan);
			}
		}
		return previousScans;
	}
}
/*
 * //NMapRun nmapRun1 = nmap4j.getResult() ; OnePassParser opp = new
 * OnePassParser() ; //NMapRun nmapRun1 = opp.parse( nmapRun,
 * OnePassParser.STRING_INPUT ) ; //ArrayList<Host> hosts=nmapRun1.getHosts();
 * //System.out.println( hosts.get(0).getAddress().getVendor() ) ;
 * //OnePassParser parser = new OnePassParser(); //NMapRun nmapRun1 =
 * parser.parse(nmap4j.getExecutionResults().getOutput(),
 * OnePassParser.STRING_INPUT);
 * 
 * //ArrayList<Host> hosts=nmapRun.getHosts();
 * 
 * StringReader strReader = new StringReader( nmapRun ) ;
 * 
 * InputSource source = new InputSource (strReader ) ; SAXParserFactory spf =
 * SAXParserFactory.newInstance(); SAXParser sp; try { sp = spf.newSAXParser();
 * 
 * NMapRunHandlerImpl nmrh = new NMapRunHandlerImpl() ; NMapXmlHandler nmxh =
 * new NMapXmlHandler( nmrh ) ;
 * 
 * sp.parse( source, nmxh ); } catch (SAXException | IOException e) { // TODO
 * Auto-generated catch block e.printStackTrace(); }catch
 * (ParserConfigurationException e) { // TODO Auto-generated catch block
 * e.printStackTrace(); }
 */

// OnePassParser opp = new OnePassParser() ;
// NMapRun nmapRun11 = opp.parse( nmapRun, OnePassParser.STRING_INPUT ) ;
// ArrayList<Host> hosts=nmapRun11.getHosts();
// System.out.println( hosts.get(0).getAddress().getVendor() ) ;
// System.out.println( hosts.get(0).getAddress().getVendor() ) ;
/*
 * Nmap4j nmap4j = new Nmap4j( "/usr/local" ) ;
 * nmap4j.includeHosts("192.168.1.1" ); nmap4j.addFlags( "-T3 -oX - -O -sV" );
 * nmap4j.execute(); if( !nmap4j.hasError()) { NMapRun nmapRun =
 * nmap4j.getResult() ; } else { System.out.println(
 * nmap4j.getExecutionResults().getErrors() ) ; }
 */