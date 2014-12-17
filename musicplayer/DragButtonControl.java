package com.iamplus.musicplayer;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.RelativeLayout;
import android.view.View.OnTouchListener;

public class DragButtonControl extends RelativeLayout implements OnTouchListener {

	public interface DragButtonControlActionListener {

		public void onDragLeft(int value);
		public void onDragRight(int value);
		public void onDragUp(int value);
		public void onDragDown(int value);
		public void onClicked();
		public void onDragStarted(boolean xAxis);
		public void onDragEnded(boolean xAxis);
	}

	private static final float MINIMUM_VALID_DRAG_DIST = 46;
	private static final int MIN_DRAG_DISTANCE = 15;
	private Context mContext;
	private static float initX;
	private static float initY;
	private boolean xAxisLocked = false;
	private boolean yAxisLocked = false;
	private CircledImageButton button;
	private ViewGroup root;
	private int startX = 0;
	private int startY = 0;
	private int parentHeight = 0;
	private int parentWidth = 0;
	private EdragAxis mDragAxis;
	private DragButtonControlActionListener mListener;
	private boolean mbEnabled = true;
	private int mSlop;
	private boolean mDragActive = false;
	private int mDragValue = 0;
	private int mMaxXVal = 0;
	private int mMaxYVal = 0;

	enum EdragAxis{
		EdragAxis_X,
		EdragAxis_Y,
		EdragAxis_XY
	}
	public DragButtonControl(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
		initControls();
	}

	public void enableControls(boolean enable) {
		mbEnabled = enable;
	}

	public void setEnableDrawArcs(boolean enable){
		if(button != null) {
			button.setEnabledDrawArcs(enable);
		}
	}
	private void initControls() {
		LayoutInflater inflater = (LayoutInflater) mContext
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		root = (ViewGroup) inflater.inflate(
				R.layout.drag_button_layout, this, true);
		button = (CircledImageButton) root.findViewById(R.id.image_button);
		button.setOnTouchListener(this);
		button.setUseThickStroke(true);
		ViewTreeObserver vto = root.getViewTreeObserver(); 
		vto.addOnGlobalLayoutListener(new OnGlobalLayoutListener() { 
			@Override 
			public void onGlobalLayout() { 
				root.getViewTreeObserver().removeOnGlobalLayoutListener(this); 

				parentWidth = root.getMeasuredWidth();
				parentHeight = root.getMeasuredHeight();
				startX = (parentWidth - button.getWidth() )/2;
				startY = (parentHeight  - button.getHeight()) /2;

				//Center the button
				RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
						button.getLayoutParams());
				layoutParams.leftMargin = startX;
				layoutParams.topMargin = startY;
				button.setLayoutParams(layoutParams);
				mMaxXVal = (int) (parentWidth / MINIMUM_VALID_DRAG_DIST);
				mMaxYVal = (int) (parentHeight / MINIMUM_VALID_DRAG_DIST);
			} 
		});
		mSlop = MIN_DRAG_DISTANCE;
		mDragAxis = EdragAxis.EdragAxis_XY;
	}

	public void setOnDragButtonControlActionListener(DragButtonControlActionListener list) {
		mListener = list;
	}


	public void setDragAxis(EdragAxis axis) {
		mDragAxis = axis;
		if(mDragAxis == EdragAxis.EdragAxis_X) {
			xAxisLocked = false;
		}else if(mDragAxis == EdragAxis.EdragAxis_Y) {
			yAxisLocked = false;
		}else if(mDragAxis == EdragAxis.EdragAxis_XY) {
			xAxisLocked = false;
			yAxisLocked = false;
		}
	}

	private AnimatorListener mAnimationlistener = new AnimatorListener(){

		@Override
		public void onAnimationCancel(Animator animation) {
		}

		@Override
		public void onAnimationEnd(Animator animation) {
			if(mListener != null) {
				if(xAxisLocked)
					mListener.onDragEnded(true);
				else
					mListener.onDragEnded(false);
			}
			mbEnabled = true;
			setDragAxis(mDragAxis);
		}

		@Override
		public void onAnimationRepeat(Animator animation) {

		}

		@Override
		public void onAnimationStart(Animator animation) {

		}

	};

	public void setDragButtonImage(int resID) {
		button.setImageResource(resID);
	}

	@Override
	public boolean onTouch(View view, MotionEvent event) {

		if(!mbEnabled) {
			return true;
		}

		switch (event.getActionMasked()) {
		case MotionEvent.ACTION_DOWN: {

			initX = event.getRawX();
			initY = event.getRawY();
			mDragActive = true;
			mDragValue = 0;
			break;
		}
		case MotionEvent.ACTION_UP:{
			if(mbEnabled) {
				if((xAxisLocked || yAxisLocked)) {
					mbEnabled = false;
					button.animate().setListener(mAnimationlistener).translationX(0).translationY(0);
				}else{
					if(mListener != null){
						mListener.onClicked();
					}
				}
			}
			break;
		}
		case MotionEvent.ACTION_MOVE: {
			if(!mDragActive)
				break;
			float deltaX = event.getRawX() - initX;
			float deltaY = event.getRawY() - initY;

			if (Math.abs(deltaX) > mSlop && !yAxisLocked){
				if(mListener != null && !xAxisLocked) {
					mListener.onDragStarted(true);
				}
				xAxisLocked = true;
			}

			if (Math.abs(deltaY) > mSlop && !xAxisLocked) {
				if(mListener != null && !yAxisLocked) {
					mListener.onDragStarted(false);
				}
				yAxisLocked = true;
			}

			if (xAxisLocked) {
				float delta = Math.abs(deltaX);
				if( delta <= MINIMUM_VALID_DRAG_DIST) {
					view.setTranslationX(deltaX);

					int val = (int) (delta / (MINIMUM_VALID_DRAG_DIST)) ;
					if(val != mDragValue) {
						mDragValue = val;
						if(deltaX < 0) {
							if(mListener != null) {
								mListener.onDragLeft(val);
							}
						}else {
							if(mListener != null) {
								mListener.onDragRight(val);
							}
						}
					}

				}else{
					/**
					 * Finger crossed the maximum swipe distance.
					 * Reset to Max allowed distance
					 */
					if( delta > MINIMUM_VALID_DRAG_DIST / 2.0){
						int val = mMaxXVal;
						if(val != mDragValue) {
							mDragValue = val;
							if(deltaX < 0) {
								view.setTranslationX(-MINIMUM_VALID_DRAG_DIST);
								if(mListener != null) {
									mListener.onDragLeft(mMaxXVal);
								}
							}else {
								view.setTranslationX(MINIMUM_VALID_DRAG_DIST);
								if(mListener != null) {
									mListener.onDragRight(mMaxXVal);
								}
							}
						}
					}
				}
			}
			else
				if(yAxisLocked) {
					float delta = Math.abs(deltaY);
					if(Math.abs(deltaY) < MINIMUM_VALID_DRAG_DIST * 1.8 ) {
						view.setTranslationY(deltaY);

						int val = (int) (delta / (MINIMUM_VALID_DRAG_DIST )) ;
						if(val != mDragValue) {
							mDragValue = val;

							if(deltaY < 0) {
								if(mListener != null) {
									mListener.onDragUp(val);
								}
							}else {
								if(mListener != null) {
									mListener.onDragDown(val);
								}
							}
						}
					}else{

						/**
						 * Finger crossed the maximum swipe distance.
						 * Reset to Max allowed distance
						 */
						int val = mMaxYVal;
						if(val != mDragValue) {
							mDragValue = val;

							if(deltaY < 0) {
								view.setTranslationY(-MINIMUM_VALID_DRAG_DIST * 1.8f);
								if(mListener != null) {
									mListener.onDragUp(mMaxYVal);
								}
							}else {
								view.setTranslationY(MINIMUM_VALID_DRAG_DIST * 1.8f);
								if(mListener != null) {
									mListener.onDragDown(mMaxYVal);
								}
							}
						}
					}
				}
			break;
		}
		default:{
			/**
			 * Touch outside and cancel cases are handled here.
			 */
			if(mbEnabled) {
				if((xAxisLocked || yAxisLocked)) {
					mbEnabled = false;
					button.animate().setListener(mAnimationlistener).translationX(0).translationY(0);
				}
			}
			break;
		}
		}
		return true;
	}


	public void setPercentage(float value){
		if(button != null){
			button.setPercentage(value);
		}
	}

	public int getMaxXValue() {

		return mMaxXVal;
	}

	public int getMaxYValue() {

		return mMaxYVal;
	}
}
