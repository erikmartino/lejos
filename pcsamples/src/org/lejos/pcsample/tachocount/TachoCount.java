package org.lejos.pcsample.tachocount;
import lejos.nxt.Motor;
import lejos.nxt.Sound;
import lejos.nxt.remote.NXTComm;
import lejos.nxt.remote.NXTCommand;
import lejos.pc.comm.NXTCommLogListener;
import lejos.pc.comm.NXTCommandConnector;
import lejos.pc.comm.NXTConnector;

/**
 * Sample to spin motors and output Tachometer counts.
 * This sample shows how to control which NXT to connect to and switch on full logging.
 * 
 * @author Lawrie Griffiths and Brian Bagnall
 *
 */
public class TachoCount {
	
	public static void main(String [] args) throws Exception {
		NXTConnector conn = new NXTConnector();
		conn.addLogListener(new NXTCommLogListener() {
			public void logEvent(String message) {
				System.out.println(message);				
			}

			public void logEvent(Throwable throwable) {
				System.err.println(throwable.getMessage());			
			}			
		});
		conn.setDebug(true);
		if (!conn.connectTo("btspp://NXT", NXTComm.LCP)) {
			System.err.println("Failed to connect");
			System.exit(1);
		}
		NXTCommandConnector.setNXTCommand(new NXTCommand(conn.getNXTComm()));
		
		System.out.println("Tachometer A: " + Motor.A.getTachoCount());
		System.out.println("Tachometer C: " + Motor.C.getTachoCount());
		Motor.A.rotate(5000);
		Motor.C.rotate(-5000);
		Thread.sleep(10000);
		Sound.playTone(1000, 1000);
		System.out.println("Tachometer A: " + Motor.A.getTachoCount());
		System.out.println("Tachometer C: " + Motor.C.getTachoCount());
		conn.close();
	}	
}