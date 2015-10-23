package com.plugin.update;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences.Editor;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;

public class UpdateApp extends CordovaPlugin {

	/* version check url */
	private String checkPath;

	private int newVerCode;

	private String newVerName;

	private String releaseNote;

	private String downloadPath;
	/* downloading */
	private static final int DOWNLOAD = 1;
	/* download finished */
	private static final int DOWNLOAD_FINISH = 2;
	/* apk save path */
	private String mSavePath;
	/* download progress */
	private int progress;
	/* cancel update */
	private boolean cancelUpdate = false;

	private Context mContext;

	private ProgressBar mProgress;
	private Dialog mDownloadDialog;

	@Override
	public boolean execute(String action, JSONArray args,
			final CallbackContext callbackContext) throws JSONException {
		this.mContext = cordova.getActivity();
		if (action.equals("checkAndUpdate")) {
			this.checkPath = args.getString(0);

			cordova.getThreadPool().execute(new Runnable() {
				public void run() {
					Looper.prepare();
					boolean r = checkAndUpdate();
					callbackContext.success(r ? 1 : 0);
					Looper.loop();
				}
			});
		} else if (action.equals("getVersionName")) {
			String s = getCurrentVerName();
			callbackContext.success(s);
		} else {
			return false;
		}
		return true;
	}

	/**
	 * check for update
	 */
	private boolean checkAndUpdate() {
		if (getServerVerInfo()) {
			int currentVerCode = getCurrentVerCode();
			if (newVerCode > currentVerCode) {
				showNoticeDialog();
				return true;
			}
		}
		return false;
	}

	/**
	 * get current app version code
	 * 
	 * @param context
	 * @return
	 */
	private int getCurrentVerCode() {
		String packageName = this.mContext.getPackageName();
		int currentVer = -1;
		try {
			currentVer = this.mContext.getPackageManager().getPackageInfo(
					packageName, 0).versionCode;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		return currentVer;
	}

	/**
	 * get current app version name
	 * 
	 * @param context
	 * @return
	 */
	private String getCurrentVerName() {
		String packageName = this.mContext.getPackageName();
		String currentVerName = "";
		try {
			currentVerName = this.mContext.getPackageManager().getPackageInfo(
					packageName, 0).versionName;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		return currentVerName;
	}

	/**
	 * get app name
	 * 
	 * @param context
	 * @return
	 */
	private String getAppName() {
		String package_name = this.mContext.getApplicationContext()
				.getPackageName();
		Resources resources = this.mContext.getApplicationContext()
				.getResources();

		return this.mContext
				.getResources()
				.getText(
						resources.getIdentifier("app_name", "string",
								package_name)).toString();
	}

	/**
	 * get new version from server
	 * 
	 * @param path
	 * @return
	 * @throws Exception
	 */
	private boolean getServerVerInfo() {
		try {
			StringBuilder verInfoStr = new StringBuilder();
			URL url = new URL(checkPath);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(5000);
			conn.setReadTimeout(5000);
			conn.connect();
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					conn.getInputStream(), "UTF-8"), 8192);
			String line = null;
			while ((line = reader.readLine()) != null) {
				verInfoStr.append(line + "\n");
			}
			reader.close();

			if (verInfoStr.length() > 0) {
				JSONObject obj = new JSONObject(verInfoStr.toString());
				newVerCode = obj.getInt("verCode");
				newVerName = obj.getString("verName");
				releaseNote = obj.getString("releaseNote");
				downloadPath = obj.getString("apkPath");
				Log.v("com.plugin.update", "downloadPath");
			}
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}

		return true;

	}

	/**
	 * show update dialog
	 */
	private void showNoticeDialog() {
		AlertDialog.Builder builder = new Builder(mContext);
		String package_name = this.mContext.getApplicationContext()
				.getPackageName();
		Resources resources = this.mContext.getApplicationContext()
				.getResources();

		builder.setTitle(resources.getIdentifier("soft_update_title", "string",
				package_name));
		builder.setMessage("v" + newVerName + "\n" + releaseNote);
		// update now
		builder.setPositiveButton(resources.getIdentifier(
				"soft_update_updatebtn", "string", package_name),
				new OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();

						showDownloadDialog();
					}
				});
		// update later
		builder.setNegativeButton(resources.getIdentifier("soft_update_later",
				"string", package_name), new OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		Dialog noticeDialog = builder.create();
		noticeDialog.show();
	}

	/**
	 * show download dialog
	 */
	private void showDownloadDialog() {
		AlertDialog.Builder builder = new Builder(mContext);
		String package_name = this.mContext.getApplicationContext()
				.getPackageName();
		Resources resources = this.mContext.getApplicationContext()
				.getResources();

		builder.setTitle(resources.getIdentifier("soft_updating", "string",
				package_name));
		// the progress bar
		final LayoutInflater inflater = LayoutInflater.from(mContext);
		View v = inflater.inflate(resources.getIdentifier(
				"softupdate_progress", "layout", package_name), null);
		mProgress = (ProgressBar) v.findViewById(resources.getIdentifier(
				"update_progress", "id", package_name));
		builder.setView(v);
		// cancel update
		builder.setNegativeButton(resources.getIdentifier("soft_update_cancel",
				"string", package_name), new OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				cancelUpdate = true;
			}
		});
		mDownloadDialog = builder.create();
		mDownloadDialog.show();
		// download apk
		downloadApk();
	}

	/**
	 * download apk file
	 */
	private void downloadApk() {
		// start a thread to download
		new downloadApkThread().start();
	}

	private Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case DOWNLOAD:
				mProgress.setProgress(progress);
				break;
			case DOWNLOAD_FINISH:
				installApk();
				break;
			default:
				break;
			}
		};
	};

	/**
	 * download file thread
	 */
	private class downloadApkThread extends Thread {
		@Override
		public void run() {
			try {
				// check if sd card exists and permission to r/w
				if (Environment.getExternalStorageState().equals(
						Environment.MEDIA_MOUNTED)) {
					// sd card path
					String sdpath = Environment.getExternalStorageDirectory()
							+ "/";
					String package_name = cordova.getActivity()
							.getApplicationContext().getPackageName();
					mSavePath = sdpath + package_name + "/apk";
					File dir = new File(mSavePath);
					if (!dir.exists())
						dir.mkdirs();

					URL url = new URL(downloadPath);

					HttpURLConnection conn = (HttpURLConnection) url
							.openConnection();
					conn.connect();
					// get apk file size
					int length = conn.getContentLength();

					InputStream is = conn.getInputStream();

					File file = new File(mSavePath);
					if (!file.exists()) {
						file.mkdir();
					}
					File apkFile = new File(mSavePath, newVerName + ".apk");
					FileOutputStream fos = new FileOutputStream(apkFile);
					int count = 0;

					byte buf[] = new byte[1024];
					// write to file
					do {
						int numread = is.read(buf);
						count += numread;
						// calculate progress
						progress = (int) (((float) count / length) * 100);
						// update progress
						mHandler.sendEmptyMessage(DOWNLOAD);
						if (numread <= 0) {
							// download complete
							mHandler.sendEmptyMessage(DOWNLOAD_FINISH);
							break;
						}
						fos.write(buf, 0, numread);
					} while (!cancelUpdate);// stop download if cancel update
					fos.close();
					is.close();
				}
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			// hide download dialog
			mDownloadDialog.dismiss();
		}
	};

	/**
	 * install apk
	 */
	private void installApk() {
		File apkfile = new File(mSavePath, newVerName + ".apk");
		if (!apkfile.exists()) {
			return;
		}

		Intent i = new Intent(Intent.ACTION_VIEW);
		i.setDataAndType(Uri.parse("file://" + apkfile.toString()),
				"application/vnd.android.package-archive");
		mContext.startActivity(i);
	}

}
