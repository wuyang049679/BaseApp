package com.wuyang.baseapp.wight.stateview;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.IntDef;
import androidx.annotation.LayoutRes;
import androidx.core.view.NestedScrollingChild;
import androidx.core.view.NestedScrollingParent;
import androidx.core.view.ScrollingView;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
import com.wuyang.baseapp.R;
import com.wuyang.baseapp.utils.Constant;
import com.wuyang.baseapp.utils.UriUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import io.reactivex.annotations.NonNull;

/**
 * StateView is an invisible, zero-sized View that can be used
 * to lazily inflate loadingView/emptyView/retryView at runtime.
 *
 * @author Nukc
 *         https://github.com/nukc
 */
public class StateView extends View {

    private TextView emptyTv;
    private TextView diyTv;

    @IntDef({EMPTY, RETRY, LOADING, COMMENT, NET_ERROR,DIY})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ViewType {
    }

    public static final int EMPTY = 0x00000000;
    public static final int RETRY = 0x00000001;
    public static final int LOADING = 0x00000002;
    public static final int COMMENT = 0x00000003;
    public static final int NET_ERROR = 0x00000004;
    public static final int DIY = 0x00000005;

    private int mEmptyResource;
    private int mRetryResource;
    private int mLoadingResource;
    private int mConmmentResource;
    private int mNetResource;
    private int mDiyResource;

    private View mEmptyView;
    private View mRetryView;
    private View mLoadingView;
    private View mCommentView;
    private View mNetView;
    private View mDiyView;

    private LayoutInflater mInflater;
    private OnRetryClickListener mRetryClickListener;
    private OnInflateListener mInflateListener;

    private RelativeLayout.LayoutParams mLayoutParams;
    private AnimatorProvider mProvider = null;

    /**
     * 注入到 activity 中
     *
     * @param activity Activity
     * @return StateView
     */
    public static StateView inject(@NonNull Activity activity) {
        return inject(activity, false);
    }

    /**
     * 注入到 activity 中
     *
     * @param activity     Activity
     * @param hasActionBar 是否有 actionbar/toolbar
     * @return StateView
     */
    public static StateView inject(@NonNull Activity activity, boolean hasActionBar) {
        ViewGroup rootView = (ViewGroup) activity.getWindow().getDecorView().findViewById(android.R.id.content);
        return inject(rootView, hasActionBar);
    }

    /**
     * 注入到 ViewGroup 中
     *
     * @param parent extends ViewGroup
     * @return StateView
     */
    public static StateView inject(@NonNull ViewGroup parent) {
        return inject(parent, false);
    }

    /**
     * 注入到 ViewGroup 中
     *
     * @param parent       extends ViewGroup
     * @param hasActionBar 是否有 actionbar/toolbar,
     *                     true: 会 setMargin top, margin 大小是 actionbarSize
     *                     false: not set
     * @return StateView
     */
    public static StateView inject(@NonNull ViewGroup parent, boolean hasActionBar) {
        // 因为 LinearLayout/ScrollView/AdapterView 的特性
        // 为了 StateView 能正常显示，自动再套一层（开发的时候就不用额外的工作量了）
        // SwipeRefreshLayout/NestedScrollView
        int screenHeight = 0;
        if (parent instanceof LinearLayout ||
                parent instanceof ScrollView ||
                parent instanceof AdapterView ||
                (parent instanceof NestedScrollingParent && parent instanceof NestedScrollingChild)) {
            ViewParent viewParent = parent.getParent();
            if (viewParent == null) {
                // create a new FrameLayout to wrap StateView and parent's childView
                FrameLayout wrapper = new FrameLayout(parent.getContext());
                ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                wrapper.setLayoutParams(layoutParams);

                if (parent instanceof LinearLayout) {
                    // create a new LinearLayout to wrap parent's childView
                    LinearLayout wrapLayout = new LinearLayout(parent.getContext());
                    wrapLayout.setLayoutParams(parent.getLayoutParams());
                    wrapLayout.setOrientation(((LinearLayout) parent).getOrientation());

                    for (int i = 0, childCount = parent.getChildCount(); i < childCount; i++) {
                        View childView = parent.getChildAt(0);
                        parent.removeView(childView);
                        wrapLayout.addView(childView);
                    }
                    wrapper.addView(wrapLayout);
                } else if (parent instanceof ScrollView || parent instanceof ScrollingView) {
                    // not recommended to inject Scrollview/NestedScrollView
                    if (parent.getChildCount() != 1) {
                        throw new IllegalStateException("the ScrollView does not have one direct child");
                    }
                    View directView = parent.getChildAt(0);
                    parent.removeView(directView);
                    wrapper.addView(directView);

                    WindowManager wm = (WindowManager) parent.getContext()
                            .getSystemService(Context.WINDOW_SERVICE);
                    DisplayMetrics metrics = new DisplayMetrics();
                    wm.getDefaultDisplay().getMetrics(metrics);
                    screenHeight = metrics.heightPixels;
                } else if (parent instanceof NestedScrollingParent &&
                        parent instanceof NestedScrollingChild) {
                    if (parent.getChildCount() == 2) {
                        View targetView = parent.getChildAt(1);
                        parent.removeView(targetView);
                        wrapper.addView(targetView);
                    } else if (parent.getChildCount() > 2) {
                        throw new IllegalStateException("the view is not refresh layout? view = "
                                + parent.toString());
                    }
                } else {
                    throw new IllegalStateException("the view does not have parent, view = "
                            + parent.toString());
                }
                // parent add wrapper
                parent.addView(wrapper);
                // StateView will be added to wrapper
                parent = wrapper;
            } else {
                FrameLayout root = new FrameLayout(parent.getContext());
                root.setLayoutParams(parent.getLayoutParams());
                ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                parent.setLayoutParams(layoutParams);

                if (viewParent instanceof ViewGroup) {
                    ViewGroup rootGroup = (ViewGroup) viewParent;
                    // 把 parent 从它自己的父容器中移除
                    rootGroup.removeView(parent);
                    // 然后替换成新的
                    rootGroup.addView(root);
                }
                root.addView(parent);
                parent = root;
            }
        }
        StateView stateView = new StateView(parent.getContext());
        if (screenHeight > 0) {
            // let StateView be shown in the center
            ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    hasActionBar ? screenHeight - stateView.getActionBarHeight() : screenHeight);
            parent.addView(stateView, params);
        } else {
            parent.addView(stateView);
        }
        if (hasActionBar) {
            stateView.setTopMargin();
        }
        return stateView;
    }

    /**
     * 注入到 View 中
     *
     * @param view instanceof ViewGroup
     * @return StateView
     */
    public static StateView inject(@NonNull View view) {
        return inject(view, false);
    }

    /**
     * 注入到 View 中
     *
     * @param view         instanceof ViewGroup
     * @param hasActionBar 是否有 actionbar/toolbar
     * @return StateView
     */
    public static StateView inject(@NonNull View view, boolean hasActionBar) {
        if (view instanceof ViewGroup) {
            ViewGroup parent = (ViewGroup) view;
            return inject(parent, hasActionBar);
        } else {
            ViewParent parent = view.getParent();
            if (parent instanceof ViewGroup) {
                return inject((ViewGroup) parent, hasActionBar);
            } else {
                throw new ClassCastException("view or view.getParent() must be ViewGroup");
            }
        }
    }

    public StateView(Context context) {
        this(context, null);
    }

    public StateView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StateView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.StateView);
        mEmptyResource = a.getResourceId(R.styleable.StateView_emptyResource, 0);
        mRetryResource = a.getResourceId(R.styleable.StateView_retryResource, 0);
        mLoadingResource = a.getResourceId(R.styleable.StateView_loadingResource, 0);
        mConmmentResource = a.getResourceId(R.styleable.StateView_conmmentResource, 0);
        mNetResource = a.getResourceId(R.styleable.StateView_neterrorResource, 0);
        mDiyResource = a.getResourceId(R.styleable.StateView_diyResource, 0);
        a.recycle();

        if (mEmptyResource == 0) {
            mEmptyResource = R.layout.base_empty;
        }
        if (mRetryResource == 0) {
            mRetryResource = R.layout.base_retry;
        }
        if (mLoadingResource == 0) {
            mLoadingResource = R.layout.base_loading;
        }

        if (mConmmentResource == 0) {
            mConmmentResource = R.layout.base_comment_null;
        }

        if (mNetResource == 0) {
            mNetResource = R.layout.base_neterror;
        }

        if (mDiyResource == 0){
            mDiyResource = R.layout.base_diy_layout;
        }

        if (attrs == null) {
            mLayoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
        } else {
            mLayoutParams = new RelativeLayout.LayoutParams(context, attrs);
        }

        setVisibility(GONE);
        setWillNotDraw(true);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(0, 0);
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void draw(Canvas canvas) {
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
    }

    @Override
    public void setVisibility(int visibility) {
        setVisibility(mEmptyView, visibility);
        setVisibility(mRetryView, visibility);
        setVisibility(mLoadingView, visibility);
        setVisibility(mCommentView, visibility);
        setVisibility(mNetView, visibility);
        setVisibility(mDiyView, visibility);
    }

    private void setVisibility(View view, int visibility) {
        if (view != null && visibility != view.getVisibility()) {
            if (mProvider != null) {
                startAnimation(view);
            } else {
                view.setVisibility(visibility);
            }
        }
    }

    public void showContent() {
        setVisibility(GONE);
    }

    public View showEmpty(String message) {
        if (mEmptyView == null) {
            mEmptyView = inflate(mEmptyResource, EMPTY);
            emptyTv = mEmptyView.findViewById(R.id.empty_message);
        }
        emptyTv.setText(message);
        showView(mEmptyView);
        return mEmptyView;
    }

    public View showDiyView(int imgRes, String message){
        if (mDiyView == null) {
            mDiyView = inflate(mDiyResource,DIY);
            diyTv = mDiyView.findViewById(R.id.state_diy_tv);
            SimpleDraweeView stateImg = mDiyView.findViewById(R.id.state_diy_img);
            stateImg.setController(getDiyController(mDiyView.getContext(),imgRes));
        }
        Log.i("zq","showDiyView" + message+"_____"+imgRes);
        diyTv.setText(message);
        showView(mDiyView);
        return mDiyView;
    }

    private DraweeController getDiyController(Context context, int imgRes) {
        //更改加载中动图的资源
        Uri uri = UriUtils.getUri(Constant.LOCALRESOURCE, imgRes, context);
        //特殊注明,draweeController是不可以被多个drawview同时使用的,因为使用过一次就会被回收了
        return Fresco.newDraweeControllerBuilder()
                .setUri(uri)
                .setAutoPlayAnimations(true) // 设置加载图片完成后是否直接进行播放
                .build();
    }

    public View showCommentEmpty(){
        if (mCommentView == null) {
            mCommentView = inflate(mConmmentResource, COMMENT);
        }
        showView(mCommentView);
        return mCommentView;
    }

    public View showNetError(){
        if (mNetView == null) {
            mNetView = inflate(mNetResource, NET_ERROR);
        }
        showView(mNetView);
        return mNetView;
    }

    public View showRetry() {
        if (mRetryView == null) {
            mRetryView = inflate(mRetryResource, RETRY);
            mRetryView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mRetryClickListener != null) {
//                        showLoading();
                        mRetryView.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                mRetryClickListener.onRetryClick();
                            }
                        }, 200);
                    }
                }
            });
        }
        showView(mRetryView);
        return mRetryView;
    }

    public View showLoading() {
        if (mLoadingView == null) {
            mLoadingView = inflate(mLoadingResource, LOADING);
            //找到动图控件
            SimpleDraweeView stateLoadingGif = mLoadingView.findViewById(R.id.state_loading);
            //设置动图控制器
            stateLoadingGif.setController(getDraweeController(mLoadingView.getContext()));
        }
        showView(mLoadingView);
        return mLoadingView;
    }

    /**
     * 获取DraweeController对象
     *
     * @param context
     * @return
     */
    private DraweeController getDraweeController(Context context) {
        //更改加载中动图的资源
        Uri uri = UriUtils.getUri(Constant.LOCALRESOURCE, R.drawable.loading, context);
        //特殊注明,draweeController是不可以被多个drawview同时使用的,因为使用过一次就会被回收了
        return Fresco.newDraweeControllerBuilder()
                .setUri(uri)
                .setAutoPlayAnimations(true) // 设置加载图片完成后是否直接进行播放
                .build();
    }

    /**
     * show the state view
     */
    private void showView(View view) {
        setVisibility(view, VISIBLE);
        hideViews(view);
    }

    /**
     * hide other views after show view
     */
    private void hideViews(View showView) {
        if (mEmptyView == showView) {
            setVisibility(mLoadingView, GONE);
            setVisibility(mRetryView, GONE);
            setVisibility(mCommentView, GONE);
            setVisibility(mNetView, GONE);
            setVisibility(mDiyView, GONE);
        } else if (mLoadingView == showView) {
            setVisibility(mEmptyView, GONE);
            setVisibility(mRetryView, GONE);
            setVisibility(mCommentView, GONE);
            setVisibility(mNetView, GONE);
            setVisibility(mDiyView, GONE);
        } else if (mRetryView == showView){
            setVisibility(mEmptyView, GONE);
            setVisibility(mLoadingView, GONE);
            setVisibility(mCommentView, GONE);
            setVisibility(mNetView, GONE);
            setVisibility(mDiyView, GONE);
        } else if (mCommentView == showView){
            setVisibility(mEmptyView, GONE);
            setVisibility(mLoadingView, GONE);
            setVisibility(mRetryView, GONE);
            setVisibility(mNetView, GONE);
            setVisibility(mDiyView, GONE);
        } else if (mNetView == showView){
            setVisibility(mEmptyView, GONE);
            setVisibility(mLoadingView, GONE);
            setVisibility(mRetryView, GONE);
            setVisibility(mCommentView, GONE);
            setVisibility(mDiyView, GONE);
        } else if (mDiyView == showView){
            setVisibility(mEmptyView, GONE);
            setVisibility(mLoadingView, GONE);
            setVisibility(mRetryView, GONE);
            setVisibility(mCommentView, GONE);
            setVisibility(mNetView, GONE);
        }
    }

    private void startAnimation(final View view) {
        final boolean toShow = view.getVisibility() == GONE;
        Animator animator = toShow ? mProvider.showAnimation(view) : mProvider.hideAnimation(view);
        if (animator == null) {
            view.setVisibility(toShow ? VISIBLE : GONE);
            return;
        }
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if (!toShow) {
                    view.setVisibility(GONE);
                }
            }

            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                if (toShow) {
                    view.setVisibility(VISIBLE);
                }
            }
        });
        animator.start();
    }

    /**
     * provider default is null
     *
     * @param provider {@link AnimatorProvider}
     */
    public void setAnimatorProvider(AnimatorProvider provider) {
        this.mProvider = provider;
        reset(mEmptyView);
        reset(mLoadingView);
        reset(mRetryView);
        reset(mNetView);
        reset(mCommentView);
        reset(mDiyView);
    }

    /**
     * reset view's property
     * 不然多次 setAnimatorProvider 后视图动画会混乱
     */
    private void reset(View view) {
        if (view != null) {
            view.setTranslationX(0f);
            view.setTranslationY(0f);
            view.setAlpha(1f);
            view.setRotation(0f);
            view.setScaleX(1f);
            view.setScaleY(1f);
        }
    }

    private View inflate(@LayoutRes int layoutResource, @ViewType int viewType) {
        final ViewParent viewParent = getParent();

        if (viewParent != null && viewParent instanceof ViewGroup) {
            if (layoutResource != 0) {
                final ViewGroup parent = (ViewGroup) viewParent;
                final LayoutInflater factory;
                if (mInflater != null) {
                    factory = mInflater;
                } else {
                    factory = LayoutInflater.from(getContext());
                }
                final View view = factory.inflate(layoutResource, parent, false);

                final int index = parent.indexOfChild(this);
                // 防止还能触摸底下的 View
                view.setClickable(true);
                // 先不显示
                view.setVisibility(GONE);

                final ViewGroup.LayoutParams layoutParams = getLayoutParams();
                if (layoutParams != null) {
                    if (parent instanceof RelativeLayout) {
                        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) layoutParams;
                        mLayoutParams.setMargins(lp.leftMargin, lp.topMargin,
                                lp.rightMargin, lp.bottomMargin);

                        parent.addView(view, index, mLayoutParams);
                    } else {
                        parent.addView(view, index, layoutParams);
                    }
                } else {
                    parent.addView(view, index);
                }

                if (mLoadingView != null && mRetryView != null && mEmptyView != null && mNetView!= null && mCommentView != null) {
                    parent.removeViewInLayout(this);
                }

                if (mInflateListener != null) {
                    mInflateListener.onInflate(viewType, view);
                }

                return view;
            } else {
                throw new IllegalArgumentException("StateView must have a valid layoutResource");
            }
        } else {
            throw new IllegalStateException("StateView must have a non-null ViewGroup viewParent");
        }
    }

    /**
     * 设置 topMargin, 当有 actionbar/toolbar 的时候
     */
    public void setTopMargin() {
        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) getLayoutParams();
        layoutParams.topMargin = getActionBarHeight();
    }

    /**
     * @return actionBarSize
     */
    public int getActionBarHeight() {
        int height = 0;
        TypedValue tv = new TypedValue();
        if (getContext().getTheme().resolveAttribute(R.attr.actionBarSize, tv, true)) {
            height = TypedValue.complexToDimensionPixelSize(tv.data, getResources().getDisplayMetrics());
        }
        return height;
    }

    /**
     * 设置 emptyView 的自定义 Layout
     *
     * @param emptyResource emptyView 的 layoutResource
     */
    public void setEmptyResource(@LayoutRes int emptyResource) {
        this.mEmptyResource = emptyResource;
    }

    /**
     * 设置 retryView 的自定义 Layout
     *
     * @param retryResource retryView 的 layoutResource
     */
    public void setRetryResource(@LayoutRes int retryResource) {
        this.mRetryResource = retryResource;
    }

    /**
     * 设置 loadingView 的自定义 Layout
     *
     * @param loadingResource loadingView 的 layoutResource
     */
    public void setLoadingResource(@LayoutRes int loadingResource) {
        mLoadingResource = loadingResource;
    }

    /**
     * Get current {@link LayoutInflater} used in {@link #inflate(int, int)}.
     */
    public LayoutInflater getInflater() {
        return mInflater;
    }

    /**
     * Set {@link LayoutInflater} to use in {@link #inflate(int, int)}, or {@code null}
     * to use the default.
     */
    public void setInflater(LayoutInflater inflater) {
        this.mInflater = inflater;
    }

    /**
     * 监听重试
     *
     * @param listener {@link OnRetryClickListener}
     */
    public void setOnRetryClickListener(OnRetryClickListener listener) {
        this.mRetryClickListener = listener;
    }

    /**
     * Listener used to receive a notification after the RetryView is clicked.
     */
    public interface OnRetryClickListener {
        void onRetryClick();
    }

    /**
     * Specifies the inflate listener to be notified after this StateView successfully
     * inflated its layout resource.
     *
     * @param inflateListener The OnInflateListener to notify of successful inflation.
     * @see OnInflateListener
     */
    public void setOnInflateListener(OnInflateListener inflateListener) {
        mInflateListener = inflateListener;
    }

    /**
     * Listener used to receive a notification after a StateView has successfully
     * inflated its layout resource.
     *
     * @see StateView#setOnInflateListener(OnInflateListener)
     */
    public interface OnInflateListener {
        /**
         * @param view The inflated View.
         */
        void onInflate(@ViewType int viewType, View view);
    }
}
