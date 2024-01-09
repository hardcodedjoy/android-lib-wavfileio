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
import java.io.FileOutputStream;

public class WavFileWriter {

	private long MAX_FILE_SIZE = 4294967295L; // 4GB - 1 Byte
	// static private final long MAX_FILE_SIZE = 20971520; // test (20MB)

	private final File file;
	private FileOutputStream fos;

	private final WavFileHeader wavFileHeader;
	private long fileSize;
	private final int bitsPerSample;
	private final int bytesPerSample;
	private final int blockAlign;
	private byte[] byteBuffer;
	private boolean needNextFile;

	public WavFileWriter(File file, int sampleRate, int numChannels, int bitsPerSample) {
		this.file = file;
		this.wavFileHeader = new WavFileHeader(sampleRate, numChannels, bitsPerSample);
		this.bitsPerSample = bitsPerSample;
		this.bytesPerSample = this.bitsPerSample / 8;
		this.blockAlign = wavFileHeader.getBlockAlign();
		this.needNextFile = false;
	}

	public boolean open() {

		try {
			fos = new FileOutputStream(file);

			// delete old content (if any) from the output file:
			fos.getChannel().truncate(0);

			wavFileHeader.getHeader(fos);
			fileSize = wavFileHeader.getHeaderSize();
			return true; // success

		} catch(Exception e) {
			e.printStackTrace(System.err);
			if(fos != null) {
				try {
					this.fos.close();
				} catch (Exception e1) {
					e1.printStackTrace(System.err);
				}
				fos = null;
			}
			return false;
		}
	}

	public File getFile() { return file; }
	public int getSampleRate() { return (int) wavFileHeader.getSampleRate(); }
	public int getNumChannels() { return wavFileHeader.getNumChannels(); }
	public int getBitsPerSample() { return bitsPerSample; }
	public int getKbps() {
		return (int)((wavFileHeader.getSampleRate() * bitsPerSample) / 1024);
	}

	// assuming len is even!!!
	synchronized public int write(float[] samples, int offset, int len, boolean swapLR) {

		int byteBufferLen = len * bytesPerSample;

		// if wanted to write more bytes than possible:
		if(fileSize + byteBufferLen > MAX_FILE_SIZE) {
			// maximum number of bytes that we can write to this file:
			byteBufferLen = (int)(MAX_FILE_SIZE - fileSize);

			// align:
			byteBufferLen /= blockAlign;
			byteBufferLen *= blockAlign;

			len = byteBufferLen / bytesPerSample;

			needNextFile = true;
			if(byteBufferLen == 0) {
				return 0; // nothing more to write to this file
			}
		}

		if(byteBuffer==null)                        { byteBuffer = new byte[byteBufferLen]; }
		else if(byteBuffer.length != byteBufferLen) { byteBuffer = new byte[byteBufferLen]; }
		FloatBufferToByteBuffer.convert(samples, offset, len, swapLR, bitsPerSample, byteBuffer);

		try {
			fos.write(byteBuffer);
			wavFileHeader.addToSubchunk2Size(byteBufferLen);
			fileSize += byteBufferLen;
			return len; // samples written
		} catch(Exception e) {
			//e.printStackTrace();
			needNextFile = false; // no point to make new file if disk full
			return 0; // disk full
		}
	}

	synchronized public boolean isNeedNextFile() { return needNextFile; }

	synchronized public long getDurationMicros() {
		return wavFileHeader.getDurationMillis() * 1000;
	}

	synchronized public long getFileSizeInBytes() { return wavFileHeader.getFileSizeInBytes(); }

	synchronized public void close() {
		try {
			// write updated wav header to file:

			fos.getChannel().position(0);
			wavFileHeader.getHeader(fos);
			//fos.flush();
			fos.close();

		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	private void setMAX_FILE_SIZE(long sizeInBytes) { MAX_FILE_SIZE = sizeInBytes; } // for debug
}