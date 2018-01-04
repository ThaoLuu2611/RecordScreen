package com.example.record_screen;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import android.media.MediaRecorder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.coremedia.iso.IsoFile;
import com.coremedia.iso.boxes.TimeToSampleBox;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.AppendTrack;
import com.googlecode.mp4parser.authoring.tracks.CroppedTrack;

public class MainActivity extends FragmentActivity {

	static final long[] ROTATE_0 = new long[] { 1, 0, 0, 1, 0, 0, 1, 0, 0 };
	static final long[] ROTATE_90 = new long[] { 0, 1, -1, 0, 0, 0, 1, 0, 0 };
	static final long[] ROTATE_180 = new long[] { -1, 0, 0, -1, 0, 0, 1, 0, 0 };
	static final long[] ROTATE_270 = new long[] { 0, -1, 1, 0, 0, 0, 1, 0, 0 };

	private long[] rotate0 = new long[] { 0x00010000, 0, 0, 0, 0x00010000, 0,
			0, 0, 0x40000000 };
	private long[] rotate90 = new long[] { 0, 0x00010000, 0, -0x00010000, 0, 0,
			0, 0, 0x40000000 };
	private long[] rotate180 = new long[] { 0x00010000, 0, 0, 0, 0x00010000, 0,
			0, 0, 0x40000000 };
	private long[] rotate270 = new long[] { -0x00010000, 0, 0, 0, -0x00010000,
			0, 0, 0, 0x40000000 };
	private static ArrayList<String> videosToMerge;
	private static AsyncTask<String, Integer, String> mergeVideos;
	private static String workingPath;
	private static boolean flag;

	//Button btn, btnstop, btnpause;
	private static Context context;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		try {
			setContentView(R.layout.activity_main);
			this.context = this;
			//recordnew();
			videosToMerge = new ArrayList<String>();
			if(videosToMerge.size()>0)videosToMerge.clear();
			if (savedInstanceState == null) {
				
				getSupportFragmentManager().beginTransaction()
						.add(R.id.container, new MainFragment()).commit();
			}

		} catch (Exception e) {
			Log.e("cua do", e.toString());
		}

	}

	
	
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
		private Button mPauseButton;
		
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
				mPauseButton=(Button) rootView.findViewById(R.id.btn_pause);
				mPauseButton.setOnClickListener(PauseOnClickListener);
				mPauseButton.setText("Pause");
			
			} catch (Exception e) {
				Log.e("cua", e.toString());
			}
			return rootView;
		}

		
		private View.OnClickListener PauseOnClickListener= new View.OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				if(flag==false) flag=true;
				else flag=false;
				if(flag==true)
				{
					// stop
					mPauseButton.setText("continue");
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
				else
					{
					flag=false;
					mPauseButton.setText("Pause");
					//start
					StringBuilder stringBuilder = new StringBuilder("screenrecord --verbose ");
					String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime())+".mp4";
					String name= Environment.getExternalStorageDirectory().getAbsolutePath().toString() +"/"+timeStamp;
					stringBuilder
							.append(" ")
							.append(name);
					videosToMerge.add(timeStamp);
					record(stringBuilder.toString());
					}
				
			}
		};
		
		private void rescanSdcard() throws Exception{     
		      Intent scanIntent = new Intent(Intent.ACTION_MEDIA_MOUNTED, 
		                Uri.parse("file://" + Environment.getExternalStorageDirectory()));   
		      IntentFilter intentFilter = new IntentFilter(Intent.ACTION_MEDIA_SCANNER_STARTED);
		      intentFilter.addDataScheme("file");     
		      context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, 
		                Uri.parse("file://" + Environment.getExternalStorageDirectory())));    
		    }
		
		private class MergeVideos extends AsyncTask<String, Integer, String> {

			// The working path where the video files are located
			private String workingPath;
			// The file names to merge
			private ArrayList<String> videosToMerge;
			// Dialog to show to the user
			//private ProgressDialog progressDialog;

			private MergeVideos(String workingPath, ArrayList<String> videosToMerge) {
				this.workingPath = workingPath;
				this.videosToMerge = videosToMerge;
			}

			@Override
			protected void onPreExecute() {
				//progressDialog = ProgressDialog.show(context, "Merging videos","Please wait...", true);
			};

			@Override
			protected String doInBackground(String... params) {
				int count = videosToMerge.size();
				try {
					Movie[] inMovies = new Movie[count];
					for (int i = 0; i < count; i++) {
						File file = new File(workingPath, videosToMerge.get(i));
						if (file.exists()) {
							FileInputStream fis = new FileInputStream(file);
							FileChannel fc = fis.getChannel();
							inMovies[i] = MovieCreator.build(fc);
							fis.close();
							fc.close();
						}
					}
					List<Track> videoTracks = new LinkedList<Track>();
					List<Track> audioTracks = new LinkedList<Track>();

					for (Movie m : inMovies) {
						for (Track t : m.getTracks()) {
							if (t.getHandler().equals("soun")) {
								audioTracks.add(t);
							}
							if (t.getHandler().equals("vide")) {
								videoTracks.add(t);
							}
							if (t.getHandler().equals("")) {

							}
						}
					}

					Movie result = new Movie();

					if (audioTracks.size() > 0) {
						result.addTrack(new AppendTrack(audioTracks
								.toArray(new Track[audioTracks.size()])));
					}
					if (videoTracks.size() > 0) {
						result.addTrack(new AppendTrack(videoTracks
								.toArray(new Track[videoTracks.size()])));
					}
					IsoFile out = new DefaultMp4Builder().build(result);

					// rotate video

					out.getMovieBox().getMovieHeaderBox().setMatrix(ROTATE_270);

					long timestamp = new Date().getTime();
					String timestampS = "" + timestamp;

					File storagePath = new File(workingPath);
					//storagePath.mkdirs();

					File myMovie = new File(storagePath, String.format(
							"output.mp4", timestampS));

					FileOutputStream fos = new FileOutputStream(myMovie);
					FileChannel fco = fos.getChannel();
					fco.position(0);
					out.getBox(fco);
					fco.close();
					fos.close();

				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				String mFileName = Environment.getExternalStorageDirectory()
						.getAbsolutePath();
				mFileName += "/output.mp4";
				//videosToMerge.add("output.mp4");
				//Log.e("TAG",videosToMerge.get(2).toString());
				return mFileName;
			}

			@Override
			protected void onPostExecute(String value) {
				super.onPostExecute(value);
			//	progressDialog.dismiss();
				for(int i=videosToMerge.size()-1;i>=0;i--)
				{
				String name= workingPath+videosToMerge.get(i).toString();
				File file = new File(name);
				boolean deleted = file.delete();
				Log.e("TAG","deleted="+String.valueOf(deleted));
				videosToMerge.remove(i);
				}
				try {
					rescanSdcard();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		}
		
		
		
		@Override
		public void onDestroy() {
			// TODO Auto-generated method stub
			super.onDestroy();
			
		}

		@Override
		public void onPause() {
			// TODO Auto-generated method stub
			super.onPause();
			if(videosToMerge.size()>0)videosToMerge.clear();
			mPauseButton.setText("Pause");
			
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
			
			
			if(videosToMerge.size()>=2)
			{
		Log.e("TAG","size="+videosToMerge.size()+"--name 0 = " + videosToMerge.get(0).toString()+"-----name 1= " +videosToMerge.get(1).toString() +"path="+ workingPath );
		//mergeVideos = 
				new MergeVideos(workingPath, videosToMerge).execute();
			}
			
			
			
			}
		};

		
//public String[] RecordName = new String[100];
public int n=0;
		private View.OnClickListener RecordOnClickListener = new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				flag=false;
				StringBuilder stringBuilder = new StringBuilder("screenrecord --verbose ");
				// "/system/bin/screenrecord");
				String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime())+".mp4";
				String name= Environment.getExternalStorageDirectory().getAbsolutePath().toString()+"/" +timeStamp;
				stringBuilder
						.append(" ")
						.append(name);
				workingPath = Environment.getExternalStorageDirectory() + "/";//Environment.getExternalStorageDirectory().getAbsolutePath().toString()+"/";		
				
				videosToMerge.add(timeStamp);
				//Log.e("TAG", "name  " + (n-1) +" = " + RecordName[n-1]);
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
						//Process process = Runtime.getRuntime().exec(new String[] { "su", "-c"});
						Process process = Runtime.getRuntime().exec(command);

						isReader = new BufferedReader(new InputStreamReader(
								process.getInputStream()));
						esReader = new BufferedReader(new InputStreamReader(
								process.getErrorStream()));

						os = new DataOutputStream(process.getOutputStream());
						//os.writeBytes(command);
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
