package com.iamplus.musicplayer;
/*   
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.RemoteControlClient;
import android.os.Binder;
import android.os.IBinder;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio.Media;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.iamplus.musicplayer.MusicRetriever.e_play_mode;

public class MusicService extends Service implements OnCompletionListener, OnPreparedListener,
OnErrorListener, MusicFocusable
{

	// The tag we put on debug messages
	final static String TAG = "iamplusMusicPlayerService";

	// These are the Intent actions that we are prepared to handle. Notice that the fact these
	// constants exist in our class is a mere convenience: what really defines the actions our
	// service can handle are the <action> tags in the <intent-filters> tag for our service in
	// AndroidManifest.xml.
	public static final String ACTION_TOGGLE_PLAYBACK = "com.iamplus.musicplayer.action.TOGGLE_PLAYBACK";
	public static final String ACTION_PLAY = "com.iamplus.musicplayer.action.PLAY";
	public static final String ACTION_PAUSE = "com.iamplus.musicplayer.action.PAUSE";
	public static final String ACTION_STOP = "com.iamplus.musicplayer.action.STOP";
	public static final String ACTION_SKIP = "com.iamplus.musicplayer.action.SKIP";
	public static final String ACTION_REWIND = "com.iamplus.musicplayer.action.REWIND";
	public static final String ACTION_URL = "com.iamplus.musicplayer.action.URL";
	public static final String ACTION_SEARCH_PLAY = "com.iamplus.musicplayer.action.SEARCH_PLAY";
	public static final String ACTION_REPEAT_ON = "com.iamplus.musicplayer.action.REPEAT_ON";
	public static final String ACTION_REPEAT_OFF = "com.iamplus.musicplayer.action.REPEAT_OFF";
	public static final String ACTION_SHUFFLE_ON = "com.iamplus.musicplayer.action.SHUFFLE_ON";
	public static final String ACTION_SHUFFLE_OFF = "com.iamplus.musicplayer.action.SHUFFLE_OFF";

	/**
	 * Local Broadcasts for Playback change notifications.
	 */

	public static final String MUSIC_BROAD_CAST_PLAYBACK_STARTED = "PlaybackStarted";
	public static final String MUSIC_BROAD_CAST_PLAYBACK_STOPPED = "PlaybackStopped";
	public static final String MUSIC_BROAD_CAST_PLAYBACK_ERROR = "PlaybackError";
	public static final String MUSIC_BROAD_CAST_PLAYBACK_SKIP = "PlaybackSkip";
	public static final String MUSIC_BROAD_CAST_PLAYBACK_PREV = "PlaybackPrev";
	
	// The volume we set the media player to when we lose audio focus, but are allowed to reduce
	// the volume instead of stopping playback.
	public static final float DUCK_VOLUME = 0.1f;

	// our media player
	MediaPlayer mPlayer = null;

	// our AudioFocusHelper object, if it's available (it's available on SDK level >= 8)
	// If not available, this will be null. Always check for null before using!
	AudioFocusHelper mAudioFocusHelper = null;

	// indicates the state our service:
	enum State {
		Stopped,    // media player is stopped and not prepared to play
		Preparing,  // media player is preparing...
		Playing,    // playback active (media player ready!). (but the media player may actually be
		// paused in this state if we don't have audio focus. But we stay in this state
		// so that we know we have to resume playback once we get focus back)
		Paused      // playback paused (media player ready!)
	};

	State mState = State.Stopped;

	enum PauseReason {
		UserRequest,  // paused by user request
		FocusLoss,    // paused because of audio focus loss
	};

	// why did we pause? (only relevant if mState == State.Paused)
	PauseReason mPauseReason = PauseReason.UserRequest;

	// do we have audio focus?
	enum AudioFocus {
		NoFocusNoDuck,    // we don't have audio focus, and can't duck
		NoFocusCanDuck,   // we don't have focus, but can play at a low volume ("ducking")
		Focused           // we have full audio focus
	}
	AudioFocus mAudioFocus = AudioFocus.NoFocusNoDuck;

	// title of the song we are currently playing
	//String mSongTitle = "";

	// whether the song we are playing is streaming from the network
	boolean mIsStreaming = false;

	// Wifi lock that we hold when streaming files from the internet, in order to prevent the
	// device from shutting off the Wifi radio
	//WifiLock mWifiLock;
	private WakeLock mWakeLock;
	// The ID we use for the notification (the onscreen alert that appears at the notification
	// area at the top of the screen as an icon -- and as text as well if the user expands the
	// notification area).
	final int NOTIFICATION_ID = 1;

	// our RemoteControlClient object, which will use remote control APIs available in
	// SDK level >= 14, if they're available.
	RemoteControlClientCompat mRemoteControlClientCompat;

	// Dummy album art we will pass to the remote control (if the APIs are available).
	//Bitmap mDummyAlbumArt;

	// The component name of MusicIntentReceiver, for use with media button and remote control
	// APIs
	ComponentName mMediaButtonReceiverComponent;

	AudioManager mAudioManager;
	//NotificationManager mNotificationManager;

	//Notification mNotification = null;
	//The current playing item
	MediaItem mCurrentPlayingItem;

	public interface PlayerEventListener {
		void onPlayerReady();
		void onPlaybackCompleted();
		void onPlayPauseStateChanged();
		void onPlayBackError(Playback_Error errorcode);
	}

	public enum Playback_Error{
		Playback_Error_Invalid_file,
		Playback_Error_No_Audio_focus
	}
	
	private final Set<Messenger> clients = new HashSet<Messenger>();
	PlayerEventListener mPlayerEventListener;

	public class MusicServiceBinder extends Binder {
		public MusicService getPlayerService() {
			return MusicService.this;
		}

		public void register(Messenger messenger)
		{
			clients.add(messenger);
		}

		public void unregister(Messenger messenger)
		{
			clients.remove(messenger);
		}
	}

	Binder musicServiceBinder = new MusicServiceBinder();

	private boolean mPausePlayback = false;

	public void setPlayerEventListener(PlayerEventListener list) {
		mPlayerEventListener = list;
	}
	/**
	 * Makes sure the media player exists and has been reset. This will create the media player
	 * if needed, or reset the existing media player if one already exists.
	 */
	void createMediaPlayerIfNeeded() {
		if (mPlayer == null) {
			mPlayer = new MediaPlayer();

			// Make sure the media player will acquire a wake-lock while playing. If we don't do
			// that, the CPU might go to sleep while the song is playing, causing playback to stop.
			//
			// Remember that to use this, we have to declare the android.permission.WAKE_LOCK
			// permission in AndroidManifest.xml.
			mPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);

			// we want the media player to notify us when it's ready preparing, and when it's done
			// playing:
			mPlayer.setOnPreparedListener(this);
			mPlayer.setOnCompletionListener(this);
			mPlayer.setOnErrorListener(this);
		}
		else
			mPlayer.reset();
	}

	@Override
	public void onCreate() {
		Log.i(TAG, "debug: Creating service");

		// Create the Wifi lock (this does not acquire the lock, this just creates it)
//		mWifiLock = ((WifiManager) getSystemService(Context.WIFI_SERVICE))
//				.createWifiLock(WifiManager.WIFI_MODE_FULL, "mylock");

		PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
		mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, this.getClass().getName());
		mWakeLock.setReferenceCounted(false);
		         
		//mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

		// create the Audio Focus Helper, if the Audio Focus feature is available (SDK 8 or above)
		if (android.os.Build.VERSION.SDK_INT >= 8)
			mAudioFocusHelper = new AudioFocusHelper(getApplicationContext(), this);
		else
			mAudioFocus = AudioFocus.Focused; // no focus feature, so we always "have" audio focus

		//mDummyAlbumArt = BitmapFactory.decodeResource(getResources(), R.drawable.albumart_mp_unknown);

		mMediaButtonReceiverComponent = new ComponentName(this, MusicIntentReceiver.class);
		
		e_play_mode emode = MusicRetriever.getPlayModePref(this, e_play_mode.e_play_mode_normal);
		MusicRetriever.getInstance().setPlayMode(emode);
	}

	/**
	 * Called when we receive an Intent. When we receive an intent sent to us via startService(),
	 * this is the method that gets called. So here we react appropriately depending on the
	 * Intent's action, which specifies what is being requested of us.
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		String action = intent.getAction();
		if(action != null) {
			if (action.equals(ACTION_TOGGLE_PLAYBACK)) processTogglePlaybackRequest();
			else if (action.equals(ACTION_PLAY)) processPlayRequest();
			else if (action.equals(ACTION_PAUSE)) processPauseRequest();
			else if (action.equals(ACTION_SKIP)) processSkipRequest();
			else if (action.equals(ACTION_STOP)) processStopRequest();
			else if (action.equals(ACTION_REWIND)) processRewindRequest();
			else if (action.equals(ACTION_URL)) processAddRequest(intent);
			else if (action.equals(ACTION_SHUFFLE_OFF)) processShuffleOff();
			else if (action.equals(ACTION_SHUFFLE_ON)) processShuffleOn();
			else if (action.equals(ACTION_REPEAT_ON)) processRepeatOff();
			else if (action.equals(ACTION_REPEAT_OFF)) processRepeatOn();
		}
		return START_NOT_STICKY; // Means we started the service, but don't want it to
		// restart in case it's killed.
	}

	private void processShuffleOn() {
		MusicRetriever.getInstance().setPlayMode(e_play_mode.e_play_mode_shuffle);

	}
	private void processRepeatOn() {
		MusicRetriever.getInstance().setPlayMode(e_play_mode.e_play_mode_repeat);

	}
	private void processRepeatOff() {
		MusicRetriever.getInstance().setPlayMode(e_play_mode.e_play_mode_normal);

	}
	private void processShuffleOff() {
		MusicRetriever.getInstance().setPlayMode(e_play_mode.e_play_mode_normal);
	}

	void processTogglePlaybackRequest() {
		
		//If no audio focus return
		if(mAudioFocus != AudioFocus.Focused) {
			tryToGetAudioFocus();
			
			if(mAudioFocus != AudioFocus.Focused){
				Toast.makeText(getApplicationContext(), R.string.audio_focus_unavailable, Toast.LENGTH_SHORT).show();
				return;
			}
		}
		
		if (mState == State.Paused) {
			// If we're paused, just continue playback and restore the 'foreground service' state.
			mState = State.Playing;
			//setUpAsForeground(mSongTitle + " (playing)");
			configAndStartMediaPlayer();
			// Tell any remote controls that our playback state is 'playing'.
			if (mRemoteControlClientCompat != null) {
				mRemoteControlClientCompat
				.setPlaybackState(RemoteControlClient.PLAYSTATE_PLAYING);
			}
		}
		else if(mState == State.Stopped) {
			processPlayRequest();
		    
		} else {
			processPauseRequest();
		}

		if(mPlayerEventListener != null) {
			mPlayerEventListener.onPlayPauseStateChanged();
		}
	}

	void processPlayRequest() {

		tryToGetAudioFocus();

		if(mAudioFocus == AudioFocus.Focused) {
			
			startPlayback();

			// Tell any remote controls that our playback state is 'playing'.
			if (mRemoteControlClientCompat != null) {
				mRemoteControlClientCompat
				.setPlaybackState(RemoteControlClient.PLAYSTATE_PLAYING);
			}
			
			if(mPlayerEventListener != null) {
				mPlayerEventListener.onPlayPauseStateChanged();
			}
			
			mWakeLock.acquire();
			
		    LocalBroadcastManager.getInstance(this).sendBroadcast
		    		(new Intent(MUSIC_BROAD_CAST_PLAYBACK_STARTED));
		}else {
			Toast.makeText(getApplicationContext(), R.string.audio_focus_unavailable, Toast.LENGTH_LONG).show();
		}
	}

	void processPauseRequest() {
		if (mState == State.Playing) {
			// Pause media player and cancel the 'foreground service' state.
			mState = State.Paused;
			mPlayer.pause();
			relaxResources(false); // while paused, we always retain the MediaPlayer
			// do not give up audio focus
			if(mWakeLock.isHeld())mWakeLock.release();
		}

		// Tell any remote controls that our playback state is 'paused'.
		if (mRemoteControlClientCompat != null) {
			mRemoteControlClientCompat
			.setPlaybackState(RemoteControlClient.PLAYSTATE_PAUSED);
		}
	}

	//Set the playback position
	void setPlaybackPosition(int seconds) {
		if (mState == State.Playing || mState == State.Paused)
			mPlayer.seekTo(seconds * 1000);
	}

	void processRewindRequest() {
		if (mState == State.Playing || mState == State.Paused) {
			tryToGetAudioFocus();
			playPrevSong(null, true);
			LocalBroadcastManager.getInstance(this).
			sendBroadcast(new Intent(MUSIC_BROAD_CAST_PLAYBACK_PREV));
		}
	}

	void processSkipRequest() {
		if (mState == State.Playing || mState == State.Paused) {
			tryToGetAudioFocus();
			playNextSong(null, true);
			LocalBroadcastManager.getInstance(this).
			sendBroadcast(new Intent(MUSIC_BROAD_CAST_PLAYBACK_SKIP));
		}
	}

	void processStopRequest() {
		processStopRequest(false);
		LocalBroadcastManager.getInstance(this).
		sendBroadcast(new Intent(MUSIC_BROAD_CAST_PLAYBACK_STOPPED));
	}

	void processStopRequest(boolean force) {
		if (mState == State.Playing || mState == State.Paused || force) {
			mState = State.Stopped;

			// let go of all resources...
			relaxResources(true);
			giveUpAudioFocus();

			// Tell any remote controls that our playback state is 'paused'.
			if (mRemoteControlClientCompat != null) {
				mRemoteControlClientCompat
				.setPlaybackState(RemoteControlClient.PLAYSTATE_STOPPED);
			}

			// service is no longer necessary. Will be started again if needed.
			stopSelf();
			
			if(mWakeLock.isHeld())mWakeLock.release();
			
			if(mPlayerEventListener != null) {
				mPlayerEventListener.onPlaybackCompleted();
			}
		}
	}

	/**
	 * Releases resources used by the service for playback. This includes the "foreground service"
	 * status and notification, the wake locks and possibly the MediaPlayer.
	 *
	 * @param releaseMediaPlayer Indicates whether the Media Player should also be released or not
	 */
	void relaxResources(boolean releaseMediaPlayer) {


		// stop and release the Media Player, if it's available
		if (releaseMediaPlayer && mPlayer != null) {
			mPlayer.reset();
			mPlayer.release();
			mPlayer = null;
			
			// stop being a foreground service
			stopForeground(true);
		}

		// we can also release the Wifi lock, if we're holding it
		//if (mWifiLock.isHeld()) mWifiLock.release();
	}

	void giveUpAudioFocus() {
		if (mAudioFocus == AudioFocus.Focused && mAudioFocusHelper != null
				&& mAudioFocusHelper.abandonFocus())
			mAudioFocus = AudioFocus.NoFocusNoDuck;
	}

	/**
	 * Reconfigures MediaPlayer according to audio focus settings and starts/restarts it. This
	 * method starts/restarts the MediaPlayer respecting the current audio focus state. So if
	 * we have focus, it will play normally; if we don't have focus, it will either leave the
	 * MediaPlayer paused or set it to a low volume, depending on what is allowed by the
	 * current focus settings. This method assumes mPlayer != null, so if you are calling it,
	 * you have to do so from a context where you are sure this is the case.
	 */
	void configAndStartMediaPlayer() {
		if (mAudioFocus == AudioFocus.NoFocusNoDuck) {
			// If we don't have audio focus and can't duck, we have to pause, even if mState
			// is State.Playing. But we stay in the Playing state so that we know we have to resume
			// playback once we get the focus back.
			if (mPlayer.isPlaying()) mPlayer.pause();
				return;
		}
		else if (mAudioFocus == AudioFocus.NoFocusCanDuck)
			mPlayer.setVolume(DUCK_VOLUME, DUCK_VOLUME);  // we'll be relatively quiet
		else
			mPlayer.setVolume(1.0f, 1.0f); // we can be loud

		if (!mPlayer.isPlaying()) mPlayer.start();
	}

	void processAddRequest(Intent intent) {
		if (mState == State.Playing || mState == State.Paused || mState == State.Stopped) {
			Log.i(TAG, "Playing from URL/path: " + intent.getData().toString());
			tryToGetAudioFocus();
			playNextSong(intent.getData().toString(),true);
		}
	}

	void tryToGetAudioFocus() {
		if (mAudioFocus != AudioFocus.Focused && mAudioFocusHelper != null
				&& mAudioFocusHelper.requestFocus()){
			mAudioFocus = AudioFocus.Focused;
		}
	}


	private ArrayList<MediaItem> getAllSongs() {  

		String[] projection = new String[] {
				MediaStore.Audio.Media._ID,
				MediaStore.Audio.Media.TITLE,
				MediaStore.Audio.Media.ARTIST,
				MediaStore.Audio.Media.ALBUM,
				MediaStore.Audio.Media.ALBUM_ID,
				//MediaStore.Audio.Media.DURATION,
		};

		String[] selectionArgs =  null;
		String sortOrder = Media.DEFAULT_SORT_ORDER;
		String selection = null;
		Cursor cursor =  getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
				projection, selection, selectionArgs, sortOrder);

		if(cursor != null) {
			if (!cursor.moveToFirst()) {
				return null;
			}    
			ArrayList<MediaItem> songlist = new ArrayList<MediaItem>();

			// retrieve the indices of the columns where the ID, title, etc. of the song are
			int artistColumn = cursor.getColumnIndex(Media.ARTIST);
			int titleColumn = cursor.getColumnIndex(Media.TITLE);
			int idcolumn = cursor.getColumnIndex(Media._ID);
			int albumcolum = cursor.getColumnIndex(Media.ALBUM);
			int albumidcolum = cursor.getColumnIndex(Media.ALBUM_ID);
			//int duration = cursor.getColumnIndex(Media.DURATION);

			// add each song to mAlbumList
			do {
				MediaItem item = new MediaItem();
				item.setSongId( cursor.getLong(idcolumn));
				item.setTitle(cursor.getString(titleColumn));
				item.setAlbum(cursor.getString(albumcolum));
				item.setArtist(cursor.getString(artistColumn));
				item.setAlbumid(cursor.getLong(albumidcolum));

				songlist.add(item);
			} while (cursor.moveToNext());

			cursor.close();

			return songlist;
		}else {
			return null;
		}
	}


	private void  startPlayback () {
		try {
			MediaItem playingItem = null;
			mIsStreaming = false; // playing a locally available song

			playingItem = MusicRetriever.getInstance().getCurrentSong();
			if (playingItem == null) {
				//Get all songs if the current list is empty
				MusicRetriever.getInstance().setSonglist(getAllSongs(), 0);
				MusicRetriever.getInstance().setPlayMode(e_play_mode.e_play_mode_shuffle);

				playingItem = MusicRetriever.getInstance().getCurrentSong();

				if(playingItem == null) {
					processStopRequest(true); // stop everything!
					return;
				}
			}
			mCurrentPlayingItem = playingItem;

			// set the source of the media player a a content URI
			createMediaPlayerIfNeeded();
			mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
			mPlayer.setDataSource(getApplicationContext(), playingItem.getURI());

			// starts preparing the media player in the background. When it's done, it will call
			// our OnPreparedListener (that is, the onPrepared() method on this class, since we set
			// the listener to 'this').
			//
			// Until the media player is prepared, we *cannot* call start() on it!
			mPlayer.prepareAsync();

			// If we are streaming from the internet, we want to hold a Wifi lock, which prevents
			// the Wifi radio from going to sleep while the song is playing. If, on the other hand,
			// we are *not* streaming, we want to release the lock if we were holding it before.
//			if (mIsStreaming) mWifiLock.acquire();
//			else if (mWifiLock.isHeld()) mWifiLock.release();
		}
		catch (IOException ex) {
			Log.e(TAG, "IOException playing first song: " + ex.getMessage());
			//ex.printStackTrace();
			Toast.makeText(getApplicationContext(), R.string.invalid_file_track, Toast.LENGTH_LONG).show();
			//Stop Playback
			processStopRequest();
			return;
		}
		//mSongTitle = mCurrentPlayingItem.getTitle();

		//setUpAsForeground(mSongTitle + " (loading)");

		// Use the media button APIs (if available) to register ourselves for media button
		// events

		MediaButtonHelper.registerMediaButtonEventReceiverCompat(
				mAudioManager, mMediaButtonReceiverComponent);

		//
		// Use the remote control APIs (if available) to set the playback state
		//
		if (mRemoteControlClientCompat == null) {
			Intent intent = new Intent(Intent.ACTION_MEDIA_BUTTON);
			intent.setComponent(mMediaButtonReceiverComponent);

			mRemoteControlClientCompat = new RemoteControlClientCompat(
					PendingIntent.getBroadcast(this /*context*/,
							0 /*requestCode, ignored*/, intent /*intent*/, 0 /*flags*/));

			RemoteControlHelper.registerRemoteControlClient(mAudioManager,
					mRemoteControlClientCompat);
		}

		mRemoteControlClientCompat.setTransportControlFlags(
				RemoteControlClient.FLAG_KEY_MEDIA_PLAY |
				RemoteControlClient.FLAG_KEY_MEDIA_PAUSE |
				RemoteControlClient.FLAG_KEY_MEDIA_NEXT |
				RemoteControlClient.FLAG_KEY_MEDIA_STOP);

		updateRemoteControlPlayingState();
	}

	public Boolean isPlaying () {
		return (mState == State.Playing);
	}

	public Boolean isPlayerInitialised () {
		return (mState == State.Playing)|| (mState == State.Paused);
	}
	//The total duration of the file being played
	public int getTotalDuration() {

		if (mState == State.Playing || mState == State.Paused) {
			return mPlayer.getDuration() / 1000;
		}
		return 0;
	}
	//The the elapsed time.
	public int getCurrentDuration() {
		if (mState == State.Playing || mState == State.Paused) {
			return mPlayer.getCurrentPosition() / 1000;
		}
		return 0;
	}

	// Returns the current playing song item
	public MediaItem getCurrentPlayingItem () {
		if (mState == State.Playing || mState == State.Paused) 
			return mCurrentPlayingItem;
		else if(mState == State.Stopped) {
			MusicRetriever.getInstance().reset();	
			mCurrentPlayingItem = MusicRetriever.getInstance().getCurrentSong();
			return mCurrentPlayingItem;
		} else
			return null;
	}

	/**
	 * Starts playing the next song. If manualUrl is null, the next song will be randomly selected
	 * from our Media Retriever (that is, it will be a random song in the user's device). If
	 * manualUrl is non-null, then it specifies the URL or path to the song that will be played
	 * next.
	 */
	void playPrevSong(String manualUrl, Boolean userAction) {
		mState = State.Stopped;
		relaxResources(false); // release everything except MediaPlayer

		try {
			MediaItem playingItem = null;
			if (manualUrl != null) {
				// set the source of the media player to a manual URL or path
				createMediaPlayerIfNeeded();
				mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
				mPlayer.setDataSource(manualUrl);
				mIsStreaming = manualUrl.startsWith("http:") || manualUrl.startsWith("https:");
			}
			else {
				mIsStreaming = false; // playing a locally available song

				playingItem = MusicRetriever.getInstance().getPrevSong();
				if (playingItem == null) {

					MusicRetriever.getInstance().reset();
					playingItem = MusicRetriever.getInstance().getCurrentSong();
				}

				mCurrentPlayingItem = playingItem;
				// set the source of the media player a a content URI
				createMediaPlayerIfNeeded();
				mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
				mPlayer.setDataSource(getApplicationContext(), playingItem.getURI());
			}

			// starts preparing the media player in the background. When it's done, it will call
			// our OnPreparedListener (that is, the onPrepared() method on this class, since we set
			// the listener to 'this').
			//
			// Until the media player is prepared, we *cannot* call start() on it!
			mPlayer.prepareAsync();

			// If we are streaming from the internet, we want to hold a Wifi lock, which prevents
			// the Wifi radio from going to sleep while the song is playing. If, on the other hand,
			// we are *not* streaming, we want to release the lock if we were holding it before.
//			if (mIsStreaming) mWifiLock.acquire();
//			else if (mWifiLock.isHeld()) mWifiLock.release();
			
			updateRemoteControlPlayingState();
		}
		catch (IOException ex) {
			Log.e("MusicService", "IOException playing next song: " + ex.getMessage());
			ex.printStackTrace();
			Toast.makeText(getApplicationContext(), R.string.invalid_file_track, Toast.LENGTH_LONG).show();
			processStopRequest();
		}
	}
	
	private void updateRemoteControlPlayingState(){
		// Tell any remote controls that our playback state is 'Playing'.
		if (mRemoteControlClientCompat != null) {
			mRemoteControlClientCompat
			.setPlaybackState(RemoteControlClient.PLAYSTATE_PLAYING);

			// Update the remote controls
			//Bitmap bitmap = MediaAlbumsAdaptor.getAlbumArtwork(getApplicationContext(), mCurrentPlayingItem.getAlbumid(), true);
			mRemoteControlClientCompat.editMetadata(true)
			.putString(MediaMetadataRetriever.METADATA_KEY_ARTIST, mCurrentPlayingItem.getArtist())
			.putString(MediaMetadataRetriever.METADATA_KEY_ALBUM, mCurrentPlayingItem.getAlbum())
			.putString(MediaMetadataRetriever.METADATA_KEY_TITLE, mCurrentPlayingItem.getTitle())
			.putLong(MediaMetadataRetriever.METADATA_KEY_DURATION,mCurrentPlayingItem.getDuration()).apply();
			//.putBitmap(RemoteControlClientCompat.MetadataEditorCompat.METADATA_KEY_ARTWORK,bitmap).apply();
			
			Log.d(TAG, "Setting Remote Control Meta data **** Start");
			Log.d(TAG, "Title = " + mCurrentPlayingItem.getTitle());
			Log.d(TAG, "Album = " + mCurrentPlayingItem.getAlbum());
			Log.d(TAG, "Artist = " + mCurrentPlayingItem.getArtist());
			Log.d(TAG, "Duration = " + mCurrentPlayingItem.getDuration());
			//Log.d(TAG, "Bitmap = " + bitmap);
			Log.d(TAG, "Setting Remote Control Meta data **** End");
		}
	}
	/**
	 * Starts playing the next song. If manualUrl is null, the next song will be randomly selected
	 * from our Media Retriever (that is, it will be a random song in the user's device). If
	 * manualUrl is non-null, then it specifies the URL or path to the song that will be played
	 * next.
	 */
	void playNextSong(String manualUrl, Boolean userAction) {
		mState = State.Stopped;
		relaxResources(false); // release everything except MediaPlayer

		try {
			MediaItem playingItem = null;
			if (manualUrl != null) {
				// set the source of the media player to a manual URL or path
				createMediaPlayerIfNeeded();
				mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
				mPlayer.setDataSource(manualUrl);
				mIsStreaming = manualUrl.startsWith("http:") || manualUrl.startsWith("https:");
			}
			else {
				mIsStreaming = false; // playing a locally available song

				playingItem = MusicRetriever.getInstance().getNextSong();
				if (playingItem == null) {

					MusicRetriever.getInstance().reset();
					playingItem = MusicRetriever.getInstance().getCurrentSong();

					if(!userAction) {
						/**
						 * Pause playback if end of playlist
						 */
						mPausePlayback = true;
					}
				}

				mCurrentPlayingItem = playingItem;
				// set the source of the media player a a content URI
				createMediaPlayerIfNeeded();
				mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
				mPlayer.setDataSource(getApplicationContext(), playingItem.getURI());
			}

			// starts preparing the media player in the background. When it's done, it will call
			// our OnPreparedListener (that is, the onPrepared() method on this class, since we set
			// the listener to 'this').
			//
			// Until the media player is prepared, we *cannot* call start() on it!
			mPlayer.prepareAsync();

			// If we are streaming from the internet, we want to hold a Wifi lock, which prevents
			// the Wifi radio from going to sleep while the song is playing. If, on the other hand,
			// we are *not* streaming, we want to release the lock if we were holding it before.
//			if (mIsStreaming) mWifiLock.acquire();
//			else if (mWifiLock.isHeld()) mWifiLock.release();
			
			updateRemoteControlPlayingState();
		}
		catch (IOException ex) {
			Log.e("MusicService", "IOException playing next song: " + ex.getMessage());
			ex.printStackTrace();
			Toast.makeText(getApplicationContext(), R.string.invalid_file_track, Toast.LENGTH_LONG).show();
			processStopRequest();
		}
	}

	/** Called when media player is done playing current song. */
	public void onCompletion(MediaPlayer player) {
		// The media player finished playing the current song, so we go ahead and start the next.
		playNextSong(null, false);
	}

	/** Called when media player is done preparing. */
	public void onPrepared(MediaPlayer player) {
		// The media player is done preparing. That means we can start playing!
		mState = State.Playing;
		// updateNotification(mSongTitle + " (playing)");
		configAndStartMediaPlayer();
		if(mPlayerEventListener != null) {
			mPlayerEventListener.onPlayerReady();
		}
		if(mPausePlayback) {
			processPauseRequest();
			mPausePlayback = false;
			
			if(mPlayerEventListener != null) {
				mPlayerEventListener.onPlayPauseStateChanged();
			}
		}
	}

	/** Updates the notification. */
	void updateNotification(String text) {
		//mNotificationManager.notify(NOTIFICATION_ID, mNotification);
	}

	/**
	 * Configures service as a foreground service. A foreground service is a service that's doing
	 * something the user is actively aware of (such as playing music), and must appear to the
	 * user as a notification. That's why we create the notification here.
	 */
	void setUpAsForeground(String text) {
//		mNotification = new Notification();
//		mNotification.tickerText = text;
//		//mNotification.icon = R.drawable.ic_stat_playing;
//		mNotification.flags |= Notification.FLAG_ONGOING_EVENT;
//		startForeground(NOTIFICATION_ID, mNotification);
	}

	/**
	 * Called when there's an error playing media. When this happens, the media player goes to
	 * the Error state. We warn the user about the error and reset the media player.
	 */
	public boolean onError(MediaPlayer mp, int what, int extra) {

		Toast.makeText(getApplicationContext(), R.string.invalid_file_track, Toast.LENGTH_LONG).show();

		Log.e(TAG, "Error: what=" + String.valueOf(what) + ", extra=" + String.valueOf(extra));

		mState = State.Stopped;
		relaxResources(true);
		giveUpAudioFocus();

		if(mPlayerEventListener != null) {
			mPlayerEventListener.onPlayBackError(Playback_Error.Playback_Error_Invalid_file);
		}
		
		return true; // true indicates we handled the error
	}

	public void onGainedAudioFocus() {
		//Toast.makeText(getApplicationContext(), "gained audio focus.", Toast.LENGTH_SHORT).show();
		mAudioFocus = AudioFocus.Focused;

		// restart media player with new focus settings
		if (mState == State.Playing)
			configAndStartMediaPlayer();
	}

	public void onLostAudioFocus(boolean canDuck) {
		//		Toast.makeText(getApplicationContext(), "lost audio focus." + (canDuck ? "can duck" :
		//				"no duck"), Toast.LENGTH_SHORT).show();
		mAudioFocus = canDuck ? AudioFocus.NoFocusCanDuck : AudioFocus.NoFocusNoDuck;

		// start/restart/pause media player with new focus settings
		if (mPlayer != null && mPlayer.isPlaying())
			configAndStartMediaPlayer();
	}


	@Override
	public void onDestroy() {
		// Service is being killed, so make sure we release our resources
		mState = State.Stopped;
		relaxResources(true);
		giveUpAudioFocus();
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return musicServiceBinder;
	}
}
