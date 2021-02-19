package com.jentsch.raspberry.pi.pico;

import javafx.application.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.*;
import com.fazecast.jSerialComm.SerialPort;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class Main extends Application {

    boolean alarm = false;
    private boolean running = true;
    private BlockingQueue<String> queue;
    private int[] servoValues = new int[3];
    private int minVal = 4000;
    private int maxVal = 8000;
    private SerialPort comPort;

    public static void main(String[] args) {
        launch(args);
    }

    public void startAlarm() {
        new Thread() {
            @Override
            public void run() {
                try {
                    int count = 0;
                    while (alarm) {
                        queue.clear();
                        queue.add("4000;4000;4000");
                        Thread.sleep(150);
                        queue.clear();
                        queue.add("8000;8000;5000");
                        Thread.sleep(150);
                        if (count > 8) alarm = false;
                        count ++;
                    }

                    queue.clear();
                    queue.add("4000;8000;4000");
                    Thread.sleep(150);
                    queue.add("4000;8000;4000");
                    Thread.sleep(150);
                    queue.add("4000;8000;4000");
                    Thread.sleep(150);
                } catch (Exception e){}
            }
        }.start();
    }

    public void portWriter (SerialPort comPort, TextArea textArea) {
        try {
            String line = "";
            while (running)
            {
                line = queue.take();
                byte[] buffer = (line + "\r\n").getBytes();
                int numWrite = comPort.writeBytes(buffer, buffer.length);
                System.out.println("Write: " + numWrite + " bytes");
                Thread.sleep(150);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void portReader (SerialPort comPort, TextArea textArea) {
        try {
            while (running) {
                while (comPort.bytesAvailable() == 0)
                    Thread.sleep(20);
                if (comPort.bytesAvailable() == -1) {
                    running = false;
                } else {
                    byte[] readBuffer = new byte[comPort.bytesAvailable()];

                    int numRead = comPort.readBytes(readBuffer, readBuffer.length);
                    String data = new String(readBuffer);
                    textArea.appendText(data);
                    System.out.println("Read: " + numRead + ":" + data);

                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }



    @Override
    public void stop(){
        alarm = false;
        running  = false;
        comPort.closePort();
    }

    @Override
    public void start(Stage primaryStage) {
        SerialPort port = selectPort(getPorts());
        primaryStage.setTitle(port.toString());

        queue = new ArrayBlockingQueue(1024);

        servoValues[0] = 4000;
        servoValues[1] = 4000;
        servoValues[2] = 4000;
        TextArea textArea = new TextArea();
        Label slider1Label = new Label("Servo 1: " + servoValues[0]);
        Slider slider1 = new Slider(minVal, maxVal, servoValues[0]);
        slider1.setBlockIncrement(50);
        Label slider2Label = new Label("Servo 2: " + servoValues[1]);
        Slider slider2 = new Slider(minVal, maxVal, servoValues[1]);
        Label slider3Label = new Label("Servo 3: " + servoValues[2]);
        Slider slider3 = new Slider(minVal, maxVal, servoValues[2]);
        Button alarmButton = new Button("Probealarm");
        alarmButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent e) {
                alarm =! alarm;
                if (alarm) {
                    startAlarm();
                }
            }
        });

        slider1.valueProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number oldValue, Number newValue) {
                slider1Label.textProperty().setValue("Servo 1: " + newValue.intValue());
                updateServo(0, newValue.intValue());
            }
        });

        slider2.valueProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number oldValue, Number newValue) {
                slider2Label.textProperty().setValue("Servo 2: " + newValue.intValue());
                updateServo(1, newValue.intValue());
            }
        });

        slider3.valueProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number oldValue, Number newValue) {
                slider3Label.textProperty().setValue("Servo 3: " + newValue.intValue());
                updateServo(2, newValue.intValue());
            }
        });

        VBox vbox = new VBox(textArea,
                slider1Label, slider1,
                slider2Label, slider2,
                slider3Label, slider3,
                alarmButton);
        vbox.setSpacing(10);

        startServoThread(port, textArea);

        Scene scene = new Scene(vbox, 600, 400);
        primaryStage.setScene(scene);
        primaryStage.show();

    }

    private void startServoThread(SerialPort comPort, TextArea textArea) {
        this.comPort = comPort;
        boolean open = comPort.openPort();
        // TODO : If open == false: Unable to connect
        System.out.println("Connected to " + comPort + " = " + open);

        Thread portReaderThread = new Thread() {
            @Override
            public void run() {
                portReader (comPort, textArea);
            }
        };
        portReaderThread.start();
        Thread portWriterThread = new Thread() {
            @Override
            public void run() {
                portWriter (comPort, textArea);
            }
        };
        portWriterThread.start();
    }

    private void updateServo(int servo, int value) {
        servoValues[servo] = value;
        queue.clear(); // Remove all prev. elements
        queue.add(servoValues[0] + ";" + servoValues[1] + ";" + servoValues[2]);
    }

    private List<SerialPort> getPorts() {
        List<SerialPort> ports = new ArrayList<>();
        SerialPort[] serialPorts = SerialPort.getCommPorts();
        for (int i = 0; i < serialPorts.length; i++) {
            ports.add(serialPorts[i]);
        }
        return ports;
    }

    public SerialPort selectPort (List<SerialPort> ports) {

        ChoiceDialog<SerialPort> dialog = new ChoiceDialog<SerialPort>(null, ports);
        dialog.initStyle(StageStyle.UTILITY);
        dialog.setTitle("Select Port");
        dialog.setHeaderText("Port:");
        dialog.setGraphic(null);
        dialog.setContentText(null);

        Optional<SerialPort> result = dialog.showAndWait();
        if (result.isPresent()){
            return result.get();
        }
        return null;
    }
}
