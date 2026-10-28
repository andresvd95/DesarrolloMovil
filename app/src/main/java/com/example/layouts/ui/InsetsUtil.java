package com.example.layouts.ui;

import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Con targetSdk 35+ Android fuerza edge-to-edge: el contenido dibuja detras
 * de la barra de estado y de navegacion salvo que la app aplique los
 * insets manualmente. Estos helpers suman el inset del sistema al padding
 * ya definido en el layout (no lo reemplazan), para que la UI no quede
 * tapada por la barra de estado (reloj/batería) ni por la de navegacion.
 */
public final class InsetsUtil {

    private InsetsUtil() {
    }

    public static void applyTopInset(View view) {
        int left = view.getPaddingLeft();
        int top = view.getPaddingTop();
        int right = view.getPaddingRight();
        int bottom = view.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(left, top + bars.top, right, bottom);
            return insets;
        });
    }

    public static void applyBottomInset(View view) {
        int left = view.getPaddingLeft();
        int top = view.getPaddingTop();
        int right = view.getPaddingRight();
        int bottom = view.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(left, top, right, bottom + bars.bottom);
            return insets;
        });
    }
}
