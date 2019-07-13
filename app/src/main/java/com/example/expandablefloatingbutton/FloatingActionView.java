
package com.example.expandablefloatingbutton;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import static com.example.expandablefloatingbutton.FloatingActionItem.RESOURCE_NOT_SET;
import static com.google.android.material.floatingactionbutton.FloatingActionButton.SIZE_AUTO;
import static com.google.android.material.floatingactionbutton.FloatingActionButton.SIZE_MINI;

public class FloatingActionView extends LinearLayout {
    private TextView actionTextView;
    private FloatingActionButton floatingButton;
    private CardView actionTextContainer;
    private boolean actionTextContainerEnable;
    @Nullable
    private FloatingActionItem floatingActionItem;
    @Nullable
    private ExpandedFloatingButton.OnActionSelectedListener actionSelectedListener;
    @FloatingActionButton.Size
    private int floatingButtonSize;
    private float actionTextContainerElevation;
    @Nullable
    private Drawable actionTextContainerBackground;

    public FloatingActionView(Context context, FloatingActionItem floatingActionItem) {
        super(context);
        this.floatingActionItem = floatingActionItem;
        init(context, null);
    }

    public FloatingActionView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public FloatingActionView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    @SuppressLint("RestrictedApi")
    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        getFloatingButton().setVisibility(visibility);
        if (isActionTextContainerEnabled()) {
            getActionTextBackground().setVisibility(visibility);
        }
    }

    @Override
    public void setOrientation(int orientation) {
        super.setOrientation(orientation);
        if (orientation == VERTICAL) {
            setActionTextContainerEnabled(false);
        } else {
            setActionText(actionTextView.getText().toString());
        }
    }

    /**
     * Return true if button has label, false otherwise.
     */
    public boolean isActionTextContainerEnabled() {
        return actionTextContainerEnable;
    }

    /**
     * Enables or disables label of button.
     */
    private void setActionTextContainerEnabled(boolean enabled) {
        actionTextContainerEnable = enabled;
        actionTextContainer.setVisibility(enabled ? View.VISIBLE : View.GONE);
    }

    /**
     * Returns FAB labels background card.
     */
    public CardView getActionTextBackground() {
        return actionTextContainer;
    }

    /**
     * Returns the {@link FloatingActionButton}.
     */
    public FloatingActionButton getFloatingButton() {
        return floatingButton;
    }

    public FloatingActionItem getFloatingActionItem() {
        if (floatingActionItem == null) {
            throw new IllegalStateException("FloatingActionItem not set yet!");
        }
        return floatingActionItem;
    }

    public void setSpeedDialActionItem(FloatingActionItem actionItem) {
        floatingActionItem = actionItem;
        setId(actionItem.getId());
        setActionText(actionItem.getActionText(getContext()));
        FloatingActionItem floatingActionItem = getFloatingActionItem();
        setLabelClickable(floatingActionItem != null && floatingActionItem.isActionTextClickable());

        int iconTintColor = actionItem.getActionButtonImageTintColor();

        Drawable drawable = actionItem.getActionButtonImageDrawable(getContext());
        if (drawable != null && iconTintColor != RESOURCE_NOT_SET) {
            drawable = DrawableCompat.wrap(drawable);
            DrawableCompat.setTint(drawable.mutate(), iconTintColor);
        }
        setFloatingButtonIcon(drawable);

        int fabBackgroundColor = actionItem.getActionButtonBackgroundColor();
        if (fabBackgroundColor == RESOURCE_NOT_SET) {
            fabBackgroundColor = UiUtils.getPrimaryColor(getContext());
        }
        setFabBackgroundColor(fabBackgroundColor);

        int labelColor = actionItem.getActionTextColor();
        if (labelColor == RESOURCE_NOT_SET) {
            labelColor = ResourcesCompat.getColor(getResources(), R.color.sd_label_text_color,
                    getContext().getTheme());
        }
        setLabelColor(labelColor);
        int labelBackgroundColor = actionItem.getLabelBackgroundColor();
        if (labelBackgroundColor == RESOURCE_NOT_SET) {
            labelBackgroundColor = ResourcesCompat.getColor(getResources(), R.color.cardview_light_background,
                    getContext().getTheme());
        }
        setLabelBackgroundColor(labelBackgroundColor);
        if (actionItem.getFloatingButtonSize() == SIZE_AUTO) {
            getFloatingButton().setSize(SIZE_MINI);
        } else {
            getFloatingButton().setSize(actionItem.getFloatingButtonSize());
        }
        setLayout(actionItem.getFloatingButtonSize());
    }

    /**
     * Set a listener that will be notified when a menu fab is selected.
     *
     * @param listener listener to set.
     */
    public void setOnActionSelectedListener(@Nullable ExpandedFloatingButton.OnActionSelectedListener listener) {
        actionSelectedListener = listener;
        if (actionSelectedListener != null) {
            setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    FloatingActionItem floatingActionItem = getFloatingActionItem();
                    if (actionSelectedListener != null
                            && floatingActionItem != null) {
                        if (floatingActionItem.isActionTextClickable()) {
                            UiUtils.performTap(getActionTextBackground());
                        } else {
                            UiUtils.performTap(getFloatingButton());
                        }
                    }
                }
            });
            getFloatingButton().setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    FloatingActionItem floatingActionItem = getFloatingActionItem();
                    if (actionSelectedListener != null
                            && floatingActionItem != null) {
                        actionSelectedListener.onActionSelected(floatingActionItem);
                    }
                }
            });

            getActionTextBackground().setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    FloatingActionItem floatingActionItem = getFloatingActionItem();
                    if (actionSelectedListener != null
                            && floatingActionItem != null
                            && floatingActionItem.isActionTextClickable()) {
                        actionSelectedListener.onActionSelected(floatingActionItem);
                    }
                }
            });
        } else {
            getFloatingButton().setOnClickListener(null);
            getActionTextBackground().setOnClickListener(null);
        }

    }

    /**
     * Init custom attributes.
     *
     * @param context context.
     * @param attrs   attributes.
     */
    private void init(Context context, @Nullable AttributeSet attrs) {
        View rootView = inflate(context, R.layout.item_floating_action, this);

        floatingButton = rootView.findViewById(R.id.actionFloatingButton);
        actionTextView = rootView.findViewById(R.id.sd_label);
        actionTextContainer = rootView.findViewById(R.id.actionTextContainer);

        setOrientation(LinearLayout.HORIZONTAL);
        setClipChildren(false);

        setSpeedDialActionItem(floatingActionItem);
    }

    private void setLayout(@FloatingActionButton.Size int size) {
        LayoutParams rootLayoutParams;
        LayoutParams fabLayoutParams = (LayoutParams) floatingButton.getLayoutParams();

        rootLayoutParams = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rootLayoutParams.gravity = Gravity.END;

        int leftMargin = getContext().getResources().getDimensionPixelOffset(R.dimen.floating_action_text_right_margin);
        int rightMargin = getContext().getResources().getDimensionPixelOffset(R.dimen.floating_action_button_right_margin);
        int bottomMargin = getContext().getResources().getDimensionPixelOffset(R.dimen.floating_action_button_bottom_margin);
        fabLayoutParams.setMargins(leftMargin, 0, rightMargin, 0);
        rootLayoutParams.setMargins(0, 0, 0, bottomMargin);

        setLayoutParams(rootLayoutParams);
        floatingButton.setLayoutParams(fabLayoutParams);
        floatingButtonSize = size;
    }

    /**
     * Sets fab drawable.
     *
     * @param mDrawable drawable to set.
     */
    private void setFloatingButtonIcon(@Nullable Drawable mDrawable) {
        floatingButton.setImageDrawable(mDrawable);
    }

    /**
     * Sets fab label․
     *
     * @param sequence label to set.
     */
    private void setActionText(@Nullable CharSequence sequence) {
        if (!TextUtils.isEmpty(sequence)) {
            actionTextView.setText(sequence);
            setActionTextContainerEnabled(getOrientation() == HORIZONTAL);
        } else {
            setActionTextContainerEnabled(false);
        }
    }

    private void setLabelClickable(boolean clickable) {
        getActionTextBackground().setClickable(clickable);
        getActionTextBackground().setFocusable(clickable);
        getActionTextBackground().setEnabled(clickable);
    }

    /**
     * Sets fab color in floating action menu.
     *
     * @param color color to set.
     */
    private void setFabBackgroundColor(@ColorInt int color) {
        floatingButton.setBackgroundTintList(ColorStateList.valueOf(color));
    }

    private void setLabelColor(@ColorInt int color) {
        actionTextView.setTextColor(color);
    }

    private void setLabelBackgroundColor(@ColorInt int color) {
        if (color == Color.TRANSPARENT) {
            actionTextContainer.setCardBackgroundColor(Color.TRANSPARENT);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                actionTextContainerElevation = actionTextContainer.getElevation();
                actionTextContainer.setElevation(0);
            } else {
                actionTextContainer.setBackgroundColor(Color.TRANSPARENT);
                actionTextContainerBackground = actionTextContainer.getBackground();
            }
        } else {
            actionTextContainer.setCardBackgroundColor(ColorStateList.valueOf(color));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (actionTextContainerElevation != 0) {
                    actionTextContainer.setElevation(actionTextContainerElevation);
                    actionTextContainerElevation = 0;
                }
            } else {
                if (actionTextContainerBackground != null) {
                    actionTextContainer.setBackground(actionTextContainerBackground);
                    actionTextContainerBackground = null;
                }
            }
        }
    }
}

