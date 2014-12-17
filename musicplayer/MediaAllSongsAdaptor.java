package com.iamplus.musicplayer;

import java.util.ArrayList;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio.Media;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class MediaAllSongsAdaptor extends MediaListAdaptor{

	public MediaAllSongsAdaptor(Context context, ContentResolver cr) {
		super(context, cr);
		startQuery(cr);
	}

	@Override
	public void onQueryFinished(Cursor cursor) {
		if (cursor == null || !cursor.moveToFirst()) {
			return;
		}    
		// retrieve the indices of the columns where the ID, title, etc. of the song are
		int artistColumn = cursor.getColumnIndex(Media.ARTIST);
		int titleColumn = cursor.getColumnIndex(Media.TITLE);
		int idcolumn = cursor.getColumnIndex(Media._ID);
		int albumcolum = cursor.getColumnIndex(Media.ALBUM);
		int albumidcolum = cursor.getColumnIndex(Media.ALBUM_ID);
		int durationcolumn = cursor.getColumnIndex(Media.DURATION);

		// add each song to mAlbumList
		do {
			MediaItem item = new MediaItem();
			item.setSongId( cursor.getLong(idcolumn));
			item.setTitle(cursor.getString(titleColumn));
			item.setAlbum(cursor.getString(albumcolum));
			item.setArtist(cursor.getString(artistColumn));
			item.setAlbumid(cursor.getLong(albumidcolum));
			item.setDuration(cursor.getLong(durationcolumn));

			mList.add(item);
			
			CharSequence title = item.getTitle();
			addToAlphabetIndex(title.charAt(0), cursor.getPosition());
			
		} while (cursor.moveToNext());

	}

	@Override
	public void startQuery(ContentResolver cr) {
		int token = 100;
		String[] projection = new String[] {
				MediaStore.Audio.Media._ID,
				MediaStore.Audio.Media.TITLE,
				MediaStore.Audio.Media.ARTIST,
				MediaStore.Audio.Media.ALBUM,
				MediaStore.Audio.Media.ALBUM_ID,
				MediaStore.Audio.Media.DURATION,
		};

		String[] selectionArgs =  null;
		String sortOrder = Media.DEFAULT_SORT_ORDER;
		String selection = null;
		new QueryHandler(cr).startQuery(token, this,  MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection, selectionArgs, sortOrder);

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

		holder.textView.setText(item.getTitle());
		
		//Set the song id /album id on the holder load the songs in album or play song
		holder.songid = item.songid;
		holder.albumid = item.albumid;
		
		//Start an asyc task to get the album art
		holder.imageView.setImageAsyncFromAlbumSource(item.getAlbumid(), mDefaultbitmap);
		return convertView;
	}

	public ArrayList<MediaItem> getCurrentSongsList () {
		return mList;
	}
	
	@Override
	public void registerMediaObserver(ContentResolver cr, ContentObserver observer) {
		cr.registerContentObserver(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true, observer);
	}

	@Override
	public boolean supportsAlphabetList() {
		return true;
	}
}
