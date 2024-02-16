# WavFileIO

<code>com.hardcodedjoy.wavfileio</code> <code>v1.0.1</code><br/>
minSdkVersion: <code>21</code><br/>
targetSdkVersion: <code>33</code><br/>

## Short description

Android library for reading and writing *.wav files


## Description

Read / Write *.wav files.

<code>public WavFileReader(File file) throws Exception { ... }</code><br/>
<code>public int read(float[] dest, int offset, int len, boolean swapLR) { ... }</code><br/>
<code>public void close() { ... }</code>

<code>public WavFileWriter(File file, int sRate, int numCh, int bitsPerSample) { ... }</code><br/>
<code>boolean open() { ... }</code><br/>
<code>public int write(float[] samples, int offset, int len, boolean swapLR) { ... }</code><br/>
<code>public void close() { ... }</code>

This repo also contains an android project that is a testbed app. See its code for more details about using the library.


## Links

developer website: [https://hardcodedjoy.com](https://hardcodedjoy.com)<br/>

