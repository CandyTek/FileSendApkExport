package com.xiaoming.desktop.filereceiver.file_receiver;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.util.Duration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class NetReceiveTask implements UdpThread.UdpThreadCallback {

	private final UdpThread udpThread;
	private final String deviceName;

	private String senderIp = null;
	private int port = 0;
	private final ArrayList<ReceiveFileItem> receiveFileItems = new ArrayList<>();

	private final NetReceiveTaskCallback callback;

	private NetTcpFileReceiveTask netTcpFileReceiveTask;

	private final HashSet<String> knownIpList = new HashSet<>();
	private final boolean mBoolLogSimplify = true;

	public NetReceiveTask(NetReceiveTaskCallback callback) throws Exception {
		this.callback = callback;
		deviceName = Tools.getUserName();
		udpThread = new UdpThread(this);
		udpThread.start();
		//本机IP
		// String tempIp2 = EnvironmentUtil.getSelfIp(context);
		// postLogInfoToCallback(context.getResources().getString(R.string.receive_log_self_ip)+tempIp2,99);
		// String tempIp= FileReceiveActivity.getIPAddress(context);
		// if(tempIp!=null&&!tempIp.equals(tempIp2)) postLogInfoToCallback(context.getResources().getString(R.string.receive_log_self_ip)+tempIp,99);
		sendOnlineBroadcastUdp();
	}

	//██ 发送在线广播报文到 ██
	public void sendOnlineBroadcastUdp() {
		IpMessage ipMessage = new IpMessage();
		ipMessage.setDeviceName(deviceName);
		ipMessage.setCommand(IpMessageConstants.MSG_ONLINE_ANSWER);
		try {
			final int portNumber = Tools.getPortNumber();
			new UdpSendTask(ipMessage.toProtocolString(),InetAddress.getByName(Constants.BROADCAST_ADDRESS),
					portNumber,null).start();
			if (!mBoolLogSimplify) {
				postLogInfoToCallback("在线报文"
						+ Constants.BROADCAST_ADDRESS + ":" + portNumber,10);
			}
		}
		catch (UnknownHostException e) {
			Tools.logError(e);
			e.printStackTrace();
		}
	}

	/**
	 * 热点模式，如果连接的是对方手机热点，则尝试将上线报文发送至routerIp(对方手机地址)
	 *
	 * @param isApMode true 将发送上线报文至router地址
	 */
	public void switchApMode(boolean isApMode) {
		// this.broadcastAddress = isApMode ? Tools.getRouterIpAddress(context) : "255.255.255.255";
		// sendOnlineBroadcastUdp();
	}

	public void sendRefuseReceivingFilesUdp() {
		IpMessage ipMessage = new IpMessage();
		ipMessage.setCommand(IpMessageConstants.MSG_RECEIVE_FILE_REFUSE);
		try {
			//发送拒绝接收文件报文
			new UdpSendTask(ipMessage.toProtocolString(),InetAddress.getByName(this.senderIp),port,null).start();
			postLogInfoToCallback("发送拒绝接收文件报文到" + this.senderIp
					+ ":" + port,51);
		}
		catch (Exception e) {
			Tools.logError(e);
			e.printStackTrace();
		}
		senderIp = null;
	}

	public void sendStopReceivingFilesCommand() {
		IpMessage ipMessage = new IpMessage();
		ipMessage.setCommand(IpMessageConstants.MSG_FILE_TRANSFERRING_INTERRUPT);
		try {
			//发送停止接收文件报文到
			new UdpSendTask(ipMessage.toProtocolString(),InetAddress.getByName(this.senderIp),port,null).start();
			postLogInfoToCallback("发送停止接收文件报文到:" + this.senderIp
					+ ":" + port,12);
		}
		catch (Exception e) {
			Tools.logError(e);
			e.printStackTrace();
		}
		try {
			if (netTcpFileReceiveTask != null) {
				netTcpFileReceiveTask.setInterrupted();
			}
		}
		catch (Exception e) {
			Tools.logError(e);
			e.printStackTrace();
		}
		senderIp = null;
	}

	/**
	 * 这个是用来修复：某种情况下，设备发送下线报文无效的问题。
	 * 问题原因一：
	 * 某种情况下，发送端只能接收到固定发给自己ip的在线报文，除了发给自己ip的报文，像255.255..这种死活接收不到，所以这里需要处理
	 * 猜想：项目原来双方握手是使用发送255报文来解决的，这有几率在某种情况另一端接收不到，
	 * 后面为了增加双方握手的几率：就有了发送端发一个在线报文255，接收端立马根据此ip 发回一个固定ip的报文，增加了握手几率，虽然其他255报文一点效果都没有
	 * 下线报文因为没有做这种“加法”，导致某情况我下线了，对方还不知道。
	 * 问题原因二：
	 * 因为是在finish内发送下线保文的，所以又有几率，使自己发送的报文为“假报文”，即发送不成功，对面接收不到的报文
	 * 所以需要在在finish 稍稍前一点 单独发送下线报文，再做其他资源回收的处理。
	 * <p>
	 * 所有对比原项目修改的地方：
	 * 增加sendOfflineBroadcastUdpReal()方法
	 * 去掉原sendOfflineBroadcastUdp方法
	 * 增加一个knownIpList 用来记录所有已收到在线报文的设备ip
	 * 原方法sendOfflineBroadcastUdp callback不要用了
	 * 方法stopTask 直接回收资源，不要等cb了
	 * activity的finish方法，在super前调用 sendOfflineBroadcastUdpReal
	 */
	public void sendOfflineBroadcastUdpReal() {
		IpMessage ipMessage = new IpMessage();
		ipMessage.setCommand(IpMessageConstants.MSG_OFFLINE);
		knownIpList.add(Constants.BROADCAST_ADDRESS);
		for (String str : knownIpList) {
			try {
				new UdpSendTask(ipMessage.toProtocolString(),InetAddress.getByName(str),Tools.getPortNumber(),
						null).start();
				//发送下线报文到
				postLogInfoToCallback("发送下线报文到:" + str,53);
			}
			catch (Exception e) {
				Tools.logError(e);
				e.printStackTrace();
			}
		}
	}

	public void sendOfflineBroadcastUdp(UdpSendTask.UdpSendTaskCallback callback) {
		//IpMessage ipMessage=new IpMessage();
		//ipMessage.setCommand(IpMessageConstants.MSG_OFFLINE);
		//try{
		//    new UdpThread.UdpSendTask(ipMessage.toProtocolString(),InetAddress.getByName(broadcastAddress),SPUtil.getPortNumber(context),callback).start();
		//    //发送下线报文到
		//    postLogInfoToCallback(context.getResources().getString(R.string.receive_log_send_offline_broadcast)+broadcastAddress,53);
		//}catch (Exception e){e.printStackTrace();}

		//for(String str:knownIpList){
		//    try{
		//        new UdpThread.UdpSendTask(ipMessage.toProtocolString(),InetAddress.getByName(str),SPUtil.getPortNumber(context),callback).start();
		//        //发送下线报文到
		//        postLogInfoToCallback(context.getResources().getString(R.string.receive_log_send_offline_broadcast)+str,53);
		//    }catch (Exception e){e.printStackTrace();}
		//}
	}
	//██ 接收确认按钮 事件 ██
	public void startReceiveTask() {
		if (netTcpFileReceiveTask != null) {
			netTcpFileReceiveTask.setInterrupted();
		}
		netTcpFileReceiveTask = new NetTcpFileReceiveTask(senderIp,receiveFileItems);
		netTcpFileReceiveTask.start();
	}

	public void stopTask() {
		udpThread.stopUdp();
		//sendOfflineBroadcastUdp(new UdpThread.UdpSendTask.UdpSendTaskCallback() {
		//    @Override
		//    public void onUdpSentCompleted(IpMessage ipMessage) {
		//        //udpThread.stopUdp();
		//    }
		//});
	}

	@Override
	public void onIpMessageReceived(@NotNull final String senderIp,final int port,@NotNull final IpMessage ipMessage) {
		switch (ipMessage.getCommand()) {
			default:
				break;
			case IpMessageConstants.MSG_REQUEST_ONLINE_DEVICES: {
				//收到在线回复请求报文
				postLogInfoToCallback("收到在线回复请求报文:"
						+ "IP地址:" + senderIp + ":" + port,91);
				IpMessage ipMessage_answer = new IpMessage();
				ipMessage_answer.setDeviceName(deviceName);
				ipMessage_answer.setCommand(IpMessageConstants.MSG_ONLINE_ANSWER);
				knownIpList.add(senderIp);
				try {
					new UdpSendTask(ipMessage_answer.toProtocolString(),InetAddress.getByName(senderIp),port,null).start();
					//发送在线广播报文到
					if (!mBoolLogSimplify) {
						postLogInfoToCallback("在线报文" + senderIp
								+ ":" + port,10);
					}
				}
				catch (UnknownHostException e) {
					Tools.logError(e);
					e.printStackTrace();
				}

			}
			break;
			case IpMessageConstants.MSG_SEND_FILE_REQUEST: {
				//收到接收文件请求报文
				postLogInfoToCallback("收到接收文件请求报文:"
						+ "IP地址:" + senderIp
						+ ":" + port,94);
				if (this.senderIp != null) {
					return;
				}
				final List<ReceiveFileItem> receiveFileItems = new ArrayList<>();
				final String deviceName = String.valueOf(ipMessage.getDeviceName());
				try {
					String[] fileInfos = ipMessage.getAdditionalMessage().split(":");
					for (String fileInfo : fileInfos) {
						String[] singleFileInfos = fileInfo.split(Tools.fileInfoSplit());
						ReceiveFileItem receiveFileItem = new ReceiveFileItem();
						receiveFileItem.setFileName(singleFileInfos[0]);
						receiveFileItem.setLength(Long.parseLong(singleFileInfos[1]));
						receiveFileItems.add(receiveFileItem);
					}
					this.senderIp = senderIp;
					this.port = port;
					this.receiveFileItems.clear();
					this.receiveFileItems.addAll(receiveFileItems);
				}
				catch (Exception e) {
					Tools.logError(e);
					e.printStackTrace();
				}
				// 这里是收到对方的信息，打开 是否允许接收文件的对话框
				if (callback != null) {
					// Platform.runLater(() -> {
					callback.onFileReceiveRequest(NetReceiveTask.this,senderIp,deviceName,receiveFileItems);
					// });
				}
			}
			break;
			case IpMessageConstants.MSG_SEND_FILE_CANCELED: {
				//收到发送端取消发送文件报文
				postLogInfoToCallback("收到发送端取消发送文件报文:"
						+ "IP地址:" + senderIp
						+ ":" + port,55);
				if (senderIp.equals(this.senderIp)) {
					if (callback != null) {
						Platform.runLater(() -> {
							callback.onSendSiteCanceled(senderIp);
						});
					}
					this.senderIp = null;
				}

			}
			break;
			case IpMessageConstants.MSG_FILE_TRANSFERRING_INTERRUPT: {
				if (netTcpFileReceiveTask != null) {
					netTcpFileReceiveTask.setInterrupted();
				}
				this.senderIp = null;
				if (callback != null) {
					Platform.runLater(() -> {
						callback.onFileReceiveInterrupted();

					});
				}
				//收到发送端终止发送文件报文
				postLogInfoToCallback("收到发送端终止发送文件报文:"
						+ "IP地址:" + senderIp
						+ ":" + port,52);
			}
			break;
			case IpMessageConstants.MSG_FILE_TRANSFERRING_ERROR: {
				if (callback != null) {
					Platform.runLater(() -> {
						callback.onFileReceivedError();

					});
				}
				//收到错误
				postLogInfoToCallback("注意! 上个文件接收出现错误，请尝试重新发送!",12);
			}
			break;
		}
	}

	private void postLogInfoToCallback(final String logInfo,final int i) {
		if (callback != null) {
			Platform.runLater(() -> {
						callback.onLog(logInfo,i);
					}
			);
		}
	}

	public interface NetReceiveTaskCallback {
		void onFileReceiveRequest(@NotNull NetReceiveTask task,@NotNull String ip,@NotNull String deviceName,@NotNull List<ReceiveFileItem> fileItems);

		void onSendSiteCanceled(@NotNull String ip);

		void onFileReceiveStarted();

		void onFileReceiveInterrupted();

		void onFileReceiveProgress(long progress,long total,@NotNull String currentWritePath,long time);

		//void onSpeed(long speedOfBytes);
		void onFileReceivedCompleted(@NotNull String error_info);

		void onFileReceivedError();

		void onLog(String logInfo,int i);
	}

	private class NetTcpFileReceiveTask extends Thread {
		private boolean isInterrupted = false;
		private final ArrayList<ReceiveFileItem> receiveFileItems = new ArrayList<>();
		private final String targetIp;
		private Socket socket;
		private long total = 0;
		private long progress = 0, progressCheck = 0;
		//private long speedOfBytes=0;
		private long checkTime = System.currentTimeMillis();

		private FileItem currentWritingFileItem = null;
		private final StringBuilder error_info = new StringBuilder();

		private NetTcpFileReceiveTask(@NotNull String targetIp,List<ReceiveFileItem> receiveFileItems) {
			this.targetIp = targetIp;
			this.receiveFileItems.addAll(receiveFileItems);
		}

		@Override
		public void run() {
			super.run();
			for (ReceiveFileItem fileItem : receiveFileItems) {
				total += fileItem.Length();
			}

			for (int i = 0;i < receiveFileItems.size();i++) {
				if (isInterrupted) {
					return;
				}
				final ReceiveFileItem receiveFileItem = receiveFileItems.get(i);
				FileItem writingFileItemThisLoop = null;
				try {
					socket = new Socket(targetIp,Tools.getPortNumber());
					if (callback != null) {
						PauseTransition delay = new PauseTransition(Duration.millis(1)); // 最短1ms也行
						delay.setOnFinished(event -> {
							// 在 UI 线程中执行（下一帧）
							// label.setText("执行完成");
							// callback.onFileReceiveStarted();
						});
						delay.play();

						// Platform.runLater(() -> {
						// 	callback.onFileReceiveStarted();
						// });
					}
					InputStream inputStream = socket.getInputStream();
					OutputStream outputStream;

					final String initialFileName = receiveFileItem.getFileName();
					String fileName = initialFileName;

					File destinationFile = new File(Tools.getInternalSavePath() + File.separator + fileName);
					int count = 1;
					while (destinationFile.exists()) {
						fileName = Tools.getFileMainName(
								initialFileName) + "(" + (count++) + ")" + "." + Tools.getFileExtensionName(initialFileName);
						destinationFile = new File(Tools.getInternalSavePath() + File.separator + fileName);
					}
					outputStream = new FileOutputStream(destinationFile);
					writingFileItemThisLoop = new FileItem(destinationFile);
					currentWritingFileItem = writingFileItemThisLoop;

					final String fileNameOfMessage = fileName;
					if (callback != null) {
						Platform.runLater(() -> {
							callback.onFileReceiveProgress(progress,total,Tools.getDisplayingExportPath()
									+ File.separator + fileNameOfMessage,1000);

						});
					}
					try {
						//开始接收第#N个文件
						postLogInfoToCallback("开始接收第#N个文件，路径#P，长度#L"
								.replace("#N",String.valueOf(i + 1))
								.replace("#P",writingFileItemThisLoop.getPath())
								.replace("#L",Tools.getFormatSize(receiveFileItem.Length())),90);
					}
					catch (Exception e) {
						Tools.logError(e);
						e.printStackTrace();
					}
					byte[] buffer = new byte[1024];
					int length;
					while ((length = inputStream.read(buffer)) != -1 && !isInterrupted) {
						outputStream.write(buffer,0,length);
						progress += length;
						//speedOfBytes+=length;
						// 这里是调节 通知progress刷新进度的频率
						//if(progress-progressCheck>400*1024){
						if ((progress - progressCheck) > 2048000) {
							long currentTime = System.currentTimeMillis();
							long nowtime = currentTime - checkTime;
							checkTime = currentTime;
							progressCheck = progress;
							if (callback != null) {
								Platform.runLater(() -> {
									callback.onFileReceiveProgress(progress,total,Tools.getDisplayingExportPath()
											+ File.separator + fileNameOfMessage,nowtime);
								});
							}

						}
						//long currentTime=System.currentTimeMillis();
						//if(currentTime-checkTime>1000){
						//	checkTime=currentTime;
						//	final long speed=speedOfBytes;
						//	speedOfBytes=0;
						//	if(callback!=null)Global.handler.post(new Runnable() {
						//		@Override
						//		public void run() {
						//			callback.onSpeed(speed);
						//		}
						//	});
						//}
					}
					outputStream.flush();
					outputStream.close();
					if (!isInterrupted) {
						currentWritingFileItem = null;
					}
					inputStream.close();
				}
				catch (Exception e) {
					Tools.logError(e);
					if (writingFileItemThisLoop != null) {
						error_info.append(writingFileItemThisLoop.getPath());
					} else {
						error_info.append(receiveFileItem.getFileName());
					}
					error_info.append(" : ");
					error_info.append(e);
					error_info.append("\n\n");
					e.printStackTrace();
					try {
						//接收第#N个文件出现异常
						postLogInfoToCallback("接收第#N个文件出现异常，异常信息：\n"
								.replace("#N",String.valueOf(i + 1)) + e,13);
					}
					catch (Exception ex) {
						Tools.logError(e);
						ex.printStackTrace();
					}
				}
				finally {
					try {
						if (socket != null) {
							socket.close();
						}
					}
					catch (Exception e) {
						Tools.logError(e);
						e.printStackTrace();
					}
				}
			}
			// EnvironmentUtil.requestUpdatingMediaDatabase(context);
			// context.sendBroadcast(new Intent(Constants.ACTION_REFRESH_AVAILIBLE_STORAGE));
			if (callback != null && !isInterrupted) {
				Platform.runLater(() -> {
					callback.onFileReceivedCompleted(error_info.toString());
				});
			}
			senderIp = null;
			//接收文件完成
			postLogInfoToCallback("★ 接收文件完成 ★",99);
		}

		void setInterrupted() {
			isInterrupted = true;
			interrupt();
			try {
				if (socket != null) {
					socket.close();
				}
			}
			catch (Exception e) {
				Tools.logError(e);
				e.printStackTrace();
			}
			try {
				if (currentWritingFileItem != null) {
					currentWritingFileItem.delete();
				}
			}
			catch (Exception e) {
				Tools.logError(e);
				e.printStackTrace();
			}
		}
	}

}
