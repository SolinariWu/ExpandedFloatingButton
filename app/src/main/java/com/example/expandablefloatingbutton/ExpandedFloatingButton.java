/*
 * Copyright 2018 Roberto Leinardi.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.expandablefloatingbutton;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.*;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import androidx.annotation.*;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.PopupMenu;
import androidx.cardview.widget.CardView;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import static com.example.expandablefloatingbutton.FloatingActionItem.RESOURCE_NOT_SET;

@SuppressWarnings({"unused", "WeakerAccess", "UnusedReturnValue"})
public class ExpandedFloatingButton extends LinearLayout implements CoordinatorLayout.AttachedBehavior {
    private static final String STATE_KEY_SUPER = "superState";
    private static final String STATE_KEY_IS_OPEN = "isOpen";
    private static final int DEFAULT_ROTATE_ANGLE = 45;
    private static final int ACTION_ANIM_DELAY = 25;
    private final InstanceState instanceState = new InstanceState();
    private List<FloatingActionView> floatingActionItems = new ArrayList<>();
    @Nullable
    private Drawable mainButtonCloseDrawable = null;
    @Nullable
    private Drawable mainButtonOpenDrawable = null;
    @Nullable
    private Drawable mainButtonOriginalDrawable;
    private FloatingActionButton mainButton;
    @IdRes
    private int mOverlayLayoutId;
    @Nullable
    private SpeedDialOverlayLayout mOverlayLayout;
    @Nullable
    private OnChangeListener onChangeListener;
    @Nullable
    private OnActionSelectedListener onActionSelectedListener;
    private OnActionSelectedListener onActionSelectedProxyListener = new OnActionSelectedListener() {
        @Override
        public boolean onActionSelected(FloatingActionItem actionItem) {
            if (onActionSelectedListener != null) {
                boolean consumed = onActionSelectedListener.onActionSelected(actionItem);
                if (!consumed) {
                    close(false);
                }
                return consumed;
            } else {
                return false;
            }
        }
    };

    public ExpandedFloatingButton(Context context) {
        super(context);
        init(context, null);
    }

    public ExpandedFloatingButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public ExpandedFloatingButton(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    public boolean getUseReverseAnimationOnClose() {
        return instanceState.useReverseAnimationOnClose;
    }

    public void setUseReverseAnimationOnClose(boolean useReverseAnimation) {
        instanceState.useReverseAnimationOnClose = useReverseAnimation;
    }

    private void setExpandedMode() {
        setOrientation(VERTICAL);
        for (FloatingActionView floatingActionView : floatingActionItems) {
            floatingActionView.setOrientation(HORIZONTAL);
        }
        close(false);
        ArrayList<FloatingActionItem> actionItems = getActionItems();
        clearActionItems();
        addAllActionItems(actionItems);
    }

    public void show() {
        show(null);
    }

    public void show(@Nullable final FloatingActionButton.OnVisibilityChangedListener listener) {
        setVisibility(VISIBLE);
        showFabWithWorkaround(mainButton, listener);
    }

    /*
     * WORKAROUND: Remove if Google will finally fix this: https://issuetracker.google.com/issues/111316656
     */
    private void showFabWithWorkaround(FloatingActionButton fab, @Nullable final FloatingActionButton.OnVisibilityChangedListener listener) {
        fab.show(new FloatingActionButton.OnVisibilityChangedListener() {
            @SuppressWarnings("unchecked")
            @Override
            public void onShown(FloatingActionButton fab) {
                try {
                    Field declaredField = fab.getClass().getDeclaredField("impl");
                    declaredField.setAccessible(true);
                    Object impl = declaredField.get(fab);
                    Class implClass = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                            ? impl.getClass().getSuperclass() : impl.getClass();
                    Method scale = implClass.getDeclaredMethod("setImageMatrixScale", Float.TYPE);
                    scale.setAccessible(true);
                    scale.invoke(impl, 1.0F);
                } catch (NoSuchMethodException e) {
                    //TODO
                    //Log.e(TAG, "Method setImageMatrixScale not found", e);
                } catch (IllegalAccessException e) {
                    //TODO
                    //Log.e(TAG, "IllegalAccessException", e);
                } catch (InvocationTargetException e) {
                    //TODO
                   // Log.e(TAG, "InvocationTargetException", e);
                } catch (NoSuchFieldException e) {
                    //TODO
                    //Log.e(TAG, "Field impl not found", e);
                }
                if (listener != null) {
                    listener.onShown(fab);
                }
            }

            @Override
            public void onHidden(FloatingActionButton fab) {
                if (listener != null) {
                    listener.onHidden(fab);
                }
            }
        });
    }

    public void hide() {
        hide(null);
    }

    public void hide(@Nullable final FloatingActionButton.OnVisibilityChangedListener listener) {
        if (isOpen()) {
            close();
            // Workaround for mainButton.hide() breaking the rotate anim
            ViewCompat.animate(mainButton).rotation(0).setDuration(0).start();
        }
        mainButton.hide(new FloatingActionButton.OnVisibilityChangedListener() {
            @Override
            public void onShown(FloatingActionButton fab) {
                super.onShown(fab);
                if (listener != null) {
                    listener.onShown(fab);
                }
            }

            @Override
            public void onHidden(FloatingActionButton fab) {
                super.onHidden(fab);
                setVisibility(INVISIBLE);
                if (listener != null) {
                    listener.onHidden(fab);
                }
            }
        });
    }

    @Nullable
    public SpeedDialOverlayLayout getOverlayLayout() {
        return mOverlayLayout;
    }

    /**
     * Add the overlay/touch guard view to appear together with the speed dial menu.
     *
     * @param overlayLayout The view to add.
     */
    public void setOverlayLayout(@Nullable SpeedDialOverlayLayout overlayLayout) {
        if (mOverlayLayout != null) {
            setOnClickListener(null);
        }
        mOverlayLayout = overlayLayout;
        if (overlayLayout != null) {
            overlayLayout.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    close();
                }
            });
            showHideOverlay(isOpen(), false);
        }
    }

    /**
     * Inflate a menu resource into this SpeedDialView. Any existing Action item will be removed.
     * <p class="note">Using the Menu resource it is possible to specify only the ID, the icon and the label of the
     * Action item. No color customization is available.</p>
     *
     * @param menuRes Menu resource to inflate
     */
    public void inflate(@MenuRes int menuRes) {
        clearActionItems();
        PopupMenu popupMenu = new PopupMenu(getContext(), new View(getContext()));
        popupMenu.inflate(menuRes);
        Menu menu = popupMenu.getMenu();
        for (int i = 0; i < menu.size(); i++) {
            MenuItem menuItem = menu.getItem(i);
            FloatingActionItem actionItem = new FloatingActionItem.Builder(menuItem.getItemId(), menuItem.getIcon())
                    .setActionText(menuItem.getTitle() != null ? menuItem.getTitle().toString() : null)
                    .create();
            addActionItem(actionItem);
        }
    }

    /**
     * Appends all of the {@link FloatingActionItem} to the end of the list, in the order that they are returned by
     * the specified
     * collection's Iterator.
     *
     * @param actionItemCollection collection containing {@link FloatingActionItem} to be added to this list
     * @return a collection containing the instances of {@link FloatingActionView} added.
     */
    public Collection<FloatingActionView> addAllActionItems(Collection<FloatingActionItem> actionItemCollection) {
        ArrayList<FloatingActionView> floatingActionViews = new ArrayList<>();
        for (FloatingActionItem floatingActionItem : actionItemCollection) {
            floatingActionViews.add(addActionItem(floatingActionItem));
        }
        return floatingActionViews;
    }

    /**
     * Appends the specified {@link FloatingActionItem} to the end of this list.
     *
     * @param floatingActionItem {@link FloatingActionItem} to be appended to this list
     * @return the instance of the {@link FloatingActionView} if the add was successful, null otherwise.
     */
    @Nullable
    public FloatingActionView addActionItem(FloatingActionItem floatingActionItem) {
        return addActionItem(floatingActionItem, floatingActionItems.size());
    }

    /**
     * Inserts the specified {@link FloatingActionItem} at the specified position in this list. Shifts the element
     * currently at that position (if any) and any subsequent elements to the right (adds one to their indices).
     *
     * @param actionItem {@link FloatingActionItem} to be appended to this list
     * @param position   index at which the specified element is to be inserted
     * @return the instance of the {@link FloatingActionView} if the add was successful, null otherwise.
     */
    @Nullable
    public FloatingActionView addActionItem(FloatingActionItem actionItem, int position) {
        return addActionItem(actionItem, position, true);
    }

    /**
     * Inserts the specified {@link FloatingActionItem} at the specified position in this list. Shifts the element
     * currently at that position (if any) and any subsequent elements to the right (adds one to their indices).
     *
     * @param actionItem {@link FloatingActionItem} to be appended to this list
     * @param position   index at which the specified element is to be inserted
     * @param animate    true to animate the insertion, false to insert instantly
     * @return the instance of the {@link FloatingActionView} if the add was successful, null otherwise.
     */
    @Nullable
    public FloatingActionView addActionItem(FloatingActionItem actionItem, int position, boolean animate) {
        FloatingActionView oldView = findFabWithLabelViewById(actionItem.getId());
        if (oldView != null) {
            return replaceActionItem(oldView.getFloatingActionItem(), actionItem);
        } else {
            FloatingActionView newView = actionItem.createFabWithLabelView(getContext());
            newView.setOnActionSelectedListener(onActionSelectedProxyListener);
            int layoutPosition = getLayoutPosition(position);
            addView(newView, layoutPosition);
            floatingActionItems.add(position, newView);
            if (isOpen()) {
                if (animate) {
                    showWithAnimationFabWithLabelView(newView, 0);
                }
            } else {
                newView.setVisibility(GONE);
            }
            return newView;
        }
    }

    /**
     * Removes the {@link FloatingActionItem} at the specified position in this list. Shifts any subsequent elements
     * to the left (subtracts one from their indices).
     *
     * @param position the index of the {@link FloatingActionItem} to be removed
     * @return the {@link FloatingActionItem} that was removed from the list
     */
    @Nullable
    public FloatingActionItem removeActionItem(int position) {
        FloatingActionItem floatingActionItem = floatingActionItems.get(position).getFloatingActionItem();
        removeActionItem(floatingActionItem);
        return floatingActionItem;
    }

    /**
     * Removes the specified {@link FloatingActionItem} from this list, if it is present. If the list does not
     * contain the element, it is unchanged.
     * <p>
     * Returns true if this list contained the specified element (or equivalently, if this list changed
     * as a result of the call).
     *
     * @param actionItem {@link FloatingActionItem} to be removed from this list, if present
     * @return true if this list contained the specified element
     */
    public boolean removeActionItem(@Nullable FloatingActionItem actionItem) {
        return actionItem != null && removeActionItemById(actionItem.getId()) != null;
    }

    /**
     * Finds and removes the first {@link FloatingActionItem} with the given ID, if it is present. If the list does not
     * contain the element, it is unchanged.
     *
     * @param idRes the ID to search for
     * @return the {@link FloatingActionItem} that was removed from the list, or null otherwise
     */
    @Nullable
    public FloatingActionItem removeActionItemById(@IdRes int idRes) {
        return removeActionItem(findFabWithLabelViewById(idRes));
    }

    /**
     * Replace the {@link FloatingActionItem} at the specified position in this list with the one provided as
     * parameter.
     *
     * @param newActionItem {@link FloatingActionItem} to use for the replacement
     * @param position      the index of the {@link FloatingActionItem} to be replaced
     * @return the instance of the new {@link FloatingActionView} if the replace was successful, null otherwise.
     */
    @Nullable
    public FloatingActionView replaceActionItem(FloatingActionItem newActionItem, int position) {
        return replaceActionItem(floatingActionItems.get(position).getFloatingActionItem(), newActionItem);
    }

    /**
     * Replace an already added {@link FloatingActionItem} with the one provided as parameter.
     *
     * @param oldFloatingActionItem the old {@link FloatingActionItem} to remove
     * @param newFloatingActionItem the new {@link FloatingActionItem} to add
     * @return the instance of the new {@link FloatingActionView} if the replace was successful, null otherwise.
     */
    @Nullable
    public FloatingActionView replaceActionItem(@Nullable FloatingActionItem oldFloatingActionItem,
                                                FloatingActionItem newFloatingActionItem) {
        if (oldFloatingActionItem == null) {
            return null;
        } else {
            FloatingActionView oldView = findFabWithLabelViewById(oldFloatingActionItem.getId());
            if (oldView != null) {
                int index = floatingActionItems.indexOf(oldView);
                if (index < 0) {
                    return null;
                }
                removeActionItem(findFabWithLabelViewById(newFloatingActionItem.getId()), null, false);
                removeActionItem(findFabWithLabelViewById(oldFloatingActionItem.getId()), null, false);
                return addActionItem(newFloatingActionItem, index, false);
            } else {
                return null;
            }
        }
    }

    /**
     * Removes all of the {@link FloatingActionItem} from this list.
     */
    public void clearActionItems() {
        Iterator<FloatingActionView> it = floatingActionItems.iterator();
        while (it.hasNext()) {
            FloatingActionView floatingActionView = it.next();
            removeActionItem(floatingActionView, it, true);
        }
    }

    @NonNull
    public ArrayList<FloatingActionItem> getActionItems() {
        ArrayList<FloatingActionItem> floatingActionItems = new ArrayList<>(this.floatingActionItems.size());
        for (FloatingActionView floatingActionView : this.floatingActionItems) {
            floatingActionItems.add(floatingActionView.getFloatingActionItem());
        }
        return floatingActionItems;
    }

    @NonNull
    @Override
    public CoordinatorLayout.Behavior getBehavior() {
        return new SnackbarBehavior();
    }

    /**
     * Set a listener that will be notified when a menu fab is selected.
     *
     * @param listener listener to set.
     */
    public void setOnActionSelectedListener(@Nullable OnActionSelectedListener listener) {
        onActionSelectedListener = listener;

        for (int index = 0; index < floatingActionItems.size(); index++) {
            final FloatingActionView floatingActionView = floatingActionItems.get(index);
            floatingActionView.setOnActionSelectedListener(onActionSelectedProxyListener);
        }
    }

    /**
     * Set Main FloatingActionButton ClickMOnOptionFabSelectedListener.
     *
     * @param onChangeListener listener to set.
     */
    public void setOnChangeListener(@Nullable final OnChangeListener onChangeListener) {
        this.onChangeListener = onChangeListener;
    }

    /**
     * Opens speed dial menu.
     */
    public void open() {
        toggle(true, true);
    }

    public void open(boolean animate) {
        toggle(true, animate);
    }

    /**
     * Closes speed dial menu.
     */
    public void close() {
        toggle(false, true);
    }

    public void close(boolean animate) {
        toggle(false, animate);
    }

    /**
     * Toggles speed dial menu.
     */
    public void toggle() {
        toggle(!isOpen(), true);
    }

    public void toggle(boolean animate) {
        toggle(!isOpen(), animate);
    }

    /**
     * Return returns true if speed dial menu is open,false otherwise.
     */
    public boolean isOpen() {
        return instanceState.isOpen;
    }

    public FloatingActionButton getMainFab() {
        return mainButton;
    }

    public float getMainFabAnimationRotateAngle() {
        return instanceState.mainButtonAnimationRotateAngle;
    }

    public void setMainFabAnimationRotateAngle(float mainFabAnimationRotateAngle) {
        instanceState.mainButtonAnimationRotateAngle = mainFabAnimationRotateAngle;
        setMainFabOpenedDrawable(mainButtonOriginalDrawable);
    }

    public void setMainFabClosedDrawable(@Nullable Drawable drawable) {
        mainButtonCloseDrawable = drawable;
        updateMainFabDrawable(false);
    }

    public void setMainFabOpenedDrawable(@Nullable Drawable drawable) {
        mainButtonOriginalDrawable = drawable;
        if (mainButtonOriginalDrawable == null) {
            mainButtonOpenDrawable = null;
        } else {
            mainButtonOpenDrawable = UiUtils.getRotateDrawable(mainButtonOriginalDrawable,
                    -getMainFabAnimationRotateAngle());
        }
        updateMainFabDrawable(false);
    }

    @ColorInt
    public int getMainFabClosedBackgroundColor() {
        return instanceState.mainButtonClosedBackgroundColor;
    }

    public void setMainFabClosedBackgroundColor(@ColorInt int mainFabClosedBackgroundColor) {
        instanceState.mainButtonClosedBackgroundColor = mainFabClosedBackgroundColor;
        updateMainFabBackgroundColor();
    }

    @ColorInt
    public int getMainFabOpenedBackgroundColor() {
        return instanceState.mainButtonOpenedBackgroundColor;
    }

    public void setMainFabOpenedBackgroundColor(@ColorInt int mainFabOpenedBackgroundColor) {
        instanceState.mainButtonOpenedBackgroundColor = mainFabOpenedBackgroundColor;
        updateMainFabBackgroundColor();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (mOverlayLayout == null) {
            SpeedDialOverlayLayout overlayLayout = getRootView().findViewById(mOverlayLayoutId);
            setOverlayLayout(overlayLayout);
        }
    }

    @Nullable
    @Override
    protected Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        instanceState.floatingActionItems = getActionItems();
        bundle.putParcelable(InstanceState.class.getName(), instanceState);
        bundle.putParcelable(STATE_KEY_SUPER, super.onSaveInstanceState());
        return bundle;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            Bundle bundle = (Bundle) state;
            InstanceState instanceState = bundle.getParcelable(InstanceState.class.getName());
            if (instanceState != null
                    && instanceState.floatingActionItems != null
                    && !instanceState.floatingActionItems.isEmpty()) {
                setUseReverseAnimationOnClose(instanceState.useReverseAnimationOnClose);
                setMainFabAnimationRotateAngle(instanceState.mainButtonAnimationRotateAngle);
                setMainFabOpenedBackgroundColor(instanceState.mainButtonOpenedBackgroundColor);
                setMainFabClosedBackgroundColor(instanceState.mainButtonClosedBackgroundColor);
                setExpandedMode();
                addAllActionItems(instanceState.floatingActionItems);
                toggle(instanceState.isOpen, false);
            }
            state = bundle.getParcelable(STATE_KEY_SUPER);
        }
        super.onRestoreInstanceState(state);
    }

    private int getLayoutPosition(int position) {
        return floatingActionItems.size() - position;
    }

    @Nullable
    private FloatingActionItem removeActionItem(@Nullable FloatingActionView view,
                                                @Nullable Iterator<FloatingActionView> it,
                                                boolean animate) {
        if (view != null) {
            FloatingActionItem floatingActionItem = view.getFloatingActionItem();
            if (it != null) {
                it.remove();
            } else {
                floatingActionItems.remove(view);
            }

            if (isOpen()) {
                if (floatingActionItems.isEmpty()) {
                    close();
                }
                if (animate) {
                    UiUtils.shrinkAnim(view, true);
                } else {
                    removeView(view);
                }
            } else {
                removeView(view);
            }
            return floatingActionItem;
        } else {
            return null;
        }
    }

    @Nullable
    private FloatingActionItem removeActionItem(@Nullable FloatingActionView view) {
        return removeActionItem(view, null, true);
    }

    private void init(Context context, @Nullable AttributeSet attrs) {
        mainButton = createMainFab();
        addView(mainButton);
        setClipChildren(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setElevation(getResources().getDimension(R.dimen.floating_button_close_elevation));
        }
        TypedArray styledAttrs = context.obtainStyledAttributes(attrs, R.styleable.ExpandedFloatingButton, 0, 0);
        try {
            setUseReverseAnimationOnClose(styledAttrs.getBoolean(R.styleable.ExpandedFloatingButton_sdUseReverseAnimationOnClose,
                    getUseReverseAnimationOnClose()));

            setMainFabAnimationRotateAngle(styledAttrs.getFloat(R.styleable.ExpandedFloatingButton_sdMainFabAnimationRotateAngle,
                    getMainFabAnimationRotateAngle()));
            @DrawableRes int openDrawableRes = styledAttrs.getResourceId(R.styleable.ExpandedFloatingButton_sdMainFabClosedSrc,
                    RESOURCE_NOT_SET);
            if (openDrawableRes != RESOURCE_NOT_SET) {
                setMainFabClosedDrawable(AppCompatResources.getDrawable(getContext(), openDrawableRes));
            }
            int closeDrawableRes = styledAttrs.getResourceId(R.styleable.ExpandedFloatingButton_sdMainFabOpenedSrc,
                    RESOURCE_NOT_SET);
            if (closeDrawableRes != RESOURCE_NOT_SET) {
                setMainFabOpenedDrawable(AppCompatResources.getDrawable(context, closeDrawableRes));
            }
            setExpandedMode();

            setMainFabClosedBackgroundColor(styledAttrs.getColor(R.styleable
                            .ExpandedFloatingButton_sdMainFabClosedBackgroundColor,
                    getMainFabClosedBackgroundColor()));
            setMainFabOpenedBackgroundColor(styledAttrs.getColor(R.styleable
                            .ExpandedFloatingButton_sdMainFabOpenedBackgroundColor,
                    getMainFabOpenedBackgroundColor()));
            mOverlayLayoutId = styledAttrs.getResourceId(R.styleable.ExpandedFloatingButton_sdOverlayLayout, RESOURCE_NOT_SET);
        } catch (Exception e) {
            //TODo
//            Log.e(TAG, "Failure setting FabWithLabelView icon", e);
        } finally {
            styledAttrs.recycle();
        }
    }

    private FloatingActionButton createMainFab() {
        FloatingActionButton floatingActionButton = new FloatingActionButton(getContext());
        LayoutParams layoutParams = new LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.gravity = Gravity.END;

        int rightMargin = getContext().getResources().getDimensionPixelOffset(R.dimen.main_floating_button_right_margin);
        int bottomMargin = getContext().getResources().getDimensionPixelOffset(R.dimen.main_floating_button_bottom_margin);
        layoutParams.setMargins(0, 0, rightMargin, bottomMargin);
        floatingActionButton.setId(R.id.expanded_floating_button);
        floatingActionButton.setLayoutParams(layoutParams);
        floatingActionButton.setClickable(true);
        floatingActionButton.setFocusable(true);
        floatingActionButton.setSize(FloatingActionButton.SIZE_NORMAL);
        floatingActionButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View view) {
                if (isOpen()) {
                    if (onChangeListener == null || !onChangeListener.onMainActionSelected()) {
                        close();
                    }
                } else {
                    open();
                }
            }
        });
        return floatingActionButton;
    }

    private void toggle(boolean show, boolean animate) {
        if (show && floatingActionItems.isEmpty()) {
            show = false;
            if (onChangeListener != null) {
                onChangeListener.onMainActionSelected();
            }
        }
        if (isOpen() == show) {
            return;
        }
        instanceState.isOpen = show;
        visibilitySetup(show, animate, instanceState.useReverseAnimationOnClose);
        updateMainFabDrawable(animate);
        updateMainFabBackgroundColor();
        showHideOverlay(show, animate);
        if (onChangeListener != null) {
            onChangeListener.onToggleChanged(show);
        }
    }

    private void updateMainFabDrawable(boolean animate) {
        if (isOpen()) {
            if (mainButtonOpenDrawable != null) {
                mainButton.setImageDrawable(mainButtonOpenDrawable);
            }
            UiUtils.rotateForward(mainButton, getMainFabAnimationRotateAngle(), animate);
        } else {
            UiUtils.rotateBackward(mainButton, animate);
            if (mainButtonCloseDrawable != null) {
                mainButton.setImageDrawable(mainButtonCloseDrawable);
            }
        }
    }

    private void updateMainFabBackgroundColor() {
        int color;
        if (isOpen()) {
            color = getMainFabOpenedBackgroundColor();
        } else {
            color = getMainFabClosedBackgroundColor();
        }
        if (color != RESOURCE_NOT_SET) {
            mainButton.setBackgroundTintList(ColorStateList.valueOf(color));
        } else {
            mainButton.setBackgroundTintList(ColorStateList.valueOf(UiUtils.getAccentColor(getContext())));
        }
    }

    private void updateElevation() {
        if (isOpen()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                setElevation(getResources().getDimension(R.dimen.floating_button_open_elevation));
            } else {
                bringToFront();
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                setElevation(getResources().getDimension(R.dimen.floating_button_close_elevation));
            }
        }
    }

    private void showHideOverlay(boolean show, boolean animate) {
        if (mOverlayLayout != null) {
            if (show) {
                mOverlayLayout.show(animate);
            } else {
                mOverlayLayout.hide(animate);
            }
        }
    }

    @Nullable
    private FloatingActionView findFabWithLabelViewById(@IdRes int id) {
        for (FloatingActionView floatingActionView : floatingActionItems) {
            if (floatingActionView.getId() == id) {
                return floatingActionView;
            }
        }
        return null;
    }

    /**
     * Set menus visibility (visible or invisible).
     */
    private void visibilitySetup(boolean visible, boolean animate, boolean reverseAnimation) {
        int size = floatingActionItems.size();
        if (visible) {
            for (int i = 0; i < size; i++) {
                FloatingActionView floatingActionView = floatingActionItems.get(i);
                floatingActionView.setAlpha(1);
                floatingActionView.setVisibility(VISIBLE);
                if (animate) {
                    showWithAnimationFabWithLabelView(floatingActionView, i * ACTION_ANIM_DELAY);
                }
            }
        } else {
            for (int i = 0; i < size; i++) {
                int index = reverseAnimation ? size - 1 - i : i;
                FloatingActionView floatingActionView = floatingActionItems.get(index);
                if (animate) {
                    if (reverseAnimation) {
                        hideWithAnimationFabWithLabelView(floatingActionView, i * ACTION_ANIM_DELAY);
                    } else {
                        UiUtils.shrinkAnim(floatingActionView, false);
                    }
                } else {
                    floatingActionView.setAlpha(0);
                    floatingActionView.setVisibility(GONE);
                }
            }
        }
    }

    private void showWithAnimationFabWithLabelView(FloatingActionView floatingActionView, int delay) {
        ViewCompat.animate(floatingActionView).cancel();
        UiUtils.enlargeAnim(floatingActionView.getFloatingButton(), delay);
        if (floatingActionView.isActionTextContainerEnabled()) {
            CardView labelBackground = floatingActionView.getActionTextBackground();
            ViewCompat.animate(labelBackground).cancel();
            Animation animation = AnimationUtils.loadAnimation(getContext(), R.anim.floating_button_fade_and_translate_in);
            animation.setStartOffset(delay);
            labelBackground.startAnimation(animation);
        }
    }

    private void hideWithAnimationFabWithLabelView(final FloatingActionView floatingActionView, int delay) {
        ViewCompat.animate(floatingActionView).cancel();
        UiUtils.shrinkAnim(floatingActionView.getFloatingButton(), delay);
        if (floatingActionView.isActionTextContainerEnabled()) {
            final CardView labelBackground = floatingActionView.getActionTextBackground();
            ViewCompat.animate(labelBackground).cancel();
            Animation animation = AnimationUtils.loadAnimation(getContext(), R.anim.floating_button_fade_and_translate_out);
            animation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationEnd(Animation animation) {
                    labelBackground.setVisibility(View.GONE);
                }

                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });
            animation.setStartOffset(delay);
            labelBackground.startAnimation(animation);
        }
    }

    /**
     * Listener for handling events on option fab's.
     */
    public interface OnChangeListener {
        /**
         * Called when the main action has been clicked.
         *
         * @return true to keep the Speed Dial open, false otherwise.
         */
        boolean onMainActionSelected();

        /**
         * Called when the toggle state of the speed dial menu changes (eg. it is opened or closed).
         *
         * @param isOpen true if the speed dial is open, false otherwise.
         */
        void onToggleChanged(boolean isOpen);
    }

    /**
     * Listener for handling events on option fab's.
     */
    public interface OnActionSelectedListener {
        /**
         * Called when a speed dial action has been clicked.
         *
         * @param actionItem the {@link FloatingActionItem} that was selected.
         * @return true to keep the Speed Dial open, false otherwise.
         */
        boolean onActionSelected(FloatingActionItem actionItem);
    }

    private static class InstanceState implements Parcelable {
        private boolean isOpen = false;
        @ColorInt
        private int mainButtonClosedBackgroundColor = RESOURCE_NOT_SET;
        @ColorInt
        private int mainButtonOpenedBackgroundColor = RESOURCE_NOT_SET;

        private float mainButtonAnimationRotateAngle = DEFAULT_ROTATE_ANGLE;
        private boolean useReverseAnimationOnClose = false;
        private ArrayList<FloatingActionItem> floatingActionItems = new ArrayList<>();

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeByte(this.isOpen ? (byte) 1 : (byte) 0);
            dest.writeInt(this.mainButtonClosedBackgroundColor);
            dest.writeInt(this.mainButtonOpenedBackgroundColor);
            dest.writeFloat(this.mainButtonAnimationRotateAngle);
            dest.writeByte(this.useReverseAnimationOnClose ? (byte) 1 : (byte) 0);
            dest.writeTypedList(this.floatingActionItems);
        }

        public InstanceState() {
        }

        protected InstanceState(Parcel in) {
            this.isOpen = in.readByte() != 0;
            this.mainButtonClosedBackgroundColor = in.readInt();
            this.mainButtonOpenedBackgroundColor = in.readInt();
            this.mainButtonAnimationRotateAngle = in.readFloat();
            this.useReverseAnimationOnClose = in.readByte() != 0;
            this.floatingActionItems = in.createTypedArrayList(FloatingActionItem.CREATOR);
        }

        public static final Creator<InstanceState> CREATOR = new Creator<InstanceState>() {
            @Override
            public InstanceState createFromParcel(Parcel source) {
                return new InstanceState(source);
            }

            @Override
            public InstanceState[] newArray(int size) {
                return new InstanceState[size];
            }
        };
    }

    @SuppressWarnings({"unused", "WeakerAccess"})
    public static class SnackbarBehavior extends CoordinatorLayout.Behavior<View> {
        private static final boolean AUTO_HIDE_DEFAULT = true;

        @Nullable
        private Rect tmpRect;
        @Nullable
        private FloatingActionButton.OnVisibilityChangedListener internalAutoHideListener;
        private boolean autoHideEnabled;

        public SnackbarBehavior() {
            super();
            autoHideEnabled = AUTO_HIDE_DEFAULT;
        }

        public SnackbarBehavior(Context context, AttributeSet attrs) {
            super(context, attrs);
            TypedArray a = context.obtainStyledAttributes(attrs,
                    com.google.android.material.R.styleable.FloatingActionButton_Behavior_Layout);
            autoHideEnabled = a.getBoolean(
                    com.google.android.material.R.styleable.FloatingActionButton_Behavior_Layout_behavior_autoHide,
                    AUTO_HIDE_DEFAULT);
            a.recycle();
        }

        private static boolean isBottomSheet(@NonNull View view) {
            final ViewGroup.LayoutParams lp = view.getLayoutParams();
            if (lp instanceof CoordinatorLayout.LayoutParams) {
                return ((CoordinatorLayout.LayoutParams) lp)
                        .getBehavior() instanceof BottomSheetBehavior;
            }
            return false;
        }

        /**
         * Returns whether the associated View automatically hides when there is
         * not enough space to be displayed.
         *
         * @return true if enabled
         */
        public boolean isAutoHideEnabled() {
            return autoHideEnabled;
        }

        /**
         * Sets whether the associated View automatically hides when there is
         * not enough space to be displayed. This works with {@link AppBarLayout}
         * and {@link BottomSheetBehavior}.
         *
         * @param autoHide true to enable automatic hiding
         */
        public void setAutoHideEnabled(boolean autoHide) {
            autoHideEnabled = autoHide;
        }

        @Override
        public void onAttachedToLayoutParams(@NonNull CoordinatorLayout.LayoutParams lp) {
            if (lp.dodgeInsetEdges == Gravity.NO_GRAVITY) {
                // If the developer hasn't set dodgeInsetEdges, lets set it to BOTTOM so that
                // we dodge any Snackbars
                lp.dodgeInsetEdges = Gravity.BOTTOM;
            }
        }

        @Override
        public boolean onDependentViewChanged(CoordinatorLayout parent, View child,
                                              View dependency) {
            if (dependency instanceof AppBarLayout) {
                // If we're depending on an AppBarLayout we will show/hide it automatically
                // if the VIEW is anchored to the AppBarLayout
                updateFabVisibilityForAppBarLayout(parent, (AppBarLayout) dependency, child);
            } else if (isBottomSheet(dependency)) {
                updateFabVisibilityForBottomSheet(dependency, child);
            }
            return false;
        }

        @Override
        public boolean onLayoutChild(CoordinatorLayout parent, View child,
                                     int layoutDirection) {
            // First, let's make sure that the visibility of the VIEW is consistent
            final List<View> dependencies = parent.getDependencies(child);
            for (int i = 0, count = dependencies.size(); i < count; i++) {
                final View dependency = dependencies.get(i);
                if (dependency instanceof AppBarLayout) {
                    if (updateFabVisibilityForAppBarLayout(
                            parent, (AppBarLayout) dependency, child)) {
                        break;
                    }
                } else if (isBottomSheet(dependency)) {
                    if (updateFabVisibilityForBottomSheet(dependency, child)) {
                        break;
                    }
                }
            }
            // Now let the CoordinatorLayout lay out the VIEW
            parent.onLayoutChild(child, layoutDirection);
            return true;
        }

        @VisibleForTesting
        void setInternalAutoHideListener(@Nullable FloatingActionButton.OnVisibilityChangedListener listener) {
            internalAutoHideListener = listener;
        }

        protected void show(View child) {
            if (child instanceof FloatingActionButton) {
                ((FloatingActionButton) child).show(internalAutoHideListener);
            } else if (child instanceof ExpandedFloatingButton) {
                ((ExpandedFloatingButton) child).show(internalAutoHideListener);
            } else {
                child.setVisibility(View.VISIBLE);
            }
        }

        protected void hide(View child) {
            if (child instanceof FloatingActionButton) {
                ((FloatingActionButton) child).hide(internalAutoHideListener);
            } else if (child instanceof ExpandedFloatingButton) {
                ((ExpandedFloatingButton) child).hide(internalAutoHideListener);
            } else {
                child.setVisibility(View.INVISIBLE);
            }
        }

        private boolean shouldUpdateVisibility(View dependency, View child) {
            final CoordinatorLayout.LayoutParams lp =
                    (CoordinatorLayout.LayoutParams) child.getLayoutParams();
            if (!autoHideEnabled) {
                return false;
            }

            if (lp.getAnchorId() != dependency.getId()) {
                // The anchor ID doesn't match the dependency, so we won't automatically
                // show/hide the VIEW
                return false;
            }

            //noinspection RedundantIfStatement
            if (child.getVisibility() != VISIBLE) {
                // The view isn't set to be visible so skip changing its visibility
                return false;
            }

            return true;
        }

        private boolean updateFabVisibilityForAppBarLayout(CoordinatorLayout parent,
                                                           AppBarLayout appBarLayout, View child) {
            if (!shouldUpdateVisibility(appBarLayout, child)) {
                return false;
            }

            if (tmpRect == null) {
                tmpRect = new Rect();
            }

            // First, let's get the visible rect of the dependency
            final Rect rect = tmpRect;
            ViewGroupUtils.getDescendantRect(parent, appBarLayout, rect);

            if (rect.bottom <= getMinimumHeightForVisibleOverlappingContent(appBarLayout)) {
                // If the anchor's bottom is below the seam, we'll animate our VIEW out
                //            child.hide(internalAutoHideListener);
                child.setVisibility(View.GONE);
            } else {
                // Else, we'll animate our VIEW back in
                //            child.show(internalAutoHideListener);
                child.setVisibility(View.VISIBLE);
            }
            return true;
        }

        private int getMinimumHeightForVisibleOverlappingContent(AppBarLayout appBarLayout) {
            int minHeight = ViewCompat.getMinimumHeight(appBarLayout);
            if (minHeight != 0) {
                return minHeight * 2;
            } else {
                int childCount = appBarLayout.getChildCount();
                return childCount >= 1 ? ViewCompat.getMinimumHeight(appBarLayout.getChildAt(childCount - 1)) * 2 : 0;
            }
        }

        private boolean updateFabVisibilityForBottomSheet(View bottomSheet,
                                                          View child) {
            if (!shouldUpdateVisibility(bottomSheet, child)) {
                return false;
            }
            CoordinatorLayout.LayoutParams lp =
                    (CoordinatorLayout.LayoutParams) child.getLayoutParams();
            if (bottomSheet.getTop() < child.getHeight() / 2 + lp.topMargin) {
                hide(child);
            } else {
                show(child);
            }
            return true;
        }
    }


    @SuppressWarnings({"unused", "WeakerAccess"})
    public static class ScrollingViewSnackbarBehavior extends SnackbarBehavior {
        private boolean wasShownAlready = false;

        public ScrollingViewSnackbarBehavior() {
        }

        public ScrollingViewSnackbarBehavior(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        @Override
        public boolean onStartNestedScroll(@NonNull CoordinatorLayout coordinatorLayout, @NonNull View child, @NonNull
                View directTargetChild, @NonNull View target, int axes, int type) {
            return true;
        }

        @Override
        public boolean layoutDependsOn(CoordinatorLayout parent, View child, View dependency) {
            if (!wasShownAlready
                    && dependency instanceof RecyclerView
                    && (
                    ((RecyclerView) dependency).getAdapter() == null
                            || ((RecyclerView) dependency).getAdapter().getItemCount() == 0)) {
                show(child);
                wasShownAlready = true;
            }
            return dependency instanceof RecyclerView || super.layoutDependsOn(parent, child, dependency);
        }

        @Override
        public void onNestedScroll(@NonNull CoordinatorLayout coordinatorLayout, @NonNull View child, @NonNull View
                target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int type) {
            super.onNestedScroll(coordinatorLayout, child, target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed,
                    type);
            wasShownAlready = false;
            if (dyConsumed > 0 && child.getVisibility() == View.VISIBLE) {
                hide(child);
            } else if (dyConsumed < 0) {
                show(child);
            }
        }
    }

    public static class NoBehavior extends CoordinatorLayout.Behavior<View> {
        public NoBehavior() {
        }

        public NoBehavior(Context context, AttributeSet attrs) {
            super(context, attrs);
        }
    }
}
