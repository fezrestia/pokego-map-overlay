package com.fezrestia.android.localcheckpointscheduler.storage;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;

import com.fezrestia.android.localcheckpointscheduler.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StorageController {
    // Log tag.
    private static final String TAG = StorageController.class.getSimpleName();

    // Master context.
    private Context mContext = null;

    // Constants.
    private static final String ROOT_DIR_PATH = "LocalCheckpointScheduler";
    private static final String PNG_FILE_EXTENSION = ".PNG";

    // File name format.
    private static final SimpleDateFormat FILE_NAME_SDF
            = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");

    // Worker.
    private ExecutorService mBackWorker = null;

    /**
     * CONSTRUCTOR.
     *
     * @param context
     */
    public StorageController(Context context) {
        mContext = context;

        createContentsRootDirectory();

        // Worker.
        mBackWorker = Executors.newSingleThreadExecutor();
    }

    /**
     * Release all resources.
     */
    public void release() {
        if (mBackWorker != null) {
            mBackWorker.shutdown();
            mBackWorker = null;
        }

        mContext = null;
    }

    private static String getApplicationStorageRootPath() {
        return Environment.getExternalStorageDirectory().getPath() + "/" + ROOT_DIR_PATH;
    }

    private static boolean createNewContentsDirectory(String contentsDirName) {
        String rootDirPath = getApplicationStorageRootPath();
        String contentsDirPath = rootDirPath + "/" + contentsDirName;

        File newDir = new File(contentsDirPath);

        return createDirectory(newDir);
    }

    private static void createContentsRootDirectory() {
        File file = new File(getApplicationStorageRootPath());
        createDirectory(file);
    }

    private static boolean createDirectory(File dir) {
        boolean isSuccess = false;

        try {
            if (!dir.exists()) {
                //if directory is not exist, create a new directory
                isSuccess = dir.mkdirs();
            } else {
                if (Log.IS_DEBUG) Log.logDebug(TAG, "Directory is already exists.");
            }
        } catch (SecurityException e) {
            e.printStackTrace();
            isSuccess = false;
        }

        if (!isSuccess) {
            if (Log.IS_DEBUG) Log.logDebug(TAG, "createDirectory() : FAILED");
        }

        return isSuccess;
    }

    /**
     * Store file in background.
     *
     * @param buffer
     * @param dir
     */
    public void storeFile(byte[] buffer, String dir) {
        if (Log.IS_DEBUG) Log.logDebug(TAG, "storeFile() : E");

        // Create directory.
        createNewContentsDirectory(dir);

        // File name.
        Calendar calendar = Calendar.getInstance();
        final String fileName = FILE_NAME_SDF.format(calendar.getTime());

        StoreFileTask task = new StoreFileTask(
                mContext,
                buffer,
                getApplicationStorageRootPath() + "/" +  dir + "/" + fileName + PNG_FILE_EXTENSION);

        // Execute.
        mBackWorker.execute(task);

        if (Log.IS_DEBUG) Log.logDebug(TAG, "storeFile() : X");
    }

    private static class StoreFileTask implements Runnable {
        private final Context mContext;
        private final byte[] mBuffer;
        private final String mFileFullPath;

        /**
         * CONSTRUCTOR.
         *
         * @param context
         * @param buffer
         * @param fileFullPath
         */
        public StoreFileTask(
                Context context,
                byte[] buffer,
                String fileFullPath) {
            mContext = context;
            mBuffer = buffer;
            mFileFullPath = fileFullPath;
        }

        @Override
        public void run() {
            // Store.
            boolean isSuccess = byte2file(mBuffer, mFileFullPath);

            if (!isSuccess) {
                return;
            }

            final CountDownLatch latch = new CountDownLatch(1);

            // Request update Media D.B.
            MediaScannerNotifier notifier = new MediaScannerNotifier(
                    mContext,
                    mFileFullPath,
                    latch);
            notifier.start();

            try {
                latch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
                throw new UnsupportedOperationException("Why thread is interrupted ?");
            }
        }
    }

    private static boolean byte2file(byte[] data, String fileName) {
        if (Log.IS_DEBUG) Log.logDebug(TAG, "byte2file() : E");
        if (Log.IS_DEBUG) Log.logDebug(TAG, "PATH=" + fileName);

        FileOutputStream fos;

        // Open stream.
        try {
            fos = new FileOutputStream(fileName);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            if (Log.IS_DEBUG) Log.logError(TAG, "File not found.");
            return false;
        }

        // Write data.
        try {
            fos.write(data);
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
            if (Log.IS_DEBUG) Log.logError(TAG, "File output stream I/O error.");

            // Close
            try {
                fos.close();
            } catch (IOException e1) {
                e.printStackTrace();
                if (Log.IS_DEBUG) Log.logError(TAG, "File output stream I/O error.");
                return false;
            }
            return false;
        }

        if (Log.IS_DEBUG) Log.logDebug(TAG, "byte2file() : X");
        return true;
    }

    private static class MediaScannerNotifier
            implements MediaScannerConnection.MediaScannerConnectionClient {
        private static final String TAG = MediaScannerNotifier.class.getSimpleName();

        private final Context mContext;
        private final String mPath;
        private final CountDownLatch mLatch;

        private MediaScannerConnection mConnection = null;
        private Uri mUri = null;

        /**
         * CONSTRUCTOR.
         *
         * @param context
         * @param path
         * @param latch
         */
        public MediaScannerNotifier(Context context, String path, CountDownLatch latch) {
            mContext = context;
            mPath = path;
            mLatch = latch;
        }

        /**
         * Start scan.
         */
        public void start() {
            mConnection = new MediaScannerConnection(mContext, this);
            mConnection.connect();
        }

        @Override
        public void onMediaScannerConnected() {
            if (Log.IS_DEBUG) Log.logDebug(TAG, "onMediaScannerConnected()");

            mConnection.scanFile(mPath, null);
        }

        @Override
        public void onScanCompleted(String path, Uri uri) {
            if (Log.IS_DEBUG) Log.logDebug(TAG, "onScanCompleted()");
            if (Log.IS_DEBUG) Log.logDebug(TAG, "URI=" + uri.getPath());

            mUri = uri;

            // Notify.
            mLatch.countDown();

            //disconnect
            mConnection.disconnect();
        }

        public Uri getUri() {
            return mUri;
        }
    }
}