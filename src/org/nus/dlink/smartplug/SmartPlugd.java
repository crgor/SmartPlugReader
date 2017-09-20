package org.nus.dlink.smartplug;

import org.opennms.netmgt.daemon.AbstractServiceDaemon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SmartPlugd extends AbstractServiceDaemon {
	
	private Thread m_smartPlug_t;
	private static final Logger m_log = LoggerFactory.getLogger(SmartPlugd.class);
	
	private static final SmartPlugd m_singleton = new SmartPlugd();
	
	protected SmartPlugd() {
		super("OpenNms.SmartPlugd");
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void onInit() {
		// TODO Auto-generated method stub
		m_smartPlug_t = new Thread(new SmartPlugReader());
		m_log.info("SmartPlugd Initialized");
	}
	
	/**
     * <p>onStart</p>
     */
    protected void onStart() {	
    	m_smartPlug_t.start();
    	m_log.info("SmartPlugd started running");
    }
    
    /**
     * <p>onStop</p>
     */
    protected void onStop() {
    	try {
			if (m_smartPlug_t != null) {
				m_smartPlug_t.interrupt();
            }
    	}catch (Exception e) {
        }
    }
    
    /**
     * <p>onPause</p>
     */
    protected void onPause() {
		//m_execution.pause();
	}

    /**
     * <p>onResume</p>
     */
    protected void onResume() {
		//m_execution.resume();
	}

    /**
     * Returns the singular instance of the <em>Testd</em> daemon. There can
     * be only one instance of this service per virtual machine.
     *
     * @return The singular instance.
     */
    public static SmartPlugd getInstance() {
        return m_singleton;
    }
}
