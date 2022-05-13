package com.liglig.base.recyclerview;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.text.Html;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;
import com.liglig.base.glide.GlideRoundTransform;
import com.liglig.base.utils.BitMapUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

/**
 * RecyclerView的多(单)布局 适配器
 * 可以实现ListView、GridView和瀑布流的效果
 *
 * 配置数据 -- 必须重写{@link #onBindHolder(RecyclerHolder, Object, int)}
 * 设置空数据页面
 *          -- {@link #setEmptyView(int)} 或 {@link #setEmptyView(View)}
 *          -- 当头部和数据源都为空时 显示EmptyView
 *          -- 如需只有当数据源为空时 显示EmptyView
 *                  重写 {@link #getEmptyCount()} 调用 {@link #getEmptyCountOfData()}
 *                  此时需重写 {@link #setEmptyLayoutParams()} 调用 {@link #setEmptyHeight(int)}
 *          -- 给EmptyView中的{@link TextView}设置文字 调用 {@link #setEmptyTips(int, String)}
 *          -- 给EmptyView中的{@link View}设置点击事件 调用 {@link #setEmptyClickListener(int, View.OnClickListener)}
 * 添加头部 -- {@link #addHeaderView(View)} -- 最多添加97条头部
 *         -- 添加头部后，部分item刷新(如{@link #notifyItemChanged(int)})时 不可直接使用原始数据源的下标
 *         -- 因父类方法为final，不可重写，添加头部时可调用自定义方法{@link #notifyItemChange2(int)}
 * 添加尾部 -- {@link #addFooterView(View)}
 *      添加头部或尾部后 其点击事件需自行处理
 * 多布局实现 -- 必须重写{@link #getItemType(T)}方法 重写时返回type类型 item的类型从0开始 依次递增
 *           -- type非0时 当前item默认宽度撑满
 *           -- 如需不撑满 需重写{@link #isItemFullSpan(int)} 根据type的值返回是否撑满
 * 点击事件 -- 给item设置 调用 {@link #setOnItemClickListener(OnItemClickListener)}
 *         -- 给item中View设置 前提: 已重写 {@link #setOnItemClickListener(OnItemClickListener)}
 *              Adapter中调用 {@link RecyclerHolder#setOnClickListener(int)}
 *                      或 {@link RecyclerHolder#setOnClickListenerAndTag(int, Object)}
 * 长按事件类似
 * 注意  -- 不可重写的方法 -- {@link #onBindViewHolder(RecyclerHolder, int)} -- 配置数据
 *                      替代的重写方法{@link #onBindHolder(RecyclerHolder, Object, int)}
 *      -- 不可重写的方法 -- {@link #getItemViewType(int)} -- 多布局时获取item的类型
 *                      替代的重写方法{@link #getItemType(T)}
 *      -- 不可重写的方法 -- {@link #isFullSpan(int)} -- 多布局时item是否撑满 默认非0撑满
 *                      替代的重写方法{@link #isItemFullSpan(int)}
 *      -- 不可重写的方法 -- {@link #getGridSpanSize(int, int)} -- 多布局 GridLayoutManager 非0不撑满时 item占用宽度
 *                      替代的重写方法{@link #getItemSpanSize(int)}
 *      -- 可选的方法 -- {@link #getEmptyCountOfData()} -- 空数据的数量（只考虑数据源的数量）
 *                      在重写{@link #getEmptyCount()}时调用
 *      -- 可选的方法 -- {@link #setEmptyHeight(int)} -- 设置空数据的高度（默认撑满）
 *                      在重写{@link #setEmptyLayoutParams()}时调用
 */
public abstract class AbsRecyclerAdapter<T> extends RecyclerView.Adapter<AbsRecyclerAdapter.RecyclerHolder> {
    protected Context context;
    protected List<T> data;
    private int[] resId;//多布局的布局文件

    public static final int TYPE_EMPTY = -1;//空数据的类型
    public static final int TYPE_GAP = -2;//顶部空白间隙的类型
    public static final int TYPE_HEADER = -3;//第一条头部的类型
    public static final int TYPE_FOOTER = -100;//第一条尾部的类型
    private List<View> mHeaderViews = new ArrayList<>();
    private List<View> mFooterViews = new ArrayList<>();
    private View mEmpty;
    private View mGap;
    private boolean hasGap = false;

    protected OnItemClickListener onItemClickListener;
    protected OnItemLongClickListener onItemLongClickListener;

    /**
     * 构造方法 -- context建议传入Activity对象(即this) 使多语言适应于app内语言环境
     */
    public AbsRecyclerAdapter(Context context, int... resId) {
        this.context = context;
        this.resId = resId;
        data = new ArrayList<>();
    }

    public AbsRecyclerAdapter(Context context, List<T> data, int... resId) {
        this.context = context;
        this.resId = resId;
        this.data = data;
    }

    public void setData(List<T> data){
        this.data = data;
        setEmptyVisible();
        notifyDataSetChanged();
    }

    public void addData(List<T> data){
        data.addAll(data);
        notifyDataSetChanged();
    }

    public void clearData(){
        data.clear();
        notifyDataSetChanged();
    }

    public List<T> getData(){
        return data;
    }

    public void deleteItem(int position){
        data.remove(position);
        notifyDataSetChanged();
    }

    /**
     * Item点击监听 -- 添加头部、尾部后，点击事件需自行处理
     *              -- 多布局时都会回调
     */
    public AbsRecyclerAdapter setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
        return this;
    }

    public interface OnItemClickListener {
        void onItemClick(View v, int position);
    }

    /**
     * Item长按监听 -- 添加头部、尾部后，长按事件需自行处理
     *              -- 多布局时都会回调
     */
    public AbsRecyclerAdapter setOnItemLongClickListener(OnItemLongClickListener onItemLongClickListener) {
        this.onItemLongClickListener = onItemLongClickListener;
        return this;
    }

    public interface OnItemLongClickListener {
        boolean onItemLongClick(View v, int position);
    }

    /**
     * 已添加的Item数量 -- 包含头部、尾部、数据、空页面
     */
    @Override
    public int getItemCount() {
        return getDataCount() + getCountOfHeadGap() + getFooterCount() + getEmptyCount();
    }

    /**
     * 数据源的数量
     */
    public int getDataCount() {
        try {
            return data == null ? 0 : data.size();
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 应该显示的空页面数量 -- 无数据时返回1
     */
    public int getEmptyCount() {
        int size = getDataCount() + getHeaderCount() + getFooterCount();
        return mEmpty != null && size == 0 ? 1 : 0;
    }

    /**
     * 应该显示的空页面数量 -- 无数据时返回1
     * -- 只考虑数据源的数量
     * 默认不调用 若需使用 请重写{@link #getEmptyCount()} 调用该方法
     */
    public int getEmptyCountOfData() {
        return mEmpty != null && getDataCount() == 0 ? 1 : 0;
    }

    /**
     * 应该显示的顶部间隙数量
     */
    public int getGapCount() {
        return hasGap && mGap != null ? 1 : 0;
    }

    public int getHeaderCount() {
        return mHeaderViews.size();
    }

    public int getCountOfHeadGap() {
        return getGapCount() + getHeaderCount();
    }

    public int getFooterCount() {
        return mFooterViews.size();
    }

    public AbsRecyclerAdapter addHeaderView(View header) {
        return addHeaderView(header, -1);
    }

    /**
     * 添加头部 -- index表示添加到第几个 负数表示追加到最后
     */
    public AbsRecyclerAdapter addHeaderView(View header, int index) {
        if (header == null) {
            throw new RuntimeException("Header is null");
        }
        if (index < 0) {
            mHeaderViews.add(header);
        } else {
            mHeaderViews.add(index, header);
        }
        notifyDataSetChanged();
        return this;
    }

    public AbsRecyclerAdapter addHeaderWithHeight(View header, int height) {
        if (header != null && height > 0) {
            header.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, height));
        }
        return addHeaderView(header);
    }

    public AbsRecyclerAdapter addHeaderView(int resId) {
        return addHeaderView(LayoutInflater.from(context).inflate(resId, null));
    }

    public View getHeader() {
        return getHeader(0);
    }

    public View getHeader(int index) {
        if (index < 0) {
            throw new RuntimeException("index is negative");
        }
        if (index >= mHeaderViews.size()) {
            throw new RuntimeException("index out of bounds");
        }
        return mHeaderViews.get(index);
    }

    public void clearHeader() {
        if (getHeaderCount() > 0) {
            mHeaderViews.clear();
            notifyDataSetChanged();
        }
    }

    /**
     * 设置头部的LayoutParams 默认是(MATCH_PARENT, WRAP_CONTENT)
     * 如需加Margin, 或其他, 请重写该方法
     */
    public void setHeaderLayoutParams(View itemView, RecyclerView.LayoutParams lp) {
        itemView.setLayoutParams(lp);
    }

    public AbsRecyclerAdapter addFooterView(View footer) {
        if (footer == null) {
            throw new RuntimeException("Footer is null");
        }
        mFooterViews.add(footer);
        notifyDataSetChanged();
        return this;
    }

    public AbsRecyclerAdapter addFooterView(int resId) {
        return addFooterView(LayoutInflater.from(context).inflate(resId, null));
    }

    public View getFooter() {
        return getFooter(0);
    }

    public View getFooter(int index) {
        if (index < 0) {
            throw new RuntimeException("index is negative");
        }
        if (index >= mFooterViews.size()) {
            throw new RuntimeException("index out of bounds");
        }
        return mFooterViews.get(index);
    }

    public void clearFooter() {
        if (getFooterCount() > 0) {
            mFooterViews.clear();
            notifyDataSetChanged();
        }
    }

    public AbsRecyclerAdapter setEmptyView(View empty) {
        if (empty == null) {
            throw new RuntimeException("Empty is null");
        }
        mEmpty = empty;
        mEmpty.setVisibility(View.GONE);
        setEmptyLayoutParams();
        return this;
    }

    public void setEmptyLayoutParams() {
        mEmpty.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT,
                RecyclerView.LayoutParams.MATCH_PARENT));
    }

    /**
     * 设置空数据提示的高度 -- 仅供外部需要时调用
     * 默认不调用 若需使用 请重写{@link #setEmptyLayoutParams()} 调用该方法
     */
    public void setEmptyHeight(int height) {
        if (mEmpty != null) {
            mEmpty.setLayoutParams(new RecyclerView.LayoutParams(
                    RecyclerView.LayoutParams.MATCH_PARENT,
                    height > 0 ? height : RecyclerView.LayoutParams.MATCH_PARENT));
        }
    }

    public void setEmptyLayoutParams(RecyclerView.LayoutParams lp) {
        if (mEmpty != null) {
            mEmpty.setLayoutParams(lp);
        }
    }

    public AbsRecyclerAdapter setEmptyView(int resId) {
        return setEmptyView(LayoutInflater.from(context).inflate(resId, null));
    }

    public void setEmptyVisible() {
        if (mEmpty == null) {
            return;
        }
        if (mEmpty.getVisibility() != View.VISIBLE) {
            mEmpty.setVisibility(View.VISIBLE);
        }
    }

    public View getEmptyView() {
        return mEmpty;
    }

    public AbsRecyclerAdapter setEmptyBackgroundResource(int resId) {
        if (mEmpty != null) {
            mEmpty.setBackgroundResource(resId);
        }
        return this;
    }

    public AbsRecyclerAdapter setEmptyBackgroundColor(int color) {
        if (mEmpty != null) {
            mEmpty.setBackgroundColor(color);
        }
        return this;
    }

    /**
     * 修改空数据提示中的图片
     */
    public AbsRecyclerAdapter setEmptyImage(int id, int imgId) {
        if (mEmpty != null) {
            View v = mEmpty.findViewById(id);
            if (v instanceof ImageView) {
                ((ImageView) v).setImageResource(imgId);
            }
        }
        return this;
    }

    /**
     * 修改空数据提示中的文字
     */
    public AbsRecyclerAdapter setEmptyTips(int id, String tips) {
        if (mEmpty != null) {
            View v = mEmpty.findViewById(id);
            if (v instanceof TextView) {
                ((TextView) v).setText(tips);
                if (v.getVisibility() != View.VISIBLE) {
                    v.setVisibility(View.VISIBLE);
                }
            }
        }
        return this;
    }

    public AbsRecyclerAdapter setEmptyTips(int id, int stringId) {
        return setEmptyTips(id, getString(stringId));
    }

    /**
     * 修改空数据中是否可见
     */
    public AbsRecyclerAdapter setEmptyVisible(int id, int visible) {
        if (mEmpty != null) {
            View v = mEmpty.findViewById(id);
            if (v != null) {
                v.setVisibility(visible);
            }
        }
        return this;
    }

    /**
     * 空数据提示中给控件设置点击事件
     */
    public AbsRecyclerAdapter setEmptyClickListener(int id, View.OnClickListener listener) {
        if (mEmpty != null) {
            View v = mEmpty.findViewById(id);
            if (v != null) {
                v.setVisibility(View.VISIBLE);
                v.setOnClickListener(listener);
            }
        }
        return this;
    }

    /**
     * 顶部添加透明间隔
     */
    public AbsRecyclerAdapter setTopGapHeight(int topGapHeight) {
        if (mGap == null) {
            mGap = new View(context);
        }
        mGap.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, topGapHeight));
        hasGap = true;
        notifyDataSetChanged();
        return this;
    }

    public AbsRecyclerAdapter setTopGapHeightDp(int dpHeight) {
        return setTopGapHeight(dp(dpHeight));
    }

    public AbsRecyclerAdapter clearTopGap() {
        hasGap = false;
        notifyDataSetChanged();
        return this;
    }

    public void notifyItemChange2(int position) {
        super.notifyItemChanged(position + getCountOfHeadGap());
    }

    public void notifyItemRangeChange2(int positionStart, int itemCount) {
        super.notifyItemRangeChanged(positionStart + getCountOfHeadGap(), itemCount);
    }

    public void notifyItemInsert2(int position) {
        super.notifyItemInserted(position + getCountOfHeadGap());
    }

    public void notifyItemMove2(int fromPosition, int toPosition) {
        super.notifyItemMoved(fromPosition + getCountOfHeadGap(), toPosition + getCountOfHeadGap());
    }

    public void notifyItemRangeInsert2(int positionStart, int itemCount) {
        super.notifyItemRangeInserted(positionStart + getCountOfHeadGap(), itemCount);
    }

    public void notifyItemRemove2(int position) {
        super.notifyItemRemoved(position + getCountOfHeadGap());
    }

    public void notifyItemRangeRemove2(int positionStart, int itemCount) {
        super.notifyItemRangeRemoved(positionStart + getCountOfHeadGap(), itemCount);
    }

    public int getResId(String name, String type) {
        return context.getResources().getIdentifier(name, type, context.getPackageName());
    }

    public int getResId(String name, String type, int zero) {
        int id = getResId(name, type);
        return id == 0 ? zero : id;
    }

    public String getString(int stringId) {
        return getString(stringId, null);
    }

    public String getString(int stringId, String zero) {
        return stringId == 0 ? zero : context.getString(stringId);
    }

    public String formatString(int stringId, Object... args) {
        return String.format(getString(stringId), args);
    }

    public String getStringInResId(String name) {
        return getStringInResId(name, null);
    }

    public String getStringInResId(String name, String zero) {
        int id = getResId(name, "string");
        return id == 0 ? zero : context.getString(id);
    }

    public int getColor(int id) {
        return ContextCompat.getColor(context, id);
    }

    public int dp(float dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                context.getResources().getDisplayMetrics());
    }

    /**
     * 因支持添加头部功能 该方法不能重写 也不能直接在外部调用
     * 使用多布局时 只能重写 {@link #getItemType(T)}
     */
    @Override
    public int getItemViewType(int position) {
        int gap = getGapCount();
        int head = getHeaderCount();
        int empty = getEmptyCount();
        if (position == 0 && gap > 0) {
            return empty > 0 && head == 0 ? TYPE_EMPTY : TYPE_GAP;
        }
        head += gap;
        if (position < head) {//头部
            return TYPE_HEADER - position + gap;
        }
        int nonFoot = head + getDataCount() + empty;//非尾部(头部、数据、空数据)的数量
        if (position >= nonFoot) {//尾部
            return TYPE_FOOTER + nonFoot - position;
        }
        if (empty > 0) {//空数据
            return TYPE_EMPTY;
        }
        return getItemType(data.get(position - head));
    }

    /**
     * 当前position所对应的Item的类型
     * 默认Item的类型为0，多布局时其他Item的类型依次递增
     */
    public int getItemType(T d) {
        return super.getItemViewType(0);
    }

    /**
     * 当前position所对应的Item 是否宽度撑满 -- 默认非0撑满
     * 因支持添加头部功能 该方法不能重写 也不能直接在外部调用
     * 若需使用 只能重写 {@link #isItemFullSpan(int)}
     */
    private boolean isFullSpan(int position) {
        int type = getItemViewType(position);
        if (type == 0) {//默认Item
            return false;
        }
        if (type < 0) {//空数据、头部、尾部 默认撑满
            return true;
        }
        //其他类型
        return isItemFullSpan(type);
    }

    /**
     * 多布局时 类型为type的item 是否宽度撑满 -- 默认非0撑满
     */
    protected boolean isItemFullSpan(int type) {
        return true;
    }

    /**
     * 当LayoutManager为GridLayoutManager 且type非0的Item不撑满时 本方法会被调用
     * 因支持添加头部功能 该方法不能重写 也不能直接在外部调用
     * 若需使用 只能重写 {@link #getItemSpanSize(int)}
     */
    private int getGridSpanSize(int position, int defaultCount) {
        int type = getItemViewType(position);
        if (type < 0) {//空数据、头部、尾部 默认宽度
            return defaultCount;
        }
        //其他类型
        return getItemSpanSize(type);
    }

    /**
     * 多布局时 类型为type的item 所占宽度 -- 默认1个单位
     * 特殊布局时使用 需先重写 {@link #isItemFullSpan(int)} 返回false
     */
    protected int getItemSpanSize(int type) {
        return 1;
    }


    /**
     * 添加StickyHeader时必须重写以下四个方法
     */
    public boolean isStickyGroupEnd(int position) {
        return false;
    }

    public boolean isStickyGroupStart(int position) {
        return false;
    }

    public int getStickyHeight() {
        return 0;
    }

    public String getStickyText(int position) {
        return null;
    }

    @NonNull
    @Override
    public RecyclerHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_GAP) {
            return new RecyclerHolder(mGap);
        }
        if (viewType == TYPE_EMPTY) {
            return new RecyclerHolder(mEmpty);
        }
        if (viewType <= TYPE_FOOTER) {
            return new RecyclerHolder(mFooterViews.get(TYPE_FOOTER - viewType));
        }
        if (viewType <= TYPE_HEADER) {
            return new RecyclerHolder(mHeaderViews.get(TYPE_HEADER - viewType));
        }
        View v = LayoutInflater.from(context).inflate(resId[viewType], parent, false);
        return new RecyclerHolder(v);
    }

    /**
     * 因支持添加头部功能 该方法不能重写 只能重写 {@link #onBindHolder(RecyclerHolder, Object, int)}
     */
    @Override
    public void onBindViewHolder(@NonNull RecyclerHolder holder, int position) {
        if (getItemViewType(position) < 0) {
            return;
        }
        int realPosition = position - getCountOfHeadGap();
        onBindHolder(holder, data.get(realPosition), realPosition);
    }


    /**
     * 配置数据
     * 如需给整条Item setTag()，请调用 {@link RecyclerHolder#setItemTag(int, Object)}
     */
    public abstract void onBindHolder(RecyclerHolder holder, T d, int position);

    /**
     * GridLayoutManager时 设置type不为0的item 占整行位置
     */
    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        RecyclerView.LayoutManager manager = recyclerView.getLayoutManager();
        if(manager instanceof GridLayoutManager) {
            final GridLayoutManager gridManager = ((GridLayoutManager) manager);
            gridManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    return isFullSpan(position) ? gridManager.getSpanCount() :
                            getGridSpanSize(position, gridManager.getSpanCount());
                }
            });
        }
    }

    /**
     * StaggeredGridLayoutManager时 设置type不为0的item 占整行位置
     */
    @Override
    public void onViewAttachedToWindow(@NonNull RecyclerHolder holder) {
        super.onViewAttachedToWindow(holder);
        ViewGroup.LayoutParams llp = holder.layoutView.getLayoutParams();
        if (llp instanceof RecyclerView.LayoutParams) {
            RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) llp;
            if (lp instanceof StaggeredGridLayoutManager.LayoutParams) {
                StaggeredGridLayoutManager.LayoutParams lpStaggered = (StaggeredGridLayoutManager.LayoutParams) lp;
                lpStaggered.setFullSpan(holder.getParent().isFullSpan(holder.getLayoutPosition()));
            } else if (holder.getItemViewType() < 0) {//LinearLayoutManager时 头部item撑满
                lp.width = RecyclerView.LayoutParams.MATCH_PARENT;
                setHeaderLayoutParams(holder.layoutView, lp);
            }
        }
    }


    public class RecyclerHolder extends RecyclerView.ViewHolder {

        Map<Integer, View> cacheMap = new HashMap<>();
        View layoutView;
        long millis;

        public RecyclerHolder(View itemView) {
            super(itemView);
            if(onItemClickListener != null) {
                itemView.setOnClickListener(v -> {
                    if (getItemViewType() >= 0) {
                        onItemClickListener.onItemClick(v, getLayoutPosition() - getCountOfHeadGap());
                    }
                });
            }
            if (onItemLongClickListener != null) {
                itemView.setOnLongClickListener(v -> {
                    if (getItemViewType() >= 0) {
                        return onItemLongClickListener.onItemLongClick(v, getLayoutPosition() - getCountOfHeadGap());
                    }
                    return true;
                });
            }
            this.layoutView = itemView;
        }

        private AbsRecyclerAdapter getParent() {
            return AbsRecyclerAdapter.this;
        }

        public View getView(int id) {
            if(cacheMap.containsKey(id)) {
                return cacheMap.get(id);
            } else {
                View v = layoutView.findViewById(id);
                cacheMap.put(id, v);
                return v;
            }
        }

        public TextView getTextView(int id) {
            return (TextView) getView(id);
        }

        public boolean isLastItem() {
            return getLayoutPosition() - getCountOfHeadGap() - getEmptyCount() == data.size() - 1;
        }

        public RecyclerHolder setItemLayoutParams(int width, int height) {
            RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) layoutView.getLayoutParams();
            lp.width = width;
            lp.height = height;
            layoutView.setLayoutParams(lp);
            return this;
        }

        public RecyclerHolder setItemMargins(int left, int top, int right, int bottom) {
            RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) layoutView.getLayoutParams();
            lp.setMargins(left, top, right, bottom);
            layoutView.setLayoutParams(lp);
            return this;
        }

        /**
         * 给整条item设置tag
         */
        public RecyclerHolder setItemTag(int tagId, Object tag) {
            layoutView.setTag(tagId, tag);
            return this;
        }

        public RecyclerHolder setMinimumHeight(int minHeight) {
            layoutView.setMinimumHeight(minHeight);
            return this;
        }

        public RecyclerHolder setItemSelected(boolean selected) {
            layoutView.setSelected(selected);
            return this;
        }

        /**
         * 给Item中控件设置点击事件，前提：需先设置 {@link #setOnItemClickListener(OnItemClickListener)}
         */
        public RecyclerHolder setOnClickListener(int id) {
            getView(id).setOnClickListener(v -> {
                if (millis <= 0) {
                    millis = System.currentTimeMillis();
                } else {
                    if (System.currentTimeMillis() - millis < 1000) {
                        return;
                    } else {
                        millis = System.currentTimeMillis();
                    }
                }
                if(onItemClickListener != null) {
                    if (getItemViewType() >= 0) {
                        onItemClickListener.onItemClick(v,
                                getLayoutPosition() - getCountOfHeadGap());
                    }
                }
            });
            return this;
        }

        public RecyclerHolder setOnClickListenerAndTag(int id, Object tag) {
            getView(id).setTag(tag);
            return setOnClickListener(id);
        }

        public RecyclerHolder setOnClickListenerAndTag(int id, int tagId, Object tag) {
            getView(id).setTag(tagId, tag);
            return setOnClickListener(id);
        }

        public RecyclerHolder setOnClickListenerAndAlpha(int id, float alpha) {
            getView(id).setAlpha(alpha);
            return setOnClickListener(id);
        }

        public RecyclerHolder setOnClickListenerAndAlphaP5(int id, boolean isP5) {
            return setOnClickListenerAndAlpha(id, isP5 ? 0.5F : 1);
        }

        public RecyclerHolder setViewLayoutParams(int id, int width, int height) {
            View v = getView(id);
            ViewGroup.LayoutParams lp = v.getLayoutParams();
            lp.width = width;
            lp.height = height;
            v.setLayoutParams(lp);
            return this;
        }

        /**
         * 给Item中控件设置Tag
         */
        public RecyclerHolder setViewTag(int id, int tagId, Object tag) {
            getView(id).setTag(tagId, tag);
            return this;
        }

        public RecyclerHolder setViewTag(int id, Object tag) {
            getView(id).setTag(tag);
            return this;
        }

        public RecyclerHolder setViewAlpha(int id, float alpha) {
            getView(id).setAlpha(Math.max(0, Math.min(1, alpha)));
            return this;
        }

        public RecyclerHolder setViewVisible(int id, int visible) {
            getView(id).setVisibility(visible);
            return this;
        }

        public RecyclerHolder setViewVisible(int id, boolean visible) {
            return setViewVisible(id, visible ? View.VISIBLE : View.GONE);
        }

        public RecyclerHolder setViewVisibleWithClickListener(int id, int visible) {
            getView(id).setVisibility(visible);
            return setOnClickListener(id);
        }

        public RecyclerHolder setViewVisibleWithClickListener(int id, boolean visible) {
            return setViewVisibleWithClickListener(id, visible ? View.VISIBLE : View.GONE);
        }

        public RecyclerHolder setViewSelected(int id, boolean selected) {
            getView(id).setSelected(selected);
            return this;
        }

        public RecyclerHolder setViewSelectedWithClickListener(int id, boolean selected) {
            setViewSelected(id, selected);
            return setOnClickListener(id);
        }

        public RecyclerHolder setViewBackgroundColor(int id, int color) {
            getView(id).setBackgroundColor(color);
            return this;
        }

        public RecyclerHolder setViewBackgroundResource(int id, int resId) {
            getView(id).setBackgroundResource(resId);
            return this;
        }

        /**
         * 绑定TextView
         */
        public RecyclerHolder bindTextView(int id, String text) {
            ((TextView) getView(id)).setText(text);
            return this;
        }

        public RecyclerHolder bindTextView(int id, int textId) {
            return bindTextView(id, getString(textId));
        }

        public RecyclerHolder bindTextViewWithBg(int id, String text, int resId) {
            TextView tv = (TextView) getView(id);
            setTextWithHtml(tv, text);
            tv.setBackgroundResource(resId);
            return this;
        }

        public RecyclerHolder bindTextViewWithBgAndColor(int id, String text, int resId, int color) {
            TextView tv = (TextView) getView(id);
            tv.setTextColor(color);
            setTextWithHtml(tv, text);
            tv.setBackgroundResource(resId);
            return this;
        }

        public RecyclerHolder bindTextViewWithBg(int id, int textId, int resId) {
            return bindTextViewWithBg(id, getString(textId), resId);
        }

        public RecyclerHolder bindTextViewWithClickListener(int id, String text) {
            bindTextViewWithHtml(id, text);
            return setOnClickListener(id);
        }

        public RecyclerHolder bindTextViewWithClickListener(int id, int textId) {
            return bindTextViewWithClickListener(id, getString(textId));
        }

        public RecyclerHolder bindTextViewWithColor(int id, String text, int color) {
            TextView tv = (TextView) getView(id);
            tv.setText(text);
            tv.setTextColor(color);
            return this;
        }

        public RecyclerHolder bindTextViewWithColorId(int id, String text, int colorId) {
            return bindTextViewWithColor(id, text, getColor(colorId));
        }

        public RecyclerHolder bindTextViewWithSelected(int id, String text, boolean isSelect) {
            bindTextViewWithHtml(id, text);
            return setViewSelected(id, isSelect);
        }

        public RecyclerHolder bindTextViewWithVisible(int id, String text, int visible) {
            bindTextViewWithHtml(id, text);
            return setViewVisible(id, visible);
        }

        public RecyclerHolder bindTextViewWithVisible(int id, String text, boolean visible) {
            return bindTextViewWithVisible(id, text, visible ? View.VISIBLE : View.GONE);
        }

        /**
         * 绑定TextView 含html标签
         */
        public RecyclerHolder bindTextViewWithHtml(int id, String text) {
            setTextWithHtml((TextView) getView(id), text);
            return this;
        }

        private void setTextWithHtml(TextView tv, String text) {
            if (TextUtils.isEmpty(text)) {
                return;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                tv.setText(Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY));
            } else {
                tv.setText(Html.fromHtml(text));
            }
        }

        public RecyclerHolder bindImageView(int id, int imgId) {
            ((ImageView) getView(id)).setImageResource(imgId);
            return this;
        }

        public RecyclerHolder bindImageView(int id, int imgId, int zero) {
            ((ImageView) getView(id)).setImageResource(imgId == 0 ? zero : imgId);
            return this;
        }

        public RecyclerHolder bindImageView(int id, Bitmap bm) {
            ((ImageView) getView(id)).setImageBitmap(bm);
            return this;
        }

        public RecyclerHolder bindImageViewGlide(int id, int imgId) {
            Glide.with(context)
                    .load(imgId)
                    .into((ImageView) getView(id));
            return this;
        }

        public RecyclerHolder bindImageViewGlide(int id, int imgId, RequestOptions ro) {
            Glide.with(context)
                    .load(imgId)
                    .apply(ro)
                    .into((ImageView) getView(id));
            return this;
        }

        /**
         * URL
         */
        public RecyclerHolder bindImageViewGlide(int id, String url) {
            Glide.with(context)
                    .load(url)
                    .into((ImageView) getView(id));
            return this;
        }

        public RecyclerHolder bindImageViewGlidePE(int id, String url, int placeId, int errorId) {
            RequestOptions ro = new RequestOptions()
                    .placeholder(placeId)
                    .error(errorId);
            return bindImageViewGlide(id, url, ro);
        }

        public RecyclerHolder bindImageViewGlidePE(int id, String url, int placeId, int errorId, int corner) {
            RequestOptions ro = RequestOptions.bitmapTransform(new GlideRoundTransform(corner))
                    .placeholder(placeId)
                    .error(errorId);
            return bindImageViewGlide(id, url, ro);
        }

        /**
         * 只缓存原始图片 -- 用来使图片宽度撑满高度自适应
         */
        public RecyclerHolder bindImageViewGlideData(int id, String url) {
            RequestOptions ro = new RequestOptions()
                    .diskCacheStrategy(DiskCacheStrategy.DATA);
            return bindImageViewGlide(id, url, ro);
        }

        public RecyclerHolder bindImageViewGlide(int id, String url, RequestOptions ro) {
            Glide.with(context)
                    .load(url)
                    .apply(ro)
                    .into((ImageView) getView(id));
            return this;
        }

        public RecyclerHolder bindImageViewGlideCircle(int id, String url) {
            RequestOptions ro = RequestOptions.bitmapTransform(new CircleCrop());
            return bindImageViewGlide(id, url, ro);
        }

        public RecyclerHolder bindImageViewGlideCirclePE(int id, String url, int placeId, int errorId) {
            RequestOptions ro = RequestOptions.bitmapTransform(new CircleCrop())
                    .placeholder(placeId)
                    .error(errorId);
            return bindImageViewGlide(id, url, ro);
        }

        /**
         * Bitmap
         */
        public RecyclerHolder bindImageViewGlide(int id, Bitmap bm) {
            Glide.with(context)
                    .load(BitMapUtil.Bitmap2Bytes(bm))
                    .into((ImageView) getView(id));
            return this;
        }



        public RecyclerHolder bindImageViewGlidePE(int id, Bitmap bm, int placeId, int errorId) {
            RequestOptions ro = new RequestOptions()
                    .placeholder(placeId)
                    .error(errorId);
            Glide.with(context)
                    .load(BitMapUtil.Bitmap2Bytes(bm))
                    .apply(ro)
                    .into((ImageView) getView(id));
            return this;
        }

        /**
         * 设置进度条进度
         */
        public RecyclerHolder bindProgressBar(int id, int num) {
            ((ProgressBar) getView(id)).setProgress(num);
            return this;
        }

        /**
         * 设置星星进度条
         */
        public RecyclerHolder bindRatingBar(int id, float rating) {
            ((RatingBar) getView(id)).setRating(rating);
            return this;
        }

    }

}
