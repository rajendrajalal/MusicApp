package com.iamplus.musicplayer;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

public class ImageViewAsync extends ImageView {

	public static final long LIST_ANIMATION_DURATION = 350;
	private Context mContext;
	private Bitmap mDefaultbitmap;
	private LoadBitmap mBitmapLoader;
	private boolean loadFromArtist;
	class LoadBitmap extends AsyncTask<Long, Integer, Bitmap>{

		View mParent;
		public LoadBitmap(View parent,boolean fromArtist){
			mParent = parent;
			loadFromArtist = fromArtist;
		}
		@Override
		protected Bitmap doInBackground(Long... params) {
			Long id = params[0];
			Bitmap bitmap = null;
			if(!loadFromArtist) {
				bitmap = MediaAlbumsAdaptor.getScaledAlbumArtwork(mContext, id, false);
			}else {
				bitmap = MediaAlbumsAdaptor.getFirstAlbumArtForArtist(mContext, id);
			}
			if(bitmap == null) {
				bitmap = mDefaultbitmap;
			}
			return bitmap;
		}
		@Override
		protected void onPostExecute(Bitmap result) {
			super.onPostExecute(result);
			setVisibility(View.VISIBLE);
			ObjectAnimator listitemAnimator  = ObjectAnimator.ofFloat(mParent, "alpha", 0.0f, 1.0f);
			listitemAnimator.setDuration(LIST_ANIMATION_DURATION);
			listitemAnimator.start();

			if(result == mDefaultbitmap) {
				setScaleType(ScaleType.CENTER);
			}else {
				setScaleType(ScaleType.CENTER_CROP);
			}
			setImageBitmap(result);
		}
		@Override
		protected void onCancelled() {
			super.onCancelled();
		}
	}

	public ImageViewAsync(Context context) {
		super(context);
		mContext = context;
	}
	public ImageViewAsync(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
	}
	public void setImageAsyncFromAlbumSource (long albumId, Bitmap defaultBitmap) {

		mDefaultbitmap = defaultBitmap;
		if(mBitmapLoader != null && !mBitmapLoader.isCancelled()) {
			mBitmapLoader.cancel(true);
		}
		setVisibility(INVISIBLE);
		mBitmapLoader = (LoadBitmap) new LoadBitmap(this, false).executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, albumId);
	}

	public void setImageAsyncFromArtistSource (long artistId, Bitmap defaultBitmap) {

		mDefaultbitmap = defaultBitmap;
		if(mBitmapLoader != null && !mBitmapLoader.isCancelled()) {
			mBitmapLoader.cancel(true);
		}
		setVisibility(INVISIBLE);
		mBitmapLoader = (LoadBitmap) new LoadBitmap(this, true).executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, artistId);
	}
}
