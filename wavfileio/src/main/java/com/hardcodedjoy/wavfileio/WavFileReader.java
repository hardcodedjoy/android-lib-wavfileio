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

import java.io.File;
import java.io.FileInputStream;

public class WavFileReader {

	private final File file;
	private final FileInputStream fis;
	private final WavFileHeader wavFileHeader;

	private final int numChannels;
	private final long sampleRate;
	private final int bitsPerSample;
	private final int wavAudioFormat;
	private final int blockAlign;
	private long durationMicros;

	private long posSamplesMultiChannel;

	private final byte[] byteBuffer;
	@SuppressWarnings("FieldCanBeLocal")
	private int bbi;
	@SuppressWarnings("FieldCanBeLocal")
	private int sample;
	private float fSample;
	@SuppressWarnings("FieldCanBeLocal")
	private int v;
	@SuppressWarnings("FieldCanBeLocal")
	private int i;

	public WavFileReader(File file) throws Exception {
		this.file = file;
		fis = new FileInputStream(file);
		wavFileHeader = WavFileHeader.fromInputStream(fis);
		numChannels = wavFileHeader.getNumChannels();
		sampleRate = wavFileHeader.getSampleRate();
		bitsPerSample = wavFileHeader.getBitsPerSample();
		wavAudioFormat = wavFileHeader.getAudioFormat();
		blockAlign = wavFileHeader.getBlockAlign();
		durationMicros = wavFileHeader.getDurationMillis()*1000;

		byteBuffer = new byte[8000000]; // big enough
	}

	int fillByteBuffer(byte[] dest, int offset, int bytesToRead) {
		int bytesRead;

        try {
			bytesRead = fis.read(dest, offset, bytesToRead);
        } catch (Exception e) {
            e.printStackTrace(System.err);
			bytesRead = -1;
        }

		if(bytesRead == -1) {
			// end of stream
            //VBLog.log("AFR Wav EOS");
			posSamplesMultiChannel = ((durationMicros + 5000)/10000)*(sampleRate/100);
			bytesRead = 0;
		} else {
			// actual samples read:
            posSamplesMultiChannel += (bytesRead / (bitsPerSample/8) / numChannels);
		}

        //VBLog.log("AFR Wav filed byte buffer with " + bytesRead + " bytes");
		return bytesRead; // actual number
	}


	public long onSeek(long posMicros) {
		//VBLog.log("onSeek " + posMicros);

		if(fis == null) { return posMicros; }
		if(wavFileHeader == null) { return posMicros; }

		try {
			// posMicros * sampleRate => max. 9,223,372,036,854,775,807
			// for 44100 Hz -> posMicros max. 2.09 x 10^14 -> 209146758 seconds -> 6.6 years
			long n = ( (posMicros*sampleRate) / 1000000 ) * blockAlign;
			fis.getChannel().position(wavFileHeader.getHeaderSize() + n);
			posMicros = ( (n/blockAlign) * 1000000 ) / sampleRate;
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
		return posMicros;
	}

	public void onClose() {
		try {
			fis.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
		durationMicros = 0;
	}

	synchronized public long getDurationMicros() {
		return durationMicros;
	}
	synchronized public long getPosMicros() {
		if(posSamplesMultiChannel == Long.MAX_VALUE) { // value set by EOS
			return durationMicros;
		} else {
			return (posSamplesMultiChannel * 1000000) / sampleRate; // works for max. len. of 6.6years @ 44100Hz
		}
	}
	synchronized public void seek(long posMicros) {
		// limit:
		if(posMicros < 0) {
			posMicros = 0;
		} else if(posMicros > durationMicros) {
			posMicros = durationMicros;
		}
		posMicros = onSeek(posMicros);
		posSamplesMultiChannel = (posMicros * sampleRate) / 1000000; // works for max. len. of 6.6years @ 44100Hz
	}
	synchronized public void close() {
		onClose();
	}

	synchronized public int read(float[] dest, int offset, int len, boolean swapLR) {

		// len = number of floats -> single-channel samples
		// convert to number of bytes:
		int bytesToRead = len * (bitsPerSample/8);
		int bytesRead = fillByteBuffer(byteBuffer, 0, bytesToRead);
		int samplesRead = bytesRead / (bitsPerSample/8);

		// fill dest[] from byte[] buffer:

		// non-swap:
		// 0, 1, 2, 3, 4, 5, 6, 7, 8, ...
		//0, 0+1, 2, 2+1, 4, 4+1
		// swap:
		// 1, 0, 3, 2, 5, 4, 7, 6, 9, ...
		//0+1, 0, 2+1, 2, 4+1, 4

		// i+1, i-1, i+1, i-1, ...

		v = 0; // index variance
		if(swapLR) { v = 1; }

		for(i=0; i<samplesRead; i++, v=-v) {

			sample = 0;

			if(bitsPerSample==32) {

				bbi = i*4;
				sample  = (sample     ) + (byteBuffer[bbi+3] & 0xFF);
				sample  = (sample << 8) + (byteBuffer[bbi+2] & 0xFF);
				sample  = (sample << 8) + (byteBuffer[bbi+1] & 0xFF);
				sample  = (sample << 8) + (byteBuffer[bbi  ] & 0xFF);

				if(wavAudioFormat == 3) {
					fSample = Float.intBitsToFloat(sample);
				} else {
					fSample = sample / 2147483648.0f;
				}
			}
			else if(bitsPerSample==24) {

				bbi = i*3;
				sample  = (sample     ) + (byteBuffer[bbi+2] & 0xFF);
				sample  = (sample << 8) + (byteBuffer[bbi+1] & 0xFF);
				sample  = (sample << 8) + (byteBuffer[bbi  ] & 0xFF);

				// store sign bit:
				sample  = sample << 8;
				sample  = sample >> 8;

				fSample = sample/8388608.0f;
			}
			else if(bitsPerSample==16) {

				bbi = i*2;
				sample  = (sample     ) + (byteBuffer[bbi+1] & 0xFF);
				sample  = (sample << 8) + (byteBuffer[bbi  ] & 0xFF);

				// store sign bit:
				sample  = sample << 16;
				sample  = sample >> 16;

				fSample = sample/32768.0f;
			}
			else if(bitsPerSample==8) {

				sample = (byteBuffer[i] & 0xFF);
				// 8-bit is unsigned

				fSample = (sample - 128) / 128.0f;
			}

			// log("dest index " + (offset + i + v) + " / " + dest.length);

			dest[offset + i + v] = fSample;
		}

		return samplesRead;
	}
}