package com.example.xyzreader.ui.fragment;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewCompat;
import android.support.v7.graphics.Palette;
import android.text.Html;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.ui.activity.ArticleDetailActivity;
import com.example.xyzreader.ui.activity.ArticleListActivity;
import com.example.xyzreader.ui.widget.AspectLockedImageView;
import com.example.xyzreader.ui.widget.DrawInsetsFrameLayout;
import com.example.xyzreader.ui.widget.ObservableScrollView;
import com.github.florent37.glidepalette.GlidePalette;

import butterknife.Bind;
import butterknife.BindBool;
import butterknife.BindColor;
import butterknife.BindDimen;
import butterknife.OnClick;

import static com.github.florent37.glidepalette.BitmapPalette.CallBack;

/**
 * A fragment representing a single Article detail screen. This fragment is
 * either contained in a {@link ArticleListActivity} in two-pane mode (on
 * tablets) or a {@link ArticleDetailActivity} on handsets.
 */
public class ArticleFragment extends BaseFragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = ArticleFragment.class.getSimpleName();

    public static final String ARG_ITEM_ID = "item_id";

    private static final String STATE_SCROLL_VIEW = "state_scroll";
    private static final float PARALLAX_FACTOR = 1.25f;

    private View mRootView;

    @Bind(R.id.article_detail_photo) AspectLockedImageView mPhotoView;
    @Bind(R.id.article_detail_photo_container) View mPhotoContainerView;
    @Bind(R.id.article_detail_title) TextView mTitleTextView;
    @Bind(R.id.article_detail_byline) TextView mBylineTextView;
    @Bind(R.id.article_detail_body) TextView mBodyTextView;
    @Bind(R.id.article_detail_content_container) View mContentContainerView;
    @Bind(R.id.article_detail_meta_bar) View mMetaBarView;
    @Bind(R.id.draw_insets_frame_layout) DrawInsetsFrameLayout mDrawInsetsFrameLayout;
    @Bind(R.id.scrollview) ObservableScrollView mScrollView;

    @BindBool(R.bool.article_detail_is_card) boolean mIsCard;
    @BindDimen(R.dimen.article_detail_card_top_margin) int mStatusBarFullOpacityBottom;

    @BindColor(R.color.theme_primary) int mColorBackground;
    @BindColor(R.color.body_text_white) int mColorTextTitle;
    @BindColor(R.color.body_text_1_inverse) int mColorTextSubtitle;

    private Cursor mCursor;
    private long mItemId;
    private ColorDrawable mStatusBarColorDrawable;

    private int mTopInset;
    private int mScrollY;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ArticleFragment() { }

    public static ArticleFragment newInstance(long itemId) {
        Bundle arguments = new Bundle();
        arguments.putLong(ARG_ITEM_ID, itemId);
        ArticleFragment fragment = new ArticleFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            mItemId = getArguments().getLong(ARG_ITEM_ID);
        }
    }

    public ArticleDetailActivity getActivityCast() {
        return (ArticleDetailActivity) getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return mRootView = inflater.inflate(R.layout.fragment_article, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mDrawInsetsFrameLayout.setOnInsetsCallback(new DrawInsetsFrameLayout.OnInsetsCallback() {
            @Override
            public void onInsetsChanged(Rect insets) {
                mTopInset = insets.top;
            }
        });

        mScrollView.setCallbacks(new ObservableScrollView.Callbacks() {
            @Override
            public void onScrollChanged() {
                mScrollY = mScrollView.getScrollY();
                getActivityCast().onUpButtonFloorChanged(mItemId, ArticleFragment.this);
                mPhotoContainerView.setTranslationY((int) (mScrollY - mScrollY / PARALLAX_FACTOR));
                updateStatusBar();
            }
        });

        ViewCompat.setElevation(mContentContainerView, getResources().getDimension(R.dimen.cardview_default_elevation));
        mBylineTextView.setMovementMethod(new LinkMovementMethod());

        mStatusBarColorDrawable = new ColorDrawable(0);
        updateStatusBar();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // In support library r8, calling initLoader for a fragment in a FragmentPagerAdapter in
        // the fragment's onCreate may cause the same LoaderManager to be dealt to multiple
        // fragments because their mIndex is -1 (haven't been added to the activity yet). Thus,
        // we do this in onActivityCreated.
        getLoaderManager().initLoader(0, null, this);
    }

    @OnClick(R.id.article_detail_share_fab)
    public void onShareFab() {
        startActivity(Intent.createChooser(ShareCompat.IntentBuilder.from(getActivity())
                .setType("text/plain")
                .setText("Some sample text")
                .getIntent(), getString(R.string.action_share)));
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newInstanceForItemId(getActivity(), mItemId);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (!isAdded()) {
            if (cursor != null) {
                cursor.close();
            }
            return;
        }

        mCursor = cursor;
        if (mCursor != null && !mCursor.moveToFirst()) {
            Log.e(TAG, "Error reading item detail cursor");
            mCursor.close();
            mCursor = null;
        }

        bindViews();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCursor = null;
        //bindViews();
    }

    public int getUpButtonFloor() {
        if (mPhotoContainerView == null || mPhotoView.getHeight() == 0) {
            return Integer.MAX_VALUE;
        }

        // account for parallax
        return mIsCard
                ? (int) mPhotoContainerView.getTranslationY() + mPhotoView.getHeight() - mScrollY
                : mPhotoView.getHeight() - mScrollY;
    }

    private void updateStatusBar() {
        int color = 0;
        if (mPhotoView != null && mTopInset != 0 && mScrollY > 0) {
            float f = progress(mScrollY,
                    mStatusBarFullOpacityBottom - mTopInset * 3,
                    mStatusBarFullOpacityBottom - mTopInset);
            color = Color.argb((int) (255 * f),
                    (int) (Color.red(mColorBackground) * 0.9),
                    (int) (Color.green(mColorBackground) * 0.9),
                    (int) (Color.blue(mColorBackground) * 0.9));
        }
        mStatusBarColorDrawable.setColor(color);
        mDrawInsetsFrameLayout.setInsetBackground(mStatusBarColorDrawable);
    }

    private void bindViews() {
        if (mRootView == null) {
            return;
        }

        if (mCursor != null) {
            mRootView.setAlpha(0);
            mRootView.setVisibility(View.VISIBLE);
            mRootView.animate().alpha(1);
            mTitleTextView.setText(mCursor.getString(ArticleLoader.Query.TITLE));
            mBylineTextView.setText(Html.fromHtml(
                    getString(R.string.article_detail_byline,
                            DateUtils.getRelativeTimeSpanString(
                                    mCursor.getLong(ArticleLoader.Query.PUBLISHED_DATE),
                                    System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                                    DateUtils.FORMAT_ABBREV_ALL).toString(),
                            mCursor.getString(ArticleLoader.Query.AUTHOR))));
            mBodyTextView.setText(Html.fromHtml(mCursor.getString(ArticleLoader.Query.BODY)));

            mPhotoView.setAspectRatio(mCursor.getFloat(ArticleLoader.Query.ASPECT_RATIO));
            String photoUrl = mCursor.getString(ArticleLoader.Query.PHOTO_URL);
            Glide.with(this)
                    .load(photoUrl)
                    .placeholder(R.color.photo_placeholder)
                    .listener(GlidePalette.with(photoUrl).intoCallBack(new CallBack() {
                        @Override public void onPaletteLoaded(Palette palette) {
                            Palette.Swatch swatch = palette.getVibrantSwatch();
                            if (swatch != null) {
                                mColorBackground = swatch.getRgb();
                                mColorTextTitle = swatch.getBodyTextColor();
                                mColorTextSubtitle = swatch.getTitleTextColor();

                                mMetaBarView.setBackgroundColor(mColorBackground);
                                mTitleTextView.setTextColor(mColorTextTitle);
                                mBylineTextView.setTextColor(mColorTextSubtitle);
                            }
                            updateStatusBar();
                        }
                    }))
                    .into(mPhotoView);
        } else {
            mRootView.setVisibility(View.GONE);
            mTitleTextView.setText("N/A");
            mBylineTextView.setText("N/A");
            mBodyTextView.setText("N/A");
        }
    }

    static float progress(float v, float min, float max) {
        return constrain((v - min) / (max - min), 0, 1);
    }

    static float constrain(float val, float min, float max) {
        if (val < min) {
            return min;
        } else if (val > max) {
            return max;
        } else {
            return val;
        }
    }
}
