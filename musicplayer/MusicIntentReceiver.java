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

package com.iamplus.musicplayer;

import java.util.ArrayList;

import com.iamplus.musicplayer.MediaSearchHelper.SearchResultsListener;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;

/**
 * Receives broadcasted intents. In particular, we are interested in the
 * android.media.AUDIO_BECOMING_NOISY and android.intent.action.MEDIA_BUTTON intents, which is
 * broadcast, for example, when the user disconnects the headphones. This class works because we are
 * declaring it in a &lt;receiver&gt; tag in AndroidManifest.xml.
 */
public class MusicIntentReceiver extends BroadcastReceiver {

	public static final String VOICE_INTENT_MUSIC = "com.iamplus.musicplayer.MUSIC";

	/**
	 * Key for setting an action
	 */
	public static final String VOICE_PLAY_INTENT_KEY = "music_intent_extra";

	/**---Key Values for VOICE_EXTRA_INTENT_KEY
	 */
	public static final int VOICE_TYPE_TOGGLE_PLAYBACK = 1;
	public static final int VOICE_TYPE_PLAY = 2;
	public static final int VOICE_TYPE_PAUSE = 3;
	public static final int VOICE_TYPE_STOP = 4;
	public static final int VOICE_TYPE_SKIP = 5;
	public static final int VOICE_TYPE_REWIND = 6;
	public static final int VOICE_TYPE_SHUFFLE_ON = 7;
	public static final int VOICE_TYPE_SHUFFLE_OFF = 8;
	public static final int VOICE_TYPE_REPEAT_ON = 9;
	public static final int VOICE_TYPE_REPEAT_OFF = 10;

	/** End -- Key Values for VOICE_EXTRA_INTENT_KEY
	 */

	/**
	 * Key for setting a music list to be played.
	 * Value should be a long[] array of Media identifiers.
	 */
	public static final String VOICE_PLAY_MEDIA_LIST_KEY = "music_list_key";

	@Override
	public void onReceive(Context context, Intent intent) {

		if(intent == null) {
			return;
		}
		if (intent.getAction().equals(android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY)) {
			// send an intent to our MusicService to telling it to pause the audio
			context.startService(new Intent(MusicService.ACTION_PAUSE));

		} else if (intent.getAction().equals(Intent.ACTION_MEDIA_BUTTON)) {
			handleRemoteCommands(context, intent);
		}else if(intent.getAction().equals(VOICE_INTENT_MUSIC)) {

			Bundle bundle  = intent.getExtras();

			if(bundle != null) {
				int keyEvent = bundle.getInt(VOICE_PLAY_INTENT_KEY, -1);
				if(keyEvent > 0) {
					handleVoiceCommands(context,keyEvent);
				}else{ 
					long[] mediaIds = bundle.getLongArray(VOICE_PLAY_MEDIA_LIST_KEY);
					if(mediaIds != null  && mediaIds.length > 0) {
						handlePlaylist(context, mediaIds);
					}
				}
			}else {
				return;
			}

		}
	}

	private void handlePlaylist(final Context context, long[] mediaIds) {

		MediaSearchHelper.convertIDsToMediaItems(context.getContentResolver(), mediaIds, new SearchResultsListener() {

			@Override
			public void onCompleted(ArrayList<MediaItem> result) {
				MusicRetriever.getInstance().setSonglist(result, 0);
				Intent musicservice = new Intent(context, MusicService.class);
				musicservice.setAction(MusicService.ACTION_PLAY);
				context.startService(musicservice);		
			}
		});
	}

	private void handleRemoteCommands(Context context, Intent intent) {

		Bundle bundle  = intent.getExtras();
		KeyEvent keyEvent = null;

		if(bundle != null) {
			keyEvent = (KeyEvent)bundle.get(Intent.EXTRA_KEY_EVENT);
			if(keyEvent == null ) {
				return;
			}
		}else {
			return;
		}

		if (keyEvent.getAction() != KeyEvent.ACTION_DOWN)
			return;

		Intent musicservice = new Intent(context, MusicService.class);

		switch (keyEvent.getKeyCode()) {
		case KeyEvent.KEYCODE_HEADSETHOOK:
		case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
		case KeyEvent.KEYCODE_MEDIA_PLAY:
		case KeyEvent.KEYCODE_MEDIA_PAUSE:
			musicservice.setAction(MusicService.ACTION_TOGGLE_PLAYBACK);
			break;
		case KeyEvent.KEYCODE_MEDIA_STOP:
			musicservice.setAction(MusicService.ACTION_STOP);
			break;
		case KeyEvent.KEYCODE_MEDIA_NEXT:
			musicservice.setAction(MusicService.ACTION_SKIP);
			break;
		case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
			// TODO: ensure that doing this in rapid succession actually plays the
			// previous song
			musicservice.setAction(MusicService.ACTION_REWIND);
			break;
		}
		context.startService(musicservice);
	}

	private void handleVoiceCommands(Context context, int keyEvent) {

		Intent musicservice = new Intent(context, MusicService.class);

		switch (keyEvent) {
		case VOICE_TYPE_PLAY:
			musicservice.setAction(MusicService.ACTION_PLAY);
			break;
		case VOICE_TYPE_PAUSE:
			musicservice.setAction(MusicService.ACTION_PAUSE);
			break;
		case VOICE_TYPE_TOGGLE_PLAYBACK:
			musicservice.setAction(MusicService.ACTION_TOGGLE_PLAYBACK);
			break;
		case VOICE_TYPE_STOP:
			musicservice.setAction(MusicService.ACTION_STOP);
			break;
		case VOICE_TYPE_SKIP:
			musicservice.setAction(MusicService.ACTION_SKIP);
			break;
		case VOICE_TYPE_REWIND:
			musicservice.setAction(MusicService.ACTION_REWIND);
			break;
		case VOICE_TYPE_REPEAT_OFF:
			musicservice.setAction(MusicService.ACTION_REPEAT_OFF);
			break;
		case VOICE_TYPE_SHUFFLE_OFF:
			musicservice.setAction(MusicService.ACTION_SHUFFLE_OFF);
			break;
		case VOICE_TYPE_SHUFFLE_ON:
			musicservice.setAction(MusicService.ACTION_SHUFFLE_ON);
			break;
		case VOICE_TYPE_REPEAT_ON:
			musicservice.setAction(MusicService.ACTION_REPEAT_ON);
			break;
		default:
			musicservice.setAction(MusicService.ACTION_PLAY);
		}
		context.startService(musicservice);
	}
}
