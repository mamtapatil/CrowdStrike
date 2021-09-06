package com.crowdStrike.engineering.controller;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import com.crowdStrike.engineering.exception.InvalidInputException;
import com.crowdStrike.engineering.exception.NMapException;
import com.crowdStrike.engineering.model.PortDisplayDTO;
import com.crowdStrike.engineering.service.AppService;

@Controller
public class AppController {

	Logger logger = LoggerFactory.getLogger(AppController.class);
	
	@Resource
	AppService appService;
	
	@RequestMapping(path="/")
	public String home() {
		return "app";	
	}
	
	@RequestMapping(value="/getOpenPorts",method=RequestMethod.GET)
	public ModelAndView getOpenPorts(@RequestParam(value = "address", required = true) String address) throws Exception {
		ModelAndView mv = new ModelAndView();
		logger.info("Address provided " + address);
		String[] addressList = address.split(",");
		if (addressList.length > 1) {
			for (String addr: addressList) {
				appService.isValidAddress(addr.trim());
			}
			List<List<PortDisplayDTO>> multiplePortList = appService.getOpenPortsMultiple(addressList);
			if (!multiplePortList.contains(Collections.EMPTY_LIST) && !multiplePortList.contains(null)) {
				mv.getModel().put("multiplePortList", multiplePortList);
			}
		} else {
			appService.isValidAddress(address);
			List<Map<String,List<PortDisplayDTO>>> previousScans = appService.generatePreviousScans(address);
			if (previousScans != null && !previousScans.isEmpty()) {
				mv.getModel().put("previousScans", previousScans);
			}
			List<PortDisplayDTO> portList = appService.getOpenPorts(address);
			if (portList != null && !portList.isEmpty()) {
				mv.getModel().put("portList", portList);
			}
			List<PortDisplayDTO> newPortList = appService.getNewPortsList();
			if (newPortList != null && !newPortList.isEmpty()) {
				mv.getModel().put("newPortList", newPortList);
			}
			List<PortDisplayDTO> removedPortList = appService.getRemovedPortsList();
			if (removedPortList != null && !removedPortList.isEmpty()) {
				mv.getModel().put("removedPortList", removedPortList);
			}
		}
		mv.setViewName("app.html");
		return mv;	
	}
	
	/*@RequestMapping(value="/getOpenPortsMultiple",method=RequestMethod.GET)
	public ModelAndView getOpenPortsMultiple(@RequestParam(value = "address", required = true) String address) throws Exception {
		ModelAndView mv = new ModelAndView();
		logger.info("Address provided " + address);
		String[] addressList = address.split(",");
		for (String addr: addressList) {
			appService.isValidAddress(addr.trim());
		}
		List<List<PortDisplayDTO>> multiplePortList = appService.getOpenPortsMultiple(addressList);
		if (!multiplePortList.contains(Collections.EMPTY_LIST) && !multiplePortList.contains(null)) {
			mv.getModel().put("multiplePortList", multiplePortList);
		}
		mv.setViewName("app.html");
		return mv;	
	}*/
	
	@RequestMapping(value="/getPortHistory.json",method=RequestMethod.GET, produces = "application/json")
	@ResponseBody
	public ModelMap getPortHistory(@RequestParam(value = "address", required = true) String address) {
		ModelAndView mv = new ModelAndView();
		try {
			appService.isValidAddress(address);
		} catch (InvalidInputException ex) {
			mv.getModel().put("error", ex.getMessage());
			return mv.getModelMap();
		}
		List<Map<String,List<PortDisplayDTO>>> previousScans = appService.generatePreviousScans(address);
		if (previousScans != null && !previousScans.isEmpty()) {
			mv.getModel().put("data", previousScans);
		}
		return mv.getModelMap();	
	}
	
	@ExceptionHandler(value = InvalidInputException.class)
    public ModelAndView handleInvalidInputException(InvalidInputException ex) {
		ModelAndView mv = new ModelAndView();
		logger.info(ex.getMessage());
		mv.getModel().put("error", ex.getMessage());
		mv.setViewName("app.html");
		return mv;
    }
	
	@ExceptionHandler(value = NMapException.class)
    public ModelAndView handleNMapException(NMapException ex) {
		ModelAndView mv = new ModelAndView();
		logger.info(ex.getMessage());
		mv.getModel().put("error", ex.getMessage());
		mv.setViewName("app.html");
		return mv;
    }
	
	@ExceptionHandler(value = Exception.class)
    public ModelAndView handleException(Exception ex) {
		ModelAndView mv = new ModelAndView();
		logger.info(ex.getMessage());
		mv.getModel().put("error", "Something went wrong. Please try again." );
		mv.setViewName("app.html");
		return mv;
    }
}
