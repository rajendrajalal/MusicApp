package com.iamplus.musicplayer;

import java.util.ArrayList;

import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio.Media;

public class MediaSearchHelper {
	public interface SearchResultsListener {
		void onCompleted(ArrayList<MediaItem> result);
	}

	private static class MediaItemListCreator extends AsyncTask<long[], Integer, ArrayList<MediaItem>>{

		SearchResultsListener mListener;
		ContentResolver mCr;
		public MediaItemListCreator(ContentResolver cr,SearchResultsListener listener){
			mListener = listener;
			mCr = cr;
		}
		@Override
		protected ArrayList<MediaItem> doInBackground(long[]... params) {

			long[] mediaIds = params[0];
			ArrayList<MediaItem> itemsList = new ArrayList<MediaItem>();

			if(mediaIds != null && mediaIds.length > 0) {

				for (int i = 0; i < mediaIds.length; i++) {

					Uri itemUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, 
							mediaIds[i]);

					String[] projection = new String[] {
							MediaStore.Audio.Media._ID,
							MediaStore.Audio.Media.TITLE,
							MediaStore.Audio.Media.ARTIST,
							MediaStore.Audio.Media.ALBUM,
							MediaStore.Audio.Media.ALBUM_ID,
					};

					String[] selectionArgs =  null;
					String sortOrder = Media.DEFAULT_SORT_ORDER;
					String selection = null;
					Cursor cursor = mCr.query(itemUri, projection, selection, selectionArgs, sortOrder);
					if(cursor != null && cursor.moveToFirst()) {
						// retrieve the indices of the columns where the ID, title, etc. of the song are
						int artistColumn = cursor.getColumnIndex(Media.ARTIST);
						int titleColumn = cursor.getColumnIndex(Media.TITLE);
						int idcolumn = cursor.getColumnIndex(Media._ID);
						int albumcolum = cursor.getColumnIndex(Media.ALBUM);
						int albumidcolum = cursor.getColumnIndex(Media.ALBUM_ID);

						MediaItem item = new MediaItem();
						item.setSongId( cursor.getLong(idcolumn));
						item.setTitle(cursor.getString(titleColumn));
						item.setAlbum(cursor.getString(albumcolum));
						item.setArtist(cursor.getString(artistColumn));
						item.setAlbumid(cursor.getLong(albumidcolum));
						itemsList.add(item);
						cursor.close();
					}
				}
			}

			return itemsList;
		}
		@Override
		protected void onPostExecute(ArrayList<MediaItem> result) {
			super.onPostExecute(result);
			if(mListener != null) {
				mListener.onCompleted(result);
			}
		}
	}

	public static void convertIDsToMediaItems(ContentResolver cr, long[] ids, SearchResultsListener listener) {
		new MediaItemListCreator(cr, listener).execute(ids);
	}

	private static class QueryHandler extends AsyncQueryHandler {

		private boolean queryFailed;
		public QueryHandler(ContentResolver cr) {
			super(cr);
		}
		@Override
		protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
			if(!queryFailed && cursor != null) {

				handleQueryComplete(cursor, (SearchResultsListener)cookie);
				cursor.close();
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

	public static void getAllSongsOfAlbum(ContentResolver cr, String albumname, SearchResultsListener listener) {

		if(albumname == null || listener == null) {
			return;
		}

		int token = 100;
		String[] projection = new String[] {
				MediaStore.Audio.Media._ID,
				MediaStore.Audio.Media.TITLE,
				MediaStore.Audio.Media.ARTIST,
				MediaStore.Audio.Media.ALBUM,
				MediaStore.Audio.Media.ALBUM_ID,
				MediaStore.Audio.Media.DURATION,
		};


		String[] selectionArgs =  {albumname};
		String sortOrder = Media.TITLE + " ASC";
		String selection = android.provider.MediaStore.Audio.Media.ALBUM
				+ "=?";
		new QueryHandler(cr).startQuery(token, listener,  MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection, selectionArgs, sortOrder);

	}

	public static void getAllSongWithTitle(ContentResolver cr, String titlename, SearchResultsListener listener) {
		int token = 100;
		String[] projection = new String[] {
				MediaStore.Audio.Media._ID,
				MediaStore.Audio.Media.TITLE,
				MediaStore.Audio.Media.ALBUM,
				MediaStore.Audio.Media.ALBUM_ID,
				MediaStore.Audio.Media.ARTIST,
				MediaStore.Audio.Media.DURATION,
		};


		String[] selectionArgs =  {titlename};
		String sortOrder = Media.TITLE + " ASC";
		String selection = android.provider.MediaStore.Audio.Media.TITLE
				+ "=?";
		new QueryHandler(cr).startQuery(token, listener,  MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection, selectionArgs, sortOrder);

	}

	public static void getAllSongsOfArtist(ContentResolver cr, String artistname, SearchResultsListener listener) {
		int token = 100;
		String[] projection = new String[] {
				MediaStore.Audio.Media._ID,
				MediaStore.Audio.Media.TITLE,
				MediaStore.Audio.Media.ALBUM,
				MediaStore.Audio.Media.ALBUM_ID,
				MediaStore.Audio.Media.ARTIST,
				MediaStore.Audio.Media.DURATION,
		};


		String[] selectionArgs =  {artistname};
		String sortOrder = Media.TITLE + " ASC";
		String selection = android.provider.MediaStore.Audio.Media.ARTIST
				+ "=?";
		new QueryHandler(cr).startQuery(token, listener,  MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection, selectionArgs, sortOrder);

	}

	private static void handleQueryComplete(Cursor cursor, SearchResultsListener listener) {

		ArrayList<MediaItem> mList = new ArrayList<MediaItem>();

		if (cursor == null || !cursor.moveToFirst() && listener != null) {

			listener.onCompleted(mList);
			return;
		}

		// retrieve the indices of the columns where the ID, title, etc. of the song are
		int artistColumn = cursor.getColumnIndex(Media.ARTIST);
		int titleColumn = cursor.getColumnIndex(Media.TITLE);
		int idcolumn = cursor.getColumnIndex(Media._ID);
		int albumcolum = cursor.getColumnIndex(Media.ALBUM);
		int idalbumid = cursor.getColumnIndex(Media.ALBUM_ID);
		int duration = cursor.getColumnIndex(Media.DURATION);

		// add each song to mAlbumList
		do {
			MediaItem item = new MediaItem();
			item.setSongId(cursor.getLong(idcolumn));
			item.setTitle(cursor.getString(titleColumn));
			item.setAlbum( cursor.getString(albumcolum));
			item.setArtist(cursor.getString(artistColumn));
			item.setAlbumid(idalbumid);
			item.setDuration(cursor.getLong(duration));

			mList.add(item);

		} while (cursor.moveToNext());

		listener.onCompleted(mList);
	}
}
