package com.iamplus.musicplayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * MusicRetriever manages play back mode, such as Shuffle, repeat etc of the currently active list.
 * @author Shiju Thomas
 *
 */
public class MusicRetriever {

	private static final String PREFS_NAME = "NowPlayingPrefs";
	private static final String PREFS_PLAY_MODE_NAME = "PlayMode";

	final String TAG = "MusicRetriever";

	// the items (songs) we have queried
	private  ArrayList<MediaItem> songlist = new ArrayList<MediaItem>();
	private  int currentPlayingIndex;
	private  static MusicRetriever instance = null;
	private  static Object lock = new Object();
	private ArrayList<Integer> shuffleList = new ArrayList<Integer>();
	enum e_play_mode {
		e_play_mode_repeat(0),
		e_play_mode_shuffle(1),
		e_play_mode_normal(2);
		int mode;
		e_play_mode(int value){
			mode = value;
		}
		int getValue() {
			return mode;
		}
		public static e_play_mode fromInt(int value) {
			switch (value) {
			case 0:	
				return e_play_mode.e_play_mode_repeat;
			case 1:	
				return e_play_mode.e_play_mode_shuffle;
			case 2:	
				return e_play_mode.e_play_mode_normal;
			}
			return null;
		}
	}

	private e_play_mode playMode = e_play_mode.e_play_mode_normal;

	public e_play_mode getPlayMode() {
		return playMode;
	}

	public void setPlayMode(e_play_mode playMode) {
		this.playMode = playMode;
		createShuffleList();
	}

	public void togglePlayMode(e_play_mode newplayMode) {
		if(playMode == newplayMode) {
			playMode = e_play_mode.e_play_mode_normal;
		} else {
			playMode = newplayMode;
		}
	}

	public static MusicRetriever getInstance() {

		synchronized (lock) {
			if(instance == null) {
				instance = new MusicRetriever();
			}
			return instance;
		}
	}

	private MusicRetriever() {
		playMode = e_play_mode.e_play_mode_normal;
	}

	//Sets the current item to first item
	public void reset() {
		synchronized (lock) {
			currentPlayingIndex = 0;
		}
	}
	public List<MediaItem> getSonglist() {
		return songlist;
	}

	public void setSonglist(ArrayList<MediaItem> songlist, int startIndex) {
		synchronized (lock) {
			if(songlist != null) {
				this.songlist = songlist;
				currentPlayingIndex = startIndex;
				createShuffleList();
			}
		}
	}

	private void createShuffleList(){
		shuffleList.clear();
		
		if(playMode == e_play_mode.e_play_mode_shuffle && songlist != null){
			for (int i = 0; i < songlist.size(); i++) {
				if(i != currentPlayingIndex)
					shuffleList.add(Integer.valueOf(i));
			}
			Collections.shuffle (shuffleList);
			shuffleList.add(0, currentPlayingIndex);		
			currentPlayingIndex = 0;
		}
		
	}

	public MediaItem getCurrentSong (){
		synchronized (lock) {

			if(playMode == e_play_mode.e_play_mode_shuffle) {

				if(shuffleList.size() > currentPlayingIndex && songlist.size() > currentPlayingIndex){
					return  songlist.get(shuffleList.get(currentPlayingIndex));
				}
			}else {
				if(songlist.size() > currentPlayingIndex) {
					return songlist.get(currentPlayingIndex);
				}
			}
			return null;
		}
	}
	public MediaItem getNextSong (){
		synchronized (lock) {

			if(playMode == e_play_mode.e_play_mode_repeat) {
				if(currentPlayingIndex == songlist.size() - 1)
					currentPlayingIndex = 0;
				else
					currentPlayingIndex++;

				return songlist.get(currentPlayingIndex); 
			}
			else if(playMode == e_play_mode.e_play_mode_normal) {

				if(songlist.size() > currentPlayingIndex + 1) {
					return songlist.get(++currentPlayingIndex); 
				}

			} else if(playMode == e_play_mode.e_play_mode_shuffle) {

				/**
				 * Pick an index from the shuffle list and use the index to get a song from the song list.
				 * When the shuffle list exhaust, we return null and the music playback stops.
				 */
				if(shuffleList.size() > currentPlayingIndex + 1 && songlist.size() > currentPlayingIndex + 1){
					return  songlist.get(shuffleList.get(++currentPlayingIndex));
				}
			}
			return null; 
		}
	}

	public MediaItem getPrevSong (){
		synchronized (lock) {

			if(currentPlayingIndex > 0) {
				return songlist.get(--currentPlayingIndex);
			}
			return null;
		}
	}
	public static e_play_mode getPlayModePref(Context context, e_play_mode defaultvalue) {
		SharedPreferences settings = context.getSharedPreferences(
				PREFS_NAME, 0);
		int val = settings.getInt(PREFS_PLAY_MODE_NAME, defaultvalue.getValue());
		e_play_mode emode = e_play_mode.fromInt(val);
		return emode;
	}

	public static void setPlayMode(int mode) {
		switch (mode) {
		case 0:
			MusicRetriever.getInstance().setPlayMode(e_play_mode.e_play_mode_normal);
			break;
		case 1:
			MusicRetriever.getInstance().setPlayMode(e_play_mode.e_play_mode_shuffle);
			break;
		case 2:
			MusicRetriever.getInstance().setPlayMode(e_play_mode.e_play_mode_repeat);
			break;
		default:
			MusicRetriever.getInstance().setPlayMode(e_play_mode.e_play_mode_normal);
			break;
		}
	}

	public static void savePlayModeToPref(Context context, e_play_mode mode) {
		SharedPreferences settings = context.getSharedPreferences(
				PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putInt(PREFS_PLAY_MODE_NAME, mode.ordinal());
		editor.commit();
	}
}
