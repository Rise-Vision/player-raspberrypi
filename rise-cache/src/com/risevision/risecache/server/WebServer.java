package com.risevision.risecache.server;

import java.io.*;

import java.net.*;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;

import com.risevision.risecache.Config;
import com.risevision.risecache.Globals;
import com.risevision.risecache.Log;

public class WebServer {

	/* static class data/methods */
	protected enum RequestType {
		UNKNOWN, CONTENT, CONFIGURATION
	};

	/* print to stdout */
	protected static void p(String s) {
		System.out.println(s);
	}

	/* print to the log file */
	protected static void log(String s) {
		if (log == null) {
			p("logging to stdout");
			log = System.out;
		}

		synchronized (log) {
			log.println(s);
			log.flush();
			Log.info(s); // write to log file
		}
	}

	static PrintStream log = null;
	/*
	 * our server's configuration information is stored in these properties
	 */
	protected static Properties props = new Properties();

	/* Where worker threads stand idle */
	static Vector<Worker> threads = new Vector<Worker>();

	/* timeout on client connections */
	static int timeout = 0;

	/* timeout on client connections */
	static Vector<Integer> ports = new Vector<>();

	/* max # worker threads */
	static int workers = Globals.MAX_CONNECTIONS_PER_PORT * Globals.NUMBER_OF_PORTS_TO_OPEN; //Chrome limits number of open connections to 6.

	static void printProps() {
		p("app folder=" + Config.appPath);
		p("timeout=" + timeout);
		p("workers=" + workers);
	}

	public static void main(String[] a) throws Exception {

		//TODO: move this to Main.main()
		printProps();
				
		/* start worker threads */
		for (int i = 0; i < workers; ++i) {
			Worker w = new Worker();
			(new Thread(w, "worker #" + i)).start();
			threads.addElement(w);
		}

		log("Server started");

		Selector selector = Selector.open();
		int portCounter = 0;

		for (int port = Config.basePort; port < Config.maxPort; port++) {
			try {
				ServerSocketChannel server = ServerSocketChannel.open();
				server.configureBlocking(false);
				server.socket().bind(new InetSocketAddress(InetAddress.getByName(null), port)); // specify localhost IP address to avoid Windows Firewall popup
				// we are only interested when accept evens occur on this socket
				server.register(selector, SelectionKey.OP_ACCEPT);
				ports.add(port);
				portCounter++;
				if (portCounter == Globals.NUMBER_OF_PORTS_TO_OPEN) {
					log("Opened ports: " + ports.toString());
					break;
				}
			} catch (Exception e) {
				if (port == Config.basePort) {
					Log.error("Cannot open master port " + port + ". Exiting application.");
					System.exit(0);
				}
			}
		}
		
		ServerPorts.init();

		while (selector.isOpen()) {
			selector.select(); //blocking
			Set<SelectionKey> readyKeys = selector.selectedKeys();
			Iterator<SelectionKey> iterator = readyKeys.iterator();
			while (iterator.hasNext()) {
				SelectionKey selectedKey = (SelectionKey) iterator.next();
				if (selectedKey.isAcceptable()) {
					SocketChannel client = ((ServerSocketChannel) selectedKey.channel()).accept();
					if (client != null) {
						Socket socket = client.socket();
						Worker w = null;
						synchronized (threads) {
							if (threads.isEmpty()) {
								Worker ws = new Worker();
								ws.setSocket(socket);
								(new Thread(ws, "additional worker")).start();
							} else {
								w = (Worker) threads.elementAt(0);
								threads.removeElementAt(0);
								w.setSocket(socket);
							}
						}
					}
				}
			}
		}
	}

	public static ServerSocket createServerSocket() throws Exception {
		//IMPORTANT! Specify local network address (InetAddress.getByName(null) is better option than just 127.0.0.1). 
		//           Otherwise 0.0.0.0 IP will be used which triggers Windows Firewall popup.
		//use default backlog value
		return new ServerSocket(Config.basePort, -1,  InetAddress.getByName(null));
	}

}
