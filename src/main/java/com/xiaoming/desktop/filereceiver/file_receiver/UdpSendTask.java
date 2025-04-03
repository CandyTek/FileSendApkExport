package com.xiaoming.desktop.filereceiver.file_receiver;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.DatagramPacket;
import java.net.InetAddress;

public class UdpSendTask extends Thread {
	private String ipMsgStr;
	private InetAddress targetInetAddress;
	private final int targetPort;
	private final UdpSendTaskCallback callback;
	public UdpSendTask(@NotNull String ipMsgStr,@NotNull InetAddress targetAddress,int targetPort,@Nullable UdpSendTaskCallback callback) {
		this.ipMsgStr = ipMsgStr;
		this.targetInetAddress = targetAddress;
		this.targetPort = targetPort;
		this.callback = callback;
	}

	@Override
	public void run() {
		try {
			if (UdpThread.datagramSocket == null || UdpThread.datagramSocket.isClosed()) {
				return;
			}
			byte[] sendBuffer = ipMsgStr.getBytes();
			DatagramPacket datagramPacket = new DatagramPacket(sendBuffer,0,sendBuffer.length,targetInetAddress,targetPort);
			UdpThread.datagramSocket.send(datagramPacket);
			System.err.println("UDPSent" + ipMsgStr + "  targetIP:" + datagramPacket.getAddress()
					.getHostName() + " targetPort:" + datagramPacket.getPort());
		}
		catch (Exception e) {
			Tools.logError(e);
			e.printStackTrace();
		}
		try {
			IpMessage ipMessage = new IpMessage(ipMsgStr);
			if (callback != null) {
				callback.onUdpSentCompleted(ipMessage);
			}
		}
		catch (Exception e) {
			Tools.logError(e);
			e.printStackTrace();
		}
	}

	public interface UdpSendTaskCallback {
		void onUdpSentCompleted(IpMessage ipMessage);
	}
}
