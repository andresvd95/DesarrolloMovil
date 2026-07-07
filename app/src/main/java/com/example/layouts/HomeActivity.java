package com.example.layouts;

import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class HomeActivity extends AppCompatActivity {

    private LinearLayout photoContainer;
    private TextView textPhotoCount;
    private PhotoGalleryManager galleryManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        photoContainer = findViewById(R.id.photoContainer);
        textPhotoCount = findViewById(R.id.textPhotoCount);
        Button buttonDelete = findViewById(R.id.buttonDelete);
        Button buttonSelectAll = findViewById(R.id.buttonSelectAll);

        galleryManager = PhotoGalleryManager.createDefault();
        renderPhotoList();

        buttonSelectAll.setOnClickListener(v -> toggleSelectAll());
        buttonDelete.setOnClickListener(v -> confirmarEliminacion());
    }

    private void renderPhotoList() {
        photoContainer.removeAllViews();
        for (int i = 0; i < galleryManager.getPhotos().size(); i++) {
            photoContainer.addView(createRow(i, galleryManager.getPhotos().get(i)));
            if (i < galleryManager.getPhotos().size() - 1) {
                photoContainer.addView(createDivider());
            }
        }
        updateCount();
    }

    private View createDivider() {
        View divider = new View(this);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1);
        divider.setLayoutParams(layoutParams);
        divider.setBackgroundColor(0xFFCFD8DC);
        return divider;
    }

    private LinearLayout createRow(int index, PhotoItem photoItem) {
        int dp = Math.round(getResources().getDisplayMetrics().density);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(16 * dp, 14 * dp, 16 * dp, 14 * dp);
        row.setBackgroundColor(index % 2 == 0 ? 0xFFFFFFFF : 0xFFE8F0FE);

        View thumb = new View(this);
        LinearLayout.LayoutParams thumbLayoutParams = new LinearLayout.LayoutParams(56 * dp, 56 * dp);
        thumbLayoutParams.setMarginEnd(14 * dp);
        thumb.setLayoutParams(thumbLayoutParams);
        thumb.setBackgroundColor(photoItem.getThumbnailColor());

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
        ));

        TextView nameTextView = new TextView(this);
        nameTextView.setText(photoItem.getName());
        nameTextView.setTextSize(14);
        nameTextView.setTextColor(0xFF011F4B);
        nameTextView.setTypeface(null, Typeface.BOLD);
        nameTextView.setMaxLines(1);

        TextView sizeTextView = new TextView(this);
        sizeTextView.setText(photoItem.getSize());
        sizeTextView.setTextSize(12);
        sizeTextView.setTextColor(0xFF37474F);

        info.addView(nameTextView);
        info.addView(sizeTextView);

        CheckBox checkBox = new CheckBox(this);
        checkBox.setButtonTintList(ColorStateList.valueOf(0xFF023E8A));
        checkBox.setChecked(photoItem.isSelected());
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> photoItem.setSelected(isChecked));

        row.addView(thumb);
        row.addView(info);
        row.addView(checkBox);
        row.setOnClickListener(v -> checkBox.setChecked(!checkBox.isChecked()));

        return row;
    }

    private void toggleSelectAll() {
        galleryManager.setAllSelected(galleryManager.hasUnselectedPhotos());
        renderPhotoList();
    }

    private void confirmarEliminacion() {
        int selectedCount = galleryManager.getSelectedCount();
        if (selectedCount == 0) {
            Toast.makeText(this, "Selecciona al menos una foto", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Eliminar fotos")
                .setMessage("¿Eliminar " + selectedCount + " foto(s) seleccionada(s)?")
                .setPositiveButton("Eliminar", (dialog, which) -> eliminarFotos())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void eliminarFotos() {
        int deletedCount = galleryManager.deleteSelected();
        renderPhotoList();
        Toast.makeText(this, deletedCount + " foto(s) eliminada(s)", Toast.LENGTH_SHORT).show();
    }

    private void updateCount() {
        textPhotoCount.setText(galleryManager.getPhotos().size() + " foto(s) en galería");
    }
}
