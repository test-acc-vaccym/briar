package net.sf.briar.plugins.bluetooth;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import net.sf.briar.api.ContactId;
import net.sf.briar.api.TransportConfig;
import net.sf.briar.api.TransportProperties;
import net.sf.briar.plugins.StreamClientTest;

// This is not a JUnit test - it has to be run manually while the server test
// is running on another machine
public class BluetoothClientTest extends StreamClientTest {

	private BluetoothClientTest(String serverAddress) {
		// Store the server's Bluetooth address and UUID
		TransportProperties p = new TransportProperties();
		p.put("address", serverAddress);
		p.put("uuid", BluetoothTest.UUID);
		Map<ContactId, TransportProperties> remote =
			Collections.singletonMap(contactId, p);
		// Create the plugin
		callback = new ClientCallback(new TransportConfig(),
				new TransportProperties(), remote);
		ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
		plugin = new BluetoothPlugin(executor, callback, 0L);
	}

	public static void main(String[] args) throws Exception {
		if(args.length != 1) {
			System.err.println("Please specify the server's Bluetooth address");
			System.exit(1);
		}
		new BluetoothClientTest(args[0]).run();
	}
}
