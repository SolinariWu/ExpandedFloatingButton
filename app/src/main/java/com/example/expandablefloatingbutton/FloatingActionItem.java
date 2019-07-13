package com.example.expandablefloatingbutton;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.*;
import androidx.appcompat.content.res.AppCompatResources;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import static com.google.android.material.floatingactionbutton.FloatingActionButton.*;

public class FloatingActionItem implements Parcelable {
    public static final int RESOURCE_NOT_SET = Integer.MIN_VALUE;
    @IdRes
    private final int id;
    @Nullable
    private final String actionText;
    @StringRes
    private final int actionTextRes;
    @DrawableRes
    private final int actionButtonRes;
    @Nullable
    private final Drawable actionButtonDrawable;
    @ColorInt
    private final int actionButtonTintColor;
    @ColorInt
    private final int actionBackgroundColor;
    @ColorInt
    private final int actionTextColor;
    @ColorInt
    private final int actionTextBackgroundColor;
    private final boolean actionTextClickable;
    @FloatingActionButton.Size
    private int actionButtonSize;

    private FloatingActionItem(Builder builder) {
        id = builder.id;
        actionText = builder.actionText;
        actionTextRes = builder.actionTextRes;
        actionButtonTintColor = builder.actionButtonTintColor;
        actionButtonRes = builder.actionButtonRes;
        actionButtonDrawable = builder.actionButtonDrawable;
        actionBackgroundColor = builder.actionBackgroundColor;
        actionTextColor = builder.actionTextColor;
        actionTextBackgroundColor = builder.actionTextBackgroundColor;
        actionTextClickable = builder.actionTextClickable;
        actionButtonSize = builder.actionButtonSize;
    }

    public int getId() {
        return id;
    }

    @Nullable
    public String getActionText(Context context) {
        if (actionText != null) {
            return actionText;
        } else if (actionTextRes != RESOURCE_NOT_SET) {
            return context.getString(actionTextRes);
        } else {
            return null;
        }
    }

    /**
     * Gets the current Drawable, or null if no Drawable has been assigned.
     *
     * @param context A context to retrieve the Drawable from (needed for FloatingActionItem.Builder(int, int).
     * @return the speed dial item drawable, or null if no drawable has been assigned.
     */
    @Nullable
    public Drawable getActionButtonImageDrawable(Context context) {
        if (actionButtonDrawable != null) {
            return actionButtonDrawable;
        } else if (actionButtonRes != RESOURCE_NOT_SET) {
            return AppCompatResources.getDrawable(context, actionButtonRes);
        } else {
            return null;
        }
    }

    @ColorInt
    public int getActionButtonImageTintColor() {
        return actionButtonTintColor;
    }

    @ColorInt
    public int getActionButtonBackgroundColor() {
        return actionBackgroundColor;
    }

    @ColorInt
    public int getActionTextColor() {
        return actionTextColor;
    }

    public int getLabelBackgroundColor() {
        return actionTextBackgroundColor;
    }

    public boolean isActionTextClickable() {
        return actionTextClickable;
    }

    public FloatingActionView createFabWithLabelView(Context context) {
        FloatingActionView floatingActionView;

        floatingActionView = new FloatingActionView(context,this);
        return floatingActionView;
    }

    @FloatingActionButton.Size
    public int getFloatingButtonSize() {
        return actionButtonSize;
    }

    public static class Builder {
        @IdRes
        private final int id;
        @DrawableRes
        private final int actionButtonRes;
        @Nullable
        private Drawable actionButtonDrawable;
        @ColorInt
        private int actionButtonTintColor = RESOURCE_NOT_SET;
        @Nullable
        private String actionText;
        @StringRes
        private int actionTextRes = RESOURCE_NOT_SET;
        @ColorInt
        private int actionBackgroundColor = RESOURCE_NOT_SET;
        @ColorInt
        private int actionTextColor = RESOURCE_NOT_SET;
        @ColorInt
        private int actionTextBackgroundColor = RESOURCE_NOT_SET;
        private boolean actionTextClickable = true;
        @FloatingActionButton.Size
        private int actionButtonSize = SIZE_NORMAL;

        /**
         * Creates a builder for a speed dial action item that uses a {@link DrawableRes} as icon.
         *
         * @param id               the identifier for this action item. The identifier must be unique to the instance
         *                         of {@link ExpandedFloatingButton}. The identifier should be a positive number.
         * @param fabImageResource resId the resource identifier of the drawable
         */
        public Builder(@IdRes int id, @DrawableRes int fabImageResource) {
            this.id = id;
            actionButtonRes = fabImageResource;
            actionButtonDrawable = null;
        }

        /**
         * Creates a builder for a speed dial action item that uses a {@link Drawable} as icon.
         * <p class="note">{@link Drawable} are not parcelables so is not possible to restore them when the view is
         * recreated for example after an orientation change. If possible always use the {@link #Builder(int, int)}</p>
         *
         * @param id       the identifier for this action item. The identifier must be unique to the instance
         *                 of {@link ExpandedFloatingButton}. The identifier should be a positive number.
         * @param drawable the Drawable to set, or null to clear the content
         */
        public Builder(@IdRes int id, @Nullable Drawable drawable) {
            this.id = id;
            actionButtonDrawable = drawable;
            actionButtonRes = RESOURCE_NOT_SET;
        }

        /**
         * Creates a builder for a speed dial action item that uses a {@link FloatingActionItem} instance to
         * initialize the default values.
         *
         * @param floatingActionItem it will be used for the default values of the builder.
         */
        public Builder(FloatingActionItem floatingActionItem) {
            id = floatingActionItem.id;
            actionText = floatingActionItem.actionText;
            actionTextRes = floatingActionItem.actionTextRes;
            actionButtonRes = floatingActionItem.actionButtonRes;
            actionButtonDrawable = floatingActionItem.actionButtonDrawable;
            actionButtonTintColor = floatingActionItem.actionButtonTintColor;
            actionBackgroundColor = floatingActionItem.actionBackgroundColor;
            actionTextColor = floatingActionItem.actionTextColor;
            actionTextBackgroundColor = floatingActionItem.actionTextBackgroundColor;
            actionTextClickable = floatingActionItem.actionTextClickable;
            actionButtonSize = floatingActionItem.actionButtonSize;
        }

        public Builder setActionText(@Nullable String text) {
            actionText = text;
            return this;
        }

        public Builder setActionTextRes(@StringRes int textRes) {
            actionTextRes = textRes;
            return this;
        }

        public Builder setActionButtonImageTintColor(int tintColor) {
            actionButtonTintColor = tintColor;
            return this;
        }

        public Builder setActionButtonBackgroundColor(@ColorInt int backgroundColor) {
            actionBackgroundColor = backgroundColor;
            return this;
        }

        public Builder setActionTextColor(@ColorInt int textColor) {
            actionTextColor = textColor;
            return this;
        }

        public Builder setActionTextBackgroundColor(@ColorInt int textBackgroundColor) {
            actionTextBackgroundColor = textBackgroundColor;
            return this;
        }

        public Builder setActionTextClickable(boolean clickable) {
            actionTextClickable = clickable;
            return this;
        }

        public FloatingActionItem create() {
            return new FloatingActionItem(this);
        }

        public Builder setFloatingButtonSize(@FloatingActionButton.Size int size) {
            actionButtonSize = size;
            return this;
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.id);
        dest.writeString(this.actionText);
        dest.writeInt(this.actionTextRes);
        dest.writeInt(this.actionButtonRes);
        dest.writeInt(this.actionButtonTintColor);
        dest.writeInt(this.actionBackgroundColor);
        dest.writeInt(this.actionTextColor);
        dest.writeInt(this.actionTextBackgroundColor);
        dest.writeByte(this.actionTextClickable ? (byte) 1 : (byte) 0);
        dest.writeInt(this.actionButtonSize);
    }

    protected FloatingActionItem(Parcel in) {
        this.id = in.readInt();
        this.actionText = in.readString();
        this.actionTextRes = in.readInt();
        this.actionButtonRes = in.readInt();
        this.actionButtonDrawable = null;
        this.actionButtonTintColor = in.readInt();
        this.actionBackgroundColor = in.readInt();
        this.actionTextColor = in.readInt();
        this.actionTextBackgroundColor = in.readInt();
        this.actionTextClickable = in.readByte() != 0;
        this.actionButtonSize = in.readInt();
    }

    public static final Creator<FloatingActionItem> CREATOR = new Creator<FloatingActionItem>() {
        @Override
        public FloatingActionItem createFromParcel(Parcel source) {
            return new FloatingActionItem(source);
        }

        @Override
        public FloatingActionItem[] newArray(int size) {
            return new FloatingActionItem[size];
        }
    };
}
