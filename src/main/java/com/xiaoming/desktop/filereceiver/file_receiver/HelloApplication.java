package com.xiaoming.desktop.filereceiver.file_receiver;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;

public class HelloApplication extends Application {

	@Override
	public void start(Stage stage) throws IOException {
		FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("hello-view.fxml"));
		Scene scene = new Scene(fxmlLoader.load(),450,620);
		stage.setTitle("文件传输");
		// 设置窗口图标
		Image icon = new Image(new ByteArrayInputStream(Base64.getDecoder().decode(Constants.BASE64_ICON)));
		stage.getIcons().add(icon);
		stage.setScene(scene);
		initView();
		stage.show();

		// 设置关闭窗口时退出程序
		stage.setOnCloseRequest(event -> {
			Platform.exit();      // 优雅退出 JavaFX 应用
			System.exit(0);       // 强制退出 JVM（可选）
		});

	}

	private void initView() {

	}
	
	public static void main(String[] args) {

		launch();
	}
}
