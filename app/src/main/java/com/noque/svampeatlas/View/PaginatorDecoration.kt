package com.noque.svampeatlas.View

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.R
import android.content.res.Resources
import androidx.recyclerview.widget.GridLayoutManager


class PaginatorDecoration(private val activeColor: Int, private val inactiveColor: Int): RecyclerView.ItemDecoration() {

    private val DP = Resources.getSystem().getDisplayMetrics().density
    private val indicatorItemLenght = 16 * DP
    private val indicatorItemPadding = 4 * DP
    private val indicatorStrokeWidth = 2 * DP
    private val indicatorHeight = 30 * DP
    private val paint = Paint()
    private val interpolator = AccelerateDecelerateInterpolator()



    override fun onDrawOver(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(indicatorStrokeWidth);
        paint.setStyle(Paint.Style.STROKE);
        paint.setAntiAlias(true);

        super.onDrawOver(canvas, parent, state)


        val itemCount = parent.adapter?.itemCount?: 0


        val totalLength = indicatorItemLenght * itemCount
        val paddingBetweenItems = Math.max(0, itemCount - 1) * indicatorItemPadding
        val indicatorTotalWidth = totalLength + paddingBetweenItems
        val indicatorStartX = (parent.width - indicatorTotalWidth) / 2f

        // center vertically in the allotted space
        val indicatorPosY = parent.height - indicatorHeight / 2f

        drawInactiveIndicators(canvas, indicatorStartX, indicatorPosY, itemCount)

        val layoutManager = parent.layoutManager as GridLayoutManager
        val activePosition = layoutManager.findFirstVisibleItemPosition()

        if (activePosition == RecyclerView.NO_POSITION) {
            return
        }

        val activeChild = layoutManager.findViewByPosition(activePosition)

        if (activeChild == null) {
            return
        }

// on swipe the active item will be positioned from [-width, 0]
// interpolate offset for smooth animation
        val progress = interpolator.getInterpolation(activeChild.left * -1 / activeChild.width.toFloat())


        drawHighlights(canvas, indicatorStartX, indicatorPosY, activePosition, progress, itemCount)

    }


    private fun drawInactiveIndicators(
        c: Canvas, indicatorStartX: Float,
        indicatorPosY: Float, itemCount: Int
    ) {
        paint.color = inactiveColor

        // width of item indicator including padding
        val itemWidth = indicatorItemLenght + indicatorItemPadding

        var start = indicatorStartX
        for (i in 0 until itemCount) {
            // draw the line for every item
            c.drawLine(
                start, indicatorPosY,
                start + indicatorItemLenght, indicatorPosY, paint
            )
            start += itemWidth
        }
    }

    private fun drawHighlights(
        c: Canvas, indicatorStartX: Float, indicatorPosY: Float,
        highlightPosition: Int, progress: Float, itemCount: Int
    ) {
        paint.color = activeColor

        // width of item indicator including padding
        val itemWidth = indicatorItemLenght + indicatorItemPadding

        if (progress == 0f) {
            // no swipe, draw a normal indicator
            val highlightStart = indicatorStartX + itemWidth * highlightPosition
            c.drawLine(
                highlightStart, indicatorPosY,
                highlightStart + indicatorItemLenght, indicatorPosY, paint
            )
        } else {
            var highlightStart = indicatorStartX + itemWidth * highlightPosition
            // calculate partial highlight
            val partialLength = indicatorItemLenght * progress

            // draw the cut off highlight
            c.drawLine(
                highlightStart + partialLength, indicatorPosY,
                highlightStart + indicatorItemLenght, indicatorPosY, paint
            )

            // draw the highlight overlapping to the next item as well
            if (highlightPosition < itemCount - 1) {
                highlightStart += itemWidth
                c.drawLine(
                    highlightStart, indicatorPosY,
                    highlightStart + partialLength, indicatorPosY, paint
                )
            }
        }
    }

}