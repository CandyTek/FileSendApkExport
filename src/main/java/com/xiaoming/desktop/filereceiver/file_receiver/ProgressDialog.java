package com.xiaoming.desktop.filereceiver.file_receiver;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class ProgressDialog {
	private Stage dialogStage;
	private ProgressBar progressBar;
	private Label messageLabel;
	private Button closeButton;
	private Runnable onCloseListener;

	public ProgressDialog(String message,boolean indeterminate) {
		progressBar = new ProgressBar();
		progressBar.setPrefWidth(300);
		progressBar.setProgress(indeterminate ? ProgressBar.INDETERMINATE_PROGRESS : 0);

		messageLabel = new Label(message);

		closeButton = new Button("取消");
		closeButton.setOnAction(e -> {
			close();
			if (onCloseListener != null) {
				onCloseListener.run();
			}
		});

		VBox vbox = new VBox(15,messageLabel,progressBar,closeButton);
		vbox.setPadding(new Insets(20));
		vbox.setAlignment(Pos.CENTER);

		dialogStage = new Stage();
		dialogStage.initModality(Modality.APPLICATION_MODAL);
		dialogStage.initStyle(StageStyle.UTILITY);
		dialogStage.setTitle("请稍候...");
		dialogStage.setScene(new Scene(vbox));
		dialogStage.setResizable(false);

		// 用户点击右上角关闭按钮时也调用关闭监听
		dialogStage.setOnCloseRequest(e -> {
			if (onCloseListener != null) {
				onCloseListener.run();
			}
		});
	}

	public void show() {
		Platform.runLater(() -> dialogStage.show());
	}

	public void close() {
		Platform.runLater(() -> dialogStage.close());
	}

	public void setProgress(double progress) {
		Platform.runLater(() -> progressBar.setProgress(progress));
	}

	public void setMessage(String message) {
		Platform.runLater(() -> messageLabel.setText(message));
	}

	public void setOnClose(Runnable listener) {
		this.onCloseListener = listener;
	}
}
