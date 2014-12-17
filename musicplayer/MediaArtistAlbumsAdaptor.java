package com.iamplus.musicplayer;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore.Audio.Albums;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class MediaArtistAlbumsAdaptor extends MediaListAdaptor{

	long mArtistid;
	public MediaArtistAlbumsAdaptor(Context context, ContentResolver cr, long artistid) {
		super(context, cr);
		mArtistid = artistid;
		startQuery(cr);
	}

	@Override
	public void startQuery(ContentResolver cr) {
		int token = 100;
		String[] projection = new String[] {
				Albums._ID,
				Albums.ALBUM,
		};

		String[] selectionArgs =  {String.valueOf(mArtistid)};
		String sortOrder = Albums.DEFAULT_SORT_ORDER;
		String selection = android.provider.MediaStore.Audio.Media.ARTIST_ID
				+ "=?";
		new QueryHandler(cr).startQuery(token, this,  Albums.EXTERNAL_CONTENT_URI, projection, selection, selectionArgs, sortOrder);
	}

	@Override
	public View getItemView(int index, View convertView, ViewGroup arg2) {
		MediaItem item =  mList.get(index);
		ViewHolder holder = null;
		if(convertView == null) {
			holder = new ViewHolder();
			
			ViewGroup layout = (ViewGroup) LayoutInflater.from(mContext).inflate(R.layout.album_list_item, null);
			holder.textView =  (TextView) layout.findViewById(R.id.list_item_text);
			holder.imageView=  (ImageViewAsync) layout.findViewById(R.id.list_item_icon);
			convertView = layout;
			convertView.setTag(holder);
			
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		
		holder.textView.setText(item.getAlbum());
		holder.albumid  = item.getAlbumid();
		
		//Start an asyc task to get the album art
		holder.imageView.setTag(index);
		holder.imageView.setImageAsyncFromAlbumSource(holder.albumid, mDefaultbitmap);
		
		return convertView;
	}

	@Override
	public void onQueryFinished(Cursor cursor) {
		if (cursor== null || !cursor.moveToFirst()) {
			return;
		}    
		// retrieve the indices of the columns where the ID, title, etc. of the song are
		int albumColumn = cursor.getColumnIndex(Albums.ALBUM);
		int idColumn = cursor.getColumnIndex(Albums._ID);


		// add each song to mAlbumList
		do {
			MediaItem item =  new MediaItem();
			item.setAlbumid(cursor.getLong(idColumn));
			item.setAlbum(cursor.getString(albumColumn));

			mList.add(item);
		} while (cursor.moveToNext());
		
	}

	@Override
	public boolean supportsAlphabetList() {
		// TODO Auto-generated method stub
		return false;
	}
}
