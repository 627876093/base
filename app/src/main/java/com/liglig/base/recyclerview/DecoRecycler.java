package com.liglig.base.recyclerview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

/**
 * RecyclerView分隔线-暂不支持瀑布流
 */
public class DecoRecycler extends RecyclerView.ItemDecoration {

    //仅以下四条可进行 | 运算
    public static final int Edge_Left = 0b0001;
    public static final int Edge_Top = 0b0010;
    public static final int Edge_Right = 0b0100;
    public static final int Edge_Bottom = 0b1000;

    public static final int Edge_NONE = 0b0000;
    public static final int Edge_All = 0b1111;
    public static final int Edge_Except_Left = Edge_All - Edge_Left;
    public static final int Edge_Except_Top = Edge_All - Edge_Top;
    public static final int Edge_Except_Right = Edge_All - Edge_Right;
    public static final int Edge_Except_Bottom = Edge_All - Edge_Bottom;

    private Drawable mDivider;
    private int headerCount;//RecyclerView的头部数量
    private int dividerPadding;//若RecyclerView设置了shape, 且stroke有宽度, 分割线可不画到边缘
    private boolean hasLeft, hasTop, hasRight, hasBottom;

    //粘性头部StickyHeader相关
    private boolean hasSticky;//是否添加StickyHeader
    private Paint paint;//Sticky画笔
    private int stickyBackground, stickyColor, stickyDivideColor;//Sticky背景 / 文字颜色 / 分割线颜色
    private int stickyPaddingLeft;//Header文字左侧间隔

    /**
     * 自行在drawable中创建分隔线
     */
    public DecoRecycler(Context context, int dividerId) {
        this(context, dividerId, Edge_Bottom, false);
    }

    public DecoRecycler(Context context, int dividerId, boolean hasSticky) {
        this(context, dividerId, Edge_Bottom, hasSticky);
    }

    /**
     * 同时设定边缘
     */
    public DecoRecycler(Context context, int dividerId, int edge) {
        this(context, dividerId, edge, false);
    }

    public DecoRecycler(Context context, int dividerId, int edge, boolean hasSticky) {
        if (dividerId != 0) {
            mDivider = ContextCompat.getDrawable(context, dividerId);
        }
        setEdgePadding(edge);
        this.hasSticky = hasSticky;
        if (hasSticky) {
            initStickyPaint();
        }
    }

    /**
     * 头部不添加分隔线时调用 -- 默认全部添加
     */
    public DecoRecycler setHeaderCount(int headerCount) {
        this.headerCount = headerCount;
        return this;
    }

    public DecoRecycler setDividerPadding(int dividerPadding) {
        this.dividerPadding = dividerPadding;
        return this;
    }

    private void initStickyPaint() {
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setDither(true);
        paint.setTextSize(20);
        stickyColor = Color.BLACK;
        stickyDivideColor = Color.TRANSPARENT;
    }

    public DecoRecycler setStickyBackground(int color) {
        this.stickyBackground = color;
        return this;
    }

    public DecoRecycler setStickyColor(int color) {
        this.stickyColor = color;
        return this;
    }

    public DecoRecycler setStickyDivideColor(int color) {
        this.stickyDivideColor = color;
        return this;
    }

    public DecoRecycler setStickySize(int sp) {
        paint.setTextSize(sp);
        return this;
    }

    public DecoRecycler setStickyPaddingLeft(int dp) {
        this.stickyPaddingLeft = dp;
        return this;
    }

    public int dp(Context context, float dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                context.getResources().getDisplayMetrics());
    }

    /**
     * 设置边缘的分割线
     * 用法: setEdgePadding(Edge_Top|Edge_Bottom)
     */
    public void setEdgePadding(int edge) {
        if (edge >= Edge_Bottom) {
            hasBottom = true;
            edge -= Edge_Bottom;
        } else {
            hasBottom = false;
        }
        if (edge >= Edge_Right) {
            hasRight = true;
            edge -= Edge_Right;
        } else {
            hasRight = false;
        }
        if (edge >= Edge_Top) {
            hasTop = true;
            edge -= Edge_Top;
        } else {
            hasTop = false;
        }
        hasLeft = edge >= Edge_Left;
    }

    /**
     * 清除已设置的边缘分割线
     */
    public void clearEdgePadding() {
        hasLeft = hasTop = hasRight = hasBottom = false;
    }

    @Override
    public void onDraw(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        if (mDivider == null) {
            return;
        }
        int spanCount = getSpanCount(parent.getLayoutManager());
        int childCount = parent.getChildCount();
        drawHorizontal(c, parent, spanCount, childCount);
        drawVertical(c, parent, spanCount, childCount);
    }

    /**
     * 每行个数
     */
    private int getSpanCount(RecyclerView.LayoutManager manager) {
        int spanCount = 1;
        if (manager instanceof GridLayoutManager) {
            spanCount = ((GridLayoutManager) manager).getSpanCount();
        } else if (manager instanceof StaggeredGridLayoutManager) {
            spanCount = ((StaggeredGridLayoutManager) manager).getSpanCount();
        }
        return spanCount;
    }

    /**
     * 画竖线 -- 适用于竖直滑动
     */
    public void drawHorizontal(Canvas c, RecyclerView parent, int spanCount, int childCount) {
        for (int i = 0; i < childCount; i++) {
            int col = i < headerCount ? 0 : (i - headerCount) % spanCount;
            View child = parent.getChildAt(i);
            RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) child.getLayoutParams();
            int top = child.getTop() - lp.topMargin + dividerPadding;
            int bottom = child.getBottom() + lp.bottomMargin - dividerPadding;
            if (col == 0 && hasLeft) {//最左侧
                int r = child.getLeft() - lp.leftMargin;
                int l = r - mDivider.getIntrinsicWidth();
                mDivider.setBounds(l, top, r, bottom);
                mDivider.draw(c);
            }
            if (col == spanCount - 1 && !hasRight) {//最右侧
                continue;
            }
            int left = child.getRight() + lp.rightMargin +
                    Math.round(ViewCompat.getTranslationX(child));
            int right = left + mDivider.getIntrinsicWidth();
            mDivider.setBounds(left, top, right, bottom);
            mDivider.draw(c);
        }
    }

    /**
     * 画横线 -- 适用于竖直滑动
     */
    public void drawVertical(Canvas c, RecyclerView parent, int spanCount, int childCount) {
        for (int i = 0; i < childCount; i++) {
            int row = getRow(i, spanCount);
            if (row < headerCount) {
                continue;
            }
            View child = parent.getChildAt(i);
            RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) child.getLayoutParams();
            int left = child.getLeft() - lp.leftMargin + dividerPadding;
            int right = child.getRight() + lp.rightMargin - dividerPadding;
            if (row == 0 && hasTop) {//第一行
                int b = child.getTop() - lp.topMargin;
                int t = b - mDivider.getIntrinsicHeight();
                mDivider.setBounds(left, t, right, b);
                mDivider.draw(c);
            }
            if (row == getRow(childCount - 1, spanCount) && !hasBottom) {//最后一行
                continue;
            }
            // divider的top 等于 item的bottom 加 marginBottom 加 Y方向上的位移
            int top = child.getBottom() + lp.bottomMargin +
                    Math.round(ViewCompat.getTranslationY(child));
            int bottom = top + mDivider.getIntrinsicHeight();
            mDivider.setBounds(left, top, right, bottom);
            mDivider.draw(c);
        }
    }

    private int getRow(int position, int spanCount) {
        return position < headerCount ? position : (position - headerCount) / spanCount + headerCount;
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent,
                               @NonNull RecyclerView.State state) {
        RecyclerView.LayoutManager manager = parent.getLayoutManager();
        int position = parent.getChildAdapterPosition(view) - headerCount;
        int spanCount = getSpanCount(manager);
        int childCount = parent.getAdapter().getItemCount() - headerCount;

        if (position < 0 || mDivider == null) {
            outRect.set(0, 0, 0, 0);
            return;
        }
        if (manager instanceof LinearLayoutManager) {
            setOffset(((LinearLayoutManager) manager).getOrientation(), outRect, position, spanCount, childCount);
        } else {
            outRect.set(0, 0, 0, 0);
        }
    }

    private void setOffset(int orientation, Rect outRect, int position, int spanCount, int childCount) {
        int width = mDivider.getIntrinsicWidth();
        int height = mDivider.getIntrinsicHeight();
        float left, right, top, bottom;
        if (orientation == LinearLayoutManager.VERTICAL) {
            int col = position % spanCount;
            if (col == 0) {//最左侧
                left = hasLeft ? width : 0;
            } else {
                if (hasLeft ^ hasRight) {//一真一假
                    left = hasLeft ? width : 0;
                } else {
                    left = (hasLeft ? spanCount - col : col) * 1.0F * width / spanCount;
                }
            }
            if (col == spanCount - 1) {//最右侧
                right = hasRight ? width : 0;
            } else {
                if (hasLeft ^ hasRight) {//一真一假
                    right = hasRight ? width : 0;
                } else {
                    right = (hasRight ? col + 1 : spanCount - col - 1) * 1.0F * width / spanCount;
                }
            }
            int row = position / spanCount;
            top = (row == 0 && hasTop) ? height : 0;
            bottom = (row == (childCount - 1) / spanCount && !hasBottom) ? 0 : height;
        } else {
            int col = position / spanCount;
            left = (col == 0 && hasLeft) ? width : 0;
            right = (col == (childCount - 1) / spanCount && !hasRight) ? 0 : width;
            int row = position % spanCount;
            if (row == 0) {//最上侧
                top = hasTop ? height : 0;
            } else {
                if (hasTop ^ hasBottom) {//一真一假
                    top = hasTop ? height : 0;
                } else {
                    top = (hasTop ? spanCount - row : row) * 1.0F * height / spanCount;
                }
            }
            if (row == spanCount - 1) {//最下侧
                bottom = hasBottom ? height : 0;
            } else {
                if (hasTop ^ hasBottom) {//一真一假
                    bottom = hasBottom ? height : 0;
                } else {
                    bottom = (hasBottom ? row + 1 : spanCount - row - 1) * 1.0F * height / spanCount;
                }
            }
        }
        outRect.set((int) left, (int) top, (int) right, (int) bottom);
    }

    /**
     * 画StickyHeader
     */
    @Override
    public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
        if (!hasSticky) {
            return;
        }
        View c0 = parent.getChildAt(0);//可见的第一个Item
        int position = parent.getChildAdapterPosition(c0);//c0的实际下标
        if (position < headerCount) {//或是头部
            return;
        }
        View c1 = parent.getChildAt(1);
        if (c1 != null) {
            AbsRecyclerAdapter adapter = (AbsRecyclerAdapter) parent.getAdapter();
            int stickyHeight = adapter.getStickyHeight();
            int left = parent.getPaddingLeft();
            int right = parent.getWidth() - parent.getPaddingRight();
            int top = 0;
            int bottom = stickyHeight;
            // 判断是否达到临界点
            // (第一个可见Item是每组的最后一个,第二个可见Item是下一组的第一个,
            //      并且第一个可见Item的底部小于StickyHeader的高度)
            // 这里直接判断item的底部位置小于header的高度有点欠妥,应该还要考虑paddingTop以及marginTop,这里展示不考虑了
            int pos = position - headerCount;
            if (adapter.isStickyGroupEnd(pos) && adapter.isStickyGroupStart(pos + 1) &&
                    c0.getBottom() <= stickyHeight) {
                bottom = c0.getBottom();
                top = bottom - stickyHeight;
            }
            // 计算文字居中时候的基线
            Rect targetRect = new Rect(left, top, right, bottom);
            Paint.FontMetricsInt fontMetrics = paint.getFontMetricsInt();
            int baseline = (targetRect.bottom + targetRect.top - fontMetrics.bottom - fontMetrics.top) / 2;
            // 背景
            paint.setColor(stickyBackground);
            c.drawRect(left, top, right, bottom, paint);
            // 文字
            paint.setColor(stickyColor);
            c.drawText(adapter.getStickyText(pos), left + stickyPaddingLeft, baseline, paint);
            //分割线
            if (stickyDivideColor != Color.TRANSPARENT) {
                paint.setColor(stickyDivideColor);
                c.drawLine(left, bottom, right, bottom, paint);
            }
        }
    }

    /**
     * 获取左侧分隔线宽度 -- 宽度: dp
     */
    public int getDecoLeftWidth() {
        return (mDivider == null || !hasLeft) ? 0 : mDivider.getIntrinsicWidth();
    }

    /**
     * 获取顶部分隔线高度 -- 宽度: dp
     */
    public int getDecoTopHeight() {
        return (mDivider == null || !hasTop) ? 0 : mDivider.getIntrinsicHeight();
    }
}
