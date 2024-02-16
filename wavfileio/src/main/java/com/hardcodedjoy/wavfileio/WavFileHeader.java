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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class WavFileHeader {

	private int headerSize;
	//private long chunkId;
	private long chunkSize;
	//private long format;

	//private long subchunk1Id;
	private long subchunk1Size;
	private int audioFormat;
	private int numChannels;
	private long sampleRate;
	private long byteRate;
	private int blockAlign;
	private int bitsPerSample;

	//private long subchunk2Id;
	private long subchunk2Size;

	private long durationMillis;


	static private String chunkIdErr = "WavHeader.fromFile() chunkId ERR";
	static private String formatErr = "WavHeader.fromFile() format ERR";
	static private String subchunk1IdErr = "WavHeader.fromFile() subchunk1Id ERR";
	static private String subchunk2IdErr = "WavHeader.fromFile() subchunk2Id ERR";

	public WavFileHeader(int sampleRate, int numChannels, int bitsPerSample) {
		this.sampleRate = sampleRate;
		this.numChannels = numChannels;
		this.bitsPerSample = bitsPerSample;

		subchunk1Size = 16; // 16
		audioFormat = 1; // PCM
		subchunk2Size = 0;

		// 32-bit must be float:
		if(this.bitsPerSample == 32) { this.audioFormat = 3; } // WAVE_FORMAT_IEEE_FLOAT

		this.blockAlign = this.numChannels * this.bitsPerSample/8;
		this.byteRate = this.sampleRate * this.blockAlign;

		this.headerSize = 44; // default 44 Bytes
	}


	static private long read32(InputStream is) throws IOException {
		int a;
		int b;
		int c;
		int d;

		a = is.read() & 0xFF;
		b = is.read() & 0xFF;
		c = is.read() & 0xFF;
		d = is.read() & 0xFF;

		return (((long)d) << 24) + (c << 16) + (b << 8) + a;
	}

	static private void write32(long val, OutputStream os) throws IOException {
		os.write( (int)( (val    ) & 0xFF ) );
		os.write( (int)( (val>> 8) & 0xFF ) );
		os.write( (int)( (val>>16) & 0xFF ) );
		os.write( (int)( (val>>24) & 0xFF ) );
	}

	static private int read16(InputStream is) throws IOException {
		int a;
		int b;

		a = is.read() & 0xFF;
		b = is.read() & 0xFF;

		return (b << 8) + a;
	}

	static private void write16(long val, OutputStream os) throws IOException {
		os.write( (int)( (val    ) & 0xFF ) );
		os.write( (int)( (val>> 8) & 0xFF ) );
	}



	public void addToSubchunk2Size(int val) {
		subchunk2Size += val;
		updateDurationMillis();
	}

	public long getSubchunk2Size() {
		return subchunk2Size;
	}


	static public WavFileHeader fromInputStream(InputStream is) throws Exception {
		WavFileHeader wh = new WavFileHeader(0, 0, 0);

		wh.headerSize = 0;

		if(is.read()!='R') throw new Exception(chunkIdErr);
		if(is.read()!='I') throw new Exception(chunkIdErr);
		if(is.read()!='F') throw new Exception(chunkIdErr);
		if(is.read()!='F') throw new Exception(chunkIdErr);
		wh.headerSize += 4;

		wh.chunkSize = read32(is);
		wh.headerSize += 4;

		//VBLog.log("chunkSize " + wh.chunkSize);

		if(is.read()!='W') throw new Exception(formatErr);
		if(is.read()!='A') throw new Exception(formatErr);
		if(is.read()!='V') throw new Exception(formatErr);
		if(is.read()!='E') throw new Exception(formatErr);
		wh.headerSize += 4;

		if(is.read()!='f') throw new Exception(subchunk1IdErr);
		if(is.read()!='m') throw new Exception(subchunk1IdErr);
		if(is.read()!='t') throw new Exception(subchunk1IdErr);
		if(is.read()!=' ') throw new Exception(subchunk1IdErr);
		wh.headerSize += 4;

		wh.subchunk1Size = read32(is);
		wh.headerSize += 4;

		//VBLog.log("subchunk1Size " + wh.subchunk1Size);

		wh.audioFormat = read16(is);
		wh.headerSize += 2;

		//VBLog.log("audioFormat " + wh.audioFormat);

		wh.numChannels = read16(is);
		wh.headerSize += 2;

		//VBLog.log("numChannels " + wh.numChannels);

		wh.sampleRate = read32(is);
		wh.headerSize += 4;

		//VBLog.log("sampleRate " + wh.sampleRate);

		wh.byteRate = read32(is);
		wh.headerSize += 4;

		wh.blockAlign = read16(is);
		wh.bitsPerSample = read16(is);
		wh.headerSize += 4;

		//VBLog.log("bitsPerSample " + wh.bitsPerSample);

		int[] subchunk2Id = {'d', 'a', 't', 'a'};
		int i = 0;
		int c;

		while (true) {
			c = is.read();
			wh.headerSize++;

			if(c == -1) {
				throw new Exception(subchunk2IdErr);
			} else if(c == subchunk2Id[i]) {
				i++;
			} else {
				i = 0;
				if(c == subchunk2Id[i]) {
					i++;
				}
			}
			if(i == subchunk2Id.length) {
				break; // found "data"
			}
		}

		//if(fis.read()!='d') throw new Exception(subchunk2IdErr);
		//if(fis.read()!='a') throw new Exception(subchunk2IdErr);
		//if(fis.read()!='t') throw new Exception(subchunk2IdErr);
		//if(fis.read()!='a') throw new Exception(subchunk2IdErr);

		wh.subchunk2Size = read32(is);
		wh.headerSize += 4;

		//VBLog.log("subchunk2Size " + wh.subchunk2Size);

		wh.updateDurationMillis();

		return wh;
	}


	public void getHeader(OutputStream os) throws IOException {

		chunkSize = 36 + subchunk2Size;

		// chunkId:
		os.write('R');
		os.write('I');
		os.write('F');
		os.write('F');

		write32(chunkSize, os);

		// format:
		os.write('W');
		os.write('A');
		os.write('V');
		os.write('E');

		// subchunk1Id
		os.write('f');
		os.write('m');
		os.write('t');
		os.write(' ');

		write32(subchunk1Size, os);

		write16(audioFormat, os);
		write16(numChannels, os);
		write32(sampleRate, os);
		write32(byteRate, os);
		write16(blockAlign, os);
		write16(bitsPerSample, os);

		// subchunk2Id
		os.write('d');
		os.write('a');
		os.write('t');
		os.write('a');

		write32(subchunk2Size, os);
	}

	private void updateDurationMillis() {
		//if(byteRate == 0) {
		//	durationMillis = 0;
		//} else {
			durationMillis = (1000 * subchunk2Size) / byteRate;
		//}
	}

	public long getDurationMillis() { return durationMillis; }
	public long getFileSizeInBytes() { return headerSize + subchunk2Size; }
	public int getBitsPerSample() { return bitsPerSample; }
	public int getAudioFormat() { return audioFormat; }
	public int getBlockAlign() { return blockAlign; }
	public int getNumChannels() { return numChannels; }
	public long getSampleRate() { return sampleRate; }
	public int getHeaderSize() { return headerSize; }

}