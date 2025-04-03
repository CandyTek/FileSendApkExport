package com.xiaoming.desktop.filereceiver.file_receiver;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

public class UdpThread extends Thread {

	public static DatagramSocket datagramSocket;

	private boolean workFlag = true;
	//private final int portNumber;
	//private DatagramPacket datagramPacket=new DatagramPacket(new byte[65500],65500);
	private final DatagramPacket datagramPacket = new DatagramPacket(new byte[65500],65500);

	private final UdpThreadCallback callback;

	public UdpThread(@Nullable UdpThreadCallback callback) throws Exception {
		super();
		this.callback = callback;
		//portNumber= SPUtil.getPortNumber(context);
		//openUdpSocket(portNumber);

		openUdpSocket(Tools.getPortNumber());

	}

	@Override
	public void run() {
		super.run();
		while (workFlag) {
			if (datagramSocket == null) {
				workFlag = false;
				return;
			}
			try {
				datagramSocket.receive(datagramPacket);
				String ipMsgStr = new String(datagramPacket.getData(),0,datagramPacket.getLength());
				System.err.println("----UDPReceived" + ipMsgStr + " fromIP:" + datagramPacket.getAddress()
						.getHostName() + " fromPost:" + datagramPacket.getPort());
				final IpMessage ipMessage = new IpMessage(ipMsgStr);
				if (callback != null) {
					callback.onIpMessageReceived(datagramPacket.getAddress().getHostAddress(),datagramPacket.getPort(),ipMessage);
				}
				datagramPacket.setLength(65500);
			}
			catch (SocketTimeoutException e) {
				// Tools.logError(e);
				// e.printStackTrace();
				// workFlag=false;
				// return;
			}
			catch (Exception e) {
				Tools.logError(e);
				e.printStackTrace();
				workFlag = false;
				return;
			}
		}
	}

	public DatagramSocket getDatagramSocket() {
		return datagramSocket;
	}

	public void stopUdp() {
		workFlag = false;
		interrupt();
		closeUdpSocket();
	}

	private static void openUdpSocket(int portNumber) throws Exception {
        if (datagramSocket != null) {
            datagramSocket.close();
        }
		datagramSocket = new DatagramSocket(portNumber);
		// datagramSocket.setSoTimeout(0);
		// datagramSocket.setSoTimeout(1000);
		// datagramSocket.setBroadcast(true);
	}

	public static void closeUdpSocket() {
        if (datagramSocket != null) {
            datagramSocket.close();
        }
		datagramSocket = null;
	}

	public interface UdpThreadCallback {
		void onIpMessageReceived(@NotNull String ip,int port,@NotNull IpMessage ipMessage);
	}

}
