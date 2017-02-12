package com.topjohnwu.magisk.receivers;

import android.net.Uri;

import com.topjohnwu.magisk.MagiskManager;
import com.topjohnwu.magisk.R;
import com.topjohnwu.magisk.asyncs.FlashZIP;
import com.topjohnwu.magisk.asyncs.SerialTask;
import com.topjohnwu.magisk.utils.Shell;
import com.topjohnwu.magisk.utils.ZipUtils;

import java.io.File;

public class MagiskDlReceiver extends DownloadReceiver {

    String mBoot;
    boolean mEnc, mVerity;

    public MagiskDlReceiver(String bootImage, boolean keepEnc, boolean keepVerity) {
        mBoot = bootImage;
        mEnc = keepEnc;
        mVerity = keepVerity;
    }

    @Override
    public void onDownloadDone(Uri uri) {
        new FlashZIP(activity, uri, mFilename) {

            @Override
            protected void preProcessing() throws Throwable {
                Shell.su(
                        "echo \"BOOTIMAGE=/dev/block/" + mBoot + "\" > /dev/.magisk",
                        "echo \"KEEPFORCEENCRYPT=" + String.valueOf(mEnc) + "\" >> /dev/.magisk",
                        "echo \"KEEPVERITY=" + String.valueOf(mVerity) + "\" >> /dev/.magisk"
                );
            }

            @Override
            protected boolean unzipAndCheck() {
                publishProgress(activity.getString(R.string.zip_install_unzip_zip_msg));
                if (Shell.rootAccess()) {
                    // We might not have busybox yet, unzip with Java
                    // We will have complete busybox after Magisk installation
                    ZipUtils.unzip(mCachedFile, new File(mCachedFile.getParent(), "magisk"));
                    Shell.su(
                            "mkdir -p " + MagiskManager.TMP_FOLDER_PATH + "/magisk",
                            "cp -af " + mCachedFile.getParent() + "/magisk/. " + MagiskManager.TMP_FOLDER_PATH + "/magisk",
                            "mv -f " + mCachedFile.getParent() + "/magisk/META-INF " + mCachedFile.getParent() + "/META-INF"
                    );
                }
                return true;
            }

            @Override
            protected void onSuccess() {
                new SerialTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        Shell.su("setprop magisk.version "
                                + String.valueOf(((MagiskManager) activity.getApplicationContext()).remoteMagiskVersion));
                        return null;
                    }
                }.exec();
                super.onSuccess();
            }
        }.exec();
    }
}
