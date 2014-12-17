package com.iamplus.musicplayer;

import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.iamplus.musicplayer.DragButtonControl.DragButtonControlActionListener;
import com.iamplus.musicplayer.MusicRetriever.e_play_mode;
import com.iamplus.musicplayer.MusicService.MusicServiceBinder;
import com.iamplus.musicplayer.MusicService.Playback_Error;
import com.iamplus.musicplayer.MusicService.PlayerEventListener;

public class NowPlayingFragment extends Fragment implements PlayerEventListener {

	public static String STARTNEWPLAYBACK_KEY = "StartNewPlayback";
	private static final int UIUPDATE_TIMER_DELAY = 1000;
	private static final int HIDE_SEEKBAR_TIMER_DELAY = 3000;
	private static final int RUNNABLE_DELAY = 350;
	private static final int VOLUME_UPDATE_DELAY = 50;

	private final NowPlayingFragment mNowPlayingActivity = this;
	private MusicService mMusicService;
	private boolean mBound = false;
	private boolean mPlaying = true;
	// private TextView mTotalTime;
	private TextView mElapsedTime;
	private final Handler mHandler = new Handler();
	private String TAG = "NowPlayingActivity";
	private boolean mStartnewlist = false;
	private RelativeLayout mRootView;
	private boolean mUserInvokedPlayerEvent = false;
	private NowPlayingFragmentEventListener mNowPlayingFragmentEventListener;

	public interface NowPlayingFragmentEventListener {
		public void onNowPlayingNavigateBack();
	}

	private Runnable mUpdateUIRunnable = new Runnable() {
		@Override
		public void run() {
			if (!mPlaying || !mBound || mMusicService == null)
				return;

			// Log.d(TAG, "Music UI Timer Running ");

			String elapsedtime;
			int elapsedtimeInsec = mMusicService.getCurrentDuration();
			int elapsedminutes = elapsedtimeInsec / 60;
			int elapsedseconds = elapsedtimeInsec % 60;
			elapsedtime = String.format("%d : %02d", elapsedminutes, elapsedseconds);
			mElapsedTime.setText(elapsedtime);
			mHandler.postDelayed(mUpdateUIRunnable, UIUPDATE_TIMER_DELAY);
		}
	};

	public void setNowPlayingFragmentEventListener(NowPlayingFragmentEventListener list) {
		mNowPlayingFragmentEventListener = list;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent musicservice = new Intent(getActivity(), MusicService.class);
		getActivity().startService(musicservice);
		Bundle args = getArguments();
		if (args != null) {
			mStartnewlist = args.getBoolean(STARTNEWPLAYBACK_KEY);
		}
	};

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		RelativeLayout vg = (RelativeLayout) inflater.inflate(R.layout.now_playing_screen, container, false);

		mRootView = (RelativeLayout) vg;
		setUpUIControls(vg);
		setUpSeekBar();
		return vg;
	}

	private Runnable mHideSeekBar = new Runnable() {
		@Override
		public void run() {
			showSeekBar(false);
		}
	};

	private void showSeekBar(boolean show) {
		final View seekbarLayout = mRootView.findViewById(R.id.seekbar_layout);
		if (show) {
			AlphaAnimation animationFadeIn = new AlphaAnimation(0.0f, 1.0f);
			animationFadeIn.setDuration(450);
			seekbarLayout.setAnimation(animationFadeIn);
			seekbarLayout.setVisibility(View.VISIBLE);
		} else {
			AlphaAnimation animationFadeOut = new AlphaAnimation(1.0f, 0.0f);
			animationFadeOut.setDuration(450);
			seekbarLayout.setAnimation(animationFadeOut);
			seekbarLayout.setVisibility(View.GONE);
		}
	}

	private void setUpSeekBar() {

		final View seekbarLayout = mRootView.findViewById(R.id.seekbar_layout);
		final SeekBar seekbar = (SeekBar) seekbarLayout.findViewById(R.id.music_seekbar);
		mElapsedTime.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if(mMusicService == null){
					return;
				}
				int totalduration = mMusicService.getTotalDuration();
				int currentduration = mMusicService.getCurrentDuration();
				int progress = (currentduration * 100) / totalduration;
				seekbar.setProgress(progress);
				showSeekBar(true);
				mHandler.postDelayed(mHideSeekBar, HIDE_SEEKBAR_TIMER_DELAY);
			}
		});

		/**
		 * To manage touch events
		 */
		seekbarLayout.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (v.getVisibility() == View.VISIBLE)
					return true;
				else
					return false;
			}
		});

		seekbar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				mHandler.postDelayed(mHideSeekBar, HIDE_SEEKBAR_TIMER_DELAY);
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				mHandler.removeCallbacks(mHideSeekBar);
			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				if(mMusicService == null){
					return;
				}
				
				if (fromUser) {
					int totalduration = mMusicService.getTotalDuration();
					int changeto = (totalduration * progress) / 100;
					mMusicService.setPlaybackPosition(changeto);
				}
			}
		});
	}

	private void updateMusicInformation() {

		if(mMusicService == null){
			return;
		}
		if (mMusicService.isPlayerInitialised()) {

			String elapsedtime;
			int elapsedtimeInsec = mMusicService.getCurrentDuration();
			int elapsedminutes = elapsedtimeInsec / 60;
			int elapsedseconds = elapsedtimeInsec % 60;
			elapsedtime = String.format("%d : %02d", elapsedminutes, elapsedseconds);
			mElapsedTime.setText(elapsedtime);

			MediaItem item = mMusicService.getCurrentPlayingItem();
			ImageView albumart = (ImageView) mRootView.findViewById(R.id.albumartview);
			albumart.setVisibility(View.VISIBLE);
			Bitmap albumArtworkBitmap = MediaAlbumsAdaptor.getAlbumArtwork(getActivity(), item.getAlbumid(), false);
			albumart.setImageBitmap(albumArtworkBitmap);

			TextView title = (TextView) mRootView.findViewById(R.id.songname);
			title.setText(item.getTitle());

			TextView album_artist = (TextView) mRootView.findViewById(R.id.artist_name);

			album_artist.setText(item.getArtist());

			// Set play pause state
			setPlayPauseState();

			enableDragButton(true);

			ImageView shuffleButton = (ImageView) mRootView.findViewById(R.id.shuffle_button);
			shuffleButton.setEnabled(true);
			
			ImageView repeatButton = (ImageView) mRootView.findViewById(R.id.repeat_button);
			repeatButton.setEnabled(true);
			
			mElapsedTime.setEnabled(true);
		} else {
			showNoPlayingScreen();
		}
	}

	private void showNoPlayingScreen() {

		enableDragButton(false);

		ImageView albumart = (ImageView) mRootView.findViewById(R.id.albumartview);

		albumart.setVisibility(View.INVISIBLE);

		TextView title = (TextView) mRootView.findViewById(R.id.songname);
		title.setText(R.string.song_name_default);

		TextView album_artist = (TextView) mRootView.findViewById(R.id.artist_name);
		album_artist.setText("");

		mElapsedTime.setText(R.string.song_time_default);
		mElapsedTime.setEnabled(false);

		ImageView shuffleButton = (ImageView) mRootView.findViewById(R.id.shuffle_button);
		shuffleButton.setEnabled(false);

		ImageView repeatButton = (ImageView) mRootView.findViewById(R.id.repeat_button);
		repeatButton.setEnabled(false);

	}

	private void enableDragButton(boolean enable) {
		DragButtonControl playButton = (DragButtonControl) mRootView.findViewById(R.id.play_drag_button);
		playButton.enableControls(enable);
	}

	private void setPlayPauseState() {

		if(mMusicService == null){
			return;
		}
		
		if (!mMusicService.isPlaying()) {
			mPlaying = false;
		} else {
			mPlaying = true;
		}
		DragButtonControl playButton = (DragButtonControl) mRootView.findViewById(R.id.play_drag_button);
		if (mPlaying) {

			mHandler.removeCallbacks(mUpdateUIRunnable);
			mHandler.postDelayed(mUpdateUIRunnable, UIUPDATE_TIMER_DELAY);
		}

		if (mUserInvokedPlayerEvent == false) {
			if (mPlaying) {
				playButton.setDragButtonImage(R.drawable.button_pause);
				playButton.setContentDescription(getResources().getString(R.string.pause_button_desc));
			} else {
				playButton.setDragButtonImage(R.drawable.button_play);
				playButton.setContentDescription(getResources().getString(R.string.play_button_desc));
			}
		}
	}

	private void setUpUIControls(View view) {

		mElapsedTime = (TextView) view.findViewById(R.id.elapsedtime);

		final DragButtonControl playButton = (DragButtonControl) view.findViewById(R.id.play_drag_button);
		final ImageView volslider = (ImageView) view.findViewById(R.id.vol_slider_control);
		final ImageView dots = (ImageView) view.findViewById(R.id.direc_dots);
		final RelativeLayout container = (RelativeLayout) view.findViewById(R.id.Controls);
		final ImageView nextButton = (ImageView) view.findViewById(R.id.next_button);
		final ImageView prevButton = (ImageView) view.findViewById(R.id.prev_button);
		final ImageView shuffleButton = (ImageView) view.findViewById(R.id.shuffle_button);
		final ImageView repeatButton = (ImageView) view.findViewById(R.id.repeat_button);

		e_play_mode emode = MusicRetriever.getPlayModePref(getActivity(), e_play_mode.e_play_mode_normal);
		switch (emode) {
		case e_play_mode_normal:
			shuffleButton.setSelected(false);
			repeatButton.setSelected(false);
			break;
		case e_play_mode_shuffle:
			shuffleButton.setSelected(true);
			repeatButton.setSelected(false);
			break;
		case e_play_mode_repeat:
			shuffleButton.setSelected(false);
			repeatButton.setSelected(true);
			break;
		default:
			shuffleButton.setSelected(false);
			repeatButton.setSelected(false);
			break;
		}

		shuffleButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View button) {
				button.setSelected(!button.isSelected());
				if (button.isSelected()) {
					MusicRetriever.savePlayModeToPref(getActivity(), e_play_mode.e_play_mode_shuffle);
					MusicRetriever.getInstance().setPlayMode(e_play_mode.e_play_mode_shuffle);
					repeatButton.setSelected(false);
				} else {
					MusicRetriever.savePlayModeToPref(getActivity(), e_play_mode.e_play_mode_normal);
					MusicRetriever.getInstance().setPlayMode(e_play_mode.e_play_mode_normal);
				}
			}
		});

		repeatButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View button) {
				button.setSelected(!button.isSelected());
				if (button.isSelected()) {
					MusicRetriever.savePlayModeToPref(getActivity(), e_play_mode.e_play_mode_repeat);
					MusicRetriever.getInstance().setPlayMode(e_play_mode.e_play_mode_repeat);
					shuffleButton.setSelected(false);
				} else {
					MusicRetriever.savePlayModeToPref(getActivity(), e_play_mode.e_play_mode_normal);
					MusicRetriever.getInstance().setPlayMode(e_play_mode.e_play_mode_normal);
				}
			}
		});

		playButton.setDragButtonImage(R.drawable.button_play);
		playButton.setEnableDrawArcs(false);
		playButton.setOnDragButtonControlActionListener(new DragButtonControlActionListener() {
			private boolean mVolumeChangePositive = false;
			int volumeValue = 0;
			int maximumVolume = 0;

			@Override
			public void onDragStarted(boolean xAxis) {

				AlphaAnimation animationFadeIn = new AlphaAnimation(0.0f, 1.0f);
				animationFadeIn.setFillAfter(true);
				animationFadeIn.setDuration(350);

				AlphaAnimation animationFadeOut = new AlphaAnimation(0.7f, 0.0f);
				animationFadeOut.setFillAfter(true);
				animationFadeOut.setDuration(350);

				if (xAxis) {
					nextButton.setAnimation(animationFadeIn);
					prevButton.setAnimation(animationFadeIn);
					playButton.setDragButtonImage(R.drawable.button_selector);

				} else {
					volslider.setAnimation(animationFadeIn);
					playButton.setEnableDrawArcs(true);
					AudioManager audioManager = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);
					volumeValue = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
					maximumVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
					playButton.setDragButtonImage(R.drawable.button_volume);
					playButton.setPercentage((float) ((float) volumeValue / (float) maximumVolume));

					Log.d(TAG, "Initial Value from control" + volumeValue);
				}

				dots.setAnimation(animationFadeOut);
				repeatButton.setAnimation(animationFadeOut);
				shuffleButton.setAnimation(animationFadeOut);

				container.setBackgroundColor(getResources().getColor(R.color.music_control_translucent_black_color));
			}

			@Override
			public void onDragEnded(boolean xAxis) {
				mUserInvokedPlayerEvent = false;

				AlphaAnimation animationFadeOut = new AlphaAnimation(0.7f, 0.0f);
				animationFadeOut.setFillAfter(true);
				animationFadeOut.setDuration(350);

				AlphaAnimation animationFadeIn = new AlphaAnimation(0.0f, 1.0f);
				animationFadeIn.setFillAfter(true);
				animationFadeIn.setDuration(350);

				if (xAxis) {
					nextButton.setAnimation(animationFadeOut);
					prevButton.setAnimation(animationFadeOut);
				} else {
					volslider.setAnimation(animationFadeOut);
					playButton.setEnableDrawArcs(false);
					mHandler.removeCallbacks(mChangeVolumeRunnable);
				}

				if (mPlaying)
					playButton.setDragButtonImage(R.drawable.button_pause);
				else
					playButton.setDragButtonImage(R.drawable.button_play);

				dots.setAnimation(animationFadeIn);
				repeatButton.setAnimation(animationFadeIn);
				shuffleButton.setAnimation(animationFadeIn);
				container.setBackgroundColor(Color.TRANSPARENT);

			}

			@Override
			public void onDragRight(int value) {
				if (mMusicService == null || mMusicService.getCurrentPlayingItem() == null)
					return;
				
				mUserInvokedPlayerEvent = true;
				Intent musicservice = new Intent(getActivity(), MusicService.class);
				musicservice.setAction(MusicService.ACTION_SKIP);
				getActivity().startService(musicservice);
			}

			@Override
			public void onDragLeft(int value) {
				if (mMusicService == null || mMusicService.getCurrentPlayingItem() == null)
					return;
				
				mUserInvokedPlayerEvent = true;
				Intent musicservice = new Intent(getActivity(), MusicService.class);
				musicservice.setAction(MusicService.ACTION_REWIND);
				getActivity().startService(musicservice);
			}

			@Override
			public void onDragUp(int value) {
				mVolumeChangePositive = true;
				/**
				 * Max 2 notches from center
				 */
				if (value <= playButton.getMaxYValue()) {
					if (volumeValue < maximumVolume) {
						increaseVolume(1);
						volumeValue++;
						playButton.setPercentage((float) ((float) volumeValue / (float) maximumVolume));
						if (value == playButton.getMaxYValue()) {
							/**
							 * Start the Runnable to handle touch and hold.
							 */
							mHandler.postDelayed(mChangeVolumeRunnable, RUNNABLE_DELAY);
						}
					}
				}
				// Log.d(TAG, "Volume change  = " + volumeValue);
			}

			@Override
			public void onDragDown(int value) {
				mVolumeChangePositive = false;
				/**
				 * Max 2 notches from center
				 */
				if (value <= playButton.getMaxYValue()) {
					if (volumeValue > 0) {
						volumeValue--;
						decreaseVolume(1);
						playButton.setPercentage((float) ((float) volumeValue / (float) maximumVolume));
						if (value == playButton.getMaxYValue()) {
							/**
							 * Start the Runnable to handle touch and hold.
							 */
							mHandler.postDelayed(mChangeVolumeRunnable, RUNNABLE_DELAY);
						}
					}
				}
				// Log.d(TAG, "Volume change  = " + volumeValue);
			}

			@Override
			public void onClicked() {
				if (mMusicService == null ||mMusicService.getCurrentPlayingItem() == null)
					return;

				Intent musicservice = new Intent(getActivity(), MusicService.class);
				musicservice.setAction(MusicService.ACTION_TOGGLE_PLAYBACK);
				getActivity().startService(musicservice);
			}

			private Runnable mChangeVolumeRunnable = new Runnable() {
				public void run() {

					if (mVolumeChangePositive) {
						if (volumeValue < maximumVolume) {
							increaseVolume(1);
							volumeValue++;
							playButton.setPercentage((float) ((float) volumeValue / (float) maximumVolume));
							mHandler.postDelayed(mChangeVolumeRunnable, VOLUME_UPDATE_DELAY);
						}
					} else {
						if (volumeValue > 0) {
							decreaseVolume(1);
							volumeValue--;
							playButton.setPercentage((float) ((float) volumeValue / (float) maximumVolume));
							mHandler.postDelayed(mChangeVolumeRunnable, VOLUME_UPDATE_DELAY);
						}
					}
					// Log.d(TAG, "Volume change  = " + volumeValue);
				}
			};
		});

		ImageView albumart = (ImageView) mRootView.findViewById(R.id.albumartview);
		albumart.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				if (mNowPlayingFragmentEventListener != null) {
					mNowPlayingFragmentEventListener.onNowPlayingNavigateBack();
				}
			}
		});
	}

	private void increaseVolume(int value) {
		AudioManager audioManager = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);
		int maxV = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		int currentV = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

		int valueUp = currentV + value;

		if ((valueUp) <= maxV) {
			audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, valueUp, 0);
		} else {
			audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxV, 0);
		}
	}

	private void decreaseVolume(int value) {
		AudioManager audioManager = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);
		int currentV = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

		int valueDown = currentV - value;

		if ((valueDown) >= 0) {
			audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, valueDown, 0);
		} else {
			audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		if (mMusicService != null) {
			updateMusicInformation();
		}
	}

	@Override
	public void onStart() {
		super.onStart();
		Intent intent = new Intent(getActivity(), MusicService.class);
		getActivity().bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
	}

	@Override
	public void onStop() {
		super.onStop();
		if (mMusicService != null) {
			mMusicService.setPlayerEventListener(null);
		}

		// Unbind from the service
		if (mBound) {
			getActivity().unbindService(mConnection);
			mBound = false;
		}
		mHandler.removeCallbacksAndMessages(null);
	}

	/** Defines callback for service binding, passed to bindService() */
	private ServiceConnection mConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			// We've bound to LocalService, cast the IBinder and get
			// LocalService instance
			MusicServiceBinder binder = (MusicServiceBinder) service;
			mMusicService = binder.getPlayerService();
			mMusicService.setPlayerEventListener(mNowPlayingActivity);
			mBound = true;
			if (mStartnewlist) {// Start a new playback
				mMusicService.processPlayRequest();
				mStartnewlist = false;
			} else { // if we are already playing display the details

				if (mMusicService.isPlaying()) {
					mHandler.removeCallbacks(mUpdateUIRunnable);
					mHandler.postDelayed(mUpdateUIRunnable, UIUPDATE_TIMER_DELAY);
				}

				mNowPlayingActivity.updateMusicInformation();
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			mBound = false;
			Log.e(TAG, "Music service disconnected");
		}
	};

	@Override
	public void onPlayerReady() {
		mHandler.removeCallbacks(mUpdateUIRunnable);
		mHandler.postDelayed(mUpdateUIRunnable, UIUPDATE_TIMER_DELAY);
		mHandler.postDelayed(new Runnable() {

			@Override
			public void run() {
				mNowPlayingActivity.updateMusicInformation();
			}
		}, RUNNABLE_DELAY);
	}

	@Override
	public void onPlaybackCompleted() {
		mHandler.removeCallbacks(mUpdateUIRunnable);
		DragButtonControl playButton = (DragButtonControl) mRootView.findViewById(R.id.play_drag_button);
		mPlaying = false;
		playButton.setDragButtonImage(R.drawable.button_play);
		updateMusicInformation();
	}

	@Override
	public void onPlayPauseStateChanged() {
		setPlayPauseState();
	}

	@Override
	public void onPlayBackError(Playback_Error code) {
	}
}
