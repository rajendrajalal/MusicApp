package com.iamplus.musicplayer;

import android.content.ContentUris;
import android.net.Uri;

public class MediaItem {

	String title;
	String album;
	String artist;
	String mimeType;
	long songid;
	long albumid;
	long artistid;
	int numberofAlbums;
	int numberoftracks;
	long duration;
	public long getDuration() {
		return duration;
	}
	public void setDuration(long duration) {
		this.duration = duration;
	}
	public long getArtistid() {
		return artistid;
	}
	public void setArtistid(long artistid) {
		this.artistid = artistid;
	}
	public void setSongid(long songid) {
		this.songid = songid;
	}
	public int getNumberOfTracks() {
		return numberoftracks;
	}
	public void setNumberOfTracks(int numberoftracks) {
		this.numberoftracks = numberoftracks;
	}
	public MediaItem ()
	{

	}
	public MediaItem(long id, String title, String album, String artist) {
		super();
		this.songid = id;
		this.title = title;
		this.album = album;
		this.artist = artist;
	}

	public long getAlbumid() {
		return albumid;
	}

	public void setAlbumid(long albumid) {
		this.albumid = albumid;
	}
	public long getSongId() {
		return songid;
	}
	public void setSongId(long id) {
		this.songid = id;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getAlbum() {
		return album;
	}
	public void setAlbum(String album) {
		this.album = album;
	}
	public String getArtist() {
		return artist;
	}
	public void setArtist(String artist) {
		this.artist = artist;
	}
	public String getMimeType() {
		return mimeType;
	}
	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}
	public Uri getURI() {
		return ContentUris.withAppendedId(
				android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songid);
	}

	public int getNumberofAlbums() {
		return numberofAlbums;
	}
	public void setNumberofAlbums(int numberofAlbums) {
		this.numberofAlbums = numberofAlbums;
	}

}
