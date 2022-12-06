module com.chat {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.fasterxml.jackson.databind;
    requires org.jetbrains.annotations;


    opens com.chat to javafx.fxml;

    exports com.chat.client.ui;
    exports com.chat.utils.message;
}