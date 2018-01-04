package com.example.record_screen;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.io.*;
import java.util.*;

import org.apache.http.NameValuePair;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.telephony.SignalStrength;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends FragmentActivity {

	Button btn, btnstop;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		try {
			setContentView(R.layout.activity_main);

			if (savedInstanceState == null) {
				getSupportFragmentManager().beginTransaction()
						.add(R.id.container, new MainFragment()).commit();
			}

		} catch (Exception e) {
			Log.e("cua do", e.toString());
		}

	}

	@Override
	protected void onPause() {
		super.onPause();

	}

	public static class MainFragment extends Fragment {
		private Context mContext;

		private View rootView;
		private EditText mWidthEditText;
		private EditText mHeightEditText;
		private EditText mBitrateEditText;
		private EditText mTimeEditText;
		private Button mRecordButton;
		private Button mStopButton;

		public MainFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			try {
				rootView = inflater.inflate(R.layout.fragment_main, container,
						false);
				mContext = getActivity();
				mRecordButton = (Button) rootView.findViewById(R.id.btn_record);
				mRecordButton.setOnClickListener(RecordOnClickListener);
				mStopButton = (Button) rootView.findViewById(R.id.btn_top);
				mStopButton.setOnClickListener(StopOnClickListener);

			
			} catch (Exception e) {
				Log.e("cua", e.toString());
			}
			return rootView;
		}

		public static final int SIGNAL_KILL = 1;
		private View.OnClickListener StopOnClickListener = new View.OnClickListener() {

			@SuppressWarnings("deprecation")
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				String pid = null;
				Process p;
				try {
					p = Runtime.getRuntime().exec("ps");
					p.waitFor();
					StringBuffer sb = new StringBuffer();
					InputStreamReader isr = new InputStreamReader(
							p.getInputStream());
					int ch;
					char[] buf = new char[1024];
					try {
						while ((ch = isr.read(buf)) != -1) {
							sb.append(buf, 0, ch);
						}
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					String[] processLinesAr = sb.toString().split("\n");
					for (String line : processLinesAr) {
						String[] comps = line.split("[\\s]+");
						if (comps.length == 9) {
							if (comps[8].equalsIgnoreCase("screenrecord")) {
								pid = comps[1];
							   android.os.Process.sendSignal(Integer.parseInt(pid),SIGNAL_KILL);	
							}
						}
					}

				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};

		

		private View.OnClickListener RecordOnClickListener = new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				StringBuilder stringBuilder = new StringBuilder("screenrecord "); // --verbose
																					// ");
				// "/system/bin/screenrecord");
				
				stringBuilder
						.append(" ")
						.append(Environment.getExternalStorageDirectory()
								.getAbsolutePath().toString())
						.append("/HethAlephTawHethWawWaw"+"recording.mp4"+ new SimpleDateFormat("ddMMyyyyHHmmss").format(new Date(0)));
				Log.d("TAG", "comamnd: " + stringBuilder.toString());
				record(stringBuilder.toString());
				
			}
		};

	
		private void record(final String command) {
			Log.d(getClass().getSimpleName(), "record()");

			final StringBuilder sbConsole = new StringBuilder();
			final StringBuilder sbErrors = new StringBuilder();

			AsyncTask<Void, Void, Void> recordTask = new AsyncTask<Void, Void, Void>() {
				@Override
				protected Void doInBackground(Void... objects) {
					Log.d(getClass().getSimpleName(),
							"recordTask.doInBackground()");

					DataOutputStream os = null;
					BufferedReader isReader = null;
					BufferedReader esReader = null;

					try {
						long start = System.currentTimeMillis();
						Process process = Runtime.getRuntime().exec(command);

						isReader = new BufferedReader(new InputStreamReader(
								process.getInputStream()));
						esReader = new BufferedReader(new InputStreamReader(
								process.getErrorStream()));

						os = new DataOutputStream(process.getOutputStream());
						os.writeBytes("exit\n");
						os.flush();

						String line;
						while ((line = esReader.readLine()) != null) {
							;
							sbErrors.append(line);
						}
						while ((line = isReader.readLine()) != null) {
							sbConsole.append(line);
						}

						process.waitFor();

						long end = System.currentTimeMillis();
						Log.d(getClass().getSimpleName(),
								"recordTask.doInBackground(); elapsed: "
										+ (end - start) + " ms");

					} catch (IOException e) {
						Log.e(getClass().getSimpleName(), "Exception: ", e);
					} catch (InterruptedException e) {
						Log.e(getClass().getSimpleName(), "Exception: ", e);
					}

					return null;
				}

				@Override
				protected void onPostExecute(Void result) {
					Log.i(getClass().getSimpleName(),
							"Output: " + sbConsole.toString());
					Log.e(getClass().getSimpleName(),
							"Errors: " + sbErrors.toString());
				}
			};

			recordTask.execute();

		}

	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
