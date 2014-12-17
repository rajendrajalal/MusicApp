package com.iamplus.musicplayer;

import java.util.ArrayList;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore.Audio.Albums;
import android.provider.MediaStore.Audio.Artists;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

public class MediaSearchFragment extends Fragment {

	public static final int MINIMUM_CHAR_TO_SEARCH = 3;
	public static final String SEARCH_PARAM = "SearchParam";
	private MediaSearchAdaptor mSearchAdaptor;
	private Context mcontext;
	private EditText mSearchView;
	private ListView mListview;
	Handler mHandler = new Handler();
	MediaListAdaptor mActiveAdaptor;
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.search_media_screen, null);

		mcontext = getActivity();

		mListview = (ListView) view.findViewById(R.id.MediaList);
		mSearchView = (EditText) view.findViewById(R.id.searchtext);
		mSearchView.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				// TODO Auto-generated method stub

			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				// TODO Auto-generated method stub

			}

			@Override
			public void afterTextChanged(Editable s) {
				String textFromEditView = s.toString();
				if(textFromEditView.length() >= MINIMUM_CHAR_TO_SEARCH) {
					mSearchAdaptor.queryUsingString(textFromEditView);
				}
			}
		});

		mSearchAdaptor = new MediaSearchAdaptor(getActivity(), getActivity().getContentResolver());

		mListview.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View view, int index,
					long arg3) {

				String mimeType = mSearchAdaptor.getItem(index).getMimeType();

				if(mimeType.compareTo(Artists.ARTIST) == 0) {

					long artistid = mSearchAdaptor.getItem(index).getSongId();//Artist/Album/Song id same
					MediaArtistSongsAdaptor songsAdaptor = new MediaArtistSongsAdaptor(getActivity(), getActivity(). getContentResolver(), artistid);
					setListAdaptor(songsAdaptor);
					mSearchView.setVisibility(View.GONE);

				}
				else 
					if(mimeType.compareTo(Albums.ALBUM)  == 0){

						long albumid = mSearchAdaptor.getItem(index).getSongId();
						MediaAlbumSongsAdaptor songsAdaptor = new MediaAlbumSongsAdaptor(getActivity(), getActivity(). getContentResolver(), albumid);
						setListAdaptor(songsAdaptor);
						mSearchView.setVisibility(View.GONE);
					}
					else {
						ArrayList< MediaItem> list = mActiveAdaptor.getCurrentSongsList();
						ArrayList< MediaItem> newList = new ArrayList<MediaItem>();
						
						if(mActiveAdaptor == mSearchAdaptor) {
							for (MediaItem mediaItem : list) {
								if((mediaItem.getMimeType().compareTo(Artists.ARTIST) != 0)||
										(mediaItem.getMimeType().compareTo(Albums.ALBUM) != 0)) {
									newList.add(mediaItem);
								}
							}
						}else {
							newList = list;
						}
						
						int offset = list.size() - newList.size();
						MusicRetriever.getInstance().setSonglist(newList, index - offset );
						Intent musicservice = new Intent(mcontext, MusicService.class);
						musicservice.setAction(MusicService.ACTION_PLAY);
						getActivity().startService(musicservice);
					}
			}
		});
		return view;
	}
	
	private void setListAdaptor( MediaListAdaptor adaptor) {
		mListview.setAdapter(adaptor);
		mActiveAdaptor = adaptor;
	}
	public void showSearchScreen(final String queryString) {

		mHandler.post(new Runnable() {
			@Override
			public void run() {
				setListAdaptor(mSearchAdaptor);
				mSearchView.setVisibility(View.VISIBLE);
				
				if(queryString != null) {
					if(queryString.length() >= MINIMUM_CHAR_TO_SEARCH) {
						mSearchAdaptor.queryUsingString(queryString);
						mSearchView.setText(queryString);
					}
				}
			}
		});
	}
}
