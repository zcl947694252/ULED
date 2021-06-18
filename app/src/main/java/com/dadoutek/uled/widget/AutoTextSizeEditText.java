package com.dadoutek.uled.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.RectF;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatEditText;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextDirectionHeuristic;
import android.text.TextDirectionHeuristics;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.TextView;


import com.dadoutek.uled.R;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;

/**
 * Created by sawdo on 2017/12/6.
 */

public class AutoTextSizeEditText extends AppCompatEditText {
    // Default minimum size for auto-sizing text in scaled pixels.
    private static final int DEFAULT_AUTO_SIZE_MIN_TEXT_SIZE_IN_SP = 12;
    // Default maximum size for auto-sizing text in scaled pixels.
    private static final int DEFAULT_AUTO_SIZE_MAX_TEXT_SIZE_IN_SP = 112;
    // Default value for the step size in pixels.
    private static final int DEFAULT_AUTO_SIZE_GRANULARITY_IN_PX = 1;
    // Use this to specify that any of the auto-size configuration int values have not been set.
    private static final float UNSET_AUTO_SIZE_UNIFORM_CONFIGURATION_VALUE = -1f;
    // Auto-size text type.
    private int mAutoSizeTextType = AUTO_SIZE_TEXT_TYPE_NONE;
    // Specify if auto-size text is needed.
    private boolean mNeedsAutoSizeText = false;
    // Step size for auto-sizing in pixels.
    private float mAutoSizeStepGranularityInPx = DEFAULT_AUTO_SIZE_GRANULARITY_IN_PX;
    // Minimum text size for auto-sizing in pixels.
    private float mAutoSizeMinTextSizeInPx = DEFAULT_AUTO_SIZE_MIN_TEXT_SIZE_IN_SP;
    // Maximum text size for auto-sizing in pixels.
    private float mAutoSizeMaxTextSizeInPx = DEFAULT_AUTO_SIZE_MAX_TEXT_SIZE_IN_SP;
    // Contains a (specified or computed) distinct sorted set of text sizes in pixels to pick from
    // when auto-sizing text.
    private int[] mAutoSizeTextSizesInPx = new int[0];

    private boolean mHasPresetAutoSizeValues = false;


    private static final RectF TEMP_RECTF = new RectF();
    private TextPaint mTempTextPaint;
    private int mAvailableWidth;
    private int mAvailableHeight;
    private float mSpacingMult;
    private float mSpacingAdd;


    // Cache of TextView methods used via reflection; the key is the method name and the value is
    // the method itself or null if it can not be found.
    private static Hashtable<String, Method> sTextViewMethodByNameCache = new Hashtable<>();
    private OnBackKeyClickListener mBackKeyClickListener;

    public interface OnBackKeyClickListener {
        void onBackClicked();
    }

    public void setOnBackKeyClickListener(OnBackKeyClickListener listener) {
        mBackKeyClickListener = listener;
    }

    public AutoTextSizeEditText(Context context) {
        super(context);
        init(context, null);
    }

    public AutoTextSizeEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public AutoTextSizeEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }


    private void init(Context context, AttributeSet attributeSet) {
        if (attributeSet == null) {
            return;
        }
        TypedArray typedArray = context.obtainStyledAttributes(attributeSet, R.styleable.AutoTextSizeEditText);
        mAutoSizeTextType = typedArray.getInt(R.styleable.AutoTextSizeEditText_aet_autoSizeTextType, AUTO_SIZE_TEXT_TYPE_NONE);
        mAutoSizeMaxTextSizeInPx = typedArray.getDimensionPixelSize(R.styleable.AutoTextSizeEditText_aet_autoSizeMaxTextSize, DEFAULT_AUTO_SIZE_MAX_TEXT_SIZE_IN_SP);
        mAutoSizeMinTextSizeInPx = typedArray.getDimensionPixelSize(R.styleable.AutoTextSizeEditText_aet_autoSizeMinTextSize, DEFAULT_AUTO_SIZE_MIN_TEXT_SIZE_IN_SP);
        mAutoSizeStepGranularityInPx = DEFAULT_AUTO_SIZE_GRANULARITY_IN_PX;
        typedArray.recycle();
        setupAutoSizeText();

    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        InputConnection inputConnection = super.onCreateInputConnection(outAttrs);
        if (inputConnection != null) {
            outAttrs.imeOptions &= ~EditorInfo.IME_FLAG_NO_ENTER_ACTION;
        }
        return inputConnection;
    }

    private boolean setupAutoSizeText() {
        if (mAutoSizeTextType == AUTO_SIZE_TEXT_TYPE_UNIFORM) {
            // Calculate the sizes set based on minimum size, maximum size and step size if we do
            // not have a predefined set of sizes or if the current sizes array is empty.
            if (!mHasPresetAutoSizeValues || mAutoSizeTextSizesInPx.length == 0) {
                int autoSizeValuesLength = 1;
                float currentSize = Math.round(mAutoSizeMinTextSizeInPx);
                while (Math.round(currentSize + mAutoSizeStepGranularityInPx)
                        <= Math.round(mAutoSizeMaxTextSizeInPx)) {
                    autoSizeValuesLength++;
                    currentSize += mAutoSizeStepGranularityInPx;
                }

                int[] autoSizeTextSizesInPx = new int[autoSizeValuesLength];
                float sizeToAdd = mAutoSizeMinTextSizeInPx;
                for (int i = 0; i < autoSizeValuesLength; i++) {
                    autoSizeTextSizesInPx[i] = Math.round(sizeToAdd);
                    sizeToAdd += mAutoSizeStepGranularityInPx;
                }
                mAutoSizeTextSizesInPx = cleanupAutoSizePresetSizes(autoSizeTextSizesInPx);
            }

            mNeedsAutoSizeText = true;
        } else {
            mNeedsAutoSizeText = false;
        }

        return mNeedsAutoSizeText;
    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
//            Log.i("main_activity", "键盘向下 ");
            if (mBackKeyClickListener != null) {
                mBackKeyClickListener.onBackClicked();
            }

            super.onKeyPreIme(keyCode, event);
            return false;
        }
        return super.onKeyPreIme(keyCode, event);
    }

    // Returns distinct sorted positive values.
    private int[] cleanupAutoSizePresetSizes(int[] presetValues) {
        final int presetValuesLength = presetValues.length;
        if (presetValuesLength == 0) {
            return presetValues;
        }
        Arrays.sort(presetValues);

        final List<Integer> uniqueValidSizes = new ArrayList<>();
        for (int i = 0; i < presetValuesLength; i++) {
            final int currentPresetValue = presetValues[i];

            if (currentPresetValue > 0
                    && Collections.binarySearch(uniqueValidSizes, currentPresetValue) < 0) {
                uniqueValidSizes.add(currentPresetValue);
            }
        }

        if (presetValuesLength == uniqueValidSizes.size()) {
            return presetValues;
        } else {
            final int uniqueValidSizesLength = uniqueValidSizes.size();
            final int[] cleanedUpSizes = new int[uniqueValidSizesLength];
            for (int i = 0; i < uniqueValidSizesLength; i++) {
                cleanedUpSizes[i] = uniqueValidSizes.get(i);
            }
            return cleanedUpSizes;
        }
    }

    @Override
    protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter);
        adjustTextSize();

    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        if (w != oldw || h != oldh && mAutoSizeTextSizesInPx.length != 0)
            adjustTextSize();
    }


    @Override
    public void setTextSize(float size) {
        float tempSize = adjustTextSize();
//        if (tempSize != 0)
//            size = tempSize;
    }

    private float adjustTextSize() {
        mAvailableWidth = getMeasuredWidth() - getTotalPaddingLeft() - getTotalPaddingRight();
        mAvailableHeight = getMeasuredHeight() - getExtendedPaddingBottom() - getExtendedPaddingTop();

        if (mAvailableWidth <= 0 || mAvailableHeight <= 0) {

//            super.setTextSize(size);
            return 0;
        }

        synchronized (TEMP_RECTF) {
            TEMP_RECTF.setEmpty();
            TEMP_RECTF.right = mAvailableWidth;
            TEMP_RECTF.bottom = mAvailableHeight;
            final float optimalTextSize = findLargestTextSizeWhichFits(TEMP_RECTF);
            if (optimalTextSize != this.getTextSize())
                super.setTextSize(TypedValue.COMPLEX_UNIT_PX, optimalTextSize);
            return optimalTextSize;
        }

    }

    private int findLargestTextSizeWhichFits(RectF availableSpace) {
        final int sizesCount = mAutoSizeTextSizesInPx.length;
        if (sizesCount == 0) {
//            setupAutoSizeText();
//            return 0;
            throw new IllegalStateException("No available text sizes to choose from.");
        }

        int bestSizeIndex = 0;
        int lowIndex = bestSizeIndex + 1;
        int highIndex = sizesCount - 1;
        int sizeToTryIndex;
        while (lowIndex <= highIndex) {
            sizeToTryIndex = (lowIndex + highIndex) / 2;
            if (suggestedSizeFitsInSpace(mAutoSizeTextSizesInPx[sizeToTryIndex], availableSpace)) {
                bestSizeIndex = lowIndex;
                lowIndex = sizeToTryIndex + 1;
            } else {
                highIndex = sizeToTryIndex - 1;
                bestSizeIndex = highIndex;
            }
        }

        return mAutoSizeTextSizesInPx[bestSizeIndex];
    }


    @Override
    public void setLineSpacing(final float add, final float mult) {
        super.setLineSpacing(add, mult);
        mSpacingMult = mult;
        mSpacingAdd = add;
    }

    private boolean suggestedSizeFitsInSpace(int suggestedSizeInPx, RectF availableSpace) {
        final CharSequence text = getText();
        final int maxLines = getMaxLines();
        if (mTempTextPaint == null) {
            mTempTextPaint = new TextPaint();
        } else {
            mTempTextPaint.reset();
        }
        mTempTextPaint.set(getPaint());
        mTempTextPaint.setTextSize(suggestedSizeInPx);

        // Needs reflection call due to being private.
        Layout.Alignment alignment = invokeAndReturnWithDefault(
                this, "getLayoutAlignment", Layout.Alignment.ALIGN_NORMAL);
        final StaticLayout layout = Build.VERSION.SDK_INT >= 23
                ? createStaticLayoutForMeasuring(
                text, alignment, Math.round(availableSpace.right), maxLines)
                : createStaticLayoutForMeasuringPre23(
                text, alignment, Math.round(availableSpace.right));
        // Lines overflow.
        if (maxLines != -1 && (layout.getLineCount() > maxLines
                || (layout.getLineEnd(layout.getLineCount() - 1)) != text.length())) {
            return false;
        }

        // Height overflow.
        if (layout.getHeight() > availableSpace.bottom) {
            return false;
        }
//       //("layout.getHeight() = " + SizeUtils.px2dp(layout.getHeight()) + " dp");

        return true;
    }

    private <T> T invokeAndReturnWithDefault(@NonNull Object object,
                                             @NonNull final String methodName, @NonNull final T defaultValue) {
        T result = null;
        boolean exceptionThrown = false;

        try {
            // Cache lookup.
            Method method = getTextViewMethod(methodName);
            result = (T) method.invoke(object);
        } catch (Exception ex) {
            exceptionThrown = true;
//            Log.w(TAG, "Failed to invoke TextView#" + methodName + "() method", ex);
        } finally {
            if (result == null && exceptionThrown) {
                result = defaultValue;
            }
        }

        return result;
    }

    @Nullable
    private Method getTextViewMethod(@NonNull final String methodName) {
        try {
            Method method = sTextViewMethodByNameCache.get(methodName);
            if (method == null) {
                method = TextView.class.getDeclaredMethod(methodName);
                if (method != null) {
                    method.setAccessible(true);
                    // Cache update.
                    sTextViewMethodByNameCache.put(methodName, method);
                }
            }

            return method;
        } catch (Exception ex) {
//            Log.w(TAG, "Failed to retrieve TextView#" + methodName + "() method", ex);
            return null;
        }
    }

    @TargetApi(23)
    private StaticLayout createStaticLayoutForMeasuring(CharSequence text,
                                                        Layout.Alignment alignment, int availableWidth, int maxLines) {
        // Can use the StaticLayout.Builder (along with TextView params added in or after
        // API 23) to construct the layout.
        final TextDirectionHeuristic textDirectionHeuristic = invokeAndReturnWithDefault(
                this, "getTextDirectionHeuristic",
                TextDirectionHeuristics.FIRSTSTRONG_LTR);

        final StaticLayout.Builder layoutBuilder = StaticLayout.Builder.obtain(
                text, 0, text.length(), mTempTextPaint, availableWidth);

        return layoutBuilder.setAlignment(alignment)
                .setLineSpacing(
                        this.getLineSpacingExtra(),
                        this.getLineSpacingMultiplier())
                .setIncludePad(this.getIncludeFontPadding())
                .setBreakStrategy(this.getBreakStrategy())
                .setHyphenationFrequency(this.getHyphenationFrequency())
                .setMaxLines(maxLines == -1 ? Integer.MAX_VALUE : maxLines)
                .setTextDirection(textDirectionHeuristic)
                .build();
    }

    @TargetApi(14)
    private StaticLayout createStaticLayoutForMeasuringPre23(CharSequence text,
                                                             Layout.Alignment alignment, int availableWidth) {
        // Setup defaults.
        float lineSpacingMultiplier = 1.0f;
        float lineSpacingAdd = 0.0f;
        boolean includePad = true;

        if (Build.VERSION.SDK_INT >= 16) {
            // Call public methods.
            lineSpacingMultiplier = this.getLineSpacingMultiplier();
            lineSpacingAdd = this.getLineSpacingExtra();
            includePad = this.getIncludeFontPadding();
        } else {
            // Call private methods and make sure to provide fallback defaults in case something
            // goes wrong. The default values have been inlined with the StaticLayout defaults.
            lineSpacingMultiplier = invokeAndReturnWithDefault(this,
                    "getLineSpacingMultiplier", lineSpacingMultiplier);
            lineSpacingAdd = invokeAndReturnWithDefault(this,
                    "getLineSpacingExtra", lineSpacingAdd);
            includePad = invokeAndReturnWithDefault(this,
                    "getIncludeFontPadding", includePad);
        }

        // The layout could not be constructed using the builder so fall back to the
        // most broad constructor.
        return new StaticLayout(text, mTempTextPaint, availableWidth,
                alignment,
                lineSpacingMultiplier,
                lineSpacingAdd,
                includePad);
    }


}
