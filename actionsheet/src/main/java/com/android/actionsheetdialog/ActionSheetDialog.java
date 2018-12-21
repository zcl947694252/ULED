/*
 *
 * MIT License
 *
 * Copyright (c) 2017 JohnyPeng
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.android.actionsheetdialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.ArrayRes;
import android.support.annotation.StringRes;
import android.support.annotation.StyleRes;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by pengjinghui on 2017/4/7.
 */

public class ActionSheetDialog extends AlertDialog {
    private static final String TAG = "ActionSheetDialog";
    protected ActionSheetDialog(Context context) {
        super(context);
    }

    protected ActionSheetDialog(Context context, boolean cancelable, OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
    }

    protected ActionSheetDialog(Context context, @StyleRes int themeResId) {
        super(context, themeResId);

    }

    @Override
    public Button getButton(int whichButton) {
        switch (whichButton) {

        }
        return super.getButton(whichButton);
    }

    public static class ActionSheetBuilder extends AlertDialog.Builder {
        private Context mContext;
        private String mTitle;
        private String mMessage;
        private String mNegativeText;
        private String mPositiveText;
        private boolean mCancelable;
        private List<ActionSheetItem> mActionSheetItems;
        private OnClickListener mNegativeClickListener;
        private OnClickListener mPositiveClickListener;
        private ActionSheetDialog mActionSheetDialog;

        //Attributes
        //Title attrs
        private int mTitleTextColor;
        private int mTitleTextSize;
        private int mTitleHeight;
        private Drawable mTitleDivider;
        private int mTitleDividerInset;
        private int mTitleDividerHeight;
        //Message attrs
        private int mMessageTextColor;
        private int mMessageTextSize;
        private int mMessageHeight;
        private Drawable mMessageDivider;
        private int mMessageDividerInset;
        private int mMessageDividerHeight;
        //Item attrs
        private int mItemTextColor;
        private int mItemTextSize;
        private int mItemHeight;
        private Drawable mItemDivider;
        private int mItemDividerInset;
        private int mItemDividerHeight;
        //Positive button attrs
        private int mPositiveTextColor;
        private int mPositiveTextSize;
        private int mPositiveHeight;
        //Cancel button attrs
        private int mCancelTextColor;
        private int mCancelTextSize;
        private int mCancelHeight;
        private int mCancelTopMargin;
        private Drawable mCancelBackground;
        //Dialog attrs
        private int mLayoutMargins;
        private int mSheetMargins;
        private Drawable mContentBackground;
        private int mWindowAnimationId;
        private static final int DEFAULT_VALUE = -1;



        public ActionSheetBuilder(Context context) {
            super(context);
            mContext = context;
            mActionSheetItems = new ArrayList<>();
            TypedArray defaultTypedArray = context.obtainStyledAttributes(R.style.ActionSheetDialogBase, R.styleable.ActionSheetDialog);
            if (null != defaultTypedArray) {
                initDefaultAttributes(defaultTypedArray);
                defaultTypedArray.recycle();
            }
        }

        public ActionSheetBuilder(Context context, int themeResId) {
            this(context);
            TypedArray typedArray = context.obtainStyledAttributes(themeResId, R.styleable.ActionSheetDialog);
            if (null != typedArray) {
                initAttributes(typedArray);
                typedArray.recycle();
            }

        }

        private void initAttributes(TypedArray typedArray) {
            if (null != typedArray) {
                mTitleTextColor = typedArray.getColor(R.styleable.ActionSheetDialog_titleTextColor, mTitleTextColor);
                mTitleTextSize = typedArray.getDimensionPixelSize(R.styleable.ActionSheetDialog_titleTextSize, mTitleTextSize);
                mTitleHeight = typedArray.getDimensionPixelSize(R.styleable.ActionSheetDialog_titleHeight, mTitleHeight);
                mTitleDivider = typedArray.getDrawable(R.styleable.ActionSheetDialog_titleDivider);
                mTitleDividerInset = typedArray.getDimensionPixelSize(R.styleable.ActionSheetDialog_titleDividerInset, mTitleDividerInset);
                mTitleDividerHeight = typedArray.getDimensionPixelSize(R.styleable.ActionSheetDialog_titleDividerHeight, mTitleDividerHeight);

                mMessageTextColor = typedArray.getColor(R.styleable.ActionSheetDialog_messageTextColor, mMessageTextColor);
                mMessageTextSize = typedArray.getDimensionPixelSize(R.styleable.ActionSheetDialog_messageTextSize, mMessageTextSize);
                mMessageHeight = typedArray.getDimensionPixelSize(R.styleable.ActionSheetDialog_messageHeight, mMessageHeight);
                mMessageDivider = typedArray.getDrawable(R.styleable.ActionSheetDialog_messageDivider);
                mMessageDividerInset = typedArray.getDimensionPixelSize(R.styleable.ActionSheetDialog_messageDividerInset, mMessageDividerInset);
                mMessageDividerHeight = typedArray.getDimensionPixelSize(R.styleable.ActionSheetDialog_messageDividerHeight, mMessageDividerHeight);

                mItemTextColor = typedArray.getColor(R.styleable.ActionSheetDialog_itemTextColor, mItemTextColor);
                mItemTextSize = typedArray.getDimensionPixelSize(R.styleable.ActionSheetDialog_itemTextSize, mItemTextSize);
                mItemHeight = typedArray.getDimensionPixelSize(R.styleable.ActionSheetDialog_itemHeight, mItemHeight);
                mItemDivider = typedArray.getDrawable(R.styleable.ActionSheetDialog_itemDivider);
                mItemDividerInset = typedArray.getDimensionPixelSize(R.styleable.ActionSheetDialog_itemDividerInset, mItemDividerInset);
                mItemDividerHeight = typedArray.getDimensionPixelSize(R.styleable.ActionSheetDialog_itemDividerHeight, mItemDividerHeight);

                mPositiveTextColor = typedArray.getColor(R.styleable.ActionSheetDialog_positiveTextColor, mPositiveTextColor);
                mPositiveTextSize = typedArray.getDimensionPixelSize(R.styleable.ActionSheetDialog_positiveTextSize, mPositiveTextSize);
                mPositiveHeight = typedArray.getDimensionPixelSize(R.styleable.ActionSheetDialog_positiveHeight, mPositiveHeight);

                mCancelTextColor = typedArray.getColor(R.styleable.ActionSheetDialog_cancelTextColor, mCancelTextColor);
                mCancelTextSize = typedArray.getDimensionPixelSize(R.styleable.ActionSheetDialog_cancelTextSize, mCancelTextSize);
                mCancelHeight = typedArray.getDimensionPixelSize(R.styleable.ActionSheetDialog_cancelHeight, mCancelHeight);
                mCancelBackground = typedArray.getDrawable(R.styleable.ActionSheetDialog_cancelBackground);
                mCancelTopMargin = typedArray.getDimensionPixelSize(R.styleable.ActionSheetDialog_cancelTopMargins, mCancelTopMargin);

                mSheetMargins = typedArray.getDimensionPixelSize(R.styleable.ActionSheetDialog_sheetMargins, mSheetMargins);
                mContentBackground = typedArray.getDrawable(R.styleable.ActionSheetDialog_contentBackground);
                mWindowAnimationId = typedArray.getResourceId(R.styleable.ActionSheetDialog_windowAnimations, mWindowAnimationId);
            }
        }

        private void initDefaultAttributes(TypedArray defaultTypedArray) {
            if (null != defaultTypedArray) {
                mTitleTextColor = defaultTypedArray.getColor(R.styleable.ActionSheetDialog_titleTextColor, DEFAULT_VALUE);
                mTitleTextSize = defaultTypedArray.getDimensionPixelSize(R.styleable.ActionSheetDialog_titleTextSize, DEFAULT_VALUE);
                mTitleHeight = defaultTypedArray.getDimensionPixelSize(R.styleable.ActionSheetDialog_titleHeight, DEFAULT_VALUE);
                mTitleDivider = defaultTypedArray.getDrawable(R.styleable.ActionSheetDialog_titleDivider);
                mTitleDividerInset = defaultTypedArray.getDimensionPixelSize(R.styleable.ActionSheetDialog_titleDividerInset, DEFAULT_VALUE);
                mTitleDividerHeight = defaultTypedArray.getDimensionPixelSize(R.styleable.ActionSheetDialog_titleDividerHeight, DEFAULT_VALUE);

                mMessageTextColor = defaultTypedArray.getColor(R.styleable.ActionSheetDialog_messageTextColor, DEFAULT_VALUE);
                mMessageTextSize = defaultTypedArray.getDimensionPixelSize(R.styleable.ActionSheetDialog_messageTextSize, DEFAULT_VALUE);
                mMessageHeight = defaultTypedArray.getDimensionPixelSize(R.styleable.ActionSheetDialog_messageHeight, DEFAULT_VALUE);
                mMessageDivider = defaultTypedArray.getDrawable(R.styleable.ActionSheetDialog_messageDivider);
                mMessageDividerInset = defaultTypedArray.getDimensionPixelSize(R.styleable.ActionSheetDialog_messageDividerInset, DEFAULT_VALUE);
                mMessageDividerHeight = defaultTypedArray.getDimensionPixelSize(R.styleable.ActionSheetDialog_messageDividerHeight, DEFAULT_VALUE);

                mItemTextColor = defaultTypedArray.getColor(R.styleable.ActionSheetDialog_itemTextColor, DEFAULT_VALUE);
                mItemTextSize = defaultTypedArray.getDimensionPixelSize(R.styleable.ActionSheetDialog_itemTextSize, DEFAULT_VALUE);
                mItemHeight = defaultTypedArray.getDimensionPixelSize(R.styleable.ActionSheetDialog_itemHeight, DEFAULT_VALUE);
                mItemDivider = defaultTypedArray.getDrawable(R.styleable.ActionSheetDialog_itemDivider);
                mItemDividerInset = defaultTypedArray.getDimensionPixelSize(R.styleable.ActionSheetDialog_itemDividerInset, DEFAULT_VALUE);
                mItemDividerHeight = defaultTypedArray.getDimensionPixelSize(R.styleable.ActionSheetDialog_itemDividerHeight, DEFAULT_VALUE);

                mPositiveTextColor = defaultTypedArray.getColor(R.styleable.ActionSheetDialog_positiveTextColor, DEFAULT_VALUE);
                mPositiveTextSize = defaultTypedArray.getDimensionPixelSize(R.styleable.ActionSheetDialog_positiveTextSize, DEFAULT_VALUE);
                mPositiveHeight = defaultTypedArray.getDimensionPixelSize(R.styleable.ActionSheetDialog_positiveHeight, DEFAULT_VALUE);

                mCancelTextColor = defaultTypedArray.getColor(R.styleable.ActionSheetDialog_cancelTextColor, DEFAULT_VALUE);
                mCancelTextSize = defaultTypedArray.getDimensionPixelSize(R.styleable.ActionSheetDialog_cancelTextSize, DEFAULT_VALUE);
                mCancelHeight = defaultTypedArray.getDimensionPixelSize(R.styleable.ActionSheetDialog_cancelHeight, DEFAULT_VALUE);
                mCancelBackground = defaultTypedArray.getDrawable(R.styleable.ActionSheetDialog_cancelBackground);
                mCancelTopMargin = defaultTypedArray.getDimensionPixelSize(R.styleable.ActionSheetDialog_cancelTopMargins, DEFAULT_VALUE);

                mSheetMargins = defaultTypedArray.getDimensionPixelSize(R.styleable.ActionSheetDialog_sheetMargins, DEFAULT_VALUE);
                mContentBackground = defaultTypedArray.getDrawable(R.styleable.ActionSheetDialog_contentBackground);
                mWindowAnimationId = defaultTypedArray.getResourceId(R.styleable.ActionSheetDialog_windowAnimations, DEFAULT_VALUE);

            }
        }


        @Override
        public ActionSheetBuilder setCancelable(boolean cancelable) {
            mCancelable = cancelable;
            return this;
        }

        @Override
        public ActionSheetBuilder setMessage(CharSequence message) {
            mMessage = (String) message;
            return this;
        }

        @Override
        public ActionSheetBuilder setMessage(@StringRes int messageId) {
            mMessage = mContext.getString(messageId);
            return this;
        }

        @Override
        public ActionSheetBuilder setTitle(CharSequence title) {
            mTitle = (String) title;
            return this;
        }

        @Override
        public ActionSheetBuilder setTitle(@StringRes int titleId) {
            mTitle = mContext.getString(titleId);
            return this;
        }

        @Override
        public ActionSheetBuilder setNegativeButton(CharSequence text, OnClickListener listener) {
            mNegativeText = (String) text;
            mNegativeClickListener = listener;
            mCancelable = true;
            return this;
        }

        @Override
        public ActionSheetBuilder setPositiveButton(CharSequence text, OnClickListener listener) {
            mPositiveText = (String) text;
            mPositiveClickListener = listener;
            return this;
        }

        @Override
        public ActionSheetBuilder setItems(CharSequence[] items, OnClickListener listener) {
            for (int i = 0; i < items.length; i++) {
                ActionSheetItem item = new ActionSheetItem((String) items[i], listener);
                mActionSheetItems.add(item);
            }
            return this;
        }

        @Override
        public ActionSheetBuilder setItems(@ArrayRes int itemsId, OnClickListener listener) {
            this.setItems(mContext.getResources().getStringArray(itemsId), listener);
            return this;
        }

        @Override
        public ActionSheetDialog create() {
            mActionSheetDialog = new ActionSheetDialog(mContext);
            Window window = mActionSheetDialog.getWindow();
            window.setGravity(Gravity.BOTTOM);
            window.setWindowAnimations(mWindowAnimationId);
            WindowManager.LayoutParams params = window.getAttributes();
            params.y = dpToPx(mSheetMargins);
            params.x = 0;
            WindowManager windowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);

            int width = windowManager.getDefaultDisplay().getWidth();
            params.width = width - 2 * mSheetMargins;
            Drawable drawable = new ColorDrawable();
            drawable.setAlpha(0);
            window.setBackgroundDrawable(drawable);
            mActionSheetDialog.setCancelable(mCancelable);
            if (mCancelable) {
                mActionSheetDialog.setCanceledOnTouchOutside(true);
            }
            initViews();
            return mActionSheetDialog;
        }
        TextView mTitleView;
        ImageView mTitleDividerView;
        TextView mMessageView;
        ImageView mMessageDividerView;
        LinearLayout mSheetItemContainer;
        LinearLayout mContentPanel;
        TextView mCancelView;
        TextView mPositiveView;
        SheetItemOnClickListener mSheetItemOnClickListener = new SheetItemOnClickListener();

        private void initViews() {
            View rootView = LayoutInflater.from(mContext)
                    .inflate(R.layout.layout_action_sheet_dialog, null);

            mContentPanel = (LinearLayout) rootView.findViewById(R.id.content_panel);
            mTitleView = (TextView) rootView.findViewById(R.id.tv_title);
            mTitleDividerView = (ImageView) rootView.findViewById(R.id.title_divider);
            mMessageView = (TextView) rootView.findViewById(R.id.tv_message);
            mMessageDividerView = (ImageView) rootView.findViewById(R.id.message_divider);
            mSheetItemContainer = (LinearLayout) rootView.findViewById(R.id.scrollView_sheet_list);
            mCancelView = (TextView) rootView.findViewById(R.id.tv_cancel);
            if (null != mContentBackground) {
                mContentPanel.setBackground(mContentBackground);
            }
            handleTitle();
            handleMessage();
            handleContent();
            handleCancel();
            handlePositive();

            mActionSheetDialog.setView(rootView);


        }

        private ImageView createDivider(Drawable background, int inset) {
            ImageView divider = new ImageView(mContext);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            Log.d(TAG, "createDivider: inset = " + inset);
            params.setMargins(inset, 0, inset, 0);
            divider.setLayoutParams(params);
            divider.setBackground(background);
            divider.setMinimumHeight(mItemDividerHeight);
            return divider;
        }

        private void handlePositive() {
            if (null != mPositiveText) {
                mPositiveView = new TextView(mContext);
                mPositiveView.setGravity(Gravity.CENTER);
                ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
                mPositiveView.setLayoutParams(params);
                //mPositiveView.setPadding(0, dpToPx(5.0f), 0, dpToPx(5.0f));
                mPositiveView.setText(mPositiveText);
                mPositiveView.setTextSize(TypedValue.COMPLEX_UNIT_PX, mPositiveTextSize);
                mPositiveView.setTextColor(mPositiveTextColor);
                mPositiveView.setTag(AlertDialog.BUTTON_POSITIVE);
                mPositiveView.setOnClickListener(mSheetItemOnClickListener);
                mPositiveView.setMinHeight(mPositiveHeight);
                mSheetItemContainer.addView(createDivider(mItemDivider, mItemDividerInset));
                mSheetItemContainer.addView(mPositiveView);
            }

        }

        private void handleCancel() {
            if (null != mCancelView) {
                if (mCancelable) {
                    mCancelView.setMinHeight(mCancelHeight);
                    mCancelView.setTextColor(mCancelTextColor);
                    mCancelView.setTextSize(TypedValue.COMPLEX_UNIT_PX, mCancelTextSize);
                    if (null != mCancelBackground) {
                        mCancelView.setBackground(mCancelBackground);
                    }
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT);
                    params.setMargins(0, mCancelTopMargin, 0, 0);
//                    params.gravity=Gravity.CENTER;
                    mCancelView.setLayoutParams(params);
                    if (null != mNegativeText) {
                        mCancelView.setText(mNegativeText);
                        mCancelView.setGravity(Gravity.CENTER);
                    }
                    mCancelView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (null != mNegativeClickListener) {
                                mNegativeClickListener.onClick(mActionSheetDialog, AlertDialog.BUTTON_NEGATIVE);
                            }
                            mActionSheetDialog.dismiss();
                        }
                    });
                }
            }
        }

        private void handleContent() {
            if (null == mActionSheetItems || mActionSheetItems.isEmpty()) {
                mSheetItemContainer.setVisibility(View.GONE);
            } else {
                for (int i = 0, size = mActionSheetItems.size(); i < size; i++) {
                    ActionSheetItem item = mActionSheetItems.get(i);
                    TextView sheetItemView = new TextView(mContext);
                    sheetItemView.setGravity(Gravity.CENTER);
                    ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT);
                    sheetItemView.setLayoutParams(params);
                    //sheetItemView.setPadding(0, dpToPx(5.0f), 0, dpToPx(5.0f));
                    sheetItemView.setText(item.text);
                    sheetItemView.setMinHeight(mItemHeight);
                    sheetItemView.setTextSize(TypedValue.COMPLEX_UNIT_PX, mItemTextSize);
                    sheetItemView.setTextColor(mItemTextColor);
                    sheetItemView.setTag(i);
                    sheetItemView.setOnClickListener(mSheetItemOnClickListener);
                    mSheetItemContainer.addView(sheetItemView);
                    if (i < (size - 1)) {
                        mSheetItemContainer.addView(createDivider(mItemDivider, mItemDividerInset));
                    }
                }

            }
        }

        private void handleMessage() {
            if (null != mMessageView) {
                if (null == mMessage) {
                    mMessageView.setVisibility(View.GONE);
                    mMessageDividerView.setVisibility(View.GONE);
                } else {
                    mMessageDividerView.setBackground(mMessageDivider);
                    mMessageDividerView.setMinimumHeight(mMessageDividerHeight);
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT);
                    params.setMargins(mMessageDividerInset, 0, mMessageDividerInset, 0);
                    mMessageDividerView.setLayoutParams(params);

                    mMessageView.setMinHeight(mMessageHeight);
                    mMessageView.setGravity(Gravity.CENTER);
                    mMessageView.setTextSize(TypedValue.COMPLEX_UNIT_PX, mMessageTextSize);
                    mMessageView.setTextColor(mMessageTextColor);
                    mMessageView.setText(mMessage);
                }
            }
        }

        private void handleTitle() {
            if (null != mTitleView) {
                if (null == mTitle) {
                    mTitleView.setVisibility(View.GONE);
                    mTitleDividerView.setVisibility(View.GONE);
                } else {
                    mTitleDividerView.setBackground(mTitleDivider);
                    mTitleDividerView.setMinimumHeight(mTitleDividerHeight);
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT);
                    params.setMargins(mTitleDividerInset, 0, mTitleDividerInset, 0);
                    mTitleDividerView.setLayoutParams(params);

                    mTitleView.setMinHeight(mTitleHeight);
                    mTitleView.setGravity(Gravity.CENTER);
                    mTitleView.setTextColor(mTitleTextColor);
                    mTitleView.setTextSize(TypedValue.COMPLEX_UNIT_PX, mTitleTextSize);
                    mTitleView.setText(mTitle);
                }
            }
        }
        public int dpToPx(float dp) {
            float scale = mContext.getResources().getDisplayMetrics().density;
            return (int) (dp * scale + 0.5f);
        }

        public int spToPx(float sp) {
            float scale = mContext.getResources().getDisplayMetrics().scaledDensity;
            return (int) (sp * scale + 0.5f);
        }


        static class ActionSheetItem {

            String text;
            OnClickListener listener;
            public ActionSheetItem(String text, OnClickListener listener) {
                this.text = text;
                this.listener = listener;
            }
        }

        public interface ActionSheetItemClickListener {
            void onClick(int position);
        }

        private class SheetItemOnClickListener implements View.OnClickListener {

            @Override
            public void onClick(View v) {

                int tag = (int) v.getTag();
                Log.d(TAG, "onClick: tag = " + tag);
                if (BUTTON_POSITIVE == tag) {
                    if (null != mPositiveClickListener) {
                        mPositiveClickListener.onClick(mActionSheetDialog, BUTTON_POSITIVE);
                        mActionSheetDialog.dismiss();
                    }
                    mActionSheetDialog.dismiss();
                } else {
                    mActionSheetItems.get(tag).listener.onClick(mActionSheetDialog, tag);
                }
            }
        }
    }
}
