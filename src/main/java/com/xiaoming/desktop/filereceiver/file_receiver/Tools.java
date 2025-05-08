package com.xiaoming.desktop.filereceiver.file_receiver;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.Tooltip;
import javafx.stage.Window;
import javafx.util.Duration;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.*;
import java.text.DecimalFormat;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Tools {
	public static void toast2(Node ownerNode,String message,int durationMillis) {
		Tooltip tooltip = new Tooltip(message);

		// 使其看起来更像 Toast
		tooltip.setAutoHide(true);
		tooltip.setOpacity(0.9);
		tooltip.setStyle("-fx-background-color: black; -fx-text-fill: white; -fx-padding: 10px; -fx-font-size: 14px;");

		// 计算 Tooltip 应该显示的位置（靠近传入的 Node）
		Point2D p = ownerNode.localToScreen(ownerNode.getBoundsInLocal().getMinX(),ownerNode.getBoundsInLocal().getMinY());

		// 显示 Tooltip
		tooltip.show(ownerNode,p.getX(),p.getY() - 30);

		// 倒计时自动关闭
		Timeline timeline = new Timeline(new KeyFrame(Duration.millis(durationMillis),e -> tooltip.hide()));
		timeline.setCycleCount(1);
		timeline.play();
	}
	public static void toast(Window window,String message,int durationMillis) {
		Tooltip tooltip = new Tooltip(message);

		// 使其看起来更像 Toast
		tooltip.setAutoHide(true);
		tooltip.setOpacity(0.9);
		tooltip.setStyle("-fx-background-color: black; -fx-text-fill: white; -fx-padding: 10px; -fx-font-size: 14px;");

		tooltip.show(window);

		// 倒计时自动关闭
		Timeline timeline = new Timeline(new KeyFrame(Duration.millis(durationMillis),e -> tooltip.hide()));
		timeline.setCycleCount(1);
		timeline.play();
	}

	// 获取当前登录的用户名（类似 Windows 的 user name）
	public static String getUserName() {
		return System.getProperty("user.name");
	}

	// 获取当前设备名（计算机名 / 主机名）
	public static String getDeviceName() {
		try {
			return java.net.InetAddress.getLocalHost().getHostName();
		}
		catch (Exception e) {
			Tools.logError(e);
			return "UnknownDevice";
		}
	}

	public static int getPortNumber() {
		return 6565;
	}
	public static File getInternalSavePath() {
		if (isWindows()) {
			return new File("C:\\receive");
		} else {
			// 获取用户的 home 目录
			String home = System.getProperty("user.home");
			return new File(home,"receive");
		}
	}
	@NotNull
	public static String getFileMainName(@NotNull String fileName) {
		try {
			final int lastIndex = fileName.lastIndexOf(".");
			if (lastIndex == -1) {
				return fileName;
			}
			return fileName.substring(0,lastIndex);
		}
		catch (Exception e) {
			Tools.logError(e);
			e.printStackTrace();
		}
		return "";
	}

	/**
	 * 截取文件扩展名，例如Test.apk 则返回 apk
	 */
	@NotNull
	public static String getFileExtensionName(@NotNull String fileName) {
		try {
			return fileName.substring(fileName.lastIndexOf(".") + 1);
		}
		catch (Exception e) {
			Tools.logError(e);
			e.printStackTrace();
		}
		return "";
	}

	public static String getDisplayingExportPath() {
		return getInternalSavePath().getAbsolutePath();
	}
	private static final DecimalFormat fileSizeDecimalFormat = getDecimalFormat(1);
	private static DecimalFormat getDecimalFormat(int pref) {
		switch (pref) {
			case 1:
				return new DecimalFormat("###0.#");
			case 2:
				return new DecimalFormat("###0.##");
			case 3:
				return new DecimalFormat("###0.###");
			default:
				return new DecimalFormat("###0");
		}
	}
	public static String[] myList3 = new String[]{"B","KB","MB","GB","TB"};

	public static String getFormatSize(long paramLong) {
		if (paramLong <= 0L) {
			return "0";
		}
		int i = (int) (Math.log10(paramLong) / Math.log10(1024.0D));
		return fileSizeDecimalFormat.format(paramLong / Math.pow(1024.0D,i)) + " " + myList3[i];
	}

	public static void logError(Exception e,String message) {
		// Exception e = new Exception();
		StackTraceElement element = e.getStackTrace()[1]; // 获取调用者信息

		String location = element.getClassName() + "." + element.getMethodName() +
				"(" + element.getFileName() + ":" + element.getLineNumber() + ")";

		System.err.println("[" + "tag" + "] " + message + " @ " + location);
	}

	public static void logError(Exception e) {
		// 打印简洁的异常信息
		System.err.println("ExceptionType: " + e.getClass().getName());
		System.err.println("ExceptionInformation: " + e.getMessage());

		// 获取调用栈
		StackTraceElement[] stackTrace = e.getStackTrace();

		// 打印首个有效的调用位置（最可能是你代码的出错点）
		if (stackTrace.length > 0) {
			StackTraceElement top = stackTrace[0];
			System.err.println("WhereToTakePlace: " + top.getClassName() + "." + top.getMethodName()
					+ " (" + top.getFileName() + ":" + top.getLineNumber() + ")");
		}

		// 如果你想打印完整堆栈信息（类似 e.printStackTrace()），也可以加上这句：
		e.printStackTrace(System.err);
	}

	public static boolean isValidIPAddress(String ipAddress) {
		String ipAddressPattern =
				"^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
						"([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
						"([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
						"([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";

		return Pattern.matches(ipAddressPattern,ipAddress);
	}
	public static String convertToBroadcastIP(String ip) {
		if (ip == null || ip.trim().isEmpty()) {
			System.out.println("IP地址不能为空！");
			return null; // 或者 throw new IllegalArgumentException("IP地址不能为空");
		}
		if (!isValidIPAddress(ip)) {
			System.out.println("IP地址不能！");
			return null; // 或者 throw new IllegalArgumentException("IP地址不能为空");
		}

		String[] parts = ip.trim().split("\\.");
		// 替换最后一段为 255
		return parts[0] + "." + parts[1] + "." + parts[2] + ".255";
	}
	/** 获得IP地址，分为两种情况，一是wifi下，二是移动网络下，得到的ip地址是不一样的 */
	public static String getIPAddress() {
		try {
			InetAddress localHost = InetAddress.getLocalHost();
			return localHost.getHostAddress();
		}
		catch (UnknownHostException e) {
			logError(e);
			return "无法获取本地IP地址: " + e.getMessage();
		}
	}
	//将得到的int类型的IP转换为String类型
	public static String intIP2StringIP(int ip) {
		return (ip & 0xFF) + "." +
				((ip >> 8) & 0xFF) + "." +
				((ip >> 16) & 0xFF) + "." +
				(ip >> 24 & 0xFF);
	}
	/**
	 * 单个文件信息的切割符 0x07
	 */
	public static String fileInfoSplit() {
		return new String(new byte[]{0x07});
	}
	//██ 获取文件信息 ██
	public static String getFileInfoMessage(List<ReceiveFileItem> receiveFileItems) {
		StringBuilder stringBuilder = new StringBuilder();
		for (ReceiveFileItem receiveFileItem : receiveFileItems) {
			stringBuilder.append(receiveFileItem.getFileName());
			stringBuilder.append("(");
			//stringBuilder.append(Formatter.formatFileSize(this, receiveFileItem.getLength()));
			stringBuilder.append(getFormatSize(receiveFileItem.Length()));
			stringBuilder.append(")");
			stringBuilder.append("\n\n");
		}
		return stringBuilder.toString();
	}

	public static boolean isWindows() {
		return getOSName().startsWith("Windows");
	}

	public static boolean isLinux() {
		return getOSName().startsWith("Linux");
	}

	public static boolean isMac() {
		return getOSName().startsWith("Mac");
	}

	private static String getOSName() {
		return System.getProperty("os.name");
	}

	public static String getPreferredAddress() {
		try {
			Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
			while (interfaces.hasMoreElements()) {
				NetworkInterface iface = interfaces.nextElement();
				// 跳过虚拟网卡和不启用的接口
				if (iface.isLoopback() || !iface.isUp() || iface.isVirtual()) {
					continue;
				}

				Enumeration<InetAddress> addresses = iface.getInetAddresses();
				while (addresses.hasMoreElements()) {
					InetAddress addr = addresses.nextElement();
					// 过滤掉 loopback 和 IPv6 地址（如不需要 IPv6 可跳过）
					if (!addr.isLoopbackAddress() && addr instanceof Inet4Address) {
						return addr.getHostAddress();
					}
				}
			}
		}
		catch (SocketException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static String getNetworkGateway() {
		StringBuilder output = new StringBuilder();
		try {
			Process process = Runtime.getRuntime().exec("tracert -h 1 -d 8.8.8.8");
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

			String line;
			int lineCount = 0;
			Pattern ipPattern = Pattern.compile("(\\d{1,3}\\.){3}\\d{1,3}");

			while ((line = reader.readLine()) != null) {
				lineCount++;
				if (lineCount == 4) { // 通常第4行是第一跳
					Matcher matcher = ipPattern.matcher(line);
					if (matcher.find()) {
						return matcher.group();
					}
					break;
				}
			}
		}
		catch (Exception e) {
			output.append("错误: ").append(e.getMessage());
		}
		return output.toString().isEmpty() ? "未能识别默认网关" : output.toString();
	}

}
