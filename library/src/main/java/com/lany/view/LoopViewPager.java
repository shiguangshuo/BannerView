package com.lany.view;

import android.content.Context;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

public class LoopViewPager extends ViewPager {
    private InnerLoopAdapter mAdapter;
    private List<OnPageChangeListener> mListeners;
    private boolean scrollable = true;

    public LoopViewPager(Context context) {
        super(context);
        super.addOnPageChangeListener(mInnerListener);
    }

    public LoopViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        super.addOnPageChangeListener(mInnerListener);
    }

    @Override
    public void setAdapter(PagerAdapter adapter) {
        mAdapter = adapter == null ? null : new InnerLoopAdapter(adapter);
        super.setAdapter(mAdapter);
        setCurrentItem(0, false);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        return this.scrollable && super.onTouchEvent(ev);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return this.scrollable && super.onInterceptTouchEvent(ev);
    }

    public void setScrollable(boolean scrollable) {
        this.scrollable = scrollable;
    }

    @Override
    public PagerAdapter getAdapter() {
        return mAdapter != null ? mAdapter.pagerAdapter : null;
    }

    @Override
    public int getCurrentItem() {
        return mAdapter != null ? mAdapter.getRealPosition(super.getCurrentItem()) : 0;
    }

    @Override
    public void setCurrentItem(int item, boolean smoothScroll) {
        super.setCurrentItem(toInnerPosition(item), smoothScroll);
    }

    @Override
    public void setCurrentItem(int item) {
        super.setCurrentItem(toInnerPosition(item), true);
    }

    @Override
    public void addOnPageChangeListener(OnPageChangeListener listener) {
        if (mListeners == null) {
            mListeners = new ArrayList<>();
        }
        mListeners.add(listener);
    }

    @Override
    public void removeOnPageChangeListener(OnPageChangeListener listener) {
        if (mListeners != null) {
            mListeners.remove(listener);
        }
    }

    @Override
    public void clearOnPageChangeListeners() {
        if (mListeners != null) {
            mListeners.clear();
        }
    }

    private void dispatchOnPageScrolled(int position, float offset, int offsetPixels) {
        if (mListeners != null) {
            for (int i = 0, z = mListeners.size(); i < z; i++) {
                OnPageChangeListener listener = mListeners.get(i);
                if (listener != null) {
                    listener.onPageScrolled(position, offset, offsetPixels);
                }
            }
        }
    }

    private void dispatchOnPageSelected(int position) {
        if (mListeners != null) {
            for (int i = 0, z = mListeners.size(); i < z; i++) {
                OnPageChangeListener listener = mListeners.get(i);
                if (listener != null) {
                    listener.onPageSelected(position);
                }
            }
        }
    }

    private void dispatchOnScrollStateChanged(int state) {
        if (mListeners != null) {
            for (int i = 0, z = mListeners.size(); i < z; i++) {
                OnPageChangeListener listener = mListeners.get(i);
                if (listener != null) {
                    listener.onPageScrollStateChanged(state);
                }
            }
        }
    }

    static int toRealPosition(int position, int count) {
        if (count <= 1) {
            return 0;
        }
        return (position - 1 + count) % count;
    }

    static int toInnerPosition(int real) {
        return real + 1;
    }

    private OnPageChangeListener mInnerListener = new OnPageChangeListener() {
        private float mPreviousOffset = -1;
        private float mPreviousPosition = -1;

        @Override
        public void onPageSelected(int position) {
            int real = mAdapter.getRealPosition(position);
            if (mPreviousPosition == real) {
                return;
            }
            mPreviousPosition = real;
            dispatchOnPageSelected(real);
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            int realPosition = position;
            if (mAdapter != null) {
                realPosition = mAdapter.getRealPosition(position);
                if (positionOffset == 0 && mPreviousOffset == 0 && (position == 0 || position == mAdapter.getCount() - 1)) {
                    setCurrentItem(realPosition, false);
                }
            }
            mPreviousOffset = positionOffset;
            if (realPosition != mAdapter.getRealCount() - 1) {
                dispatchOnPageScrolled(realPosition, positionOffset, positionOffsetPixels);
            } else if (positionOffset > .5) {
                dispatchOnPageScrolled(0, 0, 0);
            } else {
                dispatchOnPageScrolled(realPosition, 0, 0);
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            if (mAdapter != null) {
                int position = LoopViewPager.super.getCurrentItem();
                if (state == ViewPager.SCROLL_STATE_IDLE && (position == 0 || position == mAdapter.getCount() - 1)) {
                    setCurrentItem(mAdapter.getRealPosition(position), false);
                }
            }
            dispatchOnScrollStateChanged(state);
        }
    };

    private static class InnerLoopAdapter extends PagerAdapter {
        final PagerAdapter pagerAdapter;
        private SparseArray<Object> recycler = new SparseArray<>();

        InnerLoopAdapter(PagerAdapter adapter) {
            this.pagerAdapter = adapter;
        }

        @Override
        public void notifyDataSetChanged() {
            recycler = new SparseArray<>();
            super.notifyDataSetChanged();
        }

        int getRealPosition(int position) {
            return toRealPosition(position, pagerAdapter.getCount());
        }

        int getRealCount() {
            return pagerAdapter.getCount();
        }

        @Override
        public int getCount() {
            int realCount = getRealCount();
            return realCount > 1 ? (realCount + 2) : realCount;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            int real = getRealPosition(position);
            Object destroy = recycler.get(position);
            if (destroy != null) {
                recycler.remove(position);
                return destroy;
            }
            return pagerAdapter.instantiateItem(container, real);
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            int first = 1, last = getRealCount();
            if (position == first || position == last) {
                recycler.put(position, object);
            } else {
                pagerAdapter.destroyItem(container, getRealPosition(position), object);
            }
        }

        @Override
        public void finishUpdate(ViewGroup container) {
            pagerAdapter.finishUpdate(container);
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return pagerAdapter.isViewFromObject(view, object);
        }

        @Override
        public void restoreState(Parcelable bundle, ClassLoader classLoader) {
            pagerAdapter.restoreState(bundle, classLoader);
        }

        @Override
        public Parcelable saveState() {
            return pagerAdapter.saveState();
        }

        @Override
        public void startUpdate(ViewGroup container) {
            pagerAdapter.startUpdate(container);
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            pagerAdapter.setPrimaryItem(container, position, object);
        }
    }
}