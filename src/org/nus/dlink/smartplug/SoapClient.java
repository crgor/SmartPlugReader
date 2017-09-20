package org.nus.dlink.smartplug;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.soap.MessageFactory;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPConnectionFactory;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SoapClient {
	
	private static final String HNAP1_XMLNS = "http://purenetworks.com/HNAP1/";
	private static final String HNAP_METHOD = "POST";
	private static final String HNAP_BODY_ENCODING = "UTF8";
	private static final String HNAP_LOGIN_METHOD = "Login";
	
	private static final String RESULT = "Result";
	private static final String GET = "Get";
	private static final String HNAP_TEMP_METHOD = "CurrentTemperature";
	private static final String HNAP_POWER_METHOD = "CurrentPowerConsumption";
	private static final String HNAP_TOTAL_POWER_METHOD = "TotalConsumption";
	private static final String HNAP_DEVICE_METHOD = "GetDeviceSettings";
	private static final String MACID_ELEM = "DeviceMacId";
	
	private static final Logger m_log = LoggerFactory.getLogger(SoapClient.class);
	
	private class HNAPAUTH{
		public String URL; 
		public String User; 
		public String Pwd; 
		public String Result; 
		public String Challenge; 
		public String PublicKey; 
		public String Cookie; 
		public String PrivateKey;
		
	}
	
	private HNAPAUTH m_HNAPAUTH = new HNAPAUTH();
	private SOAPConnectionFactory soapConnectionFactory;
	private SOAPConnection soapConnection;
	
	public SoapClient(){
	    try {
			soapConnectionFactory = SOAPConnectionFactory.newInstance();
		    soapConnection = soapConnectionFactory.createConnection();
		} catch (UnsupportedOperationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SOAPException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private SOAPMessage soapCall(SOAPMessage message, String url){
		try
        {
			URL endpoint = new URL(null, 
					url, 
                new URLStreamHandler() {
                   @Override
                   protected URLConnection openConnection(URL url1) throws IOException {
                      URL clone_url = new URL(url1.toString());
                      HttpURLConnection clone_urlconnection = (HttpURLConnection) clone_url.openConnection();
                      clone_urlconnection.setConnectTimeout(5000);
                      clone_urlconnection.setReadTimeout(5000);
                      return(clone_urlconnection); 
                   }
            });
             // Create SOAP Connection
            SOAPMessage soapResponse = soapConnection.call(message, endpoint);
            return soapResponse;
        }
        catch (Exception e)
        {
                System.err.println("Error occurred while sending SOAP Request to Server");
                e.printStackTrace();
        }
		
		return null;
	}
	
	private HashMap<String, String> getLoginReqParams(){
		HashMap<String, String> loginParams = new HashMap<String, String>() {{
		    put("Action", "request");
		    put("Username", m_HNAPAUTH.User);
		    put("LoginPassword", "");
		    put("Captcha", "");
		}};
		
		return loginParams;
	}
	
	public HashMap<String, String> getModuleParameters(String module) {
		HashMap<String, String> moduleParams = new HashMap<String, String>() {{
		    put("ModuleID", module);
		}};
		
		return moduleParams;
	}
	
	public HashMap<String, String> getControlParameters(String module, String status) {
		HashMap<String, String> controlParams = getModuleParameters(module);
		controlParams.put("NickName", "Socket 1");
		controlParams.put("Description", "Socket 1");
		controlParams.put("OPStatus", status);
		controlParams.put("Controller", "1");
		return controlParams;
	}

	public HashMap<String, String> getRadioParameters(String radio) {
		HashMap<String, String> radioParams = new HashMap<String, String>() {{
		    put("RadioID", radio);
		}};
		
		return radioParams;
	}
	
	private HashMap<String, String> getLoginParams(){
		String login_pwd = HmacMd5.hmacDigest(m_HNAPAUTH.Challenge, m_HNAPAUTH.PrivateKey);
		HashMap<String, String> loginParams = new HashMap<String, String>() {{
		    put("Action", "login");
		    put("Username", m_HNAPAUTH.User);
		    put("LoginPassword", login_pwd.toUpperCase());
		    put("Captcha", "");
		}};
		
		return loginParams;
	}
	
	private String getHnapAuthHeader(String SoapAction, String privateKey){
		if ((SoapAction != null) && (privateKey != null) && (!SoapAction.equals("")) && (!privateKey.equals(""))){
			Date current_time = new Date();
		    Integer time_stamp = Math.round(current_time.getTime() / 1000);
		    String auth = HmacMd5.hmacDigest(time_stamp + SoapAction, privateKey);
		    return auth.toUpperCase() + " " + time_stamp.toString();
		}
		return "";
	}
	
	private SOAPBody createSOAPBody(SOAPEnvelope envelope, String method, HashMap<String, String> params) throws Exception{
		SOAPBody soapBody = envelope.getBody();
        SOAPElement methodElem = soapBody.addChildElement(method);
        methodElem.setAttribute("xmlns", HNAP1_XMLNS);
        if (params != null){
            Iterator it = params.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, String> pair = (Map.Entry)it.next();
                //System.out.println(pair.getKey() + " = " + pair.getValue());
                SOAPElement paramElem = methodElem.addChildElement(pair.getKey());
                paramElem.addTextNode(pair.getValue());
                it.remove(); // avoids a ConcurrentModificationException
            }
        }
        return soapBody;
	}
	
	private SOAPMessage createSOAPLoginRequest() throws Exception{
		MessageFactory messageFactory = MessageFactory.newInstance();
        SOAPMessage soapMessage = messageFactory.createMessage();
        SOAPPart soapPart = soapMessage.getSOAPPart();
        /*
        Construct SOAP Request Message:
        <soap:Envelope xmlns:soap="http://www.w3.org/2003/05/soap-envelope" 
                xmlns:sam="http://samples.axis2.techdive.in">
           <soap:Header/>
           <soap:Body>
              <sam:getStudentName>
                 <!--Optional:-->
                 <sam:rollNumber>3</sam:rollNumber>
              </sam:getStudentName>
           </soap:Body>
        </soap:Envelope>
         */

        // SOAP Envelope
        SOAPEnvelope envelope = soapPart.getEnvelope();
        envelope.addNamespaceDeclaration("xsd", "http://www.w3.org/2001/XMLSchema");
        envelope.addNamespaceDeclaration("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        
        createSOAPBody(envelope, HNAP_LOGIN_METHOD, getLoginReqParams());
        SOAPHeader header = soapMessage.getSOAPHeader();
        header.detachNode();
        MimeHeaders headers = soapMessage.getMimeHeaders();
        headers.addHeader("Content-Type", "text/xml; charset=utf-8");
        headers.addHeader("SOAPAction",  "\"" + HNAP1_XMLNS + HNAP_LOGIN_METHOD + "\"");
        soapMessage.saveChanges();
        
        return soapMessage;
    }
	
	public String getMacAddress(String url){
		try {
			MessageFactory messageFactory = MessageFactory.newInstance();
	        SOAPMessage soapMessage = messageFactory.createMessage();
	        SOAPPart soapPart = soapMessage.getSOAPPart();
	        // SOAP Envelope
	        SOAPEnvelope envelope = soapPart.getEnvelope();
	        envelope.addNamespaceDeclaration("xsd", "http://www.w3.org/2001/XMLSchema");
	        envelope.addNamespaceDeclaration("xsi", "http://www.w3.org/2001/XMLSchema-instance");
	        createSOAPBody(envelope, HNAP_DEVICE_METHOD, null);
	        SOAPHeader header = soapMessage.getSOAPHeader();
	        header.detachNode();
	        MimeHeaders headers = soapMessage.getMimeHeaders();
	        headers.addHeader("Content-Type", "text/xml; charset=utf-8");
	        soapMessage.saveChanges();
	        
	        try{
	        	SOAPMessage resp = soapCall(soapMessage , url);
				SOAPBody body = resp.getSOAPBody();
				if ((body != null)){
					String elemValue = body.getElementsByTagName(MACID_ELEM).item(0).getTextContent();
					return (elemValue != "") ? elemValue : "ERROR";
				}
	        }catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}catch (Exception e){
			e.printStackTrace();
		}
        
        return "";
	}
	
	public String SOAPAction(String Method, String responseElement, HashMap<String, String> params) throws Exception{
		MessageFactory messageFactory = MessageFactory.newInstance();
        SOAPMessage soapMessage = messageFactory.createMessage();
        SOAPPart soapPart = soapMessage.getSOAPPart();
        /*
        Construct SOAP Request Message:
        <soap:Envelope xmlns:soap="http://www.w3.org/2003/05/soap-envelope" 
                xmlns:sam="http://samples.axis2.techdive.in">
           <soap:Header/>
           <soap:Body>
              <sam:getStudentName>
                 <!--Optional:-->
                 <sam:rollNumber>3</sam:rollNumber>
              </sam:getStudentName>
           </soap:Body>
        </soap:Envelope>
         */

        // SOAP Envelope
        SOAPEnvelope envelope = soapPart.getEnvelope();
        envelope.addNamespaceDeclaration("xsd", "http://www.w3.org/2001/XMLSchema");
        envelope.addNamespaceDeclaration("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        createSOAPBody(envelope, Method, params);
        SOAPHeader header = soapMessage.getSOAPHeader();
        header.detachNode();
        MimeHeaders headers = soapMessage.getMimeHeaders();
        headers.addHeader("Content-Type", "text/xml; charset=utf-8");
        headers.addHeader("SOAPAction",  "\"" + HNAP1_XMLNS + Method + "\"");
        headers.addHeader("HNAP_AUTH",  getHnapAuthHeader("\"" + HNAP1_XMLNS + Method + "\"", m_HNAPAUTH.PrivateKey));
        headers.addHeader("Cookie", "uid=" + m_HNAPAUTH.Cookie);

        
        soapMessage.saveChanges();
        
        try{
        	SOAPMessage resp = soapCall(soapMessage , m_HNAPAUTH.URL);
			SOAPBody body = resp.getSOAPBody();
			if ((body != null) && (responseElement != "")){
				String elemValue = body.getElementsByTagName(responseElement).item(0).getTextContent();
				return (elemValue != "") ? elemValue : "ERROR";
			}
        }catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        return "";
    }
	
	public void SOAPClose(){
		if (soapConnection != null){
			try {
				soapConnection.close();
			} catch (SOAPException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}	
		}		
	}
	
	public boolean ClientLogin(String userName, String userPass, String url){
		
		m_HNAPAUTH.User = userName;
		m_HNAPAUTH.Pwd = userPass;
		m_HNAPAUTH.URL = url;
		
		// Login Challenge request
		try {
			SOAPMessage resp = soapCall(createSOAPLoginRequest() , url);
			SOAPBody body = resp.getSOAPBody();
			m_HNAPAUTH.Result = body.getElementsByTagName(HNAP_LOGIN_METHOD + "Result").item(0).getTextContent();
			m_HNAPAUTH.Cookie = body.getElementsByTagName("Cookie").item(0).getTextContent();
			m_HNAPAUTH.Challenge = body.getElementsByTagName("Challenge").item(0).getTextContent();
			m_HNAPAUTH.PublicKey = body.getElementsByTagName("PublicKey").item(0).getTextContent();
			m_HNAPAUTH.PrivateKey = HmacMd5.hmacDigest(m_HNAPAUTH.Challenge , m_HNAPAUTH.PublicKey + m_HNAPAUTH.Pwd).toUpperCase();
			
			String loginRes = SOAPAction(HNAP_LOGIN_METHOD, HNAP_LOGIN_METHOD + RESULT, getLoginParams());
			
			if ((loginRes != null) && (loginRes.equals("success"))){
				return true;
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// Login Request
		
		return false;
	}
}
