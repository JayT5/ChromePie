/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jt5.xposed.chromepie.view;

import java.util.ArrayList;
import java.util.List;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.content.res.XModuleResources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.jt5.xposed.chromepie.Controller;
import com.jt5.xposed.chromepie.PieControl;
import com.jt5.xposed.chromepie.R;

import de.robv.android.xposed.XSharedPreferences;

public class PieMenu extends FrameLayout {

    private static final int MAX_LEVELS = 6;
    private static final long ANIMATION = 80;

    public interface PieController {
        /**
         * called before menu opens to customize menu
         * returns if pie state has been changed
         */
        public boolean onOpen();

    }

    /**
     * A view like object that lives off of the pie menu
     */
    public interface PieView {

        interface OnLayoutListener {
            public void onPieLayout(int ax, int ay, boolean left);
        }

        public void setLayoutListener(OnLayoutListener l);

        public void layout(int anchorX, int anchorY, boolean onleft, float angle,
                int parentHeight);

        public void draw(Canvas c);

        public boolean onTouchEvent(MotionEvent evt);

    }

    private Point mCenter;
    private int mRadius;
    private int mRadiusInc;
    private int mSlop;
    private int mTouchOffset;
    private Path mPath;

    private boolean mOpen;
    private PieController mController;
    private final Controller mControl;
    private ValueAnimator mAnimator;

    private List<PieItem> mItems;
    private int mLevels;
    private int[] mCounts;
    private PieView mPieView = null;

    // sub menus
    private List<PieItem> mCurrentItems;
    private PieItem mOpenItem;

    private Drawable mBackground;
    private Paint mNormalPaint;
    private Paint mSelectedPaint;
    private Paint mSubPaint;

    // touch handling
    private PieItem mCurrentItem;

    private boolean mUseBackground = false;
    private boolean mAnimating;
    private boolean mRepositionMenu;

    private int mTriggerPosition;
    private static final int TRIGGER_LEFT = 0;
    private static final int TRIGGER_RIGHT = 1;
    private static final int TRIGGER_BOTTOM = 2;

    public PieMenu(Context context, Controller control, XModuleResources res, XSharedPreferences prefs) {
        super(context);
        mControl = control;
        init(res, prefs);
    }

    private void init(XModuleResources res, XSharedPreferences prefs) {
        mItems = new ArrayList<PieItem>();
        mLevels = 0;
        mCounts = new int[MAX_LEVELS];
        int defRadiusInc = res.getInteger(R.integer.qc_radius_increment);
        int tmpRadiusInc = prefs.getInt("pie_radius_inc", defRadiusInc);
        mRadiusInc = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, tmpRadiusInc, res.getDisplayMetrics());
        // Set start radius to 80% of radius increment
        mRadius = (int) Math.round(0.8 * mRadiusInc);
        int defSlop = res.getInteger(R.integer.qc_slop);
        int tmpSlop = prefs.getInt("pie_slop", defSlop);
        mSlop = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, tmpSlop, res.getDisplayMetrics());
        mTouchOffset = (int) res.getDimension(R.dimen.qc_touch_offset);
        mOpen = false;
        setWillNotDraw(false);
        setDrawingCacheEnabled(false);
        mCenter = new Point(0,0);
        mNormalPaint = new Paint();
        mNormalPaint.setColor(res.getColor(R.color.qc_normal));
        mNormalPaint.setAntiAlias(true);
        mSelectedPaint = new Paint();
        mSelectedPaint.setColor(res.getColor(R.color.qc_selected));
        mSelectedPaint.setAntiAlias(true);
        mSubPaint = new Paint();
        mSubPaint.setColor(res.getColor(R.color.qc_sub));
        mSubPaint.setAntiAlias(true);
        mRepositionMenu = prefs.getBoolean("reposition_menu", false);
    }

    public void setThemeColors(int themeColor) {
        int statusColor = mControl.getStatusBarColor(themeColor);
        int tintedColor = applyColorTint(themeColor, 0.2f);
        mNormalPaint.setColor(applyColorAlpha(themeColor, 224));
        mSelectedPaint.setColor(applyColorAlpha(statusColor, 224));
        mSubPaint.setColor(applyColorAlpha(tintedColor, 240));
        setTabCountBackgroundColor(applyColorTint(themeColor, 0.35f));
    }

    private void setTabCountBackgroundColor(int color) {
        List<PieItem> items = findItemsById("show_tabs");
        if (!items.isEmpty()) {
            for (PieItem item : items) {
                ((GradientDrawable) ((ViewGroup) item.getView()).getChildAt(1)
                        .getBackground()).setColor(color);
            }
        }
    }

    public void setDefaultColors(XModuleResources res) {
        mNormalPaint.setColor(res.getColor(R.color.qc_normal));
        mSelectedPaint.setColor(res.getColor(R.color.qc_selected));
        mSubPaint.setColor(res.getColor(R.color.qc_sub));
        setTabCountBackgroundColor(res.getColor(R.color.qc_tab_nr));
    }

    private int applyColorTint(int color, float factor) {
        int r1 = Color.red(color);
        int g1 = Color.green(color);
        int b1 = Color.blue(color);
        int r2 = (int) (r1 + (factor * (255 - r1)));
        int g2 = (int) (g1 + (factor * (255 - g1)));
        int b2 = (int) (b1 + (factor * (255 - b1)));
        return Color.rgb(r2, g2, b2);
    }

    private int applyColorAlpha(int color, int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    public void setController(PieController ctl) {
        mController = ctl;
    }

    public void setUseBackground(boolean useBackground) {
        mUseBackground = useBackground;
    }

    public void addItem(PieItem item) {
        // add the item to the pie itself
        mItems.add(item);
        int l = item.getLevel();
        mLevels = Math.max(mLevels, l);
        mCounts[l]++;
    }

    public void addItem(int pos, PieItem item) {
        // add the item to the pie itself
        mItems.add(pos, item);
        int l = item.getLevel();
        mLevels = Math.max(mLevels, l);
        mCounts[l]++;
    }

    public List<PieItem> getItems() {
        List<PieItem> subItems = new ArrayList<PieItem>();
        for (PieItem item : mItems) {
            subItems.add(item);
            if (item.hasItems()) {
                subItems.addAll(item.getItems());
            }
        }
        return subItems;
    }

    public PieItem getItem(int pos) {
        return mItems.get(pos);
    }

    public List<PieItem> findItemsById(String id) {
        List<PieItem> items = new ArrayList<PieItem>();
        for (PieItem item : getItems()) {
            if (item.getId() != null && item.getId().equals(id)) {
                items.add(item);
            }
        }
        return items;
    }

    public void removeItem(PieItem item) {
        mItems.remove(item);
    }

    public void clearItems() {
        mItems.clear();
    }

    private boolean onTheLeft() {
        return mTriggerPosition == TRIGGER_LEFT;
    }

    private int getTriggerPosition(float x, float y) {
        if ((x > mSlop) && (x < getWidth() - mSlop) && (y > getHeight() - mSlop)) {
            return TRIGGER_BOTTOM;
        } else if (x > getWidth() - mSlop) {
            return TRIGGER_RIGHT;
        } else if (x < mSlop) {
            return TRIGGER_LEFT;
        }
        return -1;
    }

    /**
     * guaranteed has center set
     * @param show
     */
    private void show(boolean show) {
        mOpen = show;
        if (mOpen) {
            // ensure clean state
            if (mAnimator != null) {
                mAnimator.cancel();
            }
            mAnimating = false;
            mCurrentItem = null;
            mOpenItem = null;
            mPieView = null;
            mControl.requestTabFocus();
            mCurrentItems = mItems;
            for (PieItem item : mCurrentItems) {
                item.setSelected(false);
                item.setAlpha(0f);
            }
            if (mController != null) {
                boolean changed = mController.onOpen();
            }
            layoutPie();
            animateOpen();
        }
        invalidate();
    }

    private void animateOpen() {
        mAnimator = ValueAnimator.ofFloat(0, 1);
        mAnimator.addUpdateListener(new AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                final float animFraction = animation.getAnimatedFraction();
                final float alpha = (animFraction * 2) - 1;
                for (PieItem item : mCurrentItems) {
                    item.setAnimationAngle((1 - animFraction) * (- item.getStart()));
                    if (animFraction > 0.5) item.setAlpha(alpha);
                }
                invalidate();
            }

        });
        mAnimator.setDuration(2*ANIMATION);
        mAnimator.start();
    }

    private void setCenter(int x, int y) {
        int radius = mRadiusInc + mRadius;
        if (mTriggerPosition == TRIGGER_BOTTOM) {
            if (mRepositionMenu && getWidth() > 2 * radius) {
                mCenter.x = Math.min(Math.max(x, radius), getWidth() - radius);
            } else {
                mCenter.x = x;
            }
            mCenter.y = getHeight();
        } else {
            if (onTheLeft()) {
                mCenter.x = 0;
            } else {
                mCenter.x = getWidth();
            }
            int topControlsHeight = mControl.getTopControlsHeight();
            if (mRepositionMenu && getHeight() - topControlsHeight > 2 * radius) {
                mCenter.y = Math.min(Math.max(y, radius + topControlsHeight), getHeight() - radius);
            } else {
                mCenter.y = y;
            }
        }
    }

    private void layoutPie() {
        float emptyangle = (float) Math.PI / 16;
        int rgap = 2;
        int inner = mRadius + rgap;
        int outer = mRadius + mRadiusInc - rgap;
        int gap = 1;
        for (int i = 0; i < mLevels; i++) {
            int level = i + 1;
            float sweep = (float) (Math.PI - 2 * emptyangle) / mCounts[level];
            float angle = emptyangle + sweep / 2;
            mPath = makeSlice(getDegrees(0) - gap, getDegrees(sweep) + gap, outer, inner, mCenter);
            for (PieItem item : mCurrentItems) {
                if (item.getLevel() == level) {
                    View view = item.getView();
                    if (view != null) {
                        view.measure(view.getLayoutParams().width,
                                view.getLayoutParams().height);
                        int w = view.getMeasuredWidth();
                        int h = view.getMeasuredHeight();
                        int r = inner + (outer - inner) * 2 / 3;
                        int x = (int) (r * Math.sin(angle));
                        int y = mCenter.y - (int) (r * Math.cos(angle)) - h / 2;
                        if (onTheLeft()) {
                            x = mCenter.x + x - w / 2;
                        } else {
                            x = mCenter.x - x - w / 2;
                        }
                        view.layout(x, y, x + w, y + h);
                    }
                    float itemstart = angle - sweep / 2;
                    item.setGeometry(itemstart, sweep, inner, outer);
                    angle += sweep;
                }
            }
            inner += mRadiusInc;
            outer += mRadiusInc;
        }
    }

    /**
     * converts a
     *
     * @param angle from 0..PI to Android degrees (clockwise starting at 3
     *        o'clock)
     * @return skia angle
     */
    private float getDegrees(double angle) {
        return (float) (270 - 180 * angle / Math.PI);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mOpen) {
            int state;
            if (mUseBackground) {
                int w = mBackground.getIntrinsicWidth();
                int h = mBackground.getIntrinsicHeight();
                int left = mCenter.x - w;
                int top = mCenter.y - h / 2;
                mBackground.setBounds(left, top, left + w, top + h);
                state = canvas.save();
                if (onTheLeft()) {
                    canvas.scale(-1, 1);
                } else if (mTriggerPosition == TRIGGER_BOTTOM) {
                    canvas.rotate(90, mCenter.x, mCenter.y);
                }
                mBackground.draw(canvas);
                canvas.restoreToCount(state);
            }
            // draw base menu
            PieItem last = mCurrentItem;
            if (mOpenItem != null) {
                last = mOpenItem;
            }
            for (PieItem item : mCurrentItems) {
                if (item != last) {
                    drawItem(canvas, item);
                }
            }
            if (last != null) {
                drawItem(canvas, last);
            }
            if (mPieView != null) {
                mPieView.draw(canvas);
            }
        }
    }

    private void drawItem(Canvas canvas, PieItem item) {
        if (item.getView() != null) {
            Paint p = item.isSelected() ? mSelectedPaint : mNormalPaint;
            if (!mItems.contains(item)) {
                p = item.isSelected() ? mSelectedPaint : mSubPaint;
            }
            int state = canvas.save();
            if (onTheLeft()) {
                canvas.scale(-1, 1);
            } else if (mTriggerPosition == TRIGGER_BOTTOM) {
                canvas.rotate(90, mCenter.x, mCenter.y);
            }
            float r = getDegrees(item.getStartAngle()) - 270; // degrees(0)
            canvas.rotate(r, mCenter.x, mCenter.y);
            canvas.drawPath(mPath, p);
            canvas.restoreToCount(state);
            // draw the item view
            View view = item.getView();
            state = canvas.save();
            if (mTriggerPosition == TRIGGER_BOTTOM) {
                canvas.rotate(90, mCenter.x, mCenter.y);
                canvas.rotate(-90, view.getX(), view.getY());
                canvas.translate(-view.getWidth(), 0);
            }
            canvas.translate(view.getX(), view.getY());
            view.draw(canvas);
            canvas.restoreToCount(state);
        }
    }

    private Path makeSlice(float start, float end, int outer, int inner, Point center) {
        RectF bb =
                new RectF(center.x - outer, center.y - outer, center.x + outer,
                        center.y + outer);
        RectF bbi =
                new RectF(center.x - inner, center.y - inner, center.x + inner,
                        center.y + inner);
        Path path = new Path();
        path.arcTo(bb, start, end - start, true);
        path.arcTo(bbi, end, start - end);
        path.close();
        return path;
    }

    // touch handling for pie

    @Override
    public boolean onTouchEvent(MotionEvent evt) {
        float x = evt.getX();
        float y = evt.getY();
        int action = evt.getActionMasked();
        if (MotionEvent.ACTION_DOWN == action) {
            mTriggerPosition = getTriggerPosition(x, y);
            boolean show = mTriggerPosition != -1 && PieControl.getTriggerPositions().contains(mTriggerPosition) &&
                    !mControl.isInFullscreenVideo() && (mControl.isInOverview() == (mControl.getTabCount() == 0)) &&
                    y > mControl.getTopControlsHeight();
            if (show) {
                setCenter((int) x, (int) y);
                show(true);
                return true;
            }
        } else if (MotionEvent.ACTION_UP == action) {
            if (mOpen) {
                boolean handled = false;
                if (mPieView != null) {
                    handled = mPieView.onTouchEvent(evt);
                }
                PieItem item = mCurrentItem;
                if (!mAnimating) {
                    deselect();
                }
                show(false);
                if (!handled && (item != null) && (item.getView() != null) && (item.isEnabled())) {
                    if ((item == mOpenItem) || !mAnimating) {
                        item.getView().performClick();
                    }
                }
                return true;
            }
        } else if (MotionEvent.ACTION_CANCEL == action) {
            if (mOpen) {
                show(false);
            }
            if (!mAnimating) {
                deselect();
                invalidate();
            }
            return false;
        } else if (MotionEvent.ACTION_MOVE == action) {
            if (mAnimating) return false;
            boolean handled = false;
            PointF polar = getPolar(x, y);
            int maxr = mRadius + mLevels * mRadiusInc + 50;
            if (mPieView != null) {
                handled = mPieView.onTouchEvent(evt);
            }
            if (handled) {
                invalidate();
                return false;
            }
            if (polar.y < mRadius) {
                if (mOpenItem != null) {
                    closeSub();
                } else if (!mAnimating) {
                    deselect();
                    invalidate();
                }
                return false;
            }
            if (polar.y > maxr) {
                deselect();
                show(false);
                evt.setAction(MotionEvent.ACTION_DOWN);
                if (getParent() != null) {
                    ((ViewGroup) getParent()).dispatchTouchEvent(evt);
                }
                return false;
            }
            PieItem item = findItem(polar);
            if (item == null) {
            } else if (mCurrentItem != item) {
                onEnter(item);
                if ((item != null) && item.isPieView() && (item.getView() != null)) {
                    int cx = item.getView().getLeft() + (onTheLeft()
                            ? item.getView().getWidth() : 0);
                    int cy = item.getView().getTop();
                    mPieView = item.getPieView();
                    layoutPieView(mPieView, cx, cy,
                            (item.getStartAngle() + item.getSweep()) / 2);
                }
                invalidate();
            }
        }
        // always re-dispatch event
        return false;
    }

    private void layoutPieView(PieView pv, int x, int y, float angle) {
        pv.layout(x, y, onTheLeft(), angle, getHeight());
    }

    /**
     * enter a slice for a view
     * updates model only
     * @param item
     */
    private void onEnter(PieItem item) {
        // deselect
        if (mCurrentItem != null) {
            mCurrentItem.setSelected(false);
        }
        if (item != null && item.getView() != null) {
            // clear up stack
            //playSoundEffect(SoundEffectConstants.CLICK);
            item.setSelected(true);
            mPieView = null;
            mCurrentItem = item;
            if ((mCurrentItem != mOpenItem) && mCurrentItem.hasItems()) {
                openSub(mCurrentItem);
                mOpenItem = item;
            }
        } else {
            mCurrentItem = null;
        }

    }

    private void animateOut(final PieItem fixed, AnimatorListener listener) {
        if ((mCurrentItems == null) || (fixed == null)) return;
        final float target = fixed.getStartAngle();
        mAnimator = ValueAnimator.ofFloat(0, 1);
        mAnimator.addUpdateListener(new AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                final float animFraction = animation.getAnimatedFraction();
                final float alpha = (animFraction < 0.5) ?
                        ((1 - animFraction) * 2) - 1 : 0;
                for (PieItem item : mCurrentItems) {
                    if (item != fixed) {
                        item.setAnimationAngle(animFraction
                                * (target - item.getStart()));
                        item.setAlpha(alpha);
                    }
                }
                invalidate();
            }
        });
        mAnimator.setDuration(ANIMATION);
        mAnimator.addListener(listener);
        mAnimator.start();
    }

    private void animateIn(final PieItem fixed, AnimatorListener listener) {
        if ((mCurrentItems == null) || (fixed == null)) return;
        final float target = fixed.getStartAngle();
        mAnimator = ValueAnimator.ofFloat(0, 1);
        mAnimator.addUpdateListener(new AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                final float animFraction = animation.getAnimatedFraction();
                final float alpha = (animFraction > 0.5) ?
                        (animFraction * 2) - 1 : 0;
                for (PieItem item : mCurrentItems) {
                    if (item != fixed) {
                        item.setAnimationAngle((1 - animFraction)
                                * (target - item.getStart()));
                        item.setAlpha(alpha);
                    }
                }
                invalidate();

            }

        });
        mAnimator.setDuration(ANIMATION);
        mAnimator.addListener(listener);
        mAnimator.start();
    }

    private void openSub(final PieItem item) {
        mAnimating = true;
        animateOut(item, new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator a) {
                for (PieItem item : mCurrentItems) {
                    item.setAnimationAngle(0);
                }
                mCurrentItems = new ArrayList<PieItem>(mItems.size());
                int i = 0, j = 0;
                while (i < mItems.size()) {
                    if (mItems.get(i) == item) {
                        mCurrentItems.add(item);
                    } else {
                        mCurrentItems.add(item.getItems().get(j++));
                    }
                    i++;
                }
                layoutPie();
                animateIn(item, new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator a) {
                        for (PieItem item : mCurrentItems) {
                            item.setAnimationAngle(0);
                        }
                        mAnimating = false;
                    }
                });
            }
        });
    }

    private void closeSub() {
        mAnimating = true;
        if (mCurrentItem != null) {
            mCurrentItem.setSelected(false);
        }
        animateOut(mOpenItem, new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator a) {
                for (PieItem item : mCurrentItems) {
                    item.setAnimationAngle(0);
                }
                mCurrentItems = mItems;
                mPieView = null;
                animateIn(mOpenItem, new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator a) {
                        for (PieItem item : mCurrentItems) {
                            item.setAnimationAngle(0);
                        }
                        mAnimating = false;
                        mOpenItem = null;
                        mCurrentItem = null;
                    }
                });
            }
        });
    }

    private void deselect() {
        if (mCurrentItem != null) {
            mCurrentItem.setSelected(false);
        }
        if (mOpenItem != null) {
            mOpenItem = null;
            mCurrentItems = mItems;
        }
        mCurrentItem = null;
        mPieView = null;
    }

    private PointF getPolar(float x, float y) {
        PointF res = new PointF();
        // get angle and radius from x/y
        res.x = (float) Math.PI / 2;
        x = mCenter.x - x;
        if (onTheLeft()) {
            x = -x;
        }
        y = mCenter.y - y;
        res.y = (float) Math.sqrt(x * x + y * y);
        if (mTriggerPosition == TRIGGER_BOTTOM) {
            res.x = (float) (Math.asin(x / res.y) + (Math.PI / 2));
        } else {
            if (y > 0) {
                res.x = (float) Math.asin(x / res.y);
            } else if (y < 0) {
                res.x = (float) (Math.PI - Math.asin(x / res.y ));
            }
        }
        return res;
    }

    /**
     *
     * @param polar x: angle, y: dist
     * @return the item at angle/dist or null
     */
    private PieItem findItem(PointF polar) {
        // find the matching item:
        // experienced rare NullPointerException when swiping away tab after opening overview
        if (mCurrentItems != null) {
            for (PieItem item : mCurrentItems) {
                if (inside(polar, mTouchOffset, item)) {
                    return item;
                }
            }
        }
        return null;
    }

    private boolean inside(PointF polar, float offset, PieItem item) {
        return (item.getInnerRadius() - offset < polar.y)
        && (item.getOuterRadius() - offset > polar.y)
        && (item.getStartAngle() < polar.x)
        && (item.getStartAngle() + item.getSweep() > polar.x);
    }

}
