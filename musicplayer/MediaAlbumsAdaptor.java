package com.iamplus.musicplayer;

import java.io.FileNotFoundException;
import java.io.IOException;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio.Albums;
import android.provider.MediaStore.Audio.Media;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class MediaAlbumsAdaptor extends MediaListAdaptor{

	public MediaAlbumsAdaptor(Context context, ContentResolver cr) {
		super(context, cr);
		startQuery(cr);
	}

	@Override
	public void startQuery(ContentResolver cr) {
		int token = 100;
		String[] projection = new String[] { Albums._ID, Albums.ALBUM, Albums.ARTIST, Albums.NUMBER_OF_SONGS };
		String selection = null;
		String[] selectionArgs = null;
		String sortOrder = Media.ALBUM + " ASC";

		new QueryHandler(cr).startQuery(token, this, Albums.EXTERNAL_CONTENT_URI, projection, selection, selectionArgs, sortOrder);
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
		holder.songid = item.songid;
		holder.albumid = item.albumid;
		holder.textView.setText(item.getAlbum());
		holder.imageView.setImageAsyncFromAlbumSource(item.getAlbumid(), mDefaultbitmap);

		return convertView;
	}

	@Override
	public void onQueryFinished(Cursor cursor) {
		if (cursor==null || !cursor.moveToFirst()) {
			return;
		}    

		// retrieve the indices of the columns where the ID, title, etc. of the song are
		int artistColumn = cursor.getColumnIndex(Albums.ARTIST);
		int albumColumn = cursor.getColumnIndex(Albums.ALBUM);
		int numberofsongs = cursor.getColumnIndex(Albums.NUMBER_OF_SONGS);
		int idColumn = cursor.getColumnIndex(Albums._ID);

		// add each song to mAlbumList
		do {
			MediaItem item = new MediaItem();
			item.setAlbumid(cursor.getLong(idColumn));
			item.setAlbum(cursor.getString(albumColumn));
			item.setArtist(cursor.getString(artistColumn));
			item.setNumberOfTracks(cursor.getInt(numberofsongs));
			mList.add(item);
			
			CharSequence title = item.getAlbum();
			addToAlphabetIndex(title.charAt(0), cursor.getPosition());
			
		} while (cursor.moveToNext());

	}

	public static Bitmap getAlbumArtwork (Context context, long albumid, boolean thumbnail) {
		Uri sArtworkUri = Uri
				.parse("content://media/external/audio/albumart");
		Uri albumArtUri = ContentUris.withAppendedId(sArtworkUri, albumid);
		Bitmap bitmap = null;
		try {
			bitmap = MediaStore.Images.Media.getBitmap(
					context.getContentResolver(), albumArtUri);

			if(thumbnail && bitmap != null) {
				bitmap =  Bitmap.createScaledBitmap(bitmap, 100, 100, false);
			}

		}catch (FileNotFoundException exception) {
			if(mDefaultbitmap == null) {
				try {
					mDefaultbitmap =  BitmapFactory.decodeResource(context.getResources(),
							R.drawable.albumart_mp_unknown);
				} 
				catch (OutOfMemoryError error){
				}	
			}
			bitmap = mDefaultbitmap;
		}catch (IOException e) {
		}catch (OutOfMemoryError error){
		}
		
		return bitmap;
	}
	
	public static Bitmap getScaledAlbumArtwork (Context context, long albumid, boolean scale) {
		Uri sArtworkUri = Uri
				.parse("content://media/external/audio/albumart");
		Uri albumArtUri = ContentUris.withAppendedId(sArtworkUri, albumid);
		Bitmap bitmap = null;
		try {
			bitmap = MediaStore.Images.Media.getBitmap(
					context.getContentResolver(), albumArtUri);

			if(bitmap != null && scale) {
				bitmap =  Bitmap.createScaledBitmap(bitmap, 100, 100, false);
			}

		}catch (FileNotFoundException exception) {
		}catch (IOException e) {
		}catch (OutOfMemoryError error){
		}
		
		return bitmap;
	}

	public static Bitmap getFirstAlbumArtForArtist(Context context, long artistid) {
		String[] projection = new String[] {
				MediaStore.Audio.Media.ALBUM_ID,
		};

		String[] selectionArgs =  {String.valueOf(artistid)};
		String sortOrder = Media.ALBUM + " ASC";
		String selection = android.provider.MediaStore.Audio.Media.ARTIST_ID
				+ "=?";

		Cursor cursor  = context.getContentResolver().query(Media.EXTERNAL_CONTENT_URI,
				projection, selection, selectionArgs, sortOrder);
		
		Bitmap bitmap = null;
		if(cursor != null) {
			if(cursor.moveToFirst()) {
				
				long albumid = cursor.getLong(cursor.getColumnIndex(Media.ALBUM_ID));
				Uri sArtworkUri = Uri
						.parse("content://media/external/audio/albumart");
				Uri albumArtUri = ContentUris.withAppendedId(sArtworkUri, albumid);
				
				try {
					bitmap = MediaStore.Images.Media.getBitmap(
							context.getContentResolver(), albumArtUri);

				} catch (FileNotFoundException exception) {
				} catch (IOException e) {
				} catch (OutOfMemoryError error){	
				}
			}
			cursor.close();
		}
		return bitmap;
	}
	
	@Override
	public void registerMediaObserver(ContentResolver cr, ContentObserver observer) {
		cr.registerContentObserver(Albums.EXTERNAL_CONTENT_URI, true, observer);
	}

	@Override
	public boolean supportsAlphabetList() {
		return true;
	}
}
