package darack.com.player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import java.util.StringTokenizer;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import darack.com.player.WaveformView.WaveformListener;
import darack.com.player.soundfile.CheapSoundFile;

public class ArtPlayerActivity extends Activity implements WaveformListener,
		OnClickListener, Runnable, OnCompletionListener {

	// 사용자 코드
	public static final int REQUEST_FILELIST = 1001;
	public static final int REQUEST_PLAYLIST = 1002;
	public static final int REQUEST_OPTION = 1003;
	private static final String TAG = "TEST_DEBUG";
	private static final String BASEPATH = Environment
			.getExternalStorageDirectory().toString() + "/";

	// SharedPreference key
	public static final String PLAYING = "PLAYING";
	public static final String RANDOM = "RANDOM";
	public static final String LOOPING = "LOOPING";
	// SharedPreference LOOPING 값
	public static final int LOOPING_NONE = 1;
	public static final int LOOPING_ONE = 2;
	public static final int LOOPING_ALL = 3;

	// 딜레이 타입
	public static final int DELAY_QUIT = 201;
	public static final int DELAY_SEARCH = 202;

	public static final int LOAD_BLANK_LIST = 301;
	public static final int DINAMIC_VIEW_ID = 5000;

	// 절전모드 방지
	PowerManager.WakeLock mWakeLock;

	// mp3 목록
	// public static ArrayList<Music> songs = new ArrayList<Music>();
	public static ArrayList<String> playlist = new ArrayList<String>();

	// private AdView adview;

	// mp3파일 파장분석
	private ProgressDialog mProgressDialog;
	private CheapSoundFile mSoundFile;
	// private long mLoadingStartTime;
	private long mLoadingLastUpdateTime;
	private boolean mLoadingKeepGoing;
	private int btnNum;

	private WaveformView mWaveformView;

	private ArrayList<Integer> posList;
	private LinearLayout linear;

	private Resources r;
	private NotificationManager nm;

	private TextView titlebar;
	private TextView title;
	// private TextView artist;
	// private TextView album_name;
	private TextView time;
	private TextView zero;

	private ImageView rew_btn;
	// private ImageView stop_btn;
	private ImageView play_btn;
	// private ImageView pause_btn;
	private ImageView ff_btn;
	private ImageView a_rew_btn;
	private ImageView a_ff_btn;

	private ImageView list_btn;
	private ImageView list_btn2;

	private ImageView looping;
	private ImageView random;

	private ImageView play_AB;
	private TextView atob;

	private ImageView album_art;

	private static MediaPlayer mp;
	private static AISeeker ai;

	private SeekBar seekbar;

	private Thread mp3t;
	private int myDuration;

	private int gab;
	private boolean isSetted;
	private boolean isbackPressed;
	private String playingMp3name;
	private int songPos;
	private boolean isSeeking;
	private boolean isRandom;
	private int isLooping;
	private boolean isSettedAB;
	private boolean isStartingAB;
	private boolean isPlayingAB;
	private int[] playAB;

	private int mWidth;
	private int mMaxPos;
	private int mStartPos;
	private int mEndPos;
	// private boolean mStartVisible;
	// private boolean mEndVisible;
	// private int mLastDisplayedStartPos;
	// private int mLastDisplayedEndPos;
	private int mOffset;
	private int mOffsetGoal;
	private int mFlingVelocity;
	// private int mPlayStartMsec;
	private int mPlayStartOffset;
	// private int mPlayEndMsec;
	private boolean mIsPlaying;
	// private boolean mCanSeekAccurately;
	private boolean mTouchDragging;
	private float mTouchStart;
	private int mTouchInitialOffset;
	// private int mTouchInitialStartPos;
	// private int mTouchInitialEndPos;
	private long mWaveformTouchStartMsec;
	private float mDensity;

	private final Handler h = new Handler();
	private boolean done;

	// 설정
	private SharedPreferences preference;
	private Editor editor;

	// 메세지 핸들러
	public Handler handler = new Handler() {
		@Override
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case AISeeker.MSG_PLAY:
				play();
				break;
			case AISeeker.MSG_PAUSE:
				pause();
				break;
			case LOAD_BLANK_LIST:
				// 이전 공백 리스트 없애기
				removeBlankList();
				// 공백 리스트 로드하기
				loadBlankList();

				finishOpeningSoundFile();
				break;
			}
		}

	};

	// 전화 브로드캐스트 받기
	BroadcastReceiver mMyBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			TelephonyManager tm = (TelephonyManager) context
					.getSystemService(Context.TELEPHONY_SERVICE);
			tm.listen(new PhoneStateListener() {

				@Override
				public void onCallStateChanged(int state, String incomingNumber) {
					Log.d(TAG, "     phone call state changed");
					if (state == TelephonyManager.CALL_STATE_RINGING) {
						pause();
					}

					super.onCallStateChanged(state, incomingNumber);
				}

			}, PhoneStateListener.LISTEN_CALL_STATE);
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.player);

		Log.d(TAG, "Creating ArtPlayerActivity...");

		// mp3 리스트 업데이트
		// updateSongList();

		r = getResources();

		// 이동 간격
		gab = 2000;
		// 파일 셋팅 여부
		isSetted = false;
		isbackPressed = false;
		done = false;
		songPos = -1;
		isSeeking = false;
		isSettedAB = false;
		isStartingAB = false;
		isPlayingAB = false;

		btnNum = 0;

		playAB = new int[2];

		Log.d(TAG, "Getting views...");

		// view setting
		initializeView();

		// /////////////////////////////
		mIsPlaying = false;
		mPlayStartOffset = 0;
		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);
		mDensity = metrics.density;
		// ////////////////////////////

		mp = new MediaPlayer();
		mp.setOnCompletionListener(this);
		mp.setScreenOnWhilePlaying(true);

		Log.d(TAG, "setted views.");

		ai = new AISeeker();

		// Get Notification Service
		nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

		// 설정을 불러와 셋팅하기
		loadPreperences();

		// 기본으로 재생할 음악 셋팅
		Log.d(TAG, "load preference : " + playingMp3name);
		// setMusic(playingMp3name);
		if (playlist.size() > 0) {
			setMusic(playlist.get(0));
		}

		// 광고 로드(Admob)
		// adview = (AdView) findViewById(R.id.ad);
		// AdRequest re = new AdRequest();
		// // re.setTesting(true);
		// adview.loadAd(re);

		// 전화수신 브로드캐스트 리시버 등록
		registerReceiver(mMyBroadcastReceiver, new IntentFilter(
				"android.intent.action.PHONE_STATE"));

		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "My Tag");

	}

	private void initializeView() {
		titlebar = (TextView) findViewById(R.id.titlebar);
		title = (TextView) findViewById(R.id.title);
		// artist = (TextView) findViewById(R.id.artist);
		// album_name = (TextView) findViewById(R.id.album_name);

		title.setSelected(true);

		time = (TextView) findViewById(R.id.time);
		zero = (TextView) findViewById(R.id.zero);

		rew_btn = (ImageView) findViewById(R.id.rew_btn);
		play_btn = (ImageView) findViewById(R.id.play_btn);
		ff_btn = (ImageView) findViewById(R.id.ff_btn);
		a_rew_btn = (ImageView) findViewById(R.id.a_rew_btn);
		a_ff_btn = (ImageView) findViewById(R.id.a_ff_btn);

		list_btn = (ImageView) findViewById(R.id.list_btn);
		list_btn2 = (ImageView) findViewById(R.id.list_btn2);

		mWaveformView = (WaveformView) findViewById(R.id.waveform);
		mWaveformView.setListener(this);

		looping = (ImageView) findViewById(R.id.looping);
		random = (ImageView) findViewById(R.id.random);

		play_AB = (ImageView) findViewById(R.id.play_ab);
		atob = (TextView) findViewById(R.id.atob);

		album_art = (ImageView) findViewById(R.id.album_art);

		linear = (LinearLayout) findViewById(R.id.blank_list_layout);

		// 재생파일 SeekBar
		seekbar = (SeekBar) findViewById(R.id.seekbar);
		seekbar.incrementProgressBy(1);
		seekbar.setIndeterminate(false);
		seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				if (isSetted) {
					mp.seekTo(seekBar.getProgress());
				}
				isSeeking = false;
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				isSeeking = true;

			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				// TODO Auto-generated method stub
				zero.setText(changeToMinutes(progress));
			}
		});

		Log.d(TAG, "got views.");

		rew_btn.setOnClickListener(this);
		// stop_btn.setOnClickListener(this);
		play_btn.setOnClickListener(this);
		// pause_btn.setOnClickListener(this);
		ff_btn.setOnClickListener(this);
		a_rew_btn.setOnClickListener(this);
		a_ff_btn.setOnClickListener(this);

		list_btn.setOnClickListener(this);
		list_btn2.setOnClickListener(this);

		// album_art.setOnClickListener(this);
		random.setOnClickListener(this);
		looping.setOnClickListener(this);
		play_AB.setOnClickListener(this);
	}

	private void loadBlankList() {
		// 공백 위치 모두 가져오기
		posList = ai.getQuestionPos(mSoundFile);

		for (int i = 0; i < posList.size(); i++) {
			Button blankBtn = new Button(this);
			blankBtn.setId(DINAMIC_VIEW_ID + btnNum);
			blankBtn.setHeight(50);
			blankBtn.setText("No." + (i + 1));
			blankBtn.setBackgroundColor(Color.TRANSPARENT);
			blankBtn.setTextColor(Color.WHITE);
			blankBtn.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					mp.seekTo(posList.get(v.getId() - DINAMIC_VIEW_ID));
					if (!mp.isPlaying()) {
						play();
					}
				}
			});

			btnNum++;

			linear.addView(blankBtn);
		}
	}

	private void setNotification() {
		Intent intent = new Intent(this, ArtPlayerActivity.class);

		// 액티비티 중복 실행 방지
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
				| Intent.FLAG_ACTIVITY_CLEAR_TOP
				| Intent.FLAG_ACTIVITY_SINGLE_TOP);

		PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent,
				PendingIntent.FLAG_UPDATE_CURRENT);

		String title = "Title";
		String artist = "Artist";

		if (songPos != -1) {
			// 재생 파일 정보 셋팅 필요
			Music musicInfo = Mp3TagReader.ReadMp3Info(playlist.get(songPos));

			title = musicInfo.getJemok();
			artist = musicInfo.getGasu();
		}

		// Create Notification Object
		Notification notification = new Notification(
				R.drawable.notification_icon, title, System.currentTimeMillis());

		// 진행 중에 표시하기
		notification.flags = Notification.FLAG_ONGOING_EVENT;
		notification.setLatestEventInfo(this, title, artist, pIntent);

		nm.notify(1234, notification);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		super.onCreateOptionsMenu(menu);

		menu.add(0, 0, 0, R.string.option_setting).setIcon(
				android.R.drawable.ic_menu_preferences);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		if (item.getItemId() == 0) {
			startActivityForResult(new Intent(this, SettingActivity.class),
					REQUEST_OPTION);
			return true;
		}
		return false;
	}

	private void loadPreperences() {
		// 설정 가져오기
		preference = PreferenceManager.getDefaultSharedPreferences(this);
		editor = preference.edit();
		isRandom = preference.getBoolean(RANDOM, false);
		isLooping = preference.getInt(LOOPING, LOOPING_NONE);

		// 이전 재생목록 로딩
		// playingMp3name = preference.getString(PLAYING, "");
		String str = preference.getString(PLAYING, "");
		StringTokenizer st = new StringTokenizer(str, "|");
		Log.d(TAG, "Loading List : " + str);
		Log.d(TAG, "Tokens : " + st.countTokens());

		playlist.clear();
		String temp = "";
		File file = null;
		while (st.hasMoreTokens()) {
			temp = st.nextToken();
			Log.d(TAG, "filename from preferences : " + temp);

			file = new File(temp);
			if (file.exists()) {
				// 존재하면 리스트 추가
				try {
					playlist.add(temp);
				} catch (IllegalArgumentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalStateException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			file = null;
		}

		// 자동 탐색 셋팅
		ai.setSpace(Integer.parseInt(preference.getString("SPACE", "4000")));
		ai.setTestCount(Integer.parseInt(preference
				.getString("TESTCOUNT", "12")));
		ai.setCondition(Integer.parseInt(preference.getString("CONDITION", "5")));

		// 탐색 이동 간격 셋팅
		gab = Integer.parseInt(preference.getString("GAB", "5000"));

		Log.d(TAG, "ai GAB : " + gab);
		Log.d(TAG, "ai SPACE : " + ai.getSpace());
		Log.d(TAG, "ai TESTCOUNT : " + ai.getTestCount());
		Log.d(TAG, "ai CONDITION : " + ai.getCondition());

		// 랜덤, 반복 셋팅
		setLooping(true);
		setRandom(true);
	}

	private void setMusic(String path) {
		// 파일 로딩 중일 경우 완료 할 때까지 대기
		while (mLoadingKeepGoing) {
			// new DelayTask().execute(0, 1000);
		}

		// 파일 존재 여부 체크
		Log.d(TAG, "checking test.mp3 if the file is existing...");
		File file = new File(path);
		if (file.exists()) {
			// 존재하면 파일 셋팅
			Log.d(TAG, "existed...");
			try {
				mp.reset();
				mp.setDataSource(path);
				mp.prepare();
				isSetted = true;
				// mp.setLooping(false);
				Log.d(TAG, "MP3 FILE 【" + path + "】 LOAD COMPLETE.");
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalStateException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			myDuration = mp.getDuration();
			time.setText(changeToMinutes(myDuration));
			seekbar.setMax(myDuration);
			playingMp3name = path;

			// 파일 정보를 view에 셋팅 필요
			if (!playlist.contains(path)) {
				playlist.add(path);
			}
			songPos = playlist.indexOf(path);

			Music musicInfo = Mp3TagReader.ReadMp3Info(path);
			title.setText(musicInfo.getJemok() + " - " + musicInfo.getGasu());

			// 앨범아트 셋팅
			Drawable img = musicInfo.getAlbumArt();
			if (img != null) {
				album_art.setImageDrawable(img);
			} else {
				album_art.setImageResource(R.drawable.m_icon);
			}

			mp3t = new Thread(this);

			// 진행중에 표시
			setNotification();

			// mp3 파형 분석
			loadMP3(file);

			isSetted = true;
		} else {
			// 없으면 파일 셋팅 안함
			isSetted = false;
		}
	}

	private void loadMP3(final File mFile) {
		// 파형 분석을 위한 파일 로딩
		// mLoadingStartTime = System.currentTimeMillis();
		mLoadingLastUpdateTime = System.currentTimeMillis();
		mLoadingKeepGoing = true;
		mProgressDialog = new ProgressDialog(this);
		mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		mProgressDialog.setTitle("Loading...");
		mProgressDialog.setCancelable(true);
		mProgressDialog
				.setOnCancelListener(new DialogInterface.OnCancelListener() {
					@Override
					public void onCancel(DialogInterface dialog) {
						mLoadingKeepGoing = false;
					}
				});
		mProgressDialog.show();

		final CheapSoundFile.ProgressListener listener = new CheapSoundFile.ProgressListener() {
			@Override
			public boolean reportProgress(double fractionComplete) {
				long now = System.currentTimeMillis();
				if (now - mLoadingLastUpdateTime > 100) {
					mProgressDialog
							.setProgress((int) (mProgressDialog.getMax() * fractionComplete));
					mLoadingLastUpdateTime = now;
				}
				return mLoadingKeepGoing;
			}
		};

		// Load the sound file in a background thread
		new Thread() {
			@Override
			public void run() {
				try {
					mSoundFile = CheapSoundFile.create(mFile.getAbsolutePath(),
							listener);
				} catch (final Exception e) {
					mProgressDialog.dismiss();
					e.printStackTrace();
					return;
				}

				mProgressDialog.dismiss();
				mLoadingKeepGoing = false;
				Message msg = Message.obtain(handler, LOAD_BLANK_LIST);
				handler.sendMessage(msg);
			}
		}.start();
	}

	@Override
	public void onClick(View view) {
		// 뒤로 버튼
		if (view == rew_btn && mp.isPlaying() == true) {
			// cancelAISearch(false);
			moveBack();
		}
		// 재생 버튼
		else if (view == play_btn && isSetted == true) {
			// cancelAISearch(false);
			if (mp.isPlaying()) {
				pause();
			} else {
				play();
			}
		}
		// 앞으로 버튼
		else if (view == ff_btn && mp.isPlaying() == true) {
			// cancelAISearch(false);
			moveForward();
		}
		// 이전곡 버튼
		else if (view == a_rew_btn) {
			// if (songs.size() > 1) {
			// cancelAISearch(false);
			// preSong();
			// }
			if (playlist.size() > 0) {
				preSong();
			}
		}
		// 다음곡 버튼
		else if (view == a_ff_btn) {
			// if (songs.size() > 1) {
			// cancelAISearch(false);
			// nextSong();
			// }
			if (playlist.size() > 0) {
				nextSong();
			}
		}
		// 리스트 보기(플레이리스트)
		else if (view == list_btn) {
			Log.d(TAG, "starting Mp3ListActivity");
			startActivityForResult(new Intent(this, PlayListActivity.class),
					REQUEST_PLAYLIST);
		}
		// 리스트 보기(폴더탐색)
		else if (view == list_btn2) {
			Log.d(TAG, "starting FileListActivity");
			startActivityForResult(new Intent(this, FileListActivity.class),
					REQUEST_FILELIST);
		}
		// 반복 버튼 클릭
		else if (view == looping) {
			setLooping(false);
		}
		// 랜덤 버튼 클릭
		else if (view == random) {
			setRandom(false);
		}
		// 구간반복 버튼 클릭
		else if (view == play_AB) {
			setRevival();
		}

	}

	private void setRevival() {
		if (isPlayingAB == false && isStartingAB == false && mp.isPlaying()) {
			Log.d(TAG, "Starting AB...");
			// 구간반복 시작
			isStartingAB = true;
			isSettedAB = false;
			playAB[0] = mp.getCurrentPosition();

			// 이미지 변경
			play_AB.setImageDrawable(r.getDrawable(R.drawable.atob_start));
			titlebar.setText(r.getString(R.string.starting_ab));
			atob.setText(changeToMinutes(playAB[0]) + " ~ --:--");

		} else if (isPlayingAB == false && isStartingAB == true
				&& mp.isPlaying()) {
			Log.d(TAG, "Ending AB...");
			// 구간반복 끝
			isStartingAB = false;
			isSettedAB = true;
			isPlayingAB = true;
			playAB[1] = mp.getCurrentPosition();

			// 이미지 변경
			play_AB.setImageDrawable(r.getDrawable(R.drawable.atob_end));
			atob.setText(changeToMinutes(playAB[0]) + " ~ "
					+ changeToMinutes(playAB[1]));

			// 구간 재생
			mp.seekTo(playAB[0]);
			titlebar.setText(r.getString(R.string.play_ab_str));

		} else if (isPlayingAB == true && mp.isPlaying()) {
			Log.d(TAG, "Cancel AB...");
			// 구간반복 취소
			isPlayingAB = false;

			// 이미지 변경
			play_AB.setImageDrawable(r.getDrawable(R.drawable.atob_cancel));
			titlebar.setText(r.getString(R.string.titlebar));
			atob.setText("--:-- ~ --:--");
		}
	}

	private void setLooping(boolean justSet) {
		if (justSet) {
			isLooping = (isLooping + 2) % 3;
		}
		if (isLooping == LOOPING_NONE) {
			// 무반복 재생이면 한곡 반복재생으로 바꾼다.
			looping.setImageDrawable(r.getDrawable(R.drawable.looping_one));
			isLooping = LOOPING_ONE;
			if (!justSet) {
				Toast.makeText(this, r.getString(R.string.looping_one),
						Toast.LENGTH_SHORT).show();
			}
		} else if (isLooping == LOOPING_ONE) {
			// 한곡 반복 재생이면 전곡 재생으로 바꾼다.
			looping.setImageDrawable(r.getDrawable(R.drawable.looping_all));
			isLooping = LOOPING_ALL;
			if (!justSet) {
				Toast.makeText(this, r.getString(R.string.looping_all),
						Toast.LENGTH_SHORT).show();
			}
		} else {
			// 전체 반복 재생이면 무반복 재생으로 바꾼다.
			looping.setImageDrawable(r.getDrawable(R.drawable.looping_none));
			isLooping = LOOPING_NONE;
			if (!justSet) {
				Toast.makeText(this, r.getString(R.string.looping_none),
						Toast.LENGTH_SHORT).show();
			}
		}

		// 셋팅저장
		editor.putInt(LOOPING, isLooping);
		editor.commit();
	}

	private void setRandom(boolean justSet) {
		// 설정에서 랜덤 여부 가져와서
		if (justSet) {
			isRandom = !isRandom;
		}
		if (isRandom) {
			// 랜덤 재생이면 순차재생으로 바꾼다.
			// 랜덤 아님 이미지로 바꾸고
			random.setImageDrawable(r.getDrawable(R.drawable.random_off));
			isRandom = false;
			if (!justSet) {
				Toast.makeText(this, r.getString(R.string.inOrder),
						Toast.LENGTH_SHORT).show();
			}
		} else {
			// 순차재생이면 랜덤재생으로 바꾼다.
			// 랜덤 이미지로 변경
			random.setImageDrawable(r.getDrawable(R.drawable.random_on));
			isRandom = true;
			if (!justSet) {
				Toast.makeText(this, r.getString(R.string.random),
						Toast.LENGTH_SHORT).show();
			}
		}

		// 셋팅저장
		editor.putBoolean(RANDOM, isRandom);
		editor.commit();
	}

	private void moveBack() {
		try {
			int curSeek = mp.getCurrentPosition();
			if (curSeek > gab) {
				mp.seekTo(curSeek - gab);
			} else {
				mp.seekTo(0);
			}
			// Toast.makeText(this, "REW", 500).show();
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}

	/*
	 * private void stop() { try { mp.stop(); mp.prepare(); mp.seekTo(0);
	 * mp3t.stop(); seekbar.setProgress(0); Toast.makeText(this, "STOP",
	 * 500).show(); play_btn.setImageResource(R.drawable.x_play); mp3t = new
	 * Thread(this); done = true; isSetted = false; } catch (Exception e) { //
	 * TODO: handle exception e.printStackTrace(); }
	 * 
	 * // Log.d(TAG, "MP3 FILE STOPED."); }
	 */

	private void pause() {
		try {
			if (mp.isPlaying()) {
				mp.pause();
				// mp3t.stop();
				// /////////////////////
				mIsPlaying = false;
				mWaveformView.setPlayback(-1);
				// /////////////////////
				// Toast.makeText(this, "PAUSE", 500).show();
				play_btn.setImageResource(R.drawable.x_play);
				mp3t = new Thread(this);
			}
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		done = true;
		// Log.d(TAG, "MP3 FILE PAUSED.");
	}

	private void moveForward() {
		try {
			int curSeek = mp.getCurrentPosition();
			int allSeek = mp.getDuration();
			if (curSeek + gab < allSeek) {
				mp.seekTo(curSeek + gab);
			} else {
				mp.seekTo(allSeek);
			}
			// Toast.makeText(this, "FF", 500).show();
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}

	private void play() {
		// Toast.makeText(this, "PLAY", 500).show();
		try {
			if (!mp.isPlaying()) {
				mp.start();
				mp3t.start();
				play_btn.setImageResource(R.drawable.x_pause);
				mIsPlaying = true;
				updateDisplay();
			}
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		// editor.putString(PLAYING, playingMp3name);
		// editor.commit();
		done = false;
		Log.d(TAG, "MP3 FILE is being played.");
		Log.d(TAG, "songPos : " + songPos);
	}

	// 리스트 액티비티에서 데이터 받아오기
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		Log.d(TAG, "onActivityResult()");
		if ((requestCode == REQUEST_FILELIST) && (resultCode == RESULT_OK)) {
			// 선택된 파일 재생
			Log.d(TAG, "onActivityResult() from filelist");
			songPos = -1;
			nextSong();
		} else if ((requestCode == REQUEST_PLAYLIST)
				&& (resultCode == RESULT_OK)) {
			// 선택된 파일 재생
			Log.d(TAG, "onActivityResult() from playlist");
			songPos = data.getIntExtra("POS", 0);
			Log.d(TAG, "songPos from playlist : " + songPos);

			setMusic(playlist.get(songPos));
			play();
		} else if ((requestCode == REQUEST_PLAYLIST)
				&& (resultCode == RESULT_CANCELED)) {
			Log.d(TAG, "onActivityResult() from playlist(RESULT_CANCELED");
			songPos = playlist.indexOf(playingMp3name);
		} else if ((requestCode == REQUEST_OPTION) && (resultCode == RESULT_OK)) {
			// 설정 다시 불러오기
			Log.d(TAG, "onActivityResult() from setting");
			loadPreperences();

		} else {
			super.onActivityResult(requestCode, resultCode, data);
		}
	}

	// 밀리세컨드를 분으로 바꾸기
	private String changeToMinutes(int mseconds) {
		int min = 0;
		int sec = 0;
		String minStr = "";
		String secStr = "";

		min = (int) Math.floor(mseconds / (1000 * 60));
		sec = (int) Math.floor((mseconds - (1000 * 60) * min) / 1000);

		minStr = min < 10 ? "0" + min : "" + min;
		secStr = sec < 10 ? "0" + sec : "" + sec;

		return minStr + ":" + secStr;
	}

	// // 퍼센트로 바꾸기
	// private int makePercent(int child, int parent) {
	// int per = (int) Math.floor((child * 100) / parent);
	// return per;
	// }

	// //////////////////////////////////
	// mp3 재생 SeekBar이동 및 재생 시간 설정 Thread
	private final Runnable mp3Run = new Runnable() {
		@Override
		public void run() {
			if (!done) {
				int currentDuration = mp.getCurrentPosition();
				if (!isSeeking) {
					zero.setText(changeToMinutes(currentDuration));
					seekbar.setProgress(currentDuration);
				}
				if (isPlayingAB == true && isSettedAB
						&& playAB[1] < currentDuration) {
					// 반복재생 중이고 구간을 벗어나면 다시 재생
					mp.seekTo(playAB[0]);
				}
			}
		}
	};

	@Override
	public void run() {
		// TODO Auto-generated method stub
		while (!done) {
			try {
				Thread.sleep(200);
			} catch (Exception e) {
				// TODO: handle exception
			}
			h.post(mp3Run);
		}
	}

	@Override
	public void onCompletion(MediaPlayer mp) {
		// TODO Auto-generated method stub
		try {
			if (isLooping == LOOPING_ALL || isLooping == LOOPING_NONE) {
				// 전체 반복재생 또는 일반 재생
				nextSong();
			} else if (isLooping == LOOPING_ONE) {
				// 한곡 반복재생
				play();
			}
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}

	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		done = true;
		// mp3t.stop();

		mp.release();
		mp = null;
		// songs.clear();

		// 재생목록 저장
		String list = "";
		for (int i = 0; i < playlist.size(); i++) {
			list += playlist.get(i) + "|";
		}
		Log.d(TAG, "SAVING List : " + list);
		editor.putString(PLAYING, list);
		editor.commit();

		// 진행 중 에서 삭제
		nm.cancel(1234);

		// adview.destroy();

		unregisterReceiver(mMyBroadcastReceiver);


		Log.d(TAG, "the app has been desdroyed.");
		super.onDestroy();
	}

	@Override
	public void onBackPressed() {
		// TODO Auto-generated method stub

		if (isbackPressed == false) {
			Toast.makeText(this, R.string.exit, Toast.LENGTH_SHORT).show();
			isbackPressed = true;
			new DelayTask().execute(DELAY_QUIT, 2000);
		} else {
			super.onBackPressed();
			finish();
		}
	}

	// private void cancelAISearch(boolean showMsg) {
	// if (showMsg) {
	// Toast.makeText(this, R.string.cancel_search, Toast.LENGTH_SHORT)
	// .show();
	// }
	// ai.setPossible(false);
	// isASeeking = false;
	// }

	/**
	 * 종료를 위한 딜레이 쓰레드
	 * 
	 * @author Scott
	 * 
	 */
	private class DelayTask extends AsyncTask<Integer, Void, Void> {

		@Override
		protected Void doInBackground(Integer... arg0) {
			// TODO Auto-generated method stub
			try {
				Thread.sleep(arg0[1]);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			switch (arg0[0]) {
			case DELAY_QUIT:
				isbackPressed = false;
				break;
			}

			return null;
		}

	}

	private void nextSong() {
		// 다음곡 찾기
		if (isRandom) {
			// 랜덤재생이면
			Random ran = new Random();
			songPos = ran.nextInt(playlist.size());
			setMusic(playlist.get(songPos));
		} else {
			// 순차재생이면
			if (songPos == playlist.size() - 1) {
				// 현재 재생 위치가 마지막이면
				songPos = 0;
				setMusic(playlist.get(0));
			} else {
				songPos++;
				setMusic(playlist.get(songPos));
			}
		}

		play();
	}

	private void preSong() {
		// 이전곡 찾기
		if (isRandom) {
			// 랜덤재생이면
			Random ran = new Random();
			songPos = ran.nextInt(playlist.size());
			setMusic(playlist.get(songPos));
		} else {
			// 순차재생이면
			if (songPos == 0) {
				// 현재 재생 위치가 처음이면
				songPos = playlist.size() - 1;
				setMusic(playlist.get(songPos));
			} else {
				songPos--;
				setMusic(playlist.get(songPos));
			}
		}

		play();
	}

	private void removeBlankList() {
		Button btn;
		for (int i = 0; i < btnNum; i++) {
			btn = (Button) findViewById(DINAMIC_VIEW_ID + i);
			linear.removeView(btn);
		}

		btnNum = 0;
	}

	//
	// WaveformListener
	//

	/**
	 * Every time we get a message that our waveform drew, see if we need to
	 * animate and trigger another redraw.
	 */
	@Override
	public void waveformDraw() {
		mWidth = mWaveformView.getMeasuredWidth();
		if (mOffsetGoal != mOffset)
			updateDisplay();
		else if (mIsPlaying) {
			updateDisplay();
		} else if (mFlingVelocity != 0) {
			updateDisplay();
		}
	}

	@Override
	public void waveformTouchStart(float x) {
		mTouchDragging = true;
		mTouchStart = x;
		mTouchInitialOffset = mOffset;
		mFlingVelocity = 0;
		mWaveformTouchStartMsec = System.currentTimeMillis();
	}

	@Override
	public void waveformTouchMove(float x) {
		mOffset = trap((int) (mTouchInitialOffset + (mTouchStart - x)));
		updateDisplay();
	}

	@Override
	public void waveformTouchEnd() {
		mTouchDragging = false;
		mOffsetGoal = mOffset;

		long elapsedMsec = System.currentTimeMillis() - mWaveformTouchStartMsec;
		if (elapsedMsec < 300) {
			int seekMsec = mWaveformView
					.pixelsToMillisecs((int) (mTouchStart + mOffset));
			mp.seekTo(seekMsec - mPlayStartOffset);
			if (!mIsPlaying) {
				play();
			}
		}
	}

	@Override
	public void waveformFling(float vx) {
		mTouchDragging = false;
		mOffsetGoal = mOffset;
		mFlingVelocity = (int) (-vx);
		updateDisplay();
	}

	private int trap(int pos) {
		if (pos < 0)
			return 0;
		if (pos > mMaxPos)
			return mMaxPos;
		return pos;
	}

	private synchronized void updateDisplay() {
		if (mIsPlaying) {
			int now = mp.getCurrentPosition() + mPlayStartOffset;
			int frames = mWaveformView.millisecsToPixels(now);
			mWaveformView.setPlayback(frames);
			setOffsetGoalNoUpdate(frames - mWidth / 2);
			// if (now >= mPlayEndMsec) {
			// pause();
			// }
		}

		if (!mTouchDragging) {
			int offsetDelta;

			if (mFlingVelocity != 0) {

				offsetDelta = mFlingVelocity / 30;
				if (mFlingVelocity > 80) {
					mFlingVelocity -= 80;
				} else if (mFlingVelocity < -80) {
					mFlingVelocity += 80;
				} else {
					mFlingVelocity = 0;
				}

				mOffset += offsetDelta;

				if (mOffset + mWidth / 2 > mMaxPos) {
					mOffset = mMaxPos - mWidth / 2;
					mFlingVelocity = 0;
				}
				if (mOffset < 0) {
					mOffset = 0;
					mFlingVelocity = 0;
				}
				mOffsetGoal = mOffset;
			} else {
				offsetDelta = mOffsetGoal - mOffset;

				if (offsetDelta > 10)
					offsetDelta = offsetDelta / 10;
				else if (offsetDelta > 0)
					offsetDelta = 1;
				else if (offsetDelta < -10)
					offsetDelta = offsetDelta / 10;
				else if (offsetDelta < 0)
					offsetDelta = -1;
				else
					offsetDelta = 0;

				mOffset += offsetDelta;
			}
		}

		mWaveformView.setParameters(mStartPos, mEndPos, mOffset);
		mWaveformView.invalidate();

	}

	private void setOffsetGoalNoUpdate(int offset) {
		if (mTouchDragging) {
			return;
		}

		mOffsetGoal = offset;
		if (mOffsetGoal + mWidth / 2 > mMaxPos)
			mOffsetGoal = mMaxPos - mWidth / 2;
		if (mOffsetGoal < 0)
			mOffsetGoal = 0;
	}

	private void finishOpeningSoundFile() {
		mWaveformView.setSoundFile(mSoundFile);
		mWaveformView.recomputeHeights(mDensity);

		mStartPos = mWaveformView.getStart();
		mEndPos = mWaveformView.getEnd();
		mMaxPos = mWaveformView.maxPos();
		mOffset = mWaveformView.getOffset();
		mOffsetGoal = mOffset;

		// mLastDisplayedStartPos = -1;
		// mLastDisplayedEndPos = -1;

		mTouchDragging = false;

		mOffsetGoal = 0;
		mFlingVelocity = 0;

		updateDisplay();
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		// 절전모드 방지시작
		mWakeLock.acquire();
		super.onResume();
	}
	
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		// 절전모드 방지 종료
		mWakeLock.release();
		super.onPause();
	}
}
