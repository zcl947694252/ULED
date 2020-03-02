package com.dadoutek.uled.switches

import android.content.Context
import android.support.v4.view.ViewPager
import android.view.View
import com.blankj.utilcode.util.LogUtils


/**
 * 创建者     ZCL
 * 创建时间   2020/2/21 16:50
 * 描述
 * position 指的是该内容页的位置偏移，该偏移是相对的，页面静止时，以屏幕左边界为 0，
 * 屏幕内的页面 position 为0，左边为-1，依次递减，右侧为1，依次递增。
 * 当屏幕滑动时，page2只出现一半，此时，page2 的 position 为-0.5，
 * page3 为0.5，依次类推可得出其他page 回调的 position 值
 *
 * 更新者     $
 * 更新时间   $
 * 更新描述
 */
class ScalePageTransformer(private var minScale: Float, private var context: Context) : ViewPager.PageTransformer {
    override fun transformPage(page: View, position: Float) {
        LogUtils.v("zcl--滑动$position----------移动x轴${ page.translationX}")
        if (position == 0.8f) {//必須滿足條件才能進入
            page.translationX = -77f//調節後面的界面的左間距  paddingstart25  -80    paddingstart75 則爲-270完全居中 負值能否再小 看兩邊的多出部分是否對稱 負值負移動間距與後面的圖片
        } else if (position == 0.2f){
            page.translationX = -77f//調節固定間距時兩圖之間的空白距離 paddingstart變大這個就要變小*/
        }

        val size = when {
            position < -1 -> minScale//当左划滑动时 左侧会从-1到-2 缩小为最小的minscale
            position >= -1 && position < 0 -> minScale + (1 - minScale) * (1 + position)//当滑动时一半当前界面的postion大于-1切小于0当前的界面慢慢变小
            position < 1 -> minScale + (1 - minScale) * (1 - position)//当滑动时后面进入的界面大于0慢慢变大
            else -> minScale
        }
        page.scaleY = size
    }
}