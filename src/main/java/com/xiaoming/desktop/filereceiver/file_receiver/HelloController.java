package com.xiaoming.desktop.filereceiver.file_receiver;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.stage.Window;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class HelloController implements NetReceiveTask.NetReceiveTaskCallback {

	/**
	 * 引用 FXML 中的 ListView，用于更改 ListView 样式
	 */
	@FXML
	private ListView<String> listView;

	/**
	 * ListView 的数据
	 */
	private ObservableList<String> items;

	private Window getWindow() {
		return listView.getScene().getWindow();
	}

	@FXML
	private void initialize() {
		// 当用户选择某个项时，显示提示框
		listView.getSelectionModel().selectedItemProperty().addListener((observable,oldValue,newValue) -> {
			if (newValue != null) {
				// showItemSelectedAlert(newValue);
			}
		});

		// 初始化 ObservableList
		items = FXCollections.observableArrayList();

		// 绑定到 ListView
		listView.setItems(items);

		// 设置自定义 CellFactory
		listView.setCellFactory(lv -> new ListCell<String>() {
			{
				setPrefWidth(0);
			}

			@Override
			protected void updateItem(String item,boolean empty) {
				super.updateItem(item,empty);

				if (empty || item == null) {
					setText(null);
					setStyle("");
				} else {
					setText(item);

					int important1 = 0;
					try {
						important1 = Integer.parseInt(item.substring(0,2));
					}
					catch (NumberFormatException e) {
						Tools.logError(e);
						return;
					}

					if (important1 < 11) {
						setStyle("-fx-background-color: #ccccccff;");
					} else if (important1 < 20) {
						setStyle("-fx-background-color: #CF5B5699;");
					} else if (important1 < 60) {
						setStyle("-fx-background-color: #EBC70045;");
					} else if (important1 < 99) {
						setStyle("-fx-background-color: #59A86945;");
					} else {
						setStyle("-fx-background-color: #59A869A9;");
					}

					// allow wrapping
					setWrapText(true);

				}
			}
		});
		Tools.getInternalSavePath().mkdirs();
		// String gateway = Tools.convertToBroadcastIP(getNetworkGateway());
		String gateway;
		if (Tools.isWindows()) {
			gateway = Tools.convertToBroadcastIP(Tools.getNetworkGateway());
		} else {
			gateway = Tools.convertToBroadcastIP(Tools.getPreferredAddress());
		}
		items.add("10 \t广播地址: " + gateway);
		if (gateway != null) {
			Constants.BROADCAST_ADDRESS = gateway;
		} else {
			System.exit(0);
		}
		initNetReceive();
	}

	/**
	 * 显示项被选择后的弹出框
	 */
	private void showItemSelectedAlert(String item) {
		Alert alert = new Alert(Alert.AlertType.INFORMATION);
		alert.setTitle("选择的项");
		alert.setHeaderText("你选择了：");
		alert.setContentText(item);
		alert.showAndWait();
	}

	public void showHelpDialog() {
		Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
		alert.setTitle("帮助"); // 相当于 setTitle
		alert.setHeaderText(null);

		alert.setContentText(
				"一款文件传输工具，目前软件仅与 Android 手机上的 APK kit、APK 导出工具搭配使用，用于接收软件发来的文件。\n\n请对方安装好（APK kit、APK导出工具）软件，并和当前设备处于同一个局域网(相同WiFi 或 开启热点让对方连接)，发送端选择需分享的应用后点击“直接发送”，再选择本设备即可。\n" +
						"\n" +
						"如果本设备连接的是发送端的热点，可尝试勾选“热点模式”。\n" +
						"\n" +
						"如果发送端未检测到本设备，试试“重新广播”按钮，若处于同一个Wifi请检查路由是否有“AP隔离”。\n" +
						"\n" +
						"文件将保存至“导出路径”，重复名称文件将自动重命名保存。\n" +
						"\n" +
						"传输速度存在木桶效应：网络带宽(100Mbps/1000Mbps, 2.4GHz/5GHz…)、储存读写速度(UFS/EMMC…)、CPU处理速度、设备心情…");
		// ButtonType confirmButton = new ButtonType("确定");
		ButtonType shareButton = new ButtonType("前往本软件下载地址");

		alert.getButtonTypes().setAll(shareButton,ButtonType.CLOSE);

		// 显示对话框并处理点击
		Optional<ButtonType> result = alert.showAndWait();
		if (result.isPresent()) {
			ButtonType buttonType = result.get();
			if (buttonType == shareButton) {
				// 用户点击了“分享此应用”
				// Global.shareThisApp(); // 假设你也有个静态方法
			} else if (buttonType == ButtonType.CLOSE) {
				System.err.println("Click Close");
			} else {
				System.err.println("other");
			}
		} else {
			// 真的点了右上角 X 或其他方式关闭了窗口
			System.out.println("other2");
		}
	}

	public void onHelpButtonClick(ActionEvent actionEvent) {
		showHelpDialog();
	}
	public void onHelloButtonClick(ActionEvent actionEvent) {
		Random r = new Random();
		int i = r.nextInt(50);
		items.add("" + i);
	}

	// 其他代码逻辑...

	private NetReceiveTask netReceiveTask;

	@Nullable
	private ProgressDialog2 receiving_dialog = null;

	// 	private AlertDialog request_dialog;
	// 	private Dialog refreshMideaProgressDialog;
	//
	//
	// 	private ListAdapter listAdapter;
	// 	//public ArrayList<String> trustIpList = new ArrayList<>();
	// 	//private Boolean stopTimer=false;
	// 	private Boolean continueRefresh=true;
	//
	// 	private final ArrayList<MessageBean> logMessages = new ArrayList<>();
	private Timer timer;
	private TimerTask tTask = null;

	protected void initNetReceive() {
		// 绑定端口失败
		// 	new Thread(() -> {
		try {
			netReceiveTask = new NetReceiveTask(this);
		}
		catch (Exception e) {
			Tools.logError(e);
			// e.printStackTrace();
			// new AlertDialog.Builder(this)
			// 		.setTitle(getResources().getString(R.string.word_error))
			// 		.setMessage(getResources().getString(R.string.info_bind_port_error) + e)
			// 		.setCancelable(false)
			// 		.setPositiveButton(getResources().getString(R.string.dialog_button_confirm), (dialog, which) -> finish())
			// 		.show();
		}
		// }).start();

		// 重新发送报文 按钮
		// findViewById(R.id.activity_file_receive_refresh).setOnClickListener(v -> {
		// 	if (netReceiveTask != null) netReceiveTask.sendOnlineBroadcastUdp();
		// });
		// finish里可能不会自动销毁，必须主动调用销毁
		timer = new Timer();
		tTaskInit();
		timer.schedule(tTask,1000,1300);

		System.err.println("".equals(Tools.getIPAddress()) ? "null" : Tools.getIPAddress());
	}

	//
	// 	//██ task_定时发送报文 ██
	private void tTaskInit() {
		// 再次调用Task的时候，必须重新生成，不然会崩溃
		if (tTask != null) {
			tTask.cancel();
		}
		tTask = new TimerTask() {
			@Override
			public void run() {
				//TODO: 定时做某件事情
				// if (stopTimer) timer.cancel();

				if (netReceiveTask != null) {
					netReceiveTask.sendOnlineBroadcastUdp();
				}
			}
		};
	}

	// 	//██ 文件接受请求 ██
	@Override
	public void onFileReceiveRequest(@NotNull NetReceiveTask task,@NotNull String ip,@NotNull String deviceName,
									 @NotNull List<ReceiveFileItem> fileItems) {
		timer.cancel();
		netReceiveTask.startReceiveTask();
	}

	// 	//██ 打印日志 ██
	@Override
	public void onLog(String logInfo,int i) {
		MessageBean messageBean = new MessageBean(System.currentTimeMillis(),logInfo,i);
		items.add(messageBean.getColor() + "\t" + messageBean.getFormattedTime() + "\t" + messageBean.getMessage());
		listView.scrollTo(items.size() - 1);

		// logMessages.add(messageBean);
		// if (listAdapter != null) {
		// 	listAdapter.notifyDataSetChanged();
		// 	if(logMessages.size()!=0)listview.smoothScrollToPosition(logMessages.size()-1);
		//if(linearLayoutManager!=null){
		//    linearLayoutManager.smoothScrollToPosition(recyclerView,null,logMessages.size()>0?logMessages.size()-1:0);
		//}
		// }
	}
	// 	//██ 文件接收中断 ██
	@Override
	public void onFileReceiveInterrupted() {
		if (receiving_dialog != null) {
			receiving_dialog.close();
			receiving_dialog = null;
		}
		Tools.toast(getWindow(),"对方终止了发送文件",3000);
		startRefresh();
	}
	// 	//██ 发送方 取消发送 ██
	@Override
	public void onSendSiteCanceled(@NotNull String ip) {
		// if (request_dialog != null && request_dialog.isShowing()) {
		// 	request_dialog.cancel();
		// 	request_dialog = null;
		// }
		// ShowToast.toast(this, getResources().getString(R.string.toast_send_canceled));
	}
	// 	//██ 当文件开始接收 ██
	@Override
	public void onFileReceiveStarted() {
		// if (request_dialog != null && request_dialog.isShowing()) {
		// 	request_dialog.cancel();
		// 	request_dialog = null;
		// }
		if (receiving_dialog == null) {
			receiving_dialog = new ProgressDialog2("正在传输",true);
			receiving_dialog.show();
			System.err.println("showdialog");
		}
	}
	// 	//██ 文件传输的进度dialog ██
	@Override
	public void onFileReceiveProgress(long progress,long total,@NotNull String currentWritePath,long speed) {
		if (receiving_dialog != null) {
			receiving_dialog.setProgress(progress);
			//receiving_dialog.setCurrentFileInfo(getResources().getString(R.string.dialog_file_receiving_att) + currentWritePath);
		}
	}

	// 	//██ 传输速度相关 ██
	//@Override
	//public void onSpeed(long speedOfBytes) {
	//	if (receiving_dialog != null) receiving_dialog.setSpeed(speedOfBytes);
	//}
	// 	//██ 当文件传输完成 ██
	@Override
	public void onFileReceivedCompleted(@NotNull String error_info) {
		if (receiving_dialog != null) {
			receiving_dialog.close();
			receiving_dialog = null;
			System.err.println("close dialog");
		}
		if (error_info.isEmpty()) {
			Tools.toast(getWindow(),"文件接收完成",2000);
		} else {
			// new AlertDialog.Builder(this)
			// 		.setTitle(getResources().getString(R.string.dialog_receive_error_title))
			// 		.setMessage(getResources().getString(R.string.dialog_receive_error_message) + error_info)
			// 		.setPositiveButton(getResources().getString(R.string.dialog_button_confirm), (dialog, which) -> {
			// 		})
			// 		.show();
		}
		startRefresh();
	}
	private void startRefresh() {
		// if(!continueRefresh)return;
		if (timer != null) {
			timer.cancel();
		}
		Timer timer = new Timer();
		tTaskInit();
		timer.schedule(tTask,1000,1800);
	}

	// 	//██ 收到发送方信息：发送异常 ██
	@Override
	public void onFileReceivedError() {}

}
