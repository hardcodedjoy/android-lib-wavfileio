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

package com.hardcodedjoy.wavfileio;

public class FloatBufferToByteBuffer {

    static public void convert(float[] samples, int offset, int len, boolean swapLR, int bitsPerSample, byte[] byteBuffer) {
        long sample;
        float fSample;

        int end = offset + len;

        // non-swap:
        // 0, 1, 2, 3, 4, 5, 6, 7, 8, ...
        //0, 0+1, 2, 2+1, 4, 4+1
        // swap:
        // 1, 0, 3, 2, 5, 4, 7, 6, 9, ...
        //0+1, 0, 2+1, 2, 4+1, 4

        // i+1, i-1, i+1, i-1, ...

        int v = 0; // index variance
        if(swapLR) { v = 1; }

        for(int i=offset, j=0; i<end; i++, v=-v) {

            fSample = samples[i+v];

			/*if(fSample > 1.0f) {
				fSample = 1.0f;
			}
			if(fSample < -1.0f) {
				fSample = -1.0f;
			}*/

            if(fSample > 0.99999f) { fSample = 0.99999f; }
            if(fSample < -0.99999f) { fSample = -0.99999f; }

            if(bitsPerSample==32) {

                sample = (long)(fSample * 2147483647);
                byteBuffer[j++] = (byte)sample; sample = sample >> 8;
                byteBuffer[j++] = (byte)sample; sample = sample >> 8;
                byteBuffer[j++] = (byte)sample; sample = sample >> 8;
                byteBuffer[j++] = (byte)sample;
            }
            else if(bitsPerSample==24) {

                sample = (long)(fSample * 8388607);
                byteBuffer[j++] = (byte)sample; sample = sample >> 8;
                byteBuffer[j++] = (byte)sample; sample = sample >> 8;
                byteBuffer[j++] = (byte)sample;
            }
            else if(bitsPerSample==16) {

                sample = (long)(fSample * 32767);
                byteBuffer[j++] = (byte)sample; sample = sample >> 8;
                byteBuffer[j++] = (byte)sample;
            }
            else if(bitsPerSample==8) {

                sample = ((long)(fSample * 127)) + 127;
                byteBuffer[j++] = (byte)sample;
            }
        }
    }
}
