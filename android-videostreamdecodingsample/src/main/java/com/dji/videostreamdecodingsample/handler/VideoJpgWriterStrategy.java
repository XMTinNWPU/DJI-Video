package com.dji.videostreamdecodingsample.handler;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

//import dji.common.camera.CameraSettingsDef;
import dji.common.error.DJIError;
import dji.common.util.CommonCallbacks;
import dji.sdk.camera.Camera;
import com.dji.videostreamdecodingsample.Logger;
import com.dji.videostreamdecodingsample.VideoDecodingApplication;
import com.dji.videostreamdecodingsample.media.DJIVideoStreamDecoder;

/**
 * Created by dji on 16.01.2017.
 */

class VideoJpgWriterStrategy extends HandleStrategy
        implements DJIVideoStreamDecoder.IYuvDataListener2{

    //static boolean isRecording = false;

    public VideoJpgWriterStrategy(final Socket client){
        super(client);
        Logger.log("Video_enter");
        DJIVideoStreamDecoder.getInstance().setYuvDataListener2(this);

    }

    @Override
    protected void readMessage(InputStream istream) throws IOException {
        throw new UnsupportedOperationException("Unexpected input");
    }

    @Override
    protected void writeMessage(OutputStream ostream) throws IOException{

    }

    @Override
    protected boolean isNeedRead() { return false; }

    @Override
    protected boolean isNeedWrite() {
        return false;
    }

    @Override
    public void onYuvDataReceived2(byte[] yuvFrame, int width, int height) {
        int frameIndex = DJIVideoStreamDecoder.getInstance().frameIndex;
        //Logger.log("yuvoutReceived\n");
        if (frameIndex % 15 == 0) { //need for preventing out of memory error
            try {
                Logger.log("ready");
                convertYuvFormatToNv21(yuvFrame, width, height);
                Logger.log("convertend");
                YuvImage yuvImage = new YuvImage(yuvFrame, ImageFormat.NV21, width, height, null);

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                yuvImage.compressToJpeg(new Rect(0, 0, width, height), 50, out);
                byte[] jpeg = out.toByteArray();

                Logger.log("Send frame with index "+frameIndex);
                Logger.log("Size: " + jpeg.length);

                DataOutputStream dos = new DataOutputStream(client.getOutputStream());
                dos.writeInt(frameIndex);
                dos.writeInt(jpeg.length);
//                writeInt(client.getOutputStream(), frameIndex);
//                writeInt(client.getOutputStream(), jpeg.length);
                client.getOutputStream().write(jpeg);
            } catch (RuntimeException e) {
                Logger.log("JpgWriter: " + e.getMessage() + " w: " + width + " h: " + height
                        + " a: " + yuvFrame.length);
            } catch (IOException e) {
                Logger.log("JpgWriter: " + e.getMessage());
                try {
                    client.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    private void convertYuvFormatToNv21(byte[] yuvFrame, int width, int height) {
        byte[] y = new byte[width * height];
        byte[] u = new byte[width * height / 4];
        byte[] v = new byte[width * height / 4];
        byte[] nu = new byte[width * height / 4]; //
        byte[] nv = new byte[width * height / 4];
        System.arraycopy(yuvFrame, 0, y, 0, y.length);
        Logger.log("copy1");
        for (int i = 0; i < u.length; i++) {
            v[i] = yuvFrame[y.length + 2 * i];
            u[i] = yuvFrame[y.length + 2 * i + 1];
        }
        int uvWidth = width / 2;
        int uvHeight = height / 2;
        for (int j = 0; j < uvWidth / 2; j++) {
            for (int i = 0; i < uvHeight / 2; i++) {
                byte uSample1 = u[i * uvWidth + j];
                byte uSample2 = u[i * uvWidth + j + uvWidth / 2];
                byte vSample1 = v[(i + uvHeight / 2) * uvWidth + j];
                byte vSample2 = v[(i + uvHeight / 2) * uvWidth + j + uvWidth / 2];
                nu[2 * (i * uvWidth + j)] = uSample1;
                nu[2 * (i * uvWidth + j) + 1] = uSample1;
                nu[2 * (i * uvWidth + j) + uvWidth] = uSample2;
                nu[2 * (i * uvWidth + j) + 1 + uvWidth] = uSample2;
                nv[2 * (i * uvWidth + j)] = vSample1;
                nv[2 * (i * uvWidth + j) + 1] = vSample1;
                nv[2 * (i * uvWidth + j) + uvWidth] = vSample2;
                nv[2 * (i * uvWidth + j) + 1 + uvWidth] = vSample2;
            }
        }
        byte[] bytes = new byte[yuvFrame.length];
        //nv21test
        System.arraycopy(y, 0, bytes, 0, y.length);
        Logger.log("copy2");
        for (int i = 0; i < u.length; i++) {
            yuvFrame[y.length + (i * 2)] = nv[i];
            yuvFrame[y.length + (i * 2) + 1] = nu[i];
        }
    }

    @Override
    protected void initialize() {
        DJIVideoStreamDecoder.getInstance().setYuvDataListener2(this);
        //startRecord();
    }

    @Override
    protected void interrupt() {
        //stopRecord();
        DJIVideoStreamDecoder.getInstance().setYuvDataListener2(null);
    }



}
