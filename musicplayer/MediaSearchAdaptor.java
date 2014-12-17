package com.iamplus.musicplayer;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio.Albums;
import android.provider.MediaStore.Audio.Artists;
import android.provider.MediaStore.Audio.Media;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class MediaSearchAdaptor extends MediaListAdaptor{
	ContentResolver mCr;
	String mfilter;
	public MediaSearchAdaptor(Context context, ContentResolver cr) {
		super(context, cr);
		mCr = cr;
	}

	public void queryUsingString(String filter) {
		mfilter = filter;
		mList.clear();
		startQuery(mCr);
	}
	@Override
	public void startQuery(ContentResolver cr) {
		int token = 100;
		String[] projection = new String[] {
                BaseColumns._ID,   // this will be the artist, album or track ID
                MediaStore.Audio.Media.MIME_TYPE, // MIME type of audio file, or "artist" or "album"
                MediaStore.Audio.Artists.ARTIST,
                MediaStore.Audio.Albums.ALBUM,
                MediaStore.Audio.Media.TITLE,
        };

        Uri searchURi = Uri.parse("content://media/external/audio/search/fancy/" +
                Uri.encode(mfilter));
     
		String selection = null;
		String[] selectionArgs = null;
		String sortOrder = Media.ALBUM + " ASC";
		
		new QueryHandler(cr).startQuery(token, this,searchURi, projection, selection, selectionArgs, sortOrder);
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

		//Set the song id /album id on the holder load the songs in album or play song
		String mimeType = item.getMimeType();
		
		if(mimeType == null){
			holder.textView.setText(item.getTitle());
			
		}
		else
		if(mimeType.compareTo(Artists.ARTIST) == 0) {
			holder.textView.setText(item.getArtist());
		}
		else 
		if(mimeType.compareTo(Albums.ALBUM)  == 0){
			holder.textView.setText(item.getAlbum());
		}
		else {
			holder.textView.setText(item.getTitle());
		}	
		//holder.imageView.setImageAsyncFromAlbumSource(item.getAlbumid(), mDefaultbitmap);

		return convertView;
	}

	@Override
	public void onQueryFinished(Cursor cursor) {
		if (!cursor.moveToFirst()) {
            return;
        }    

        // retrieve the indices of the columns where the ID, title, etc. of the song are
        int artistColumn = cursor.getColumnIndex(MediaStore.Audio.Artists.ARTIST);
        int albumColumn = cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM);
        int titleColumn = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
        int mimeColumn = cursor.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE);
        int idColumn = cursor.getColumnIndex(BaseColumns._ID);


        // add each song to mAlbumList
        do {
        	MediaItem item = new MediaItem();
        	item.setArtist(cursor.getString(artistColumn));
        	item.setAlbum(cursor.getString(albumColumn));
        	item.setTitle(cursor.getString(titleColumn));
        	item.setMimeType(cursor.getString(mimeColumn));
        	item.setSongId(cursor.getLong(idColumn));
     
        	mList.add(item);
        } while (cursor.moveToNext());
		
	}

	public MediaItem getItem(int index) {
		if(index < mList.size())
			return mList.get(index);
		
		return null;
	}

	@Override
	public boolean supportsAlphabetList() {
		return false;
	}
}
