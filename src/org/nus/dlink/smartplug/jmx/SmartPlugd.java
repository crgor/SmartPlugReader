package org.nus.dlink.smartplug.jmx;

public class SmartPlugd implements SmartPlugdMBean {

    /**
     * <p>init</p>
     */
    public void init() {
    	org.nus.dlink.smartplug.SmartPlugd testd = org.nus.dlink.smartplug.SmartPlugd.getInstance();
        testd.init();
    }

    /**
     * <p>start</p>
     */
    public void start() {
    	org.nus.dlink.smartplug.SmartPlugd testd = org.nus.dlink.smartplug.SmartPlugd.getInstance();
        testd.start();
    }

    /**
     * <p>stop</p>
     */
    public void stop() {
    	org.nus.dlink.smartplug.SmartPlugd testd = org.nus.dlink.smartplug.SmartPlugd.getInstance();
        testd.stop();
    }

    /**
     * <p>getStatus</p>
     *
     * @return a int.
     */
    public int getStatus() {
    	org.nus.dlink.smartplug.SmartPlugd testd = org.nus.dlink.smartplug.SmartPlugd.getInstance();
        return testd.getStatus();
    }

    /**
     * <p>status</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String status() {
        return "status";
    }

    /**
     * <p>getStatusText</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getStatusText() {
        return "status";
    }
}
