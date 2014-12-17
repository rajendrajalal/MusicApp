package com.iamplus.musicplayer;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.provider.MediaStore.Audio.Artists;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class MediaArtistsAdaptor extends MediaListAdaptor{
	
	public MediaArtistsAdaptor(Context context, ContentResolver cr) {
		super(context, cr);
		startQuery(cr);
	}

	@Override
	public void startQuery(ContentResolver cr) {
		int token = 100;
		String[] projection = new String[] { Artists._ID, Artists.ARTIST, Artists.NUMBER_OF_ALBUMS, Artists.NUMBER_OF_TRACKS };
		String selection = null;
		String[] selectionArgs = null;
		String sortOrder = Artists.ARTIST + " ASC";
		
		new QueryHandler(cr).startQuery(token, this, Artists.EXTERNAL_CONTENT_URI, projection, selection, selectionArgs, sortOrder);
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
		holder.artistid = item.artistid;
		holder.textView.setText(item.artist);
		
		holder.imageView.setImageAsyncFromArtistSource(item.getArtistid(), mDefaultbitmap);

		return convertView;
	}

	@Override
	public void onQueryFinished(Cursor cursor) {
		if (cursor == null || !cursor.moveToFirst()) {
            return;
        }    
        // retrieve the indices of the columns where the ID, title, etc. of the song are
        int artistColumn = cursor.getColumnIndex(Artists.ARTIST);
        int albumColumn = cursor.getColumnIndex(Artists.NUMBER_OF_ALBUMS);
        int numberoftracks = cursor.getColumnIndex(Artists.NUMBER_OF_TRACKS);
        int idColumn = cursor.getColumnIndex(Artists._ID);


        // add each song to mAlbumList
        do {
        	MediaItem item =  new MediaItem();
        	item.setArtistid(cursor.getLong(idColumn));
        	item.setArtist(cursor.getString(artistColumn));
        	item.setNumberofAlbums(cursor.getInt(albumColumn));
        	item.setNumberOfTracks(cursor.getInt(numberoftracks));
        	
        	mList.add(item);
        	
        	/**
        	 * To add the alphabet index list
        	 */
			CharSequence title = item.getArtist();
			addToAlphabetIndex(title.charAt(0), cursor.getPosition());
			
        } while (cursor.moveToNext());
	}
	
	@Override
	public void registerMediaObserver(ContentResolver cr, ContentObserver observer) {
		cr.registerContentObserver(Artists.EXTERNAL_CONTENT_URI, true, observer);
	}

	@Override
	public boolean supportsAlphabetList() {
		return true;
	}
}
