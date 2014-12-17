package com.iamplus.musicplayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

import com.iamplus.musicplayer.MediaListAdaptor.MediaLoadListener;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;
import android.widget.TextView;

public class MediaListFragment extends Fragment implements MediaLoadListener {


	public interface ListItemClickListener{
		public  void onItemClick(ListView l, View v, int position);
	}

	private ListItemClickListener mListener;
	private ListView mListView;
	private ListView mAlphabetListView;
	private MediaListAdaptor mAdaptor;
	private TextView mEmptyString;
	private int mSelectedIndex;
	public MediaListFragment() {

	}
	public void setOnItemClickListener(ListItemClickListener listener) {
		mListener = listener;
	}
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View root = inflater.inflate(R.layout.media_list_view_screen, null);
		mListView = (ListView) root.findViewById(R.id.media_list_view);
		mAlphabetListView = (ListView) root.findViewById(R.id.alphabet_list_view);
		mEmptyString = (TextView) root.findViewById(R.id.emptyString);
		mListView.setAdapter(mAdaptor);
		mListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View v, int position,
					long arg3) {
				mListener.onItemClick(mListView, v, position);

			}
		});
		mListView.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				
				if(mAdaptor.supportsAlphabetList() && mAdaptor.mAlphabetMap.size() > 1){
					
					mAlphabetListView.setVisibility(View.VISIBLE);
					
					AlphabetListAdapter adaptor = new AlphabetListAdapter(getActivity());
					ArrayList<Character> alphalist = new ArrayList<Character>();
				 	
					Set<Character> keys =  mAdaptor.mAlphabetMap.keySet();
					
					for (Character character : keys) {
						alphalist.add(character);
					}
					
					Collections.sort(alphalist);
					adaptor.setAlphabetList(alphalist);
					
					mAlphabetListView.setAdapter(adaptor);
					
					mAlphabetListView.setOnItemClickListener(new OnItemClickListener() {

						@Override
						public void onItemClick(AdapterView<?> arg0, View view,
								int arg2, long position) {
							
							mAlphabetListView.setVisibility(View.GONE);
							
							Character seq = (Character) view.getTag();
							int newpos = mAdaptor.mAlphabetMap.get(seq);
							mListView.setSelection(newpos);
						}
					});
					return true;
				}
				return false;
			}
		});
		return root;
	}

	public void setSelectedIndex(int index){
		mSelectedIndex = index;
	}
	
	public void setListAdapter(MediaListAdaptor adaptor) {

		if(mListView != null) {
			mListView.setAdapter(adaptor);
		}
		mAdaptor = adaptor;
		adaptor.setOnLoadListener(this);
	}
	@Override
	public void onLoadCompleted() {
		mAlphabetListView.setVisibility(View.GONE);
		if(mAdaptor.getCount() > 0) {
			mEmptyString.setVisibility(View.GONE);
		}else{
			mEmptyString.setVisibility(View.VISIBLE);
		}
		if(mListView != null){
			if(mListView.getAdapter().getCount() > mSelectedIndex){
				mListView.setSelection(mSelectedIndex);
			}
		}
	}
}
