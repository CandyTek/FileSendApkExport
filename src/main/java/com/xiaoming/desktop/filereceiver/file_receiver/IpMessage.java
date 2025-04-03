package com.xiaoming.desktop.filereceiver.file_receiver;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class IpMessage {
	private String version;
	private String packageNumber;
	private String deviceName = "PREF_DEVICE_NAME_DEFAULT";
	private int command = -1;
	private String additionalMessage = "null";

	public IpMessage() {
		version = String.valueOf(IpMessageConstants.VERSION);
		packageNumber = String.valueOf(System.currentTimeMillis());
	}

	public IpMessage(String protocolString) {
		String[] args = protocolString.split(":");
		version = args[0];
		packageNumber = args[1];
		deviceName = args[2];
		command = Integer.parseInt(args[3]);
		if (args.length >= 5) {
			additionalMessage = args[4];
		}
		for (int i = 5;i < args.length;i++) {
			additionalMessage += ":" + args[i];
		}
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getDeviceName() {
		return deviceName;
	}

	public void setDeviceName(String deviceName) {
		this.deviceName = deviceName;
	}

	public int getCommand() {
		return command;
	}

	public void setCommand(int command) {
		this.command = command;
	}

	public String getAdditionalMessage() {
		return additionalMessage;
	}

	public void setAdditionalMessage(String additionalMessage) {
		this.additionalMessage = additionalMessage;
	}

	public String getPackageNumber() {
		return packageNumber;
	}

	public String toProtocolString() {
		StringBuilder builder = new StringBuilder();
		builder.append(version);
		builder.append(":");
		builder.append(packageNumber);
		builder.append(":");
		builder.append(deviceName);
		builder.append(":");
		builder.append(command);
		builder.append(":");
		builder.append(additionalMessage);
		return builder.toString();
	}

	@Override
	@NotNull
	public String toString() {
		return toProtocolString();
	}

	/**
	 * 通过FileItem的list集合获取一个发送文件请求的IpMessage
	 *
	 * @param sendFiles 要发送的文件
	 */
	@Nullable
	public static IpMessage getSendingFileRequestIpMessgae(@NotNull List<FileItem> sendFiles) {
		try {
			StringBuilder ipMsg_addtional = new StringBuilder();
			for (int i = 0;i < sendFiles.size();i++) {
				StringBuilder ipMsg_fileInfo = new StringBuilder();
				FileItem fileItem = sendFiles.get(i);
				ipMsg_fileInfo.append(fileItem.getName());
				ipMsg_fileInfo.append(Tools.fileInfoSplit());
				ipMsg_fileInfo.append(fileItem.length());
				ipMsg_addtional.append(ipMsg_fileInfo.toString());
                if (i < sendFiles.size() - 1) {
                    ipMsg_addtional.append(":");
                }
			}

			IpMessage ipMessage = new IpMessage();
			ipMessage.setCommand(IpMessageConstants.MSG_SEND_FILE_REQUEST);
			ipMessage.setDeviceName(Tools.getUserName());
			ipMessage.setAdditionalMessage(ipMsg_addtional.toString());
			return ipMessage;
		}
		catch (Exception e) {
			Tools.logError(e);
			e.printStackTrace();
		}
		return null;
	}
}
