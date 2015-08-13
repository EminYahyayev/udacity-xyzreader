/*
 * Copyright 2015.  Emin Yahyayev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.xyzreader.ui;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.ui.widget.AspectLockedImageView;
import com.squareup.picasso.Picasso;

import butterknife.Bind;
import butterknife.ButterKnife;

final class ArticlesAdapter extends RecyclerView.Adapter<ArticlesAdapter.ArticleHolder> {

    public interface OnArticleClickListener {
        void onArticleSelected(long articleId);

        OnArticleClickListener DUMMY = new OnArticleClickListener() {
            @Override public void onArticleSelected(long articleId) {}
        };
    }

    private final Context mContext;
    private final LayoutInflater mInflater;

    private OnArticleClickListener mListener = OnArticleClickListener.DUMMY;
    private Cursor mCursor;

    public ArticlesAdapter(Context context, Cursor cursor) {
        mInflater = LayoutInflater.from(context);
        mContext = context;
        mCursor = cursor;
        setHasStableIds(true);
    }

    public ArticlesAdapter setListener(@NonNull OnArticleClickListener listener) {
        mListener = listener;
        return this;
    }

    @Override
    public long getItemId(int position) {
        mCursor.moveToPosition(position);
        return mCursor.getLong(ArticleLoader.Query._ID);
    }

    @Override
    public ArticleHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ArticleHolder(mInflater.inflate(R.layout.item_article, parent, false));
    }

    @Override
    public void onBindViewHolder(ArticleHolder holder, int position) {
        holder.bind(position);
    }

    @Override
    public int getItemCount() {
        return mCursor.getCount();
    }

    final class ArticleHolder extends RecyclerView.ViewHolder {
        @Bind(R.id.article_item_photo) AspectLockedImageView thumbnailView;
        @Bind(R.id.article_item_title) TextView titleView;
        @Bind(R.id.article_item_subtitle) TextView subtitleView;

        public ArticleHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);

            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mListener.onArticleSelected(getItemId());
                }
            });
        }

        public void bind(int position) {
            mCursor.moveToPosition(position);

            titleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));
            subtitleView.setText(
                    DateUtils.getRelativeTimeSpanString(
                            mCursor.getLong(ArticleLoader.Query.PUBLISHED_DATE),
                            System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_ALL).toString()
                            + " by "
                            + mCursor.getString(ArticleLoader.Query.AUTHOR));

            thumbnailView.setAspectRatio(mCursor.getFloat(ArticleLoader.Query.ASPECT_RATIO));
            Picasso.with(mContext)
                    .load(mCursor.getString(ArticleLoader.Query.THUMB_URL))
                    .into(thumbnailView);
        }
    }
}
