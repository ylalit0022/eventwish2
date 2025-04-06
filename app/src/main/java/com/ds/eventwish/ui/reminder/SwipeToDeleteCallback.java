package com.ds.eventwish.ui.reminder;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import com.ds.eventwish.R;

public abstract class SwipeToDeleteCallback extends ItemTouchHelper.SimpleCallback {
    private final Paint paint;
    private final ColorDrawable deleteBackground;
    private final ColorDrawable completeBackground;
    private final Drawable deleteIcon;
    private final Drawable completeIcon;
    private final int iconMargin;

    public SwipeToDeleteCallback(Context context) {
        super(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
        
        paint = new Paint();
        deleteBackground = new ColorDrawable(Color.parseColor("#FF0000"));
        completeBackground = new ColorDrawable(Color.parseColor("#4CAF50"));
        
        deleteIcon = ContextCompat.getDrawable(context, R.drawable.ic_delete);
        completeIcon = ContextCompat.getDrawable(context, R.drawable.ic_check);
        iconMargin = context.getResources().getDimensionPixelSize(R.dimen.swipe_icon_margin);
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, 
                         @NonNull RecyclerView.ViewHolder viewHolder,
                         @NonNull RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onChildDraw(@NonNull Canvas canvas, 
                           @NonNull RecyclerView recyclerView,
                           @NonNull RecyclerView.ViewHolder viewHolder,
                           float dX, float dY, 
                           int actionState, 
                           boolean isCurrentlyActive) {
        
        if (actionState != ItemTouchHelper.ACTION_STATE_SWIPE) {
            return;
        }

        View itemView = viewHolder.itemView;
        int itemHeight = itemView.getBottom() - itemView.getTop();

        // Draw background
        if (dX > 0) { // Swiping right (complete)
            completeBackground.setBounds(itemView.getLeft(), 
                                      itemView.getTop(),
                                      itemView.getLeft() + ((int) dX),
                                      itemView.getBottom());
            completeBackground.draw(canvas);

            // Draw complete icon
            if (completeIcon != null) {
                int iconTop = itemView.getTop() + (itemHeight - completeIcon.getIntrinsicHeight()) / 2;
                int iconLeft = itemView.getLeft() + iconMargin;
                int iconRight = itemView.getLeft() + iconMargin + completeIcon.getIntrinsicWidth();
                int iconBottom = iconTop + completeIcon.getIntrinsicHeight();
                completeIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                completeIcon.draw(canvas);
            }
        } else if (dX < 0) { // Swiping left (delete)
            deleteBackground.setBounds(itemView.getRight() + ((int) dX),
                                    itemView.getTop(),
                                    itemView.getRight(),
                                    itemView.getBottom());
            deleteBackground.draw(canvas);

            // Draw delete icon
            if (deleteIcon != null) {
                int iconTop = itemView.getTop() + (itemHeight - deleteIcon.getIntrinsicHeight()) / 2;
                int iconRight = itemView.getRight() - iconMargin;
                int iconLeft = itemView.getRight() - iconMargin - deleteIcon.getIntrinsicWidth();
                int iconBottom = iconTop + deleteIcon.getIntrinsicHeight();
                deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                deleteIcon.draw(canvas);
            }
        }

        super.onChildDraw(canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }
}
