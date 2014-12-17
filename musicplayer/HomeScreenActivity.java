package com.iamplus.musicplayer;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Intent;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Adapter;
import android.widget.ListView;

import com.iamplus.musicplayer.AlbumDetailsFragment.AlbumDetailsEventListener;
import com.iamplus.musicplayer.ArtistDetailsFragment.ArtistDetailsEventListener;
import com.iamplus.musicplayer.MediaListFragment.ListItemClickListener;
import com.iamplus.musicplayer.NowPlayingFragment.NowPlayingFragmentEventListener;

public class HomeScreenActivity extends Activity implements NowPlayingFragmentEventListener, AlbumDetailsEventListener, ArtistDetailsEventListener{

	public enum E_Music_View_Type{
		E_Music_View_Type_Songs,
		E_Music_View_Type_Albums,
		E_Music_View_Type_Artists,
		E_Music_View_Type_None
	}
	
	private MediaListAdaptor mAdaptor;
	private MediaListFragment mListview;
	private Handler mHandler = new Handler(Looper.getMainLooper());
	private MediaSearchFragment mSearchFragment;
	private MediaContentObserver mObserver;
	private NowPlayingFragment mNowPlayingFrag;
	private E_Music_View_Type mPrevMusicViewType = E_Music_View_Type.E_Music_View_Type_Albums;
	private long mPrevMusicScreenExtraInfo = -1;
	private int mPreviousAlbumSelectedIndex = 0;
	private int mPreviousArtistSelectedIndex = 0;
	private int mPreviousSongSelectedIndex = 0;

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_home_screen);

		mObserver = new  MediaContentObserver(mHandler);

		mAdaptor = new MediaAlbumsAdaptor(this,getContentResolver());

		mListview = new MediaListFragment();

		setListAsActiveFragment(R.string.menu_string_albums);

		mListview.setOnItemClickListener(new ListItemClickListener() {

			@Override
			public void onItemClick(ListView listview, View view, int position) {

				Adapter adaptor = listview.getAdapter();

				if(adaptor instanceof MediaAlbumsAdaptor) {
					ViewHolder holder= (ViewHolder) view.getTag();
					setAlbumDetailsFragment(holder.albumid);
					mPreviousAlbumSelectedIndex = position;
				} 
				else
				if(adaptor instanceof MediaArtistsAdaptor) {
					ViewHolder holder= (ViewHolder) view.getTag();
					setArtistDetailsFragment(holder.artistid);
					mPreviousArtistSelectedIndex = position;
				}
				else 
				if(adaptor instanceof MediaAllSongsAdaptor) {
					//Start the now playing view
					MusicRetriever.getInstance().setSonglist(mAdaptor.getCurrentSongsList(), position);
					Intent musicservice = new Intent(listview.getContext(), MusicService.class);
					musicservice.setAction(MusicService.ACTION_PLAY);
					startService(musicservice);
					setNowPlayingAsActiveFragment(false, E_Music_View_Type.E_Music_View_Type_Songs, position);
					mPreviousSongSelectedIndex = position;
				}
			}
		});

		mListview.setListAdapter(mAdaptor);
	}

	private void handleSearchIntent(Intent intent) {
		if(intent != null && intent.getAction() == MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH) {
			Bundle bundle = intent.getExtras();
			if(bundle != null) {
				String song = bundle.getString(SearchManager.QUERY);
				setSearchAsActiveFragment(song);
			}
		}
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		
		Bundle bundle = null;
		String fragment = null;
		if (intent != null) {
			bundle = intent.getExtras();
			if ( bundle != null) {
				fragment = bundle.getString("com.iamplus.aware.FRAGMENT_CLASS");
				if (fragment != null) {
					selectItem(fragment);
				}
			} 
		}
		handleSearchIntent(intent);
	}

	private void selectItem(String menuType) {

		/*
		 * Remove the previous observer.
		 */
		removeContentObserver(mAdaptor);

		if(menuType.equalsIgnoreCase(getResources().getString(R.string.menu_string_albums))){
			setListAsActiveFragment(R.string.menu_string_albums);
			mAdaptor = new MediaAlbumsAdaptor(this,getContentResolver());
			mListview.setListAdapter(mAdaptor);
			mListview.setSelectedIndex(mPreviousAlbumSelectedIndex);
		}else 
		if(menuType.equalsIgnoreCase(getResources().getString(R.string.menu_string_artists))){
			setListAsActiveFragment(R.string.menu_string_artists);
			mAdaptor = new MediaArtistsAdaptor(this,getContentResolver());
			mListview.setListAdapter(mAdaptor);
			mListview.setSelectedIndex(mPreviousArtistSelectedIndex);
		}else 
		if(menuType.equalsIgnoreCase(getResources().getString(R.string.menu_string_now_playing))){
			setNowPlayingAsActiveFragment(false, E_Music_View_Type.E_Music_View_Type_None, 0);
		}else 
		if(menuType.equalsIgnoreCase(getResources().getString(R.string.menu_string_all_songs))){
			setListAsActiveFragment(R.string.menu_string_all_songs);
			mAdaptor = new MediaAllSongsAdaptor(this,getContentResolver());
			mListview.setListAdapter(mAdaptor);
			mListview.setSelectedIndex(mPreviousSongSelectedIndex);
		}
		setContentObserver(mAdaptor);
	}

	private void setSearchAsActiveFragment(String queryString) {
		getFragmentManager().beginTransaction()
		.replace(R.id.content_frame, mSearchFragment)
		.commit();

		mSearchFragment.showSearchScreen(queryString);
	}
	
	private void setListAsActiveFragment(int labelId) {
		String label = getResources().getString(labelId);
		if (label != null) {
			setListAsActiveFragment(label);
		}
	}

	private void setListAsActiveFragment(String label) {
        setIndexHighlightedForInAppMenu(label);
		getFragmentManager().beginTransaction()
		.replace(R.id.content_frame, mListview)
		.commit();
	}

	private void setAlbumDetailsFragment(long albumid) {

		AlbumDetailsFragment adf = new AlbumDetailsFragment();

		Bundle args = new Bundle();
		args.putLong(AlbumDetailsFragment.ALBUM_ID_KEY, albumid);
		adf.setArguments(args);
		adf.setAlbumDetailsEventListener(this);
		setIndexHighlightedForInAppMenu(getResources().getString(R.string.menu_string_albums));
		
		getFragmentManager().beginTransaction()
		.replace(R.id.content_frame, adf)
		.commit();
	}

	private void setArtistDetailsFragment(long albumid) {

		ArtistDetailsFragment adf = new ArtistDetailsFragment();

		Bundle args = new Bundle();
		args.putLong(ArtistDetailsFragment.ARTIST_ID_KEY, albumid);
		adf.setArguments(args);
		adf.setArtistDetailsEventListener(this);
		
		setIndexHighlightedForInAppMenu(getResources().getString(R.string.menu_string_artists));
		
		getFragmentManager().beginTransaction()
		.replace(R.id.content_frame, adf)
		.commit();
	}

	public void setNowPlayingAsActiveFragment(boolean newlist, E_Music_View_Type type, long extrainfo) {

		if(mNowPlayingFrag == null) {
			mNowPlayingFrag = new NowPlayingFragment();
			mNowPlayingFrag.setNowPlayingFragmentEventListener(this);
		}
		if(newlist) {
			Bundle args = new Bundle();
			args.putBoolean(NowPlayingFragment.STARTNEWPLAYBACK_KEY, true);
			mNowPlayingFrag.setArguments(args);
		}
		setIndexHighlightedForInAppMenu(getResources().getString(R.string.menu_string_now_playing));
		
		getFragmentManager().beginTransaction()
		.replace(R.id.content_frame, mNowPlayingFrag)
		.commit();
		
		if(type != E_Music_View_Type.E_Music_View_Type_None ) {
			mPrevMusicScreenExtraInfo = extrainfo;
			mPrevMusicViewType = type;
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		setContentObserver(mAdaptor);
	}

	@Override
	protected void onPause() {
		super.onPause();
		removeContentObserver(mAdaptor);
	}
	@Override
	protected void onDestroy() {
		super.onDestroy();

	}

	private class MediaContentObserver extends ContentObserver{
		public MediaContentObserver(Handler handler) {
			super(handler);
		}

		@Override
		public void onChange(boolean selfChange) {
			super.onChange(selfChange);

			if(!selfChange) {
				if(mAdaptor != null) {
					mAdaptor.startQuery(getContentResolver());
				}
			}
		}
	}

	private void setContentObserver(MediaListAdaptor adaptor) {
		if(adaptor != null) {
			adaptor.registerMediaObserver(getContentResolver(), mObserver);
		}
	}

	private void removeContentObserver(MediaListAdaptor adaptor) {
		if(adaptor != null) {
			adaptor.unregisterMediaObserver(getContentResolver(), mObserver);
		}
	}

	@Override
	public void onNowPlayingNavigateBack() {
		if(mPrevMusicViewType == E_Music_View_Type.E_Music_View_Type_Songs){
			setListAsActiveFragment(R.string.menu_string_all_songs);
			mAdaptor = new MediaAllSongsAdaptor(this,getContentResolver());
			mListview.setListAdapter(mAdaptor);
			mListview.setSelectedIndex(mPreviousSongSelectedIndex);
		}else
		if(mPrevMusicViewType == E_Music_View_Type.E_Music_View_Type_Albums){
			setAlbumDetailsFragment(mPrevMusicScreenExtraInfo);
		}else
		if(mPrevMusicViewType == E_Music_View_Type.E_Music_View_Type_Artists){
			setArtistDetailsFragment(mPrevMusicScreenExtraInfo);
		}
	}

	@Override
	public void onArtistDetailsNavigateBack() {
		selectItem(getResources().getString(R.string.menu_string_artists));
	}

	@Override
	public void onAlbumDetailsNavigateBack() {
		selectItem(getResources().getString(R.string.menu_string_albums));
	}
}
