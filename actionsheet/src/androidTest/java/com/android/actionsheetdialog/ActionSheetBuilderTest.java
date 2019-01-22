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

import android.content.Context;
import android.support.test.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by jonny.peng on 2017/4/12.
 */
public class ActionSheetBuilderTest {
    private ActionSheetDialog.ActionSheetBuilder mActionSheetBuilder;
    private Context mContext;
    @Before
    public void setUp() throws Exception {
        mContext = InstrumentationRegistry.getTargetContext();
        mActionSheetBuilder = new ActionSheetDialog.ActionSheetBuilder(mContext);

    }

    @Test
    public void setCancelable() throws Exception {
        assertEquals(mActionSheetBuilder, mActionSheetBuilder.setCancelable(true));
        assertEquals(true, mActionSheetBuilder.mCancelable);
        assertEquals(mActionSheetBuilder, mActionSheetBuilder.setCancelable(false));
        assertEquals(false, mActionSheetBuilder.mCancelable);
    }

    @Test
    public void setMessage() throws Exception {

    }

    @Test
    public void setMessage1() throws Exception {

    }

    @Test
    public void setTitle() throws Exception {

    }

    @Test
    public void setTitle1() throws Exception {

    }

    @Test
    public void setNegativeButton() throws Exception {

    }

    @Test
    public void setPositiveButton() throws Exception {

    }

    @Test
    public void setItems() throws Exception {

    }

    @Test
    public void setItems1() throws Exception {

    }

    @Test
    public void create() throws Exception {

    }

    @Test
    public void dpToPx() throws Exception {

    }

    @Test
    public void spToPx() throws Exception {

    }

}