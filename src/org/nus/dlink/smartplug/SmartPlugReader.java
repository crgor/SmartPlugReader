package org.nus.dlink.smartplug;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.jfree.util.Log;
import org.opennms.netmgt.dao.api.SPlugDao;
import org.opennms.netmgt.model.SPlug;
import org.opennms.netmgt.dao.api.IpInterfaceDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opennms.core.spring.BeanUtils;

import javax.xml.soap.*;
import javax.xml.transform.*;
import javax.xml.transform.stream.*;

import org.springframework.beans.factory.access.BeanFactoryReference;

public class SmartPlugReader implements Runnable {
	
	private static final String RESULT = "Result";
	private static final String GET = "Get";
	private static final String HNAP_TEMP_METHOD = "CurrentTemperature";
	private static final String HNAP_POWER_METHOD = "CurrentPowerConsumption";
	private static final String HNAP_POWER_METHOD_RESP = "CurrentConsumption";
	private static final String HNAP_TOTAL_POWER_METHOD_RESP = "TotalConsumption";
	private static final String HNAP_TOTAL_POWER_METHOD = "GetPMWarningThreshold";
	private static final String HNAP_SOCKET_METHOD = "SetSocketSettings";
	private static final String HNAP_DEVICE_METHOD = "GetDeviceSettings";
	private static final String MACID_ELEM = "DeviceMacId";
	
	private static final String HANP_REBOOT_METHOD = "Reboot";
	
	private static final String HTTP_TAG = "http://";
	private static final String HNAP_TAG = "/HNAP1";
	private static final String LOGIN_USER = "admin";
	
	private static final Logger m_log = LoggerFactory
			.getLogger(SmartPlugReader.class);
	
	private BeanFactoryReference bf;
	private IpInterfaceDao m_ipInterfaceDao;
	private SPlugDao m_sPlugDao;
	
	private boolean m_init = false;
	
	private static final String fileReboot = "D:\\Reddy\\DevReboot.txt";
	BufferedWriter writer;
	
	private void initialise(){
		m_log.debug("Initialising beans for SmartPlugReader");
		if (bf == null){
			bf = BeanUtils.getBeanFactory("daoContext");
		}
		if (m_ipInterfaceDao == null){
			m_ipInterfaceDao = BeanUtils.getBean(bf, "ipInterfaceDao",
					IpInterfaceDao.class);
		}
		if (m_sPlugDao == null){
			m_sPlugDao = BeanUtils.getBean(bf, "sPlugDao",
					SPlugDao.class);
		}
		
		if ((m_ipInterfaceDao != null) && (m_sPlugDao != null)){
			m_init = true;
		}
		
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		m_log.debug("Starting SmartPlugReader ");
		SoapClient cl = new SoapClient();
		SaveDataToDatabase svData = new SaveDataToDatabase();
		HashMap<String, Boolean> restarted = null;
		HashMap<String, Boolean> loggedIn = new HashMap<String, Boolean>();
		Boolean restartTime = true;
		try {
			writer = new BufferedWriter(new FileWriter(fileReboot, true));
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		while (true){
			long startTime = System.currentTimeMillis();
			try {
				if (m_init){
					Map<InetAddress, Integer> ifaces = m_ipInterfaceDao.getInterfacesForNodes();
					Calendar rightNow = Calendar.getInstance();
					/*if (rightNow.get(Calendar.HOUR_OF_DAY) == 0){
						if (restarted == null){
							restarted = new HashMap<String, Boolean>();	
						}
						restartTime = true;
					}else {
						restartTime = false;
						if(restarted != null){
							Iterator<String> it = restarted.keySet().iterator();
				            writer.write("--------------------------------\n");
				            writer.write((new Date()).toString() + "\n");
					        while (it.hasNext()) {
					            String line = it.next();
					            String entryLine = line + " -> " + restarted.get(line) + "\n";
					            writer.write(entryLine);
					            writer.flush();
					            //writer.close();
					        }
						}
						restarted = null;
					}*/
					for (Map.Entry<InetAddress, Integer> entry : ifaces.entrySet()) {
						String ipadd = entry.getKey().getHostAddress();
						String url = HTTP_TAG + ipadd + HNAP_TAG;
						String macaddr = cl.getMacAddress(url).toLowerCase();
						if ((macaddr == null) || (macaddr.equals(""))){
							m_log.debug("NULL Mac Address for " + entry.getKey().getHostAddress());
							continue;
						}
						SPlug plug = m_sPlugDao.findByMac(macaddr);
						if (plug == null){
							m_log.debug("No Plug found for " + macaddr);
							continue;
						}
						if (!loggedIn.containsKey(ipadd)){
							if (cl.ClientLogin(LOGIN_USER, plug.getPinCode(), url)){
								loggedIn.put(ipadd, true);
							}else {
								m_log.debug("PLUG Login failed for " + ipadd);
							}
						}
						if (loggedIn.containsKey(ipadd)){
							try {
								cl.SOAPAction(HNAP_SOCKET_METHOD, HNAP_SOCKET_METHOD + RESULT, cl.getControlParameters("1", "true"));
								String power= cl.SOAPAction(GET + HNAP_POWER_METHOD, HNAP_POWER_METHOD_RESP, cl.getModuleParameters("2"));
								//System.out.println("Active Power " + power);
								String temp= cl.SOAPAction(GET + HNAP_TEMP_METHOD, HNAP_TEMP_METHOD, cl.getModuleParameters("2"));
								//System.out.println("Temperature " + temp);
								String total= cl.SOAPAction(HNAP_TOTAL_POWER_METHOD, HNAP_TOTAL_POWER_METHOD_RESP, cl.getModuleParameters("2"));
								//System.out.println(total);
								//svData.savePlugData(macaddr, Float.parseFloat(power), Float.parseFloat(total), Integer.parseInt(temp));
								m_log.debug("PLUG " + macaddr + " " + power  + " " + temp + " " + total);
								if (restartTime){
									if (!power.isEmpty()){
										if (Double.parseDouble(power) < 5){
											if (!restarted.containsKey(ipadd)){
												// Reboot
												String reboot = cl.SOAPAction(HANP_REBOOT_METHOD, HANP_REBOOT_METHOD + RESULT, null);
												if ((!reboot.isEmpty()) && (reboot.equals("OK"))){
													restarted.put(ipadd, true);
													//m_log.debug("PLUG " + ipadd + " rebooted");
												}												
											}
										}	
									}
								}
								
							} catch (Exception e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					}
				}else {
					initialise();
				}
			}catch (Exception e){
				e.printStackTrace();
			}

			long stopTime = System.currentTimeMillis();
			long execTime = stopTime - startTime;
			if (execTime < 5000){
				try {
					Thread.sleep(5000 - execTime);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

}
