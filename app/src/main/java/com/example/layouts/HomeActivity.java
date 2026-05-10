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

import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity {

    private LinearLayout photoContainer;
    private TextView textPhotoCount;
    private final List<LinearLayout> photoRows = new ArrayList<>();

    private static final String[] NOMBRES = {
            "IMG_20240315_142233.jpg",
            "IMG_20240316_091045.jpg",
            "IMG_20240317_180532.jpg",
            "PHOTO_20240318_203018.jpg",
            "Screenshot_2024-03-19.jpg",
            "IMG_20240320_164520.jpg",
            "IMG_20240321_095811.jpg",
            "PHOTO_20240322_110045.jpg"
    };

    private static final String[] TAMANOS = {
            "3.2 MB", "2.8 MB", "4.1 MB", "1.9 MB",
            "0.8 MB", "3.7 MB", "2.4 MB", "5.1 MB"
    };

    // Tonos de azul para las miniaturas simuladas
    private static final int[] COLORES_THUMB = {
            0xFF023e8a, 0xFF0077b6, 0xFF0096c7,
            0xFF023e8a, 0xFF48cae4, 0xFF0077b6,
            0xFF00b4d8, 0xFF023e8a
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        photoContainer = findViewById(R.id.photoContainer);
        textPhotoCount = findViewById(R.id.textPhotoCount);
        Button buttonDelete    = findViewById(R.id.buttonDelete);
        Button buttonSelectAll = findViewById(R.id.buttonSelectAll);

        buildPhotoList();
        updateCount();

        buttonSelectAll.setOnClickListener(v -> toggleSelectAll());
        buttonDelete.setOnClickListener(v -> confirmarEliminacion());
    }

    private void buildPhotoList() {
        for (int i = 0; i < NOMBRES.length; i++) {
            photoContainer.addView(createRow(i));

            // Divisor
            View div = new View(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1);
            div.setLayoutParams(lp);
            div.setBackgroundColor(0xFFCFD8DC);
            photoContainer.addView(div);
        }
    }

    private LinearLayout createRow(int i) {
        int dp = Math.round(getResources().getDisplayMetrics().density);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(16 * dp, 14 * dp, 16 * dp, 14 * dp);
        row.setBackgroundColor(i % 2 == 0 ? 0xFFFFFFFF : 0xFFE8F0FE);

        // Miniatura (bloque de color simulando foto)
        View thumb = new View(this);
        LinearLayout.LayoutParams thumbLp = new LinearLayout.LayoutParams(56 * dp, 56 * dp);
        thumbLp.setMarginEnd(14 * dp);
        thumb.setLayoutParams(thumbLp);
        thumb.setBackgroundColor(COLORES_THUMB[i]);

        // Info (nombre + tamaño)
        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvNombre = new TextView(this);
        tvNombre.setText(NOMBRES[i]);
        tvNombre.setTextSize(14);
        tvNombre.setTextColor(0xFF011f4b);
        tvNombre.setTypeface(null, Typeface.BOLD);
        tvNombre.setMaxLines(1);

        TextView tvTamano = new TextView(this);
        tvTamano.setText(TAMANOS[i]);
        tvTamano.setTextSize(12);
        tvTamano.setTextColor(0xFF37474F);
        tvTamano.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        info.addView(tvNombre);
        info.addView(tvTamano);

        // CheckBox
        CheckBox checkBox = new CheckBox(this);
        checkBox.setButtonTintList(ColorStateList.valueOf(0xFF023e8a));

        row.addView(thumb);
        row.addView(info);
        row.addView(checkBox);

        // Tocar la fila también marca el checkbox
        row.setOnClickListener(v -> checkBox.setChecked(!checkBox.isChecked()));

        photoRows.add(row);
        return row;
    }

    private void toggleSelectAll() {
        boolean haySinSeleccionar = false;
        for (LinearLayout row : photoRows) {
            CheckBox cb = (CheckBox) row.getChildAt(2);
            if (!cb.isChecked()) { haySinSeleccionar = true; break; }
        }
        for (LinearLayout row : photoRows) {
            ((CheckBox) row.getChildAt(2)).setChecked(haySinSeleccionar);
        }
    }

    private void confirmarEliminacion() {
        List<LinearLayout> seleccionadas = getSeleccionadas();
        if (seleccionadas.isEmpty()) {
            Toast.makeText(this, "Selecciona al menos una foto", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Eliminar fotos")
                .setMessage("¿Eliminar " + seleccionadas.size() + " foto(s) seleccionada(s)?")
                .setPositiveButton("Eliminar", (d, w) -> eliminarFotos(seleccionadas))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void eliminarFotos(List<LinearLayout> seleccionadas) {
        for (LinearLayout row : seleccionadas) {
            photoContainer.removeView(row);
            photoRows.remove(row);
        }
        Toast.makeText(this,
                seleccionadas.size() + " foto(s) eliminada(s)",
                Toast.LENGTH_SHORT).show();
        updateCount();
    }

    private List<LinearLayout> getSeleccionadas() {
        List<LinearLayout> result = new ArrayList<>();
        for (LinearLayout row : photoRows) {
            if (((CheckBox) row.getChildAt(2)).isChecked()) result.add(row);
        }
        return result;
    }

    private void updateCount() {
        textPhotoCount.setText(photoRows.size() + " foto(s) en galería");
    }
}
