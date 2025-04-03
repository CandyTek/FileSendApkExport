package com.xiaoming.desktop.filereceiver.file_receiver;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class ProgressDialog2 {
	private Stage dialogStage;
	private ProgressBar progressBar;
	private Label messageLabel;

	public ProgressDialog2(String message,boolean indeterminate) {
		progressBar = new ProgressBar();
		progressBar.setPrefWidth(300);
		progressBar.setProgress(indeterminate ? ProgressBar.INDETERMINATE_PROGRESS : 0);

		messageLabel = new Label(message);

		VBox vbox = new VBox(15,messageLabel,progressBar);
		vbox.setPadding(new Insets(20));
		vbox.setAlignment(Pos.CENTER);

		dialogStage = new Stage();
		dialogStage.initModality(Modality.APPLICATION_MODAL);
		dialogStage.initStyle(StageStyle.UTILITY);
		dialogStage.setTitle("正在处理...");
		dialogStage.setScene(new Scene(vbox));
		dialogStage.setResizable(false);
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
}
