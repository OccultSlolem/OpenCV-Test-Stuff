package com.uberlyuber;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("FieldCanBeLocal")
public class CVAppController {

    @FXML
    private ImageView currentFrame;
    @FXML
    private Button button;

    private VideoCapture capture = new VideoCapture();
    private boolean cameraActive = false;
    private final int cameraId = 0;
    private ScheduledExecutorService timer;


    public void start(Stage primaryStage) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("CVApp.fxml"));
        BorderPane root = loader.load();
    }

    @FXML
    protected void startCamera(ActionEvent actionEvent) {
        if(!cameraActive) { //Camera is not active
            capture.open(cameraId);

            if(capture.isOpened()) { //Camera stream is available
                cameraActive = true;

                Runnable frameGrabber = new Runnable() {
                    Image imageToShow = grabFrame();
                    @Override
                    public void run() {
                        //Grab and process the frame
                        currentFrame.setImage(grabFrame());
                    }
                };

                timer = Executors.newSingleThreadScheduledExecutor();
                timer.scheduleAtFixedRate(frameGrabber,0,33, TimeUnit.MILLISECONDS);

                button.setText("Stop Camera");
            } else { //Stream fails to open
                System.out.println("Fatal: Camera connection did not open");
            }
        } else { //Camera is already active
            cameraActive = false;
            button.setText("Start Camera");
        }
    }

    /**
     * Gets a frame from the current opened video stream (if active)
     *
     * @return The {@link Mat} to show
     */
    private Image grabFrame() {
        Mat frame = new Mat();
        MatOfByte buffer = new MatOfByte();

        //Check if capture is opened
        if(capture.isOpened()) {
            try {
                capture.read(frame);
                if(!frame.empty()) {
                    Imgproc.cvtColor(frame, frame, Imgproc.COLOR_BGR2GRAY);
                    Imgcodecs.imencode(".png",frame,buffer);
                }
            } catch(Exception e) {
                e.printStackTrace();
            }
        }


        return new Image(new ByteArrayInputStream(buffer.toArray()));
    }

    /**
     * Stops acquiring new frames and releases resources
     */
    private void stopAcquisition() {

        //Attempt to shutdown the timer
        if(timer != null && !timer.isShutdown()) {
            try {
                timer.shutdown();
                timer.awaitTermination(33,TimeUnit.MILLISECONDS);
            } catch(InterruptedException e) {
                System.out.println("Error in stopping the camera:");
                e.printStackTrace();
            }
        }

        //Release capture resources
        if(capture.isOpened()) {
            capture.release();
        }
    }
}
