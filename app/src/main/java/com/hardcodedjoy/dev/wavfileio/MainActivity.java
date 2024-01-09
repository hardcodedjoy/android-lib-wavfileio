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

import com.hardcodedjoy.appbase.activity.SingleActivity;
import com.hardcodedjoy.appbase.contentview.CvAboutBase;
import com.hardcodedjoy.appbase.contentview.CvTM;
import com.hardcodedjoy.audioinput.AudioInput;
import com.hardcodedjoy.noisoid.Noisoid;

public class MainActivity extends SingleActivity {
    static {
        setInitialCvClass(CvMain.class);
        setSettingsClass(Settings.class);
        CvTM.setSettingsCvClass(CvSettings.class);
        CvAboutBase.setAppVersion(BuildConfig.VERSION_NAME, BuildConfig.TIMESTAMP);
        CvAboutBase.addInfoAboutOpenSourceLib(AudioInput.about());
        CvAboutBase.addInfoAboutOpenSourceLib(Noisoid.about());
    }
}