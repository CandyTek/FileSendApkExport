module com.xiaoming.desktop.filereceiver.file_receiver {
	requires javafx.controls;
	requires javafx.fxml;
	requires org.jetbrains.annotations;

	opens com.xiaoming.desktop.filereceiver.file_receiver to javafx.fxml;
	exports com.xiaoming.desktop.filereceiver.file_receiver;
}
