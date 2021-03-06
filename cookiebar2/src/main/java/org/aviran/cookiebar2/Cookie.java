package org.aviran.cookiebar2;

import android.animation.Animator;
import android.content.Context;
import android.graphics.Color;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

final class Cookie extends FrameLayout implements View.OnTouchListener {

    private long slideOutAnimationDuration = 500;
    private Animation slideOutAnimation;

    private ViewGroup layoutCookie;
    private TextView tvTitle;
    private TextView tvMessage;
    private ImageView ivIcon;
    private TextView btnAction;
    private long duration = 2000;
    private int layoutGravity = Gravity.BOTTOM;
    private float initialDragX;
    private float initialDragY;
    private float dismissOffsetThresholdH;
    private float dismissOffsetThresholdV;
    private float viewWidth;
    private float viewHeight;
    private boolean swipedOut;


    public Cookie(@NonNull final Context context) {
        this(context, null);
    }

    public Cookie(@NonNull final Context context, @Nullable final AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Cookie(@NonNull final Context context, @Nullable final AttributeSet attrs,
                  final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public int getLayoutGravity() {
        return layoutGravity;
    }

    private void initViews(View rootView) {

        if (rootView != null) {
            addView(rootView, new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
        } else {
            inflate(getContext(), R.layout.layout_cookie, this);
        }

        layoutCookie = findViewById(R.id.cookie);
        tvTitle = findViewById(R.id.tv_title);
        tvMessage = findViewById(R.id.tv_message);
        ivIcon = findViewById(R.id.iv_icon);
        btnAction = findViewById(R.id.btn_action);

        validateLayoutIntegrity();
        initDefaultStyle(getContext());
        layoutCookie.setOnTouchListener(this);
    }

    private void validateLayoutIntegrity() {
        if (layoutCookie == null || tvTitle == null || tvMessage == null ||
                ivIcon == null || btnAction == null) {

            throw new RuntimeException("Your custom cookie view is missing one of the default required views");
        }
    }


    /**
     * Init the default text color or background color. You can change the default style by set the
     * Theme's attributes.
     * <p>
     * <pre>
     *  <style name="AppTheme" parent="Theme.AppCompat.Light.DarkActionBar">
     *          <item name="cookieTitleColor">@color/default_title_color</item>
     *          <item name="cookieMessageColor">@color/default_message_color</item>
     *          <item name="cookieActionColor">@color/default_action_color</item>
     *          <item name="cookieBackgroundColor">@color/default_bg_color</item>
     *  </style>
     * </pre>
     */
    private void initDefaultStyle(Context context) {
        //Custom the default style of a cookie
        int titleColor = ThemeResolver.getColor(context, R.attr.cookieTitleColor, Color.WHITE);
        int messageColor = ThemeResolver.getColor(context, R.attr.cookieMessageColor, Color.WHITE);
        int actionColor = ThemeResolver.getColor(context, R.attr.cookieActionColor, Color.WHITE);
        int backgroundColor = ThemeResolver.getColor(context, R.attr.cookieBackgroundColor,
                ContextCompat.getColor(context, R.color.default_bg_color));

        tvTitle.setTextColor(titleColor);
        tvMessage.setTextColor(messageColor);
        btnAction.setTextColor(actionColor);
        layoutCookie.setBackgroundColor(backgroundColor);
    }

    public void setParams(final CookieBar.Params params) {
        initViews(params.customView);

        duration = params.duration;
        layoutGravity = params.layoutGravity;

        float scale = getContext().getResources().getDisplayMetrics().density;

        //Icon
        if (params.iconResId != 0) {
            ivIcon.setVisibility(VISIBLE);
            ivIcon.setBackgroundResource(params.iconResId);
            if (params.iconAnimator != null) {
                params.iconAnimator.setTarget(ivIcon);
                params.iconAnimator.start();

            }
        }

        //Title
        int div = 0;
        if (!TextUtils.isEmpty(params.title)) {
            div = (int) (8 * scale + 0.5f);

            tvTitle.setVisibility(VISIBLE);
            tvTitle.setText(params.title);
            if (params.titleColor != 0) {
                tvTitle.setTextColor(ContextCompat.getColor(getContext(), params.titleColor));
            }
        }

        //Message
        if (!TextUtils.isEmpty(params.message)) {
            MarginLayoutParams layoutParams = (MarginLayoutParams) tvMessage.getLayoutParams();
            layoutParams.topMargin = div;
            tvMessage.setLayoutParams(layoutParams);

            tvMessage.setVisibility(VISIBLE);
            tvMessage.setText(params.message);
            if (params.messageColor != 0) {
                tvMessage.setTextColor(ContextCompat.getColor(getContext(), params.messageColor));
            }
        }

        //Action
        if (!TextUtils.isEmpty(params.action) && params.onActionClickListener != null) {
            btnAction.setVisibility(VISIBLE);
            btnAction.setText(params.action);
            btnAction.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    params.onActionClickListener.onClick();
                    dismiss();
                }
            });

            //Action Color
            if (params.actionColor != 0) {
                btnAction.setTextColor(ContextCompat.getColor(getContext(), params.actionColor));
            }
        }

        //Background
        if (params.backgroundColor != 0) {
            layoutCookie
                    .setBackgroundColor(ContextCompat.getColor(getContext(), params.backgroundColor));
        }

        int padding = getContext().getResources().getDimensionPixelSize(R.dimen.default_padding);
        if (layoutGravity == Gravity.BOTTOM) {
            layoutCookie.setPadding(padding, padding, padding, padding);
        }

        createInAnim();
        createOutAnim();
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        viewWidth = getWidth();
        viewHeight = layoutCookie.getMeasuredHeight();
        dismissOffsetThresholdH = viewWidth / 3;
        dismissOffsetThresholdV = viewHeight / 2.5f;

        if (layoutGravity == Gravity.TOP) {
            super.onLayout(changed, l, 0, r, layoutCookie.getMeasuredHeight());
        } else {
            super.onLayout(changed, l, t, r, b);
        }
    }

    private void createInAnim() {
        Animation slideInAnimation = AnimationUtils.loadAnimation(getContext(),
                layoutGravity == Gravity.BOTTOM ? R.anim.slide_in_from_bottom : R.anim.slide_in_from_top);
        slideInAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        dismiss();
                    }
                }, duration);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        setAnimation(slideInAnimation);
    }

    private void createOutAnim() {
        slideOutAnimation = AnimationUtils.loadAnimation(getContext(),
                layoutGravity == Gravity.BOTTOM ? R.anim.slide_out_to_bottom : R.anim.slide_out_to_top);
        slideOutAnimationDuration = slideOutAnimation.getDuration();
        slideOutAnimation.setFillAfter(true);
        slideOutAnimation.setFillBefore(false);
        slideOutAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {

            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }

    public void dismiss() {
        dismiss(null);
    }

    private boolean isDissing = false;// 是否正在退出界面

    public void dismiss(final CookieBarDismissListener listener) {
        if (swipedOut) {
            removeFromParent();
            return;
        }

        if (isDissing) {// 如果正在关闭，就不继续了
            return;
        }
        isDissing = true;

        slideOutAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(final Animation animation) {
            }

            @Override
            public void onAnimationEnd(final Animation animation) {
                if (listener != null) {
                    listener.onDismiss();
                }
                removeFromParent();

                isDissing = false;
            }

            @Override
            public void onAnimationRepeat(final Animation animation) {
            }
        });

        long speed = listener == null ? slideOutAnimationDuration : slideOutAnimationDuration / 2;
        slideOutAnimation.setDuration(speed);
        startAnimation(slideOutAnimation);
    }

    private void removeFromParent() {
        postDelayed(new Runnable() {
            @Override
            public void run() {
                ViewParent parent = getParent();
                if (parent != null) {
                    Cookie.this.clearAnimation();
                    ((ViewGroup) parent).removeView(Cookie.this);
                }
            }
        }, 200);
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                initialDragX = motionEvent.getRawX();
                initialDragY = motionEvent.getRawY();
                return true;

            case MotionEvent.ACTION_UP:
                if (!swipedOut) {

                    // 原代码：手指松开，回到原位
                    /*view.animate()
                            .x(0)
                            .y(0)
                            .alpha(1)
                            .setDuration(200)
                            .start();*/

                    // 新增代码：判断单击，关闭
                    float offsetX = motionEvent.getRawX() - initialDragX;
                    float offsetY = motionEvent.getRawY() - initialDragY;
                    float xy2 = offsetX * offsetX + offsetY * offsetY;

                    if (Math.sqrt(xy2) < getContext().getResources().getDisplayMetrics().density * 4) {
                        // 单击，退出
                        dismiss();
                        swipedOut = true;// 防止重复点击
                    } else {
                        // 回到原位置
                        view.animate()
                                .x(0)
                                .y(layoutGravity == Gravity.TOP ? 0 : (getHeight() - viewHeight))
                                .alpha(1)
                                .setDuration(200)
                                .start();
                    }
                }
                return true;

            case MotionEvent.ACTION_MOVE:
                if (swipedOut) {
                    return true;
                }

                // 原代码：这段代码仅支持左右滑动
                /*float offset = motionEvent.getRawX() - initialDragX;
                float alpha = 1 - Math.abs(offset / viewWidth);
                long duration = 0;

                if (Math.abs(offset) > dismissOffsetThreshold) {
                    offset = viewWidth * Math.signum(offset);
                    alpha = 0;
                    duration = 200;
                    swipedOut = true;
                }

                view.animate()
                        .setListener(swipedOut ? getDestroyListener() : null)
                        .x(offset)
                        .alpha(alpha)
                        .setDuration(duration)
                        .start();*/

                // 新增代码：以下代码支持左右滑动 和 位置方向滑动（在顶部，向上滑动。在底部，可向下滑动）来关闭

                float offsetX = motionEvent.getRawX() - initialDragX;
                float offsetY = motionEvent.getRawY() - initialDragY;
                float alpha = 1 - Math.abs(offsetX / viewWidth);
                long duration = 0;

                if (Math.abs(offsetX) > dismissOffsetThresholdH) {
                    offsetX = viewWidth * Math.signum(offsetX);
                    alpha = 0;
                    duration = 200;
                    swipedOut = true;
                }

                // 显示在顶部的不能下移，显示在底部的不能上移
                offsetY = layoutGravity == Gravity.TOP ? Math.min(offsetY, 0) : Math.max(offsetY, 0);
                if (Math.abs(offsetY) > dismissOffsetThresholdV) {
                    if (layoutGravity == Gravity.TOP)
                        offsetY = viewHeight * -1;
                    else
                        offsetY = getHeight();
                    duration = 150;
                    swipedOut = true;
                } else {
                    if (layoutGravity != Gravity.TOP)
                        offsetY = offsetY + (getHeight() - viewHeight);
                }

                view.animate()
                        .setListener(swipedOut ? getDestroyListener() : null)
                        .x(offsetX)
                        .y(offsetY)
                        .alpha(alpha)
                        .setDuration(duration)
                        .start();

                return true;

            default:
                return false;
        }
    }

    private Animator.AnimatorListener getDestroyListener() {
        return new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                removeFromParent();
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        };
    }

    public interface CookieBarDismissListener {
        void onDismiss();
    }
}
