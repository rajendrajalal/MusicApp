package com.iamplus.musicplayer;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.ImageButton;

public class CircledImageButton extends ImageButton {

	private static final float STROKE_WIDTH = 0.5f;
	private static final float THICK_STROKE_WIDTH = 24f;
	private static final int ANIM_TIME = 60;

	// percentage of circle to be covered in White color, rest will be grey.
	private float mPercentage = 1.0f;
	private int mPrimaryColor = getResources().getColor(R.color.music_app_color);
	private boolean mInDrag;
	private Paint mPaint;
	private ImageButton mButton;
	private CircleAnimation mCircleAnimation;
	private boolean mDrawArc = true;

	public CircledImageButton(Context context) {
		this(context, null);
	}

	public CircledImageButton(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public CircledImageButton(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		mButton = this;
		mPaint = new Paint();
		mPaint.setAntiAlias(true);
		mPaint.setDither(true);
		mPaint.setStrokeCap(Paint.Cap.BUTT);
		mPaint.setStyle(Paint.Style.STROKE);
	}
	
	/**
	 * Sets primary color that covers the percentage(check setPercentage) of the
	 * circle. Default white.
	 * 
	 * @param color
	 *            - Color. Ex 0xffffffff for white.
	 */
	public void setPrimaryColor(int color){
		mPrimaryColor = color;
	}
	

	/**
	 * Sets the percentage of circle that primary color covers, rest is covered
	 * by secondary color. Value range 0f to 1f.
	 * 
	 * @param percentage
	 */
	public void setPercentage(float percentage) {
		if (mPercentage != percentage) {
			if (mCircleAnimation != null && !mCircleAnimation.hasEnded()) {
				mCircleAnimation.cancel();
				mCircleAnimation.reset();
			}
			animateCircle(mPercentage, percentage);
		}
	}

	/**
	 * 
	 * @return Returns the percentage the circle represents, ie the percentage
	 *         of circle covered by primary color.
	 */
	public float getPercentage() {
		return mPercentage;
	}
	
	/**
	 * Sets whether to use the thick stroke.
	 * 
	 * @param thickStroke
	 *            true to use thick stroke or false to use normal stroke
	 */
	public void setUseThickStroke(boolean thickStroke) {
		if(mInDrag != thickStroke){
			mInDrag = thickStroke;
			invalidate();
		}
	}

	public void setEnabledDrawArcs(boolean draw){
		mDrawArc = draw;
		invalidate();
	}
	@Override
	protected void onDraw(Canvas canvas) {
		if(mDrawArc) {
			drawArcs(canvas, getWidth(), getHeight());
		}
		super.onDraw(canvas);
	}

	protected float getDecelerateValue(float p) {
		return p * p * p * p * p * p * p * p * p * p * p;
	}
	
	/**
	 * Sets the size of Circle using decelerated percentage.
	 * 
	 * @param percentage
	 *            relative percent with respect to initial size of circle.
	 */
	public void setCircleSizeDeccelerated(float percentage) {
		invalidate();
	}
	
	/**
	 * Change size of Circle
	 * 
	 * @param diameter
	 *            diameter of the circle
	 */
	public void setCircleSize(int diameter){
		invalidate();
	}

	private void drawArcs(Canvas c, int width, int height) {
		mPaint.setStrokeWidth(STROKE_WIDTH);
		// firstCircleDrawPoint is circle diameter + circle width
		float firstCircleDrawPoint = THICK_STROKE_WIDTH - STROKE_WIDTH / 2;
		RectF rect = new RectF(firstCircleDrawPoint, firstCircleDrawPoint,
				width - firstCircleDrawPoint, height - firstCircleDrawPoint);
		float degree = mPercentage * 360f;
		if (mInDrag) {
			mPaint.setStrokeWidth(THICK_STROKE_WIDTH);
			float sizeChange = -(THICK_STROKE_WIDTH - STROKE_WIDTH) / 2;
			rect.inset(sizeChange, sizeChange);
		}
		mPaint.setStyle(Paint.Style.STROKE);
		mPaint.setColor(mPrimaryColor);
		c.drawArc(rect, -90f, degree, false, mPaint);
	}

	private void animateCircle(float start, float end) {
		if (mCircleAnimation == null) {
			mCircleAnimation = new CircleAnimation();
		}

		mCircleAnimation.setDuration(ANIM_TIME);
		mCircleAnimation.setstartEndValue(start, end);
		mButton.startAnimation(mCircleAnimation);
	}

	private class CircleAnimation extends Animation {
		float start, end, total;

		public void setstartEndValue(float start, float end) {
			this.start = start;
			this.end = end;
			total = end - start;
		}

		@Override
		protected void applyTransformation(float interpolatedTime,
				Transformation t) {
			mPercentage = start + (total * interpolatedTime);
			mButton.invalidate();
		}

		@Override
		public void cancel() {
			super.cancel();
			mPercentage = end;
			mButton.invalidate();
		}
	}
}

