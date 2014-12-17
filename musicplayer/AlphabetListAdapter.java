package com.iamplus.musicplayer;

import java.util.ArrayList;
import com.iamplus.musicplayer.R;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class AlphabetListAdapter extends BaseAdapter {

	private Context mContext;
	private ArrayList<Character> mAlphabets;

	public void setAlphabetList(ArrayList<Character> list) {
		mAlphabets = list;
	}

	public AlphabetListAdapter(Context context) {
		super();
		mContext = context;
	}
	@Override
	public int getCount() {
		return mAlphabets.size();
	}

	@Override
	public Object getItem(int position) {
		return null;
	}

	@Override
	public long getItemId(int position) {
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		
		TextView alphaview = null;
		if (convertView == null) {
			alphaview = (TextView) View.inflate(mContext, R.layout.alphabet_list_item, null);
		}else{
			alphaview = (TextView) convertView;
		}
		Character seq = mAlphabets.get(position);
		char[] chars = new char[1];
		chars[0] = seq;
		String alphabet = new String(chars);
		alphaview.setText(alphabet);
		alphaview.setTag(seq);
		return alphaview;
	}
}
