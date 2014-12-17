package com.iamplus.musicplayer;

import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.iamplus.musicplayer.HomeScreenActivity.E_Music_View_Type;
import com.iamplus.musicplayer.MediaListAdaptor.MediaLoadListener;

public class ArtistDetailsFragment extends Fragment {

	public static String ARTIST_ID_KEY = "Artist_id";
	private MediaArtistSongsAdaptor mSongsAdaptor;
	private ListView mListview;
	private ArtistDetailsEventListener mListener;
	
	public interface ArtistDetailsEventListener {
		public void onArtistDetailsNavigateBack();
	}
	
	public void setArtistDetailsEventListener(ArtistDetailsEventListener list){
		mListener = list;
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		View vg = inflater.inflate(R.layout.album_details_screen, container, false);

		setUpUIControls(vg);
		IntentFilter filter = new IntentFilter();
		filter.addAction(MusicService.MUSIC_BROAD_CAST_PLAYBACK_SKIP);
		filter.addAction(MusicService.MUSIC_BROAD_CAST_PLAYBACK_PREV);
		LocalBroadcastManager.getInstance(getActivity()).
		registerReceiver(mPlayBackBroadCastReceiver, filter);
		return vg;
	}
	
	@Override
	public void onResume() {
		super.onResume();
		mListview.invalidateViews();
		mListview.setEnabled(true);
	}
	
	@Override
	public void onDestroyView() {
		super.onDestroyView();
		LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mPlayBackBroadCastReceiver);
		
	}
	
	private void setUpUIControls(final View  rootView) {

		mListview = (ListView) rootView.findViewById(R.id.MediaList);

		Bundle extras = getArguments();
		long artistid = 0;
		if (extras != null) {
			artistid = extras.getLong(ARTIST_ID_KEY);
		}
		
		final long currentArtistId  = artistid;
		mSongsAdaptor = new MediaArtistSongsAdaptor(getActivity(), getActivity(). getContentResolver(), artistid);
		mListview.setAdapter(mSongsAdaptor);

		mListview.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int index,
					long arg3) {

				if(index > 0) {// First item is the artist name 

					//index - 1, as we are repeating the first item to display the album art
					MusicRetriever.getInstance().setSonglist(mSongsAdaptor.getCurrentSongsList(), index -1);
					Intent musicservice = new Intent(getActivity(), MusicService.class);
					musicservice.setAction(MusicService.ACTION_PLAY);
					getActivity().startService(musicservice);

					HomeScreenActivity  activity = (HomeScreenActivity) getActivity();
					activity.setNowPlayingAsActiveFragment(false, E_Music_View_Type.E_Music_View_Type_Artists, currentArtistId);
					mListview.setEnabled(false);
				} else {
					if(mListener != null){
						mListener.onArtistDetailsNavigateBack();
					}
				}
			}
		});

		mSongsAdaptor.setOnLoadListener(new MediaLoadListener() {

			@Override
			public void onLoadCompleted() {

				if(mSongsAdaptor.getCount() == 0) {
					RelativeLayout layout = (RelativeLayout) rootView;

					TextView invalidItem = new TextView(getActivity());
					invalidItem.setText(R.string.invalid_file_artist);
					invalidItem.setTextSize(getResources().getDimension(R.dimen.font_size_medium));
					invalidItem.setTextColor(getResources().getColor(R.color.music_white_color));
					invalidItem.setGravity(Gravity.CENTER);
					RelativeLayout.LayoutParams params = new 
							RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, 
									RelativeLayout.LayoutParams.WRAP_CONTENT);
					params.addRule(RelativeLayout.CENTER_IN_PARENT);
					layout.addView(invalidItem, params);
				}
			}
		});
	}
	BroadcastReceiver mPlayBackBroadCastReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			mListview.invalidateViews();
		}
	};
}
