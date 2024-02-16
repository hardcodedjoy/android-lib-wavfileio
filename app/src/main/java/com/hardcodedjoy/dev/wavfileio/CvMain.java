/*

MIT License

Copyright © 2024 HARDCODED JOY S.R.L. (https://hardcodedjoy.com)

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

*/

package com.hardcodedjoy.dev.wavfileio;

import android.Manifest;
import android.annotation.SuppressLint;
import android.media.AudioFormat;
import android.media.MediaRecorder;
import android.net.Uri;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.FileProvider;

import com.hardcodedjoy.appbase.IntentUtil;
import com.hardcodedjoy.appbase.activity.PermissionUtil;
import com.hardcodedjoy.appbase.contentview.CvTMLL;
import com.hardcodedjoy.appbase.gui.GuiLinker;
import com.hardcodedjoy.audioinput.AudioCable;
import com.hardcodedjoy.audioinput.MicInput;
import com.hardcodedjoy.noisoid.Noisoid;
import com.hardcodedjoy.noisoid.Source;
import com.hardcodedjoy.wavfileio.WavFileReader;
import com.hardcodedjoy.wavfileio.WavFileWriter;

import java.io.File;

@SuppressLint("ViewConstructor")
public class CvMain extends CvTMLL {

    static private final int BUFFER_MILLIS = 150;
    static private final int BUFFER_COUNT = 2;
    static private final int NUM_CHANNELS = 2;
    static private final int SAMPLE_RATE = 48000;

    private MicInput micInput;
    private float[][] buffer;
    private int bufferIndex;
    private int indexInBuffer;

    private WavFileWriter wavFileWriter;
    private WavFileReader wavFileReader;
    private Noisoid noisoid;

    private TextView tvStatus;

    public CvMain() {
        // add initialization code here (that must run only one time)
    }

    @Override
    public void show() {
        super.show();
        // add code to run every time this ContentView appears on screen

        LinearLayout ll = findViewById(R.id.appbase_ll_content);
        ll.removeAllViews();
        inflate(getActivity(), R.layout.main, ll);

        tvStatus = findViewById(R.id.tv_status);

        GuiLinker.setOnClickListenerToAllButtons(ll, view -> {
            int id = view.getId();
            if(id == R.id.btn_rec) { onRec(); }
            if(id == R.id.btn_stop) { onStop(); }
            if(id == R.id.btn_play) { onPlay(); }
            if(id == R.id.btn_share) { onShare(); }
        });
    }

    private void onRec() {
        PermissionUtil.runWithPermission(Manifest.permission.RECORD_AUDIO, this::doRec);
    }

    private void doRec() {
        if(wavFileWriter != null) { wavFileWriter.close(); }

        File file = new File(getTestFilePath());
        wavFileWriter = new WavFileWriter(file, SAMPLE_RATE, NUM_CHANNELS, 16);
        boolean success = wavFileWriter.open();
        if(!success) {
            wavFileWriter = null;
            return;
        }

        int audioSource = MediaRecorder.AudioSource.DEFAULT;
        if(android.os.Build.VERSION.SDK_INT >= 24) {
            audioSource = MediaRecorder.AudioSource.UNPROCESSED;
        }
        int chFormat = AudioFormat.CHANNEL_IN_STEREO;

        micInput = new MicInput(audioSource, chFormat, SAMPLE_RATE);
        boolean micInitOK = micInput.init(getActivity());
        if(!micInitOK) {
            micInput = null;
            wavFileWriter.close();
            wavFileWriter = null;
            return;
        }

        // buffer will contain data of both channels (left, right)
        int floats = (int)(BUFFER_MILLIS * (long) SAMPLE_RATE * NUM_CHANNELS / 1000);
        buffer = new float[BUFFER_COUNT][floats];
        bufferIndex = 0;
        indexInBuffer = 0;

        micInput.connectOutputTo(new AudioCable() {
            @Override
            public void send(float[] sample) { onMicSample(sample); }
            @Override
            public void endOfFrame() {}
            @Override
            public void endOfStream() {}
        });
        micInput.start();
    }

    private void onMicSample(float[] sample) {
        for(float f : sample) {
            buffer[bufferIndex][indexInBuffer++] = f * 50;
        }

        indexInBuffer %= buffer[0].length;

        if(indexInBuffer == 0) { // that buffer full

            float[] filledBuffer = buffer[bufferIndex];

            new Thread() {
                @Override
                public void run() {
                    WavFileWriter w = CvMain.this.wavFileWriter;
                    if(w == null) { return; }
                    setStatus("recording " + w.getDurationMicros());
                    w.write(filledBuffer, 0, filledBuffer.length, false);
                }
            }.start();

            // switch buffer:
            bufferIndex++;
            bufferIndex %= buffer.length;
        }
    }


    private void onStop() {
        if(micInput != null) {
            micInput.stop();
            micInput = null;
        }

        if(wavFileWriter != null) {
            wavFileWriter.close();
            wavFileWriter = null;
        }

        if(noisoid != null) { noisoid.stop(); }

        setStatus("stopped");
    }

    private void onPlay() {
        if(noisoid != null) { noisoid.stop(); }
        noisoid = new Noisoid(SAMPLE_RATE, BUFFER_MILLIS);
        noisoid.start();

        try {
            wavFileReader = new WavFileReader(new File(getTestFilePath()));
        } catch (Exception e) {
            wavFileReader = null;
            noisoid.stop();
            return;
        }

        Source source = new Source() {
            @Override
            public void readTo(float[] buf, int offset, int len) {
                setStatus("playing " + wavFileReader.getPosMicros());
                int read = wavFileReader.read(buf, offset, len, false);
                if(read == 0) { onStop(); }
            }
        };

        noisoid.addSource(source);
    }

    private void onShare() {
        File file = new File(getTestFilePath());
        Uri uri = FileProvider.getUriForFile(
                getActivity(), BuildConfig.APPLICATION_ID + ".provider", file);
        IntentUtil.shareFile(uri, getString(R.string.share), "audio/*");
    }

    static private String getTestFilePath() {
        return getActivity().getFilesDir().getAbsolutePath() + "/rec.wav";
    }

    private void setStatus(String status) {
        runOnUiThread(() -> tvStatus.setText(status));
    }
}