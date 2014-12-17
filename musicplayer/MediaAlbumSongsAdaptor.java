package com.iamplus.musicplayer;

import java.util.ArrayList;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio.Media;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class MediaAlbumSongsAdaptor extends MediaListAdaptor{

	long mAlbumid;
	public MediaAlbumSongsAdaptor(Context context, ContentResolver cr, long albumid) {
		super(context, cr);
		mAlbumid = albumid;
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
		int duration = cursor.getColumnIndex(Media.DURATION);

		// add each song to mAlbumList
		do {
			MediaItem item = new MediaItem();
			item.setSongId(cursor.getLong(idcolumn));
			item.setTitle(cursor.getString(titleColumn));
			item.setAlbum( cursor.getString(albumcolum));
			item.setArtist(cursor.getString(artistColumn));
			item.setAlbumid(mAlbumid);
			item.setDuration(cursor.getLong(duration));

			mList.add(item);
			//To display the album art for the first item and then display it again in the list.
			if(mList.size() == 1) {
				mList.add(item);
			}
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
				MediaStore.Audio.Media.DURATION,
		};


		String[] selectionArgs =  {String.valueOf(mAlbumid)};
		String sortOrder = Media.TITLE + " ASC";
		String selection = android.provider.MediaStore.Audio.Media.ALBUM_ID
				+ "=?";
		new QueryHandler(cr).startQuery(token, this,  MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection, selectionArgs, sortOrder);

	}

	@Override
	public int getViewTypeCount() {
		return 2;
	}
	@Override
	public int getItemViewType(int index) {

		if(index == 0)
			return 0;
		else 
			return 1;
	}

	@Override
	public View getItemView(int index, View convertView, ViewGroup arg2) {
		MediaItem item =  mList.get(index);

		ViewHolder holder = null;
		if(convertView == null) {
			holder = new ViewHolder();

			if(getItemViewType(index) == 0) {
				ViewGroup layout = (ViewGroup) LayoutInflater.from(mContext).inflate(R.layout.album_list_item, null);
				holder.textView =  (TextView) layout.findViewById(R.id.list_item_text);
				holder.imageView=  (ImageViewAsync) layout.findViewById(R.id.list_item_icon);
				layout.findViewById(R.id.close_button).setVisibility(View.VISIBLE);
				convertView = layout;
				convertView.setTag(holder);
			}else {
				ViewGroup layout = (ViewGroup) LayoutInflater.from(mContext).inflate(R.layout.album_list_item_small, null);
				holder.textView =  (TextView) layout.findViewById(R.id.list_item_title);
				holder.textNumber =  (TextView) layout.findViewById(R.id.list_item_number);
				holder.textViewDuration =  (TextView) layout.findViewById(R.id.list_item_duration);
				convertView = layout;
				convertView.setTag(holder);
			}

		} else {
			holder = (ViewHolder) convertView.getTag();
		}


		if(getItemViewType(index) == 1) {
			holder.textView.setText(item.getTitle());
			
			MediaItem currentItem = MusicRetriever.getInstance().getCurrentSong();
			
			if(currentItem != null && currentItem.songid == item.songid) {
				holder.textNumber.setTextColor(mContext.getResources().getColor(R.color.music_white_color));
			}else {
				holder.textNumber.setTextColor(mContext.getResources().getColor(R.color.music_white_color_60));
			}
			holder.textNumber.setText(Integer.toString(index));
			
			String elapsedtime;
			int elapsedtimeInsec = (int) item.getDuration() / 1000;
			int elapsedminutes = elapsedtimeInsec / 60;
			int elapsedseconds = elapsedtimeInsec % 60;
			elapsedtime = String.format("%d : %02d", elapsedminutes,
					elapsedseconds);
			
			holder.textViewDuration.setText(elapsedtime);
		}else {
			holder.textView.setText(item.getAlbum());
			holder.imageView.setImageAsyncFromAlbumSource(item.getAlbumid(), mDefaultbitmap);
		}

		//Set the song id /album id on the holder load the songs in album or play song
		holder.songid = item.songid;
		holder.albumid = item.albumid;
		
		return convertView;
	}

	public ArrayList<MediaItem> getCurrentSongsList () {
		
		mList.remove(0);
		return mList;
	}

	@Override
	public boolean supportsAlphabetList() {
		return false;
	}
}
