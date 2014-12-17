package com.iamplus.musicplayer;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;

public abstract class MediaListAdaptor implements ListAdapter{

	public interface MediaLoadListener{
		public void onLoadCompleted();
	}

	public static final int LIST_ANIMATION_DURATION  = 350;

	abstract public void onQueryFinished(Cursor cr);
	abstract public void startQuery(ContentResolver cr);
	abstract public View getItemView(int index, View arg1, ViewGroup arg2);
	abstract public boolean supportsAlphabetList();

	private DataSetObserver mListObserver;
	protected Context mContext; 
	protected static Bitmap mDefaultbitmap;
	protected ArrayList<MediaItem> mList = new ArrayList<MediaItem>();
	private MediaLoadListener mLoadListener;
	private static Object mLock = new Object();
	protected HashMap<Character, Integer> mAlphabetMap = new HashMap<Character, Integer>();
	
	protected static class QueryHandler extends AsyncQueryHandler {

		private boolean queryFailed;
		public QueryHandler(ContentResolver cr) {
			super(cr);
		}
		@Override
		protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
			
			synchronized (mLock) {
				MediaListAdaptor adaptor = (MediaListAdaptor) cookie;
				adaptor.mList.clear();
				adaptor.mAlphabetMap.clear();
				if(!queryFailed && cursor != null) {
					adaptor.onQueryFinished(cursor);
					cursor.close();
				}
				adaptor.mListObserver.onChanged();
				if(adaptor.mLoadListener != null){
					adaptor.mLoadListener.onLoadCompleted();
				}
			}
		}

		@Override
		public void startQuery(int token, Object cookie, Uri uri,
				String[] projection, String selection, String[] selectionArgs,
				String orderBy) {

			queryFailed = false;
			try {
				super.startQuery(token, cookie, uri, projection, selection, selectionArgs,
						orderBy);

			} catch (Exception e) {
				queryFailed = true;
			}
		}
	}

	public void unregisterMediaObserver(ContentResolver cr, ContentObserver observer) {
		cr.unregisterContentObserver(observer);
	}

	public void registerMediaObserver(ContentResolver cr, ContentObserver observer) {
		//Not IMPL
		// Derived classes to override.
	}
	
	public  MediaListAdaptor(Context context,ContentResolver cr) {
		mContext = context;
		if(mDefaultbitmap == null) {
			try {
				mDefaultbitmap =  BitmapFactory.decodeResource(mContext.getResources(),
						R.drawable.albumart_mp_unknown);
			} catch (Exception e) {
			} catch (OutOfMemoryError e) {
			}
		}
	}

	public void setOnLoadListener(MediaLoadListener list){
		mLoadListener = list;
	}

	public ArrayList<MediaItem> getCurrentSongsList (){
		synchronized (mLock) {
			return mList;
		}
	}

	@Override
	public int getCount() {
		synchronized (mLock) {
			return mList.size();
		}
	}

	@Override
	public Object getItem(int arg0) {
		return null;
	}

	@Override
	public long getItemId(int arg0) {
		return 0;
	}

	@Override
	public int getItemViewType(int arg0) {
		return 0;
	}

	@Override
	public View getView(int index, View arg1, ViewGroup arg2) {

		return getItemView(index, arg1, arg2);
	}

	@Override
	public int getViewTypeCount() {
		return 1;
	}

	@Override
	public boolean hasStableIds() {
		return false;
	}

	@Override
	public boolean isEmpty() {
		return false;
	}

	@Override
	public void registerDataSetObserver(DataSetObserver arg0) {
		mListObserver = arg0;
	}

	@Override
	public void unregisterDataSetObserver(DataSetObserver arg0) {
		arg0 = null;
	}

	@Override
	public boolean areAllItemsEnabled() {
		return true;
	}

	@Override
	public boolean isEnabled(int arg0) {
		return true;
	}
	
	protected void addToAlphabetIndex(Character alphabet, int position){
		if(!mAlphabetMap.containsKey(alphabet)){
			mAlphabetMap.put(alphabet, position);
		}
	}
}
