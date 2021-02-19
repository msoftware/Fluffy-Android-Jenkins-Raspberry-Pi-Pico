package com.jentsch.raspberry.pi.pico;

import com.fazecast.jSerialComm.SerialPort;
import com.offbytwo.jenkins.JenkinsServer;
import com.offbytwo.jenkins.model.Build;
import com.offbytwo.jenkins.model.BuildResult;
import com.offbytwo.jenkins.model.JobWithDetails;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class Main {

    private boolean running = true;
    private BlockingQueue<String> queue;
    private int[] servoValues = new int[3];
    private int minVal = 4000;
    private int maxVal = 8000;
    private SerialPort comPort;
    private JenkinsServer jenkins;

    private static String comPortName = null;
    private static String jenkinsUrlString = null;
    private static String jenkinsJobName = null;
    private static String jenkinsUsername = null;
    private static String jenkinsPassword = null;
    private boolean isJenkinsRunning = false;


    public static void main(String[] args)
            throws Exception
    {
        readConfig("config.properties");
        Main m = new Main();
        m.start();
    }

    public void portWriter (SerialPort comPort) {
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

    public void portReader (SerialPort comPort) {
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
                    if (data.trim().equals("BUTTON")) {
                        runJenkinsJob();
                        queue.add("8000;4000;4000");
                        Thread.sleep(300);
                        queue.add("4000;8000;4000");
                        Thread.sleep(500);
                    }
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void runJenkinsJob()
    {
        if (isJenkinsRunning) {
            System.out.println("Jenkins Job is running. Please wait.");
        } else {
            try {
                JobWithDetails jobDetails = jenkins.getJob(jenkinsJobName);
                int nextBuildNumber = jobDetails.getNextBuildNumber();
                System.out.println("Run Jenkins Job: " + nextBuildNumber);
                Map<String, String> params = new HashMap<>();
                jobDetails.build(params, null, true);
                startJenkinsJobWatcher(nextBuildNumber);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void startJenkinsJobWatcher(int buildNumber) {
        new Thread() {
            @Override
            public void run() {
                while (true) {
                    isJenkinsRunning = true;
                    System.out.println("Wait for build: " + buildNumber);
                    try {
                        Thread.sleep(15000);
                        JobWithDetails jobDetails = jenkins.getJob(jenkinsJobName);
                        List<Build> builds = jobDetails.getBuilds();
                        for (Build build : builds) {
                            if (build.getNumber() == buildNumber) {
                                if (!build.details().isBuilding()) {
                                    BuildResult results = build.details().getResult();
                                    showBuildResult(results);
                                    isJenkinsRunning = false;
                                    return;
                                } else {
                                    System.out.println("building .....");
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();
    }

    private void showBuildResult(BuildResult results) {
        try {
            if (results.equals(BuildResult.SUCCESS)) {
                for (int i = 0; i < 20; i++) {
                    queue.add("8000;4000;4000");
                    Thread.sleep(200);
                    queue.add("4000;8000;4000");
                    Thread.sleep(200);
                }
            } else {
                try {
                    for (int i = 0; i < 8; i++) {
                        queue.clear();
                        queue.add("4000;4000;4000");
                        Thread.sleep(150);
                        queue.clear();
                        queue.add("8000;8000;5000");
                        Thread.sleep(150);
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void finalize() throws Throwable {
        // TODO
        super.finalize();
        running  = false;
        comPort.closePort();
    }

    public void start() throws Exception
    {
        SerialPort port = selectPort(getPorts());
        queue = new ArrayBlockingQueue(1024);
        URI jenkinsUrl = new URI(jenkinsUrlString);
        jenkins = new JenkinsServer(jenkinsUrl, jenkinsUsername, jenkinsPassword);

        servoValues[0] = 4000;
        servoValues[1] = 4000;
        servoValues[2] = 4000;
        startServoThread(port);
    }

    private void startServoThread(SerialPort comPort) {
        this.comPort = comPort;
        boolean open = comPort.openPort();
        // TODO : If open == false: Unable to connect
        System.out.println("Connected to " + comPort + " = " + open);

        Thread portReaderThread = new Thread() {
            @Override
            public void run() {
                portReader (comPort);
            }
        };
        portReaderThread.start();
        Thread portWriterThread = new Thread() {
            @Override
            public void run() {
                portWriter (comPort);
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

        for (SerialPort port : ports) {
            if (port.toString().equals(comPortName)) {
                return port;
            }
        }
        System.out.println("Error: Port not found.");
        if (ports.size() == 0) {
            System.out.println("No ports available.");
        } else {
            System.out.println("Ports available:");
            for (SerialPort port : ports) {
                System.out.println(port.toString());
            }
        }
        System.exit(-1);
        return null;
    }

    public static void readConfig(String filename) {
        Configurations configs = new Configurations();
        try
        {
            Configuration config = configs.properties(new File(filename));
            comPortName = config.getString("pi.port");
            jenkinsUrlString = config.getString("jenkins.url");
            jenkinsJobName = config.getString("jenkins.job.name");
            jenkinsUsername = config.getString("jenkins.username");
            jenkinsPassword = config.getString("jenkins.password");
        }
        catch (Exception e)
        {
            e.printStackTrace();
            System.exit(-1);
        }
    }
}
